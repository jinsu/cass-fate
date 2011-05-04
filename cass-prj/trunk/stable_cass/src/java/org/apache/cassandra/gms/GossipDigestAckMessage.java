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
import java.util.*;

import org.apache.cassandra.io.ICompactSerializer;
import java.net.InetAddress;



/**
 * This message gets sent out as a result of the receipt of a GossipDigestSynMessage by an
 * endpoint. This is the 2 stage of the 3 way messaging in the Gossip protocol.
 */

class GossipDigestAckMessage
{
    private static ICompactSerializer<GossipDigestAckMessage> serializer_;
    static
    {
        serializer_ = new GossipDigestAckMessageSerializer();
    }
    
    List<GossipDigest> gDigestList_ = new ArrayList<GossipDigest>();
    Map<InetAddress, EndPointState> epStateMap_ = new HashMap<InetAddress, EndPointState>();
    
    static ICompactSerializer<GossipDigestAckMessage> serializer()
    {
        return serializer_;
    }
    
    GossipDigestAckMessage(List<GossipDigest> gDigestList, Map<InetAddress, EndPointState> epStateMap)
    {
        gDigestList_ = gDigestList;
        epStateMap_ = epStateMap;
    }
    
    void addGossipDigest(InetAddress ep, int generation, int version)
    {
        gDigestList_.add( new GossipDigest(ep, generation, version) );
    }
    
    List<GossipDigest> getGossipDigestList()
    {
        return gDigestList_;
    }
    
    Map<InetAddress, EndPointState> getEndPointStateMap()
    {
        return epStateMap_;
    }
}

class GossipDigestAckMessageSerializer implements ICompactSerializer<GossipDigestAckMessage>
{
    public void serialize(GossipDigestAckMessage gDigestAckMessage, DataOutputStream dos) throws IOException
    {
        GossipDigestSerializationHelper.serialize(gDigestAckMessage.gDigestList_, dos);
        dos.writeBoolean(true); // 0.6 compatibility
        EndPointStatesSerializationHelper.serialize(gDigestAckMessage.epStateMap_, dos);
    }

    public GossipDigestAckMessage deserialize(DataInputStream dis) throws IOException
    {
        List<GossipDigest> gDigestList = GossipDigestSerializationHelper.deserialize(dis);
        dis.readBoolean(); // 0.6 compatibility
        Map<InetAddress, EndPointState> epStateMap = EndPointStatesSerializationHelper.deserialize(dis);
        return new GossipDigestAckMessage(gDigestList, epStateMap);
    }
}
