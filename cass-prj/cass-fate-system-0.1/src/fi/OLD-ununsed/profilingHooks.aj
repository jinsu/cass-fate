
package org.fi;


import org.fi.*;
import org.fi.FMServer.FailType;

import java.io.*;
// import java.util.*;
import java.net.InetSocketAddress;
import java.lang.Thread;
import java.lang.StackTraceElement;


import org.aspectj.lang.Signature; // include this for Signature, etc!
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.SourceLocation;

// hdfs specifics (Bad)
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;

import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.DFSClient.LeaseChecker;
import org.apache.hadoop.hdfs.DFSClient.DNAddrPair;
import org.apache.hadoop.hdfs.DFSClient.BlockReader;
import org.apache.hadoop.hdfs.DFSClient.DFSInputStream;
import org.apache.hadoop.hdfs.DFSClient.DFSDataInputStream;
import org.apache.hadoop.hdfs.DFSClient.DFSOutputStream;
import org.apache.hadoop.hdfs.DFSClient.DFSOutputStream.Packet;
import org.apache.hadoop.hdfs.DFSClient.DFSOutputStream.DataStreamer;
import org.apache.hadoop.hdfs.DFSClient.DFSOutputStream.ResponseProcessor;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.datanode.DataNode;


// Files that client use
//fs.FSDataOutputStream
//fs.FSDataOutputStream$PositionCache
//fs.FSOutputSummer
//fs.FileSystem
//fs.FileSystem$Cache
//fs.FileUtil
//fs.FsShell
//io.IOUtils


// *********************************************************************
// Profiling
// *********************************************************************
public aspect profilingHooks {



  // #####################################################################
  // #####              HDFS SPECIFIC  ADVICES                        ####
  // #####################################################################

  // ************************************ HDFS specific pointcuts

  pointcut specificClientNameNode()
    : ((within(org.apache.hadoop.hdfs..*) ||
        within(org.apache.hadoop.io..*)   ||
        within(org.apache.hadoop.fs..*))
       &&
       !within(org.apache.hadoop.hdfs.server..*)
       &&
       call (* NameNode.*(..)));

  pointcut specificClientExecution()
    : ((within(org.apache.hadoop.hdfs..*) ||
        within(org.apache.hadoop.io..*)   ||
        within(org.apache.hadoop.fs..*))
       &&
       !within(org.apache.hadoop.hdfs.server..*)
       &&
       execution (* *.*(..)));


  /*
  // this will give lots of traces ... careful
  before() : specificClientNameNode() || specificClientExecution() {

  Thread t = Thread.currentThread();
  FMStackTrace fst = new FMStackTrace(t.getStackTrace());
  FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
  System.out.format("# ####################################### ");
  System.out.format("[%s][%s] \n",
  Util.getClassName(thisJoinPoint),
  Util.getMethodName(thisJoinPoint));
  System.out.format("%s\n", fjp);
  System.out.format("%s\n\n", fst);
  }
  */




  // #####################################################################
  // #####                     P O I N T C U T S                      ####
  // #####################################################################


  // *********************************** General pointcuts

  pointcut targetCalls(ClassWC c)
    : (target(c) &&
       !within(org.fi.*))
    &&
    ( call (* InputStream+.*(..))      ||
      call (* File+.*(..))             ||
      call (* OutputStream+.*(..))     ||
      call (* RandomAccessFile+.*(..)) ||
      call (* FileChannel+.*(..))      ||
      call (* Reader+.*(..))           ||
      call (* Writer+.*(..))
      );


  pointcut argCalls(ClassWC c)
    : (args(c) &&
       !within(org.fi.*))
    &&
    ( call (* ByteArrayOutputStream+.*(..,ClassWC+,..))  );




  // #####################################################################
  // #####                      A D V I C E S                         ####
  // #####################################################################

  /*
  // ************************************************ call ClassWC.**(..)
  Object around (ClassWC c) : targetCalls(c) {
  insertProfileHook(thisJoinPoint, c);
  return proceed(c);
  }

  // ************************************************ call BAOS.**(ClassWC)
  Object around (ClassWC c) : argCalls(c) {
  insertProfileHook(thisJoinPoint, c);
  return proceed(c);
  }

  // ****************************************************** new FIS
  // find instantiations where contexts should be passed
  // should this be FileInputStream+?? How about if
  // the client instantiate the FileInputStream with a wrapper
  // such as in TrackingFileInputStream (we currently don't intercept this)
  Object around(File f) : call (FileInputStream.new(File)) && args(f) {
  insertProfileHook(thisJoinPoint, (ClassWC)f);
  return proceed(f);
  }

  // ****************************************************** new FOS
  // cannot catch FOS, don't know why
  Object around(File f) : call (FileOutputStream.new(File)) && args(f) {
  insertProfileHook(thisJoinPoint, (ClassWC)f);
  return proceed(f);
  }
  Object around(FileDescriptor fd)
  : call (FileOutputStream.new(FileDescriptor)) && args(fd) {
  Object fos = proceed(fd);
  insertProfileHook(thisJoinPoint, (ClassWC)fos);
  return fos;
  }


  // ****************************************************** new RAF
  Object around(File f, String s)
  : call (RandomAccessFile.new(File,String)) && args(f,s) {
  insertProfileHook(thisJoinPoint, (ClassWC)f);
  return proceed(f,s);
  }

  // ****************************************************** general context passing
  // pass the context (this is a generic advice)
  Object around() : call (ClassWC+.new(..,ClassWC+,..)) {
  Object[] args = thisJoinPoint.getArgs();
  for (int i = 0; i < args.length; i++) {
  if (args[i] instanceof ClassWC) {
  ClassWC a = (ClassWC)(args[i]);
  insertProfileHook(thisJoinPoint, a);
  break;
  }
  }
  return proceed();
  }
  */


}

