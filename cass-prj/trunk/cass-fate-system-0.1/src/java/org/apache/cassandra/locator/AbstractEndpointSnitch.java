package org.apache.cassandra.locator;
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


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * An endpoint snitch tells Cassandra information about network topology that it can use to route
 * requests more efficiently (with "sortByProximity").  Of the abstract methods, isOnSameRack
 * and isInSameDataCenter are always required; getLocation is only used by DatacenterShardStrategy.
 */
public abstract class AbstractEndpointSnitch implements IEndPointSnitch
{
    /**
     * Determines if 2 nodes are in the same rack in the data center.
     * @param host a specified endpoint
     * @param host2 another specified endpoint
     * @return true if on the same rack false otherwise
     * @throws UnknownHostException
     */
    abstract public boolean isOnSameRack(InetAddress host, InetAddress host2) throws UnknownHostException;

    /**
     * Determines if 2 nodes are in the same data center.
     * @param host a specified endpoint
     * @param host2 another specified endpoint
     * @return true if in the same data center false otherwise
     * @throws UnknownHostException
     */
    abstract public boolean isInSameDataCenter(InetAddress host, InetAddress host2) throws UnknownHostException;

    /**
     * Determines the name of the datacenter this endpoint lives in.
     * @param endpoint
     * @return the name of the datacenter the endpoint lives in
     */
    abstract public String getLocation(InetAddress endpoint) throws UnknownHostException;

    public List<InetAddress> getSortedListByProximity(final InetAddress address, Collection<InetAddress> unsortedAddress)
    {
        List<InetAddress> preferred = new ArrayList<InetAddress>(unsortedAddress);
        sortByProximity(address, preferred);
        return preferred;
    }

    public List<InetAddress> sortByProximity(final InetAddress address, List<InetAddress> addresses)
    {
        Collections.sort(addresses, new Comparator<InetAddress>()
        {
            public int compare(InetAddress a1, InetAddress a2)
            {
                return compareEndpoints(address, a1, a2);
            }
        });
        return addresses;
    }

    public int compareEndpoints(InetAddress target, InetAddress a1, InetAddress a2)
    {
        try
        {
            if (target.equals(a1) && !target.equals(a2))
                return -1;
            if (target.equals(a2) && !target.equals(a1))
                return 1;
            if (isOnSameRack(target, a1) && !isOnSameRack(target, a2))
                return -1;
            if (isOnSameRack(target, a2) && !isOnSameRack(target, a1))
                return 1;
            if (isInSameDataCenter(target, a1) && !isInSameDataCenter(target, a2))
                return -1;
            if (isInSameDataCenter(target, a2) && !isInSameDataCenter(target, a1))
                return 1;
            return 0;
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException(e);
        }
    }
}
