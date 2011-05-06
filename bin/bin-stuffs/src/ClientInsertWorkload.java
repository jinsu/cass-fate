
package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;



public class ClientInsertWorkload implements Workload {
  
  private Driver     driver;
  private Cass       cass;
  private Utility    u;
  private Experiment exp;

  // workload specific
  private String KEYSPACE = "Keyspace1";
  private String COLUMNFAMILY = "Standard1";
  private String COLUMNPATH = "experimentValue";
  private String VALUE = "berkeley";
  private static final String ENCODING = "UTF8";


  // *******************************************
  public ClientInsertWorkload(Driver driver, Experiment exp) {
    this.driver = driver;
    this.cass = driver.getCass();
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
    
    u.createNewFile(Driver.EXPERIMENT_RUN_FLAG);
    
    String key = String.format("key-%03d", exp.getExpNum());
    cass.insertEntry(key, VALUE , exp);
    
    u.deleteFile(Driver.EXPERIMENT_RUN_FLAG);    
  }


  // *******************************************
  public void preSetup() {

    cass.setEncoding(ENCODING);
    cass.setKeyspace(KEYSPACE);
    cass.setColumnFamily(COLUMNFAMILY);
    cass.setColumnPath(COLUMNPATH);

    
    
    // 2. enable failures (and optimizer)
    Driver.enableFailureManager();
    Driver.enableClientOptimizer();
    Driver.enableFrog();
    Driver.enableCoverage();
    
    
    // 3. let's make sure, we setup the connection before we go
    // into the run with failure
    cass.assertConnection();
    
  }

  // *******************************************
  public void postSetup() {    

    // 4. stop the failure
    Driver.disableCoverage();
    Driver.disableFrog();
    Driver.disableFailureManager();
    Driver.disableClientOptimizer();
    
    // get entry
    String key = String.format("key-%03d", exp.getExpNum());
    cass.getEntry(key, exp);
    
    // then delete the file
    //cass.delete(key, exp);
    
  }

  
}