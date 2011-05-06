/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.db;

import java.util.*;
import java.io.IOException;
import java.io.File;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.Future;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.commitlog.CommitLogSegment;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.io.SSTableDeletingReference;
import org.apache.cassandra.io.SSTableReader;
import org.apache.cassandra.io.util.FileUtils;

import java.net.InetAddress;

import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.utils.*;
import org.apache.cassandra.db.filter.*;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

import org.apache.log4j.Logger;

public class Table 
{
    public static final String SYSTEM_TABLE = "system";

    private static final Logger logger = Logger.getLogger(Table.class);
    private static final String SNAPSHOT_SUBDIR_NAME = "snapshots";
    /* we use this lock to drain updaters before calling a flush. */
    static final ReentrantReadWriteLock flusherLock = new ReentrantReadWriteLock(true);

    private static Timer flushTimer = new Timer("FLUSH-TIMER");
    private final boolean waitForCommitLog;

    // This is a result of pushing down the point in time when storage directories get created.  It used to happen in
    // CassandraDaemon, but it is possible to call Table.open without a running daemon, so it made sense to ensure
    // proper directories here.
    static
    {
        try
        {
            DatabaseDescriptor.createAllDirectories();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /*
     * This class represents the metadata of this Table. The metadata
     * is basically the column family name and the ID associated with
     * this column family. We use this ID in the Commit Log header to
     * determine when a log file that has been rolled can be deleted.
    */
    public static class TableMetadata
    {
        private static HashMap<String,TableMetadata> tableMetadataMap = new HashMap<String,TableMetadata>();
        private static Map<Integer, String> idCfMap_ = new HashMap<Integer, String>();

        static
        {
            try
            {
                DatabaseDescriptor.storeMetadata();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        public static synchronized Table.TableMetadata instance(String tableName) throws IOException
        {
            if ( tableMetadataMap.get(tableName) == null )
            {
                tableMetadataMap.put(tableName, new Table.TableMetadata());
            }
            return tableMetadataMap.get(tableName);
        }

        /* The mapping between column family and the column type. */
        private Map<String, String> cfTypeMap_ = new HashMap<String, String>();
        private Map<String, Integer> cfIdMap_ = new HashMap<String, Integer>();

        public void add(String cf, int id)
        {
            add(cf, id, "Standard");
        }
        
        public void add(String cf, int id, String type)
        {
            if (logger.isDebugEnabled())
              logger.debug("adding " + cf + " as " + id);
            assert !idCfMap_.containsKey(id);
            cfIdMap_.put(cf, id);
            idCfMap_.put(id, cf);
            cfTypeMap_.put(cf, type);
        }
        
        public boolean isEmpty()
        {
            return cfIdMap_.isEmpty();
        }

        int getColumnFamilyId(String columnFamily)
        {
            return cfIdMap_.get(columnFamily);
        }

        public static String getColumnFamilyName(int id)
        {
            return idCfMap_.get(id);
        }
        
        String getColumnFamilyType(String cfName)
        {
            return cfTypeMap_.get(cfName);
        }

        Set<String> getColumnFamilies()
        {
            return cfIdMap_.keySet();
        }
        
        int size()
        {
            return cfIdMap_.size();
        }
        
        boolean isValidColumnFamily(String cfName)
        {
            return cfIdMap_.containsKey(cfName);
        }

        public String toString()
        {
            return "TableMetadata(" + FBUtilities.mapToString(cfIdMap_) + ")";
        }

        public static int getColumnFamilyCount()
        {
            return idCfMap_.size();
        }

        public static String getColumnFamilyIDString()
        {
            return FBUtilities.mapToString(tableMetadataMap);
        }
    }

    /** Table objects, one per keyspace.  only one instance should ever exist for any given keyspace. */
    private static final Map<String, Table> instances = new NonBlockingHashMap<String, Table>();

    /* Table name. */
    public final String name;
    /* Handle to the Table Metadata */
    private final Table.TableMetadata tableMetadata;
    /* ColumnFamilyStore per column family */
    private final Map<String, ColumnFamilyStore> columnFamilyStores = new HashMap<String, ColumnFamilyStore>();
    // cache application CFs since Range queries ask for them a _lot_
    private SortedSet<String> applicationColumnFamilies;

    public static Table open(String table) throws IOException
    {
        Table tableInstance = instances.get(table);
        if (tableInstance == null)
        {
            // instantiate the Table.  we could use putIfAbsent but it's important to making sure it is only done once
            // per keyspace, so we synchronize and re-check before doing it.
            synchronized (Table.class)
            {
                tableInstance = instances.get(table);
                if (tableInstance == null)
                {
                    tableInstance = new Table(table);
                    instances.put(table, tableInstance);
                }
            }
        }
        return tableInstance;
    }
        
    public Set<String> getColumnFamilies()
    {
        return tableMetadata.getColumnFamilies();
    }

    public Collection<ColumnFamilyStore> getColumnFamilyStores()
    {
        return Collections.unmodifiableCollection(columnFamilyStores.values());
    }

    public ColumnFamilyStore getColumnFamilyStore(String cfName)
    {
        return columnFamilyStores.get(cfName);
    }

    /**
     * Do a cleanup of keys that do not belong locally.
     */
    public void forceCleanup()
    {
        if (name.equals(SYSTEM_TABLE))
            throw new RuntimeException("Cleanup of the system table is neither necessary nor wise");

        Set<String> columnFamilies = tableMetadata.getColumnFamilies();
        for ( String columnFamily : columnFamilies )
        {
            ColumnFamilyStore cfStore = columnFamilyStores.get( columnFamily );
            if ( cfStore != null )
                cfStore.forceCleanup();
        }   
    }
    
    
    /**
     * Take a snapshot of the entire set of column families with a given timestamp.
     * 
     * @param clientSuppliedName the tag associated with the name of the snapshot.  This
     *                           value can be null.
     */
    public void snapshot(String clientSuppliedName) throws IOException
    {
        String snapshotName = Long.toString(System.currentTimeMillis());
        if (clientSuppliedName != null && !clientSuppliedName.equals(""))
        {
            snapshotName = snapshotName + "-" + clientSuppliedName;
        }

        for (ColumnFamilyStore cfStore : columnFamilyStores.values())
        {
            cfStore.snapshot(snapshotName);
        }
    }


    /**
     * Clear all the snapshots for a given table.
     */
    public void clearSnapshot() throws IOException
    {
        for (String dataDirPath : DatabaseDescriptor.getAllDataFileLocations())
        {
            String snapshotPath = dataDirPath + File.separator + name + File.separator + SNAPSHOT_SUBDIR_NAME;
            File snapshotDir = new File(snapshotPath);
            if (snapshotDir.exists())
            {
                if (logger.isDebugEnabled())
                    logger.debug("Removing snapshot directory " + snapshotPath);
                FileUtils.deleteDir(snapshotDir);
            }
        }
    }

    /*
     * This method is invoked only during a bootstrap process. We basically
     * do a complete compaction since we can figure out based on the ranges
     * whether the files need to be split.
    */
    public List<SSTableReader> forceAntiCompaction(Collection<Range> ranges, InetAddress target)
    {
        List<SSTableReader> allResults = new ArrayList<SSTableReader>();
        Set<String> columnFamilies = tableMetadata.getColumnFamilies();
        for ( String columnFamily : columnFamilies )
        {
            ColumnFamilyStore cfStore = columnFamilyStores.get( columnFamily );
            try
            {
                allResults.addAll(CompactionManager.instance.submitAnticompaction(cfStore, ranges, target).get());
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return allResults;
    }
    
    /*
     * This method is an ADMIN operation to force compaction
     * of all SSTables on disk. 
    */
    public void forceCompaction()
    {
        Set<String> columnFamilies = tableMetadata.getColumnFamilies();
        for ( String columnFamily : columnFamilies )
        {
            ColumnFamilyStore cfStore = columnFamilyStores.get( columnFamily );
            if ( cfStore != null )
                CompactionManager.instance.submitMajor(cfStore);
        }
    }

    List<SSTableReader> getAllSSTablesOnDisk()
    {
        List<SSTableReader> list = new ArrayList<SSTableReader>();
        Set<String> columnFamilies = tableMetadata.getColumnFamilies();
        for ( String columnFamily : columnFamilies )
        {
            ColumnFamilyStore cfStore = columnFamilyStores.get( columnFamily );
            if ( cfStore != null )
                list.addAll(cfStore.getSSTables());
        }
        return list;
    }

    private Table(String table) throws IOException
    {
        name = table;
        waitForCommitLog = DatabaseDescriptor.getCommitLogSync() == DatabaseDescriptor.CommitLogSync.batch;
        tableMetadata = Table.TableMetadata.instance(table);
        for (String columnFamily : tableMetadata.getColumnFamilies())
        {
            columnFamilyStores.put(columnFamily, ColumnFamilyStore.createColumnFamilyStore(table, columnFamily));
        }

        // check 10x as often as the lifetime, so we can exceed lifetime by 10% at most
        int checkMs = DatabaseDescriptor.getMemtableLifetimeMS() / 10;
        flushTimer.schedule(new TimerTask()
        {
            public void run()
            {
                for (ColumnFamilyStore cfs : columnFamilyStores.values())
                {
                    try
                    {
                        cfs.forceFlushIfExpired();
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, checkMs, checkMs);
    }

    public int getColumnFamilyId(String columnFamily)
    {
        return tableMetadata.getColumnFamilyId(columnFamily);
    }

    /**
     * Selects the specified column family for the specified key.
    */
    @Deprecated // single CFs could be larger than memory
    public ColumnFamily get(String key, String cfName) throws IOException
    {
        ColumnFamilyStore cfStore = columnFamilyStores.get(cfName);
        assert cfStore != null : "Column family " + cfName + " has not been defined";
        return cfStore.getColumnFamily(new IdentityQueryFilter(key, new QueryPath(cfName)));
    }

    public Row getRow(QueryFilter filter) throws IOException
    {
        ColumnFamilyStore cfStore = columnFamilyStores.get(filter.getColumnFamilyName());
        ColumnFamily columnFamily = cfStore.getColumnFamily(filter);
        return new Row(filter.key, columnFamily);
    }

    /**
     * This method adds the row to the Commit Log associated with this table.
     * Once this happens the data associated with the individual column families
     * is also written to the column family store's memtable.
    */
    public void apply(RowMutation mutation, Object serializedMutation, boolean writeCommitLog) throws IOException
    {
        HashMap<ColumnFamilyStore,Memtable> memtablesToFlush = new HashMap<ColumnFamilyStore, Memtable>(2);

        // write the mutation to the commitlog and memtables
        flusherLock.readLock().lock();
        try
        {
            if (writeCommitLog)
            {
                CommitLog.instance().add(mutation, serializedMutation);
            }
        
            for (ColumnFamily columnFamily : mutation.getColumnFamilies())
            {
                Memtable memtableToFlush;
                ColumnFamilyStore cfs = columnFamilyStores.get(columnFamily.name());
                if ((memtableToFlush=cfs.apply(mutation.key(), columnFamily)) != null)
                    memtablesToFlush.put(cfs, memtableToFlush);

                ColumnFamily cachedRow = cfs.getRawCachedRow(mutation.key());
                if (cachedRow != null)
                    cachedRow.addAll(columnFamily);
            }
        }
        finally
        {
            flusherLock.readLock().unlock();
        }

        // flush memtables that got filled up.  usually mTF will be empty and this will be a no-op
        for (Map.Entry<ColumnFamilyStore, Memtable> entry : memtablesToFlush.entrySet())
            entry.getKey().maybeSwitchMemtable(entry.getValue(), writeCommitLog);
    }

    public List<Future<?>> flush() throws IOException
    {
        List<Future<?>> futures = new ArrayList<Future<?>>();
        for (String cfName : columnFamilyStores.keySet())
        {
            Future<?> future = columnFamilyStores.get(cfName).forceFlush();
            if (future != null)
                futures.add(future);
        }
        return futures;
    }

    // for binary load path.  skips commitlog.
    void load(RowMutation rowMutation) throws IOException
    {
        String key = rowMutation.key();
                
        for (ColumnFamily columnFamily : rowMutation.getColumnFamilies())
        {
            Collection<IColumn> columns = columnFamily.getSortedColumns();
            for (IColumn column : columns)
            {
                ColumnFamilyStore cfStore = columnFamilyStores.get(new String(column.name(), "UTF-8"));
                cfStore.applyBinary(key, column.value());
            }
        }
    }

    public String getDataFileLocation(long expectedCompactedFileSize)
    {
        String path = DatabaseDescriptor.getDataFileLocationForTable(name, expectedCompactedFileSize);
        if (path == null)
        {
            // retry after GCing to force unmap of compacted SSTables so they can be deleted
            StorageService.instance.requestGC();
            try
            {
                Thread.sleep(SSTableDeletingReference.RETRY_DELAY * 2);
            }
            catch (InterruptedException e)
            {
                throw new AssertionError(e);
            }
            path = DatabaseDescriptor.getDataFileLocationForTable(name, expectedCompactedFileSize);
        }
        return path;
    }

    public static String getSnapshotPath(String dataDirPath, String tableName, String snapshotName)
    {
        return dataDirPath + File.separator + tableName + File.separator + SNAPSHOT_SUBDIR_NAME + File.separator + snapshotName;
    }

    public static Iterable<Table> all()
    {
        Function<String, Table> transformer = new Function<String, Table>()
        {
            public Table apply(String tableName)
            {
                try
                {
                    return Table.open(tableName);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
        return Iterables.transform(DatabaseDescriptor.getTables(), transformer);
    }
}
