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

package org.apache.cassandra.utils;

import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import org.apache.cassandra.cache.ICacheExpungeHook;

public class ExpiringMap<K, V>
{
    private class CacheableObject
    {
        private V value_;
        public long age;

        CacheableObject(V o)
        {
            value_ = o;
            age = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object o)
        {
            return value_.equals(o);
        }

        @Override
        public int hashCode()
        {
            return value_.hashCode();
        }

        V getValue()
        {
            return value_;
        }

        boolean isReadyToDie(long expiration)
        {
            return ((System.currentTimeMillis() - age) > expiration);
        }
    }

    private class CacheMonitor extends TimerTask
    {
        private long expiration_;

        CacheMonitor(long expiration)
        {
            expiration_ = expiration;
        }

        @Override
        public void run()
        {
            Map<K, V> expungedValues = new HashMap<K, V>();
            synchronized (cache_)
            {
                Enumeration<K> e = cache_.keys();
                while (e.hasMoreElements())
                {
                    K key = e.nextElement();
                    CacheableObject co = cache_.get(key);
                    if (co != null && co.isReadyToDie(expiration_))
                    {
                        V v = co.getValue();
                        if (null != v)
                        {
                            expungedValues.put(key, v);
                        }
                        cache_.remove(key);
                    }
                }
            }

            /* Calling the hooks on the keys that have been expunged */
            for (Entry<K, V> entry : expungedValues.entrySet())
            {
                K key = entry.getKey();
                V value = entry.getValue();
                
                ICacheExpungeHook<K, V> hook = hooks_.remove(key);
                if (hook != null)
                {
                    hook.callMe(key, value);
                }
            }
            expungedValues.clear();
        }
    }

    private Hashtable<K, CacheableObject> cache_;
    private Map<K, ICacheExpungeHook<K, V>> hooks_;
    private Timer timer_;
    private static int counter_ = 0;
    private static final Logger LOGGER = Logger.getLogger(ExpiringMap.class);

    private void init(long expiration)
    {
        if (expiration <= 0)
        {
            throw new IllegalArgumentException("Argument specified must be a positive number");
        }

        cache_ = new Hashtable<K, CacheableObject>();
        hooks_ = new Hashtable<K, ICacheExpungeHook<K, V>>();
        timer_ = new Timer("CACHETABLE-TIMER-" + (++counter_), true);
        timer_.schedule(new CacheMonitor(expiration), expiration, expiration);
    }

    /*
    * Specify the TTL for objects in the cache
    * in milliseconds.
    */
    public ExpiringMap(long expiration)
    {
        init(expiration);
    }

    public void shutdown()
    {
        timer_.cancel();
    }

    public void put(K key, V value)
    {
        cache_.put(key, new CacheableObject(value));
    }

    public void put(K key, V value, ICacheExpungeHook<K, V> hook)
    {
        put(key, value);
        hooks_.put(key, hook);
    }

    public V get(K key)
    {
        V result = null;
        CacheableObject co = cache_.get(key);
        if (co != null)
        {
            result = co.getValue();
        }
        return result;
    }

    public V remove(K key)
    {
        CacheableObject co = cache_.remove(key);
        V result = null;
        if (co != null)
        {
            result = co.getValue();
        }
        return result;
    }

    public long getAge(K key)
    {
        long age = 0;
        CacheableObject co = cache_.get(key);
        if (co != null)
        {
            age = co.age;
        }
        return age;
    }

    public int size()
    {
        return cache_.size();
    }

    public boolean containsKey(K key)
    {
        return cache_.containsKey(key);
    }

    public boolean isEmpty()
    {
        return cache_.isEmpty();
    }

    public Set<K> keySet()
    {
        return cache_.keySet();
    }
}
