
package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.LinkedList;


public class Experiment {

  Driver driver;
  private String type = "UNKNOWN";
  int expNum;
  Utility u;
  String failureHashFile;
  LinkedList<String> hashFailures = new LinkedList<String>();
  
  // *******************************************
  public Experiment(Driver driver, int expNum) {
    u = driver.getUtility();
    this.driver = driver;
    this.expNum = expNum;
  }

  // *******************************************
  public int getExpNum() {
    return expNum;
  }

  // *******************************************
  public void setType(String type) {
    this.type = type;
  }

  // ********************************************************
  // use the utilities in the driver
  // 1. rm all storage files
  // 2. check and restart any deadnodes
  // 2. reset single crash mode
  public void prepare() {

    driver.resetFailureFlags();

  }

  // *******************************************
  // print the experiment number ...
  public void printBegin() {


    String full =
      ("## ################################################# ##\n");
    String side =
      ("##                                                   ##\n");
    String middle = String.format
      ("##         E X P E R I M E N T   #  %04d             ##\n", expNum);

    u.print("\n\n");
    u.print(full);
    u.print(full);
    u.print(side);
    u.print(middle);
    u.print(side);
    u.print(full);
    u.print(full);
    u.print("\n\n");
  }


  // ***************************************
  public void addFailure(String hash) {

    hashFailures.add(hash);

  }

  // *******************************************
  // print the experiment number ...
  public void printEnd() {

    String[] tmp = hashFailures.toArray(new String[hashFailures.size()]);

    String buf = String.format("\n    [experiment-%02d][", expNum);
    for (int i = 0; i < tmp.length; i++)  {
      buf += tmp[i];
      if (i != tmp.length-1)
	buf += ", ";
    }
    buf += "] \n\n";
    
    u.print(buf);
  }

}