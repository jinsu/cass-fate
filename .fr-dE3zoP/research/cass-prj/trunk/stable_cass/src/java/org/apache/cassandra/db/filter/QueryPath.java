package org.apache.cassandra.db.filter;
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


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;

import org.apache.commons.lang.ArrayUtils;

import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ColumnPath;
import org.apache.cassandra.db.ColumnSerializer;

public class QueryPath
{
    public final String columnFamilyName;
    public final byte[] superColumnName;
    public final byte[] columnName;

    public QueryPath(String columnFamilyName, byte[] superColumnName, byte[] columnName)
    {
        this.columnFamilyName = columnFamilyName;
        this.superColumnName = superColumnName;
        this.columnName = columnName;
    }

    public QueryPath(ColumnParent columnParent)
    {
        this(columnParent.column_family, columnParent.super_column, null);
    }

    public QueryPath(String columnFamilyName, byte[] superColumnName)
    {
        this(columnFamilyName, superColumnName, null);
    }

    public QueryPath(String columnFamilyName)
    {
        this(columnFamilyName, null);
    }

    public QueryPath(ColumnPath column_path)
    {
        this(column_path.column_family, column_path.super_column, column_path.column);
    }

    public static QueryPath column(byte[] columnName)
    {
        return new QueryPath(null, null, columnName);
    }

    @Override
    public String toString()
    {
        return "QueryPath(" +
               "columnFamilyName='" + columnFamilyName + '\'' +
               ", superColumnName='" + superColumnName + '\'' +
               ", columnName='" + columnName + '\'' +
               ')';
    }

    public void serialize(DataOutputStream dos) throws IOException
    {
        assert !"".equals(columnFamilyName);
        assert superColumnName == null || superColumnName.length > 0;
        assert columnName == null || columnName.length > 0;
        dos.writeUTF(columnFamilyName == null ? "" : columnFamilyName);
        ColumnSerializer.writeName(superColumnName == null ? ArrayUtils.EMPTY_BYTE_ARRAY : superColumnName, dos);
        ColumnSerializer.writeName(columnName == null ? ArrayUtils.EMPTY_BYTE_ARRAY : columnName, dos);
    }

    public static QueryPath deserialize(DataInputStream din) throws IOException
    {
        String cfName = din.readUTF();
        byte[] scName = ColumnSerializer.readName(din);
        byte[] cName = ColumnSerializer.readName(din);
        return new QueryPath(cfName.isEmpty() ? null : cfName, scName.length == 0 ? null : scName, cName.length == 0 ? null : cName);
    }
}
