#!/bin/sh


# ------------------------------------------------------
# change this appropriately !!!!!!!!!!!!!!!!
# ------------------------------------------------------
cassandra_home=/home/jinsu/research/cass-prj/trunk/stable_cass
#cassandra_home=`dirname $0`/..
#dirname $0 is . which is /home/jinsu/research/stable_cass/bin

FM_MY_JARS=/home/jinsu/research/java-rtjar
# ------------------------------------------------------



# adding:
# ------------------------
# 1) cassandra classes
# ------------------------
cassandra_classes="$cassandra_home/build/classes"
CLASSPATH=${CLASSPATH}:$cassandra_classes
# ------------------------
# 2) conf folder
# ------------------------
# nothing here
# ------------------------
# 3) lib jars
# ------------------------
for jar in $cassandra_home/lib/*.jar; do
    CLASSPATH=$CLASSPATH:$jar
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



# --------------------------------------------------
# Now set JVM_OPTS

# ignored for fi:
#        -Xrunjdwp:transport=dt_socket,server=y,address=127.0.0.2:8889,suspend=n \
#        -Dcom.sun.management.jmxremote.port=8082 \
#       -Dcom.sun.management.jmxremote.ssl=false \
#        -Dcom.sun.management.jmxremote.authenticate=false \
# --------------------------------------------------
JVM_OPTS=" \
        -ea \
        -Xms128M \
        -Xmx1G \
        -XX:TargetSurvivorRatio=90 \
        -XX:+AggressiveOpts \
        -XX:+UseParNewGC \
        -XX:+UseConcMarkSweepGC \
        -XX:+CMSParallelRemarkEnabled \
        -XX:+HeapDumpOnOutOfMemoryError \
        -XX:SurvivorRatio=128 \
        -XX:MaxTenuringThreshold=0 \
        -Xdebug \
         $FM_BOOT_OPTS"



