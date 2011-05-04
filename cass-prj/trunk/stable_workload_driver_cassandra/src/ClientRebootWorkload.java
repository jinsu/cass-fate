
package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;



public class ClientRebootWorkload implements Workload {
  
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
  public ClientRebootWorkload(Driver driver, Experiment exp) {
    this.driver = driver;
    this.cass = driver.getCass();
    this.u = driver.getUtility();
    this.exp = exp;
  }
  

  // *******************************************
  // the algorithm
  // *******************************************
  public void run() {


    // 1. setup for this specific workloadClientRebootWorkload
    preSetup();
    

    // 3. the exact workload where we want to run with failure
    runWithFailure();
    
    // 5. run post setup 
    postSetup();
  }
  
  
  // *******************************************
  public void runWithFailure() {

		//experiment with the rebooting process in Cassandra
		
		driver.restartDeadDataNodes();
		u.sleep(500);
		
		u.println("Check dead nodes after restarting nodes...");
		driver.checkDeadNodes();
		
		// Delete the experiment tag
		u.deleteFile(Driver.EXPERIMENT_RUN_FLAG); 
		u.println("");

	
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
		
		//Jungmin added for a reboot workload
		
		NodeProcess[] nps = driver.getNodeProcesses();
		
    String key1 = String.format("key-%03d", exp.getExpNum());
		String key2 = String.format("key-%03d", exp.getExpNum() + 1);
		String key3 = String.format("key-%03d", exp.getExpNum() + 2);
		
    String value2 = "EECS";
		String value3 = "DEPARTMENT";
		
		cass.insertEntry(key1, VALUE , exp);
		cass.insertEntry(key2, value2 , exp);
		cass.insertEntry(key3, value3 , exp);
		u.println("");
		
		u.println("Check dead nodes before killing nodes....");
		driver.checkDeadNodes();
		
	  driver.killCass();
		//driver.killNode("node1");
		u.sleep(300);
		
		u.println("Check dead nodes after killing nodes....");
		driver.checkDeadNodes();
		
		u.createNewFile(Driver.EXPERIMENT_RUN_FLAG);
		u.println("");

  }

  // *******************************************
  public void postSetup() { 
		
    // 4. stop the failure
    Driver.disableCoverage();
    Driver.disableFrog();
    Driver.disableFailureManager();
    Driver.disableClientOptimizer();
		
    // get entry
    //String key1 = String.format("key-%03d", exp.getExpNum());
		//String key2 = String.format("key-%03d", exp.getExpNum() + 1);
    //String key3 = String.format("key-%03d", exp.getExpNum() + 2);

    //cass.getEntry(key1, exp);
		//cass.getEntry(key2, exp);
		//cass.getEntry(key3, exp);
		
    // then delete the file
    //cass.delete(key, exp);
    
  }

  
}
