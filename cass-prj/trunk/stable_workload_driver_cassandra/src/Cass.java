

package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ColumnPath;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class Cass {


  Cassandra.Client client;
  Utility u;

  //Jinsu
  Driver driver;
  static ConsistencyLevel consis;


  String keyspace;
  String columnFamily;
  ColumnPath columnPath;
  String encoding;

  // *******************************************
  public Cass(Driver d) {
		this.driver = d;
    this.u = d.getUtility();
    consis = getConsistencyLevel();
  }


  // *******************************************
  // we only want to do this if client is null
  // we cannot create start this in Cass construct
  // because we don't know when namenode is up or down
  // *******************************************
  private void reconnectToCass() {
  	//u.sleep(200);
		u.println(new Date(u.now()) + " rTC(1) ---- "+ u.diff());
    int trial = 1;

    TTransport tr = new TSocket("localhost", 9160); //this takes .5sec
    u.println(new Date(u.now()) + " rTC(2) ---- "+ u.diff());
    TProtocol proto = new TBinaryProtocol(tr);
    long then = System.currentTimeMillis();
    waitForNodesToJoin();
		long took = System.currentTimeMillis() - then;
		u.println(new Date(u.now()) + " rTC(3) ---- wFNTJ took " + took + " ms");

    while (client == null) {

      try {

        u.print("- Trying to connect to Cass ... \n");

        // note that if the server is not waken up yet,
        // this will wait forever, because I hacked
        // the proxy a while ago


        client = new Cassandra.Client(proto);

        tr.open();

        u.print(String.format("- Connected to client ... \n"));



      } catch (Exception e) {
        //u.EXCEPTION(" In Cass construct", e);
        client = null;
        u.sleep(500);
      }
    }
  }

  // *******************************************
  private void waitForNodesToJoin() {
  	//Thread.yield();
  	/*
    while(true) {

      boolean allgood = true;

			u.println(new Date(u.now()) + " wFNTJ(loop 1) ---- "+ u.diff());
      for (int i = 0; i < Driver.NUM_OF_CASS_NODES; i++) {

        String fileLocation = String.format("%s/node%d.log",
                                            Driver.CASS_LOGS_DIR, i);
        String pattern = String.format("cluster");
        String cmdout = u.runCommand(String.format("grep -a %s %s",
                                                   pattern, fileLocation));

        if (i == 0 && (!cmdout.contains("127.0.0.11") ||
                       !cmdout.contains("127.0.0.12") ||
                       !cmdout.contains("127.0.0.13"))) {
          allgood = false;
          break;
        }
        if (i == 1 && (!cmdout.contains("127.0.0.1") ||
                       !cmdout.contains("127.0.0.12") ||
                       !cmdout.contains("127.0.0.13"))) {
          allgood = false;
          break;
        }
        if (i == 2 && (!cmdout.contains("127.0.0.1") ||
                       !cmdout.contains("127.0.0.11") ||
                       !cmdout.contains("127.0.0.13"))) {
          allgood = false;
          break;
        }
        if (i == 3 && (!cmdout.contains("127.0.0.1") ||
                       !cmdout.contains("127.0.0.11") ||
                       !cmdout.contains("127.0.0.12"))) {
          allgood = false;
          break;
        }

      }

			u.println(new Date(u.now()) + " wFNTJ(loop 2) ---- "+ u.diff());
      if (allgood) {
        u.print("- Seed knows about All non-seed nodes in the cluster...\n");
        break;
      }

      u.print("- Waiting until all nodes are in the cluster ... \n");
      u.sleep(500);
    }
		*/
		//**********THIS NEEDS IMPROVEMENT.
		//Jinsu:

		for(int i = 0; i < Driver.NUM_OF_CASS_NODES; i++) {
			driver.waitForNodeRegistration("node"+i);
		}

    u.createNewFile(Driver.NODES_CONNECTED_FLAG);

    u.print("- All nodes connected\n");

  }




  public void setKeyspace(String ks) {
    keyspace = ks;
  }
  public String getKeyspace(){
    return keyspace;
  }

  public void setColumnFamily(String cf) {
    columnFamily = cf;
  }

  public String getColumnFamily() {
    return columnFamily;
  }

  public void setColumnPath(String cp) {
    try {
      columnPath = new ColumnPath(columnFamily);
      columnPath.setColumn(cp.getBytes(encoding));
    } catch(Exception e) {
      u.EXCEPTION("In setColumnPath.Cass ", e);
    }
  }

  public ColumnPath getColumnPath() {
    return columnPath;
  }

  public void setEncoding(String ecd) {
    encoding = ecd;
  }

  public String getEncoding() {
    return encoding;
  }


  // *******************************************
  public void assertConnection() {
  	u.println(new Date(u.now()) + " aC(1)---- "+ u.diff());
    if (client == null) {
    	u.sleep(500);
    	u.println(new Date(u.now()) + " aC(1.5)---- "+ u.diff());
    	reconnectToCass();
    }

    u.println(new Date(u.now()) + " ac(2)---- "+ u.diff());
  }


  // *******************************************
  public void insertEntry(String key, String value, Experiment exp) {

    u.println("- Cass.insertEntry : [ " + key + ", " + value + " ]" + "... Consistency => " + consis);

    try {
       long timestamp = System.currentTimeMillis();

        client.insert(keyspace, key, columnPath, value.getBytes(encoding),
                      timestamp, consis);



      } catch (Exception e) {

        u.EXCEPTION("Cass.insertEntry fails", e);

        u.ERROR("Cass.insertEntry fails");

				//REMOVE THIS LATER
    		//JINSU: I want to see the out file for successful this experiment.
    		String from = Driver.CASS_LOGS_DIR;
    		String to = exp.getExpNumDir().getAbsolutePath();
    		u.copyFile(from+"fi.out", to+"/fi.out");
				u.copyFile(from+"node0.out", to+"/node0.out");
				u.copyFile(from+"node1.out", to+"/node1.out");
				u.copyFile(from+"node2.out", to+"/node2.out");
				u.copyFile(from+"node3.out", to+"/node3.out");

        // if we get here, the experiment has failed
        exp.markFailFromNonFrog();
        exp.addNonFrogReport("Cass.insertEntry() " + exp.getExpNum() +" FAILS!");

        //REMOVE THIS LATER
        //JINSU: I want to see the exception's name..
        exp.addNonFrogReport("--- Exception =>\t" + e.toString());


        exp.addExceptionToNonFrogReport(e);

      }

      u.print("- End of Cass.insertEntry!!! Yay\n");

  }





  // *******************************************
  public void getEntry(String key, Experiment exp) {
    u.println("- Cass.getEntry " + key +  "... consis " + consis);

    // if the Experiment already fails .. no need to move on
    if (exp.isFail()) {
      return;
    }

    try {

      //read single column

      //String exp_key = key + exp.getExpNum();
      u.println("single column:");
      Column col = client.get(keyspace, key, columnPath,
                              //ConsistencyLevel.QUORUM)
                              consis)
        .getColumn();

      u.println("column name: " + new String(col.name, encoding));
      u.println("column value: " + new String(col.value, encoding));
      u.println("column timestamp: " + new Date(col.timestamp));
    } catch (Exception e) {

        u.EXCEPTION("Cass.getEntry fails", e);

        u.ERROR("Cass.getEntry fails");

				//REMOVE THIS LATER
    		//JINSU: I want to see the out file for successful this experiment.
    		String from = Driver.CASS_LOGS_DIR;
    		String to = exp.getExpNumDir().getAbsolutePath();
    		u.copyFile(from+"fi.out", to+"/fi.out");
				u.copyFile(from+"node0.out", to+"/node0.out");
				u.copyFile(from+"node1.out", to+"/node1.out");
				u.copyFile(from+"node2.out", to+"/node2.out");
				u.copyFile(from+"node3.out", to+"/node3.out");

      u.EXCEPTION("Cass.getEntry fails", e);

      u.ERROR("Cass.getEntry fails");

      exp.markFailFromNonFrog();
      exp.addNonFrogReport("Cass.getEntry(" + key + ") FAILS!");
      //JINSU: I want to see the exception's name..
      exp.addNonFrogReport("--- Exception =>\t" + e.toString());
      exp.addNonFrogReport("--- Exception Message =>\t" + e.getMessage());

      exp.addExceptionToNonFrogReport(e);
    }
  }


  // *******************************************
  public void getEntry(String key, Experiment exp, String consistency) {
    ConsistencyLevel consis;
    if (consistency.equalsIgnoreCase("all")) {
      consis = ConsistencyLevel.ALL;
    } else if (consistency.equalsIgnoreCase("quorum")) {
      consis = ConsistencyLevel.QUORUM;
    } else if (consistency.equalsIgnoreCase("one")) {
      consis = ConsistencyLevel.ONE;
    } else {
      u.WARNING("Consistency level is not supported. going to default case");
      consis = ConsistencyLevel.ONE;
    }
    u.print("- Cass.getEntry " + key +  "...\n");

    // if the Experiment already fails .. no need to move on
    if (exp.isFail()) {
      return;
    }

    try {

      //read single column

      //String exp_key = key + exp.getExpNum();
      u.println("single column:");
      Column col = client.get(keyspace, key, columnPath,
          //ConsistencyLevel.QUORUM)
             consis)
               .getColumn();

      u.println("column name: " + new String(col.name, encoding));
      u.println("column value: " + new String(col.value, encoding));
      u.println("column timestamp: " + new Date(col.timestamp));
    } catch (Exception e) {
				//REMOVE THIS LATER
    		//JINSU: I want to see the out file for successful this experiment.
    		String from = Driver.CASS_LOGS_DIR;
    		String to = exp.getExpNumDir().getAbsolutePath();
    		u.copyFile(from+"fi.out", to+"/fi.out");
				u.copyFile(from+"node0.out", to+"/node0.out");
				u.copyFile(from+"node1.out", to+"/node1.out");
				u.copyFile(from+"node2.out", to+"/node2.out");
				u.copyFile(from+"node3.out", to+"/node3.out");

      u.EXCEPTION("Cass.getEntry fails", e);

      u.ERROR("Cass.getEntry fails");

      exp.markFailFromNonFrog();
      exp.addNonFrogReport("Cass.getEntry(" + key + ") FAILS!");
      exp.addExceptionToNonFrogReport(e);
    }
  }

  // *******************************************
  public void delete(String key, Experiment exp) {
    try {
        u.println("performing consistency " + consis);

        //String exp_key = key + exp.getExpNum();
      long timestamp = System.currentTimeMillis();
      client.remove(keyspace, key, columnPath, timestamp, consis);
    } catch (Exception e) {
				//REMOVE THIS LATER
    		//JINSU: I want to see the out file for successful this experiment.
    		String from = Driver.CASS_LOGS_DIR;
    		String to = exp.getExpNumDir().getAbsolutePath();
    		u.copyFile(from+"fi.out", to+"/fi.out");
				u.copyFile(from+"node0.out", to+"/node0.out");
				u.copyFile(from+"node1.out", to+"/node1.out");
				u.copyFile(from+"node2.out", to+"/node2.out");
				u.copyFile(from+"node3.out", to+"/node3.out");

      u.EXCEPTION("Cass.delete fails", e);

    exp.markFailFromNonFrog();
      exp.addNonFrogReport("Cass.delete(" + key + ") FAILS!");
      exp.addExceptionToNonFrogReport(e);

    }
  }

  // *******************************************
  public void delete(String key, Experiment exp, String consistency) {

      ConsistencyLevel consis;
      if (consistency.equalsIgnoreCase("all")) {
          consis = ConsistencyLevel.ALL;
      } else if (consistency.equalsIgnoreCase("quorum")) {
          consis = ConsistencyLevel.QUORUM;
      } else if (consistency.equalsIgnoreCase("one")) {
          consis = ConsistencyLevel.ONE;
      } else {
          u.WARNING("Consistency level is not supported. going to default case");
          consis = ConsistencyLevel.ONE;
      }

    try {
      //String exp_key = key + exp.getExpNum();
      long timestamp = System.currentTimeMillis();
      client.remove(keyspace, key, columnPath, timestamp, consis);
    } catch (Exception e) {
      u.EXCEPTION("Cass.delete fails", e);

    exp.markFailFromNonFrog();
      exp.addNonFrogReport("Cass.delete(" + key + ") FAILS!");
      exp.addExceptionToNonFrogReport(e);

    }
  }

  //jinsu: figure out the consistency level
  public ConsistencyLevel getConsistencyLevel() {

      ConsistencyLevel consis;
      String consistency = driver.CONSISTENCY;
      if (consistency.equalsIgnoreCase("all")) {
          consis = ConsistencyLevel.ALL;
      } else if (consistency.equalsIgnoreCase("quorum")) {
          consis = ConsistencyLevel.QUORUM;
      } else if (consistency.equalsIgnoreCase("one")) {
          consis = ConsistencyLevel.ONE;
      } else {
          u.WARNING("Consistency level is not supported. going to default case");
          consis = ConsistencyLevel.ALL;
      }

      return consis;
  }

}
