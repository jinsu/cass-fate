 

package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


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
    
    // just initialize driver
    Driver d = new Driver();
    
    // let's run the driver
    d.run();
    
  }


  
}