package org.apache.cassandra.db.marshal;
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


import java.util.UUID;
import org.apache.cassandra.utils.FBUtilities;

public class TimeUUIDType extends AbstractType
{
    public int compare(byte[] o1, byte[] o2)
    {
        if (o1.length == 0)
        {
            return o2.length == 0 ? 0 : -1;
        }
        if (o2.length == 0)
        {
            return 1;
        }
        int res = compareTimestampBytes(o1, o2);
        if (res != 0)
            return res;
        return FBUtilities.compareByteArrays(o1, o2);
    }

    private static int compareTimestampBytes(byte[] o1, byte[] o2)
    {
        int d = (o1[6] & 0xF) - (o2[6] & 0xF);
        if (d != 0) return d;
        d = (o1[7] & 0xFF) - (o2[7] & 0xFF);
        if (d != 0) return d;
        d = (o1[4] & 0xFF) - (o2[4] & 0xFF);
        if (d != 0) return d;
        d = (o1[5] & 0xFF) - (o2[5] & 0xFF);
        if (d != 0) return d;
        d = (o1[0] & 0xFF) - (o2[0] & 0xFF);
        if (d != 0) return d;
        d = (o1[1] & 0xFF) - (o2[1] & 0xFF);
        if (d != 0) return d;
        d = (o1[2] & 0xFF) - (o2[2] & 0xFF);
        if (d != 0) return d;
        return (o1[3] & 0xFF) - (o2[3] & 0xFF);
    }

    public String getString(byte[] bytes)
    {
        if (bytes.length == 0)
        {
            return "";
        }
        if (bytes.length != 16)
        {
            throw new MarshalException("UUIDs must be exactly 16 bytes");
        }
        UUID uuid = LexicalUUIDType.getUUID(bytes);
        if (uuid.version() != 1)
        {
            throw new MarshalException("TimeUUID only makes sense with version 1 UUIDs");
        }
        return uuid.toString();
    }
}
