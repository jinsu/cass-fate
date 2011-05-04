
 package org.fi;

 import java.io.*;
 import java.nio.channels.*;

 import java.util.Iterator;
 import java.util.Map;
 import java.util.Set;
 import java.util.SortedMap;
 import java.util.TreeMap;



 public class ClientAppendWorkload implements Workload {

   private Driver     driver;
   private Hdfs       hdfs;
   private Utility    u;
   private Experiment exp;

   // *******************************************
   public ClientAppendWorkload(Driver driver, Experiment exp) {
     this.driver = driver;
     this.hdfs = driver.getHdfs();
     this.u = driver.getUtility();
     this.exp = exp;
   }


   // *******************************************
   // the algorithm
   // *******************************************
   public void run() {


     // 1. setup for this specific workload
     preSetup();


     // 3. the exact workload where we want to run with failure
     runWithFailure();


     // 5. run post setup 
     postSetup();
   }


   // *******************************************
   public void runWithFailure() {

       // prepare the new file we want to put to hdfs, and put it
       // if there is exception it's caught inside hdfs and
       // put it in exp
     String dest = String.format("file-%03d", exp.getExpNum());
     hdfs.appendFile(dest, exp);

   }

   // *******************************************
   public void postSetup() {    
     // 4. stop the failure
     Driver.disableFrog();
     Driver.disableFailureManager();
     Driver.disableClientOptimizer();


     String dest = String.format("file-%03d", exp.getExpNum());
     //  hdfs.getFileAfterAppend(dest, exp);
     //     if (exp.isFail()) 
     //     u.print(">> thanh: sleep for 20 seconds after re-append");
     //     u.sleep(20 * 1000);
     //     hdfs.retryAfterAppend(dest, exp);
     //     hdfs.lsViaShell();
     // then delete the file
     hdfs.delete(dest, exp);
    
  }

  // *******************************************
  // we clear VirtualWaitTime
  // and create a new file, to prepare for append
  public void preSetup() {
    // some specific 
    hdfs.clearVirtualWaitTime();
    // this should be done without failure injection
    String dest = String.format("file-%03d", exp.getExpNum());
    hdfs.putFile(dest, exp);

     // 2. enable failures (and optimizer)
     Driver.enableFailureManager();
     Driver.enableClientOptimizer();
     Driver.enableFrog();
     Driver.enableCoverage();
    


  }
  
}