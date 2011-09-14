
package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;



public class ClientReadRepairWorkload implements Workload {

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
  //private static final String CONSIS = "QUORUM";
  //private static String CONSIS = "ALL";


  // *******************************************
  public ClientReadRepairWorkload(Driver driver, Experiment exp) {
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
    //TODO remove this
    //temporary measure
    u.createNewFile(Driver.EXPERIMENT_RUN_FLAG);

    //create a flag for this repairworkload
    u.createNewFile(Driver.READ_REPAIR_FLAG);

    // get entry which is reading the data.
    String key = String.format("key-%03d", exp.getExpNum());

    //u.println("performing consistency all");
    //cass.getEntry(key, exp, "all");

    //u.println("performing consistency " + CONSIS);
    //cass.getEntry(key, exp, CONSIS);

	//u.println("performing consistency one");
    //cass.getEntry(key, exp, "one");
    cass.getEntry(key, exp);
    u.sleep(3000);

    //delete the repair flag
    u.deleteFile(Driver.READ_REPAIR_FLAG);

    // prepare the new file we want to put to hdfs, and put it
    // if there is exception it's caught inside hdfs and
    // put it in exp

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

    // let's insert a keypair that we are going to test for the
    // read repair functionality.

    String key = String.format("key-%03d", exp.getExpNum());
    cass.insertEntry(key, VALUE , exp);

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
    //cass.getEntry(key, exp);

    // then delete the file
    //cass.delete(key, exp, CONSIS);
    cass.delete(key, exp);

  }


}
