package org.apache.cassandra.streaming;
/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.cassandra.io.ICompactSerializer;

class PendingFile
{
    private static ICompactSerializer<PendingFile> serializer_;

    static
    {
        serializer_ = new InitiatedFileSerializer();
    }

    public static ICompactSerializer<PendingFile> serializer()
    {
        return serializer_;
    }

    private String targetFile_;
    private final long expectedBytes_;
    private final String table_;
    private long ptr_;

    public PendingFile(String targetFile, long expectedBytes, String table)
    {
        targetFile_ = targetFile;
        expectedBytes_ = expectedBytes;
        table_ = table;
        ptr_ = 0;
    }

    public void update(long ptr)
    {
        ptr_ = ptr;
    }

    public long getPtr()
    {
        return ptr_;
    }

    public String getTable()
    {
        return table_;
    }

    public String getTargetFile()
    {
        return targetFile_;
    }

    public void setTargetFile(String file)
    {
        targetFile_ = file;
    }

    public long getExpectedBytes()
    {
        return expectedBytes_;
    }

    public boolean equals(Object o)
    {
        if ( !(o instanceof PendingFile) )
            return false;

        PendingFile rhs = (PendingFile)o;
        return targetFile_.hashCode() == rhs.hashCode();
    }

    public int hashCode()
    {
        return toString().hashCode();
    }

    public String toString()
    {
        return targetFile_ + ":" + expectedBytes_;
    }

    private static class InitiatedFileSerializer implements ICompactSerializer<PendingFile>
    {
        public void serialize(PendingFile sc, DataOutputStream dos) throws IOException
        {
            dos.writeUTF(sc.targetFile_);
            dos.writeLong(sc.expectedBytes_);
            dos.writeUTF(sc.table_);
        }

        public PendingFile deserialize(DataInputStream dis) throws IOException
        {
            String targetFile = dis.readUTF();
            long expectedBytes = dis.readLong();
            String table = dis.readUTF();
            return new PendingFile(targetFile, expectedBytes, table);
        }
    }
}
