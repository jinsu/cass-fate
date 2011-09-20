

package org.fi;

import org.fi.*;

import java.io.*;
import java.net.InetSocketAddress;


public class FMDriver {



  // ***********************************************
  public static void parseArguments(String args[]) {
    int argsLen = (args == null) ? 0 : args.length;
    for (int i = 0; i < argsLen; i++) {
      String cmd = args[i];

      // this option is used when we run this normally
      // if (cmd.equals(enableFailureArg))  {
      // enableFailure = true;
      // System.out.println("  Failure is ON ... !!!");
      // }

      // else {
      // Util.ERROR("Unrecognized command arg [" + cmd + "]");
      // System.exit(0);
      // }
    }
  }

  // ***********************************************
  public static void main(String[] args) {


    System.out.println("FMDriver.Main: Starting ...");

    // parseArguments(args);
    // create new falure scheduler instance
    FMServer fm = new FMServer();
    System.out.println("FMDRIVER.Main: before calling fm.start()");
    System.out.flush();
    fm.start();
    System.out.println("FMDriver.Main: after calling fm.start()");
    System.out.flush();

    /*
    try {
      fm.initialize();

    } catch (InterruptedException e) {

      System.out.println("FMDriver.Main: Exception ...");
      e.printStackTrace();
    }
    */


    System.out.println("FMDriver.Main: Stopping ...");
  }
}








