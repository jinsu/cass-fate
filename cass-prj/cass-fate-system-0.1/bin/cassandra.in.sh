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

#######################################################################
# please modify cassandra_home to the path of your cassandra directory.
#######################################################################
#cassandra_home=`dirname $0`/..
cassandra_home=/home/jinsu/research/cass-prj/cass-fate-system-0.1

#FM_MY_JARS=/home/jinsu/research/java-rtjar

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
#cassandra_classes="$cassandra_home/build/classes"
#CLASSPATH=${CLASSPATH}:$cassandra_classes
# ------------------------
# 2) conf folder
# ------------------------
#CASSANDRA_CONF="$cassandra_home/conf"
#CLASSPATH=${CLASSPATH}:$CASSANDRA_CONF
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



# #######################################################################
# jinsu, boot class path, add FI_BOOT_OPTS,
# #######################################################################


# -------------------------------------------------------
# please modify these three entries accordingly  (see MAC and Linux configuration)
# -------------------------------------------------------
# This is a possible MAC configuration
# -------------------------------------------------------

#FI_JAVA_CLASSES_DIR=/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Classes/
#FI_JSSE="$FI_JAVA_CLASSES_DIR/jsse.jar"
#FI_JCE="$FI_JAVA_CLASSES_DIR/jce.jar"

# -------------------------------------------------------
# This is a possible Linux configuration
# -------------------------------------------------------
FI_JAVA_CLASSES_DIR=/usr/lib/jvm/java-6-sun/jre/lib
FI_JSSE="$FI_JAVA_CLASSES_DIR/jsse.jar"
FI_JCE="$FI_JAVA_CLASSES_DIR/jce.jar"




# -------------------------------------------------------
# boot classs
# -------------------------------------------------------
FI_LIB_DIR="$cassandra_home/lib/fi"
FI_WOVENRT="$cassandra_home/build/woven-rt.jar"
FI_BOOT_OPTS="-Xbootclasspath:$FI_WOVENRT:$FI_JSSE:$FI_JCE"
# -------------------------------------------------------
# add extra classes stuff
# -------------------------------------------------------
FI_JOL="$FI_LIB_DIR/jol/jol.jar"
#FI_OLG="build/classes/olg.jar"
FI_RPC1="$FI_LIB_DIR/xmlrpc/xmlrpc-client-3.1.3.jar"
FI_RPC2="$FI_LIB_DIR/xmlrpc/xmlrpc-server-3.1.3.jar"
FI_RPC3="$FI_LIB_DIR/xmlrpc/xmlrpc-common-3.1.3.jar"
FI_RPC4="$FI_LIB_DIR/xmlrpc/ws-commons-util-1.0.2.jar"
FI_RPC5="$FI_LIB_DIR/xmlrpc/commons-logging-1.1.jar"
# -------------------------------------------------------
# the final classpath
# -------------------------------------------------------
CLASSPATH=${CLASSPATH}:${FI_JOL} #:${FI_OLG}
CLASSPATH=${CLASSPATH}:${FI_RPC1}:${FI_RPC2}:${FI_RPC3}:${FI_RPC4}:${FI_RPC5}
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

touch /tmp/cassandra-fi-build-info
echo "Cassandra run classpath --> $CLASSPATH" >> /tmp/cassandra-fi-build-info
echo "Cassandra run homepath --> $cassandra_home" >> /tmp/cassandra-fi-build-info
# -------------------------------------------------------



# Arguments to pass to the JVM
JVM_OPTS=" \
        -ea \
        -Xms512M \
        -Xmx512M \
        -XX:+UseParNewGC \
        -XX:+UseConcMarkSweepGC \
        -XX:+CMSParallelRemarkEnabled \
        -XX:SurvivorRatio=8 \
        -XX:MaxTenuringThreshold=1 \
        -XX:+HeapDumpOnOutOfMemoryError \
        -Dcom.sun.management.jmxremote.port=8080 \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Dcom.sun.management.jmxremote.authenticate=false
        $FI_BOOT_OPTS"
