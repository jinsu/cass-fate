
package org.fi;

import jol.core.JolSystem;
import jol.core.Runtime;
import jol.types.basic.BasicTupleSet;
import jol.types.basic.Tuple;
import jol.types.basic.TupleSet;
import jol.types.exception.JolRuntimeException;
import jol.types.exception.UpdateException;
import jol.types.table.TableName;
import jol.types.table.Table.Callback;
import jol.types.table.Table;


import java.io.*;
import org.fi.*;
import org.fi.FMServer.FailType;

import org.aspectj.lang.Signature; // include this for Signature, etc!
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.SourceLocation;



import java.net.InetSocketAddress;
import java.lang.Thread;
import java.lang.StackTraceElement;

import java.lang.management.ManagementFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.permission.PermissionStatus;



// Profiling send to FMServer a list of String
// actually it will go to FMServer first
// the list of String is basically what the event should
// schedule


public class Profiling {
  

  
  public Profiling() { }
  
  
  // *********************************************** profile hook
  public static void insertProfileHook(JoinPoint jp, ClassWC c) {
    
    if (jp.toString().contains("java.io.PrintStream"))
      return;
    
    // print stack
    // I don't care about null context in hadoop
    if (c.getContext() == null &&
        jp.toString().contains("hadoop")) {
      return;
    }
    
    System.out.println("");

    // print the context
    if (c.getContext() == null) {
      System.out.println("  [file][CONTEXT IS NULL / error / warning]");
    }
    else {
      FMContext ctx = new FMContext(c.getContext().getTargetIO());
      System.out.println(ctx);
    }

    // print joinpoint
    FMJoinPoint fjp = new FMJoinPoint(jp);
    System.out.println(fjp);


    Thread t = Thread.currentThread();
    FMStackTrace fst = new FMStackTrace(t.getStackTrace());
    System.out.println(fst);

    System.out.println();

  }


}
