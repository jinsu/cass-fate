#!/bin/sh


if [ "x$NODE_ID" = "x" ]; then

    echo ""
    echo "  FATAL ERROR: NODE_ID has not been specified !!! "
    echo ""    
    exit

fi

nodeId=$NODE_ID

cassandra_home=$(awk '/Cassandra run homepath/ {save=$NF} END {print save}' /tmp/cassandra-fi-build-info)
#cassandra_home=/home/jinsu/research/cass-prj/trunk/stable_cass

#FM_MY_JARS=/home/jinsu/research/java-rtjar


# adding:
# ------------------------
# 1) cassandra classes
# ------------------------
#cassandra_classes="$cassandra_home/build/classes"
#CLASSPATH=${CLASSPATH}:$cassandra_classes
# ------------------------
# 2) conf folder
# ------------------------
#CASSANDRA_CONF="$cassandra_home/conf/conf$nodeId"
#CLASSPATH=${CLASSPATH}:$CASSANDRA_CONF
# ------------------------
# 3) lib jars
# ------------------------
#for jar in $cassandra_home/lib/*.jar; do
#    CLASSPATH=$CLASSPATH:$jar
#done
# ------------------------
# 4) aspect stuff#
# ------------------------
#FM_ASPECTJ1="$FM_MY_JARS/aspectj/aspectjrt-1.6.4.jar"
#FM_ASPECTJ2="$FM_MY_JARS/aspectj/aspectjtools-1.6.4.jar"
#CLASSPATH=${CLASSPATH}:${FM_ASPECTJ1}:${FM_ASPECTJ2}
# ------------------------
# 5) JOL stuff
# ------------------------
#FM_JOL="$FM_MY_JARS/jol/jol.jar"
#CLASSPATH=${CLASSPATH}:${FM_JOL}
# ------------------------
# 6) RPC stuffs
# ------------------------
#FM_RPC1="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/xmlrpc-client-3.1.3.jar"
#FM_RPC2="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/xmlrpc-server-3.1.3.jar"
#FM_RPC3="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/xmlrpc-common-3.1.3.jar"
#FM_RPC4="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/ws-commons-util-1.0.2.jar"
#FM_RPC5="$FM_MY_JARS/apache-xmlrpc-3.1.3/lib/commons-logging-1.1.jar"
#CLASSPATH=${CLASSPATH}:${FM_RPC1}:${FM_RPC2}:${FM_RPC3}:${FM_RPC4}:${FM_RPC5}
# ------------------------
# 7) boot opts
# ------------------------
#FM_WOVENRT="$cassandra_home/build/woven-rt.jar"
#FM_BOOT_OPTS="-Xbootclasspath:$FM_WOVENRT"


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
CASSANDRA_CONF="$cassandra_home/conf/conf$nodeId"
CLASSPATH=${CLASSPATH}:$CASSANDRA_CONF
# ------------------------
# 3) lib jars
# ------------------------
for jar in $cassandra_home/lib/*.jar; do
    CLASSPATH=${CLASSPATH}:$jar
done

#touch /tmp/cassandra-fi-build-info
#echo "Hadoop run classpath --> $CLASSPATH" >> /tmp/cassandra-fi-build-info
# -------------------------------------------------------




# ------------------------------------------------------
# setting up node address and jmx address automatically
# ------------------------------------------------------
#nodeAddr=127.0.0.2:8889
#jmxPort=8082
#jmxPort=`expr 808 + $nodeId`
#nodePort=`expr 8889 + $nodeId`
#node0 starts at 127.0.0.1
#node1 starts at 127.0.0.11

#node0 has 127.0.0.1:8888
#jmxport=8080

nodePort=`expr 9000 + $nodeId`
nodeIpLast=`expr 10 + $nodeId`
nodeIp="127.0.0.$nodeIpLast"
nodeAddr="$nodeIp:$nodePort"
jmxPort=`expr 10000 + $nodeId`


# ----------------------------
echo  ""
echo  "nodeId = $nodeId"
echo  "nodeIp = $nodeIp"
echo  ""

#jinsu testing...
#TESTING= "-XX:+UseCompressedOops \\" # enables compressed references, reducing memory overhead on 64bit JVMs

# --------------------------------------------------
# Now set JVM_OPTS
# --------------------------------------------------
JVM_OPTS=" \
        -ea \
        -Xms512M \
        -Xmx512M \
        -XX:TargetSurvivorRatio=90 \
        -XX:+AggressiveOpts \
        -XX:+UseParNewGC \
        -XX:+UseConcMarkSweepGC \
        -XX:+CMSParallelRemarkEnabled \
        -XX:+HeapDumpOnOutOfMemoryError \
        -XX:SurvivorRatio=128 \
        -XX:MaxTenuringThreshold=0 \
        -Xdebug \
        -Xrunjdwp:transport=dt_socket,server=y,address=$nodeAddr,suspend=n \
        -Dcom.sun.management.jmxremote.port=$jmxPort \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Dcom.sun.management.jmxremote.authenticate=false \
	$FI_BOOT_OPTS"



