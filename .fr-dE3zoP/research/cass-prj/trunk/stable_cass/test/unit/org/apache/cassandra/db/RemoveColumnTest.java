/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.cassandra.db;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import static junit.framework.Assert.assertNull;
import org.apache.cassandra.db.filter.IdentityQueryFilter;
import org.apache.cassandra.db.filter.NamesQueryFilter;
import org.apache.cassandra.db.filter.QueryPath;
import org.apache.cassandra.CleanupHelper;

public class RemoveColumnTest extends CleanupHelper
{
    @Test
    public void testRemoveColumn() throws IOException, ExecutionException, InterruptedException
    {
        Table table = Table.open("Keyspace1");
        ColumnFamilyStore store = table.getColumnFamilyStore("Standard1");
        RowMutation rm;

        // add data
        rm = new RowMutation("Keyspace1", "key1");
        rm.add(new QueryPath("Standard1", null, "Column1".getBytes()), "asdf".getBytes(), 0);
        rm.apply();
        store.forceBlockingFlush();

        // remove
        rm = new RowMutation("Keyspace1", "key1");
        rm.delete(new QueryPath("Standard1", null, "Column1".getBytes()), 1);
        rm.apply();

        ColumnFamily retrieved = store.getColumnFamily(new NamesQueryFilter("key1", new QueryPath("Standard1"), "Column1".getBytes()));
        assert retrieved.getColumn("Column1".getBytes()).isMarkedForDelete();
        assertNull(ColumnFamilyStore.removeDeleted(retrieved, Integer.MAX_VALUE));
        assertNull(ColumnFamilyStore.removeDeleted(store.getColumnFamily(new IdentityQueryFilter("key1", new QueryPath("Standard1"))), Integer.MAX_VALUE));
    }
}