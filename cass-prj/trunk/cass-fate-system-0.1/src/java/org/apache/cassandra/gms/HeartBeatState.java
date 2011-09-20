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

package org.apache.cassandra.gms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.cassandra.io.ICompactSerializer;


/**
 * HeartBeat State associated with any given endpoint. 
 */

class HeartBeatState
{
    private static ICompactSerializer<HeartBeatState> serializer_;
    
    static
    {
        serializer_ = new HeartBeatStateSerializer();
    }
    
    int generation_;
    int version_;

    HeartBeatState(int generation)
    {
        this(generation, 0);
    }
    
    HeartBeatState(int generation, int version)
    {
        generation_ = generation;
        version_ = version;
    }

    public static ICompactSerializer<HeartBeatState> serializer()
    {
        return serializer_;
    }
    
    int getGeneration()
    {
        return generation_;
    }

    void updateHeartBeat()
    {
        version_ = VersionGenerator.getNextVersion();
    }
    
    int getHeartBeatVersion()
    {
        return version_;
    }
}

class HeartBeatStateSerializer implements ICompactSerializer<HeartBeatState>
{
    public void serialize(HeartBeatState hbState, DataOutputStream dos) throws IOException
    {
        dos.writeInt(hbState.generation_);
        dos.writeInt(hbState.version_);
    }
    
    public HeartBeatState deserialize(DataInputStream dis) throws IOException
    {
        return new HeartBeatState(dis.readInt(), dis.readInt());
    }
}
