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
package org.apache.cassandra.dht;

import java.util.Arrays;

import org.apache.cassandra.utils.FBUtilities;

public class BytesToken extends Token<byte[]>
{
    public BytesToken(byte... token)
    {
        super(token);
    }
    
    @Override
    public String toString()
    {
        return "Token(bytes[" + FBUtilities.bytesToHex(token) + "])";
    }

    @Override
    public int compareTo(Token<byte[]> o)
    {
        return FBUtilities.compareByteArrays(token, o.token);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        return prime + Arrays.hashCode(token);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof BytesToken))
            return false;
        BytesToken other = (BytesToken) obj;
        return Arrays.equals(token, other.token);
    }

}
