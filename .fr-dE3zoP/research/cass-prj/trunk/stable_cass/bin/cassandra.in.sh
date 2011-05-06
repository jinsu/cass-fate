# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


#cassandra_home=`dirname $0`/..
cassandra_home=/home/jinsu/research/cass-prj/trunk/stable_cass

FM_MY_JARS=/home/jinsu/research/java-rtjar

#jinsu commented out FROM

# The directory where Cassandra's configs live (required)
#CASSANDRA_CONF=$cassandra_home/conf

# This can be the path to a jar file, or a directory containing the 
# compiled classes. NOTE: This isn't needed by the startup script,
# it's just used here in constructing the classpath.
#cassandra_bin=$cassandra_home/build/classes

#cassandra_bin=$cassandra_home/build/cassandra.jar #this was commented out to begin with.

# JAVA_HOME can optionally be set here
#JAVA_HOME=/usr/local/jdk6 #this was commented out to begin with.

# The java classpath (required)
#CLASSPATH=$CASSANDRA_CONF:$cassandra_bin

#for jar in $cassandra_home/lib/*.jar; do
#    CLASSPATH=$CLASSPATH:$jar
#done

#jinsu commented out TO

# adding:
# ------------------------
# 1) cassandra classes
# ------------------------
cassandra_classes="$cassandra_home/build/classes"
CLASSPATH=${CLASSPATH}:$cassandra_classes
# ------------------------
# 2) conf folder
# ------------------------
CASSANDRA_CONF="$cassandra_home/conf"
CLASSPATH=${CLASSPATH}:$CASSANDRA_CONF
# ------------------------
# 3) lib jars
# ------------------------
for jar in $cassandra_home/lib/*.jar; do
    CLASSPATH=${CLASSPATH}:$jar
done
# ------------------------
# 4) aspect stuff
# ------------------------
FM_ASPECTJ1="$FM_MY_JARS/aspectj/aspectjrt-1.6.4.jar"
FM_ASPECTJ2="$FM_MY_JARS/aspectj/aspectjtools-1.6.4.jar"
CLASSPATH=${CLASSPATH}:${FM_ASPECTJ1}:${FM_ASPECTJ2}
# ------------------------
# 5) JOL stuff
# ------------------------
FM_JOL="$FM_MY_JARS/jol/jol.jar"
CLASSPATH=${CLASSPATH}:${FM_JOL}
# ------------------------
# 6) RPC stuffs
# ------------------------
FM_RPC1="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/xmlrpc-client-3.1.3.jar"
FM_RPC2="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/xmlrpc-server-3.1.3.jar"
FM_RPC3="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/xmlrpc-common-3.1.3.jar"
FM_RPC4="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/ws-commons-util-1.0.2.jar"
FM_RPC5="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/commons-logging-1.1.jar"
CLASSPATH=${CLASSPATH}:${FM_RPC1}:${FM_RPC2}:${FM_RPC3}:${FM_RPC4}:${FM_RPC5}
# ------------------------
# 7) boot opts
# ------------------------
FM_WOVENRT="$cassandra_home/build/woven-rt.jar"
FM_BOOT_OPTS="-Xbootclasspath:$FM_WOVENRT"


# Arguments to pass to the JVM
JVM_OPTS=" \
        -ea \
        -Xms512M \
        -Xmx1G \
        -XX:+UseParNewGC \
        -XX:+UseConcMarkSweepGC \
        -XX:+CMSParallelRemarkEnabled \
        -XX:SurvivorRatio=8 \
        -XX:MaxTenuringThreshold=1 \
        -XX:+HeapDumpOnOutOfMemoryError \
        -Dcom.sun.management.jmxremote.port=8080 \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Dcom.sun.management.jmxremote.authenticate=false
        $FM_BOOT_OPTS"
