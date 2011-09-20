

package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/*
 * For testing org.apache.cassandra.Util functions. Jin-Su
 *
import java.util.*;
import java.net.*;
import org.apache.cassandra.Util;
*/

public class Main {

  // Just some utility (just like org.fi.Util)
  // remember that since we are in the same package org.fi,
  // don't add conflicting class names with HDFS fi classes
  static Utility u = new Utility();



  // *******************************************
  public Main() {

  }


  // *******************************************
  public static void main(String[] args) throws IOException {
    //System.exit(0);
    // just initialize driver

    //testing org.apache.cassandra.Util functions
    /*
    if(false) {
    Util u = new Util();
    System.out.println("testing orderEndpoints function : ");
    List<InetAddress> testList = new ArrayList<InetAddress>(4);
    byte[] b0 = new byte[4];
    b0[0] = new Integer(127).byteValue();
    b0[1] = new Integer(0).byteValue();
    b0[2] = new Integer(0).byteValue();
    b0[3] = new Integer(1).byteValue();
    byte[] b1 = new byte[4];
    b1[0] = new Integer(127).byteValue();
    b1[1] = new Integer(0).byteValue();
    b1[2] = new Integer(0).byteValue();
    b1[3] = new Integer(11).byteValue();
    byte[] b2 = new byte[4];
    b2[0] = new Integer(127).byteValue();
    b2[1] = new Integer(0).byteValue();
    b2[2] = new Integer(0).byteValue();
    b2[3] = new Integer(12).byteValue();
    byte[] b3 = new byte[4];
    b3[0] = new Integer(127).byteValue();
    b3[1] = new Integer(0).byteValue();
    b3[2] = new Integer(0).byteValue();
    b3[3] = new Integer(13).byteValue();


    InetAddress node0 = InetAddress.getByAddress(b0);
    InetAddress node1 = InetAddress.getByAddress(b1);
    InetAddress node2 = InetAddress.getByAddress(b2);
    InetAddress node3 = InetAddress.getByAddress(b3);
    testList.add(node0);
    testList.add(node2);
    testList.add(node3);
    testList.add(node1);
    Collection<InetAddress> sortList = Util.orderEndpoints(testList);
    }
    */


    Driver d = new Driver();

    // let's run the driver
    d.run();

  }



}
