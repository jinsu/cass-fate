
// this is a "command line usage" to connect to the FM


package org.fi;

import org.fi.*;

import java.io.*;
import java.net.InetSocketAddress;


public class FMAdmin {

  // DEPRECATED

  // we used to use this to communicate something to the
  // FMServer .. but guess what ... before we used
  // Hadoop RPC which is very slow, so we alleviate that
  // by just writing a file .. to a local file system as
  // the flagger
  // in the future we might want to just use XML RPC which
  // is much faster

  /*


    private static final String enableFailureOnFly = "-enable";
    private static final String disableFailureOnFly = "-disable";

    private static FMProtocol fmp = null;


    // ***********************************************
    public static void connect() {

    Configuration conf = new Configuration();
    InetSocketAddress addr =
    new InetSocketAddress(FMServer.bindAddr, FMServer.port);

    try {
    fmp = (FMProtocol)
    RPC.getProxy(FMProtocol.class, FMProtocol.versionID, addr, conf);
    } catch (IOException e) {
    Util.ERROR("cannot contact FM");
    e.printStackTrace();
    System.exit(0);
    }
    }

    // ***********************************************
    public static void parseArguments(String args[]) {
    int argsLen = (args == null) ? 0 : args.length;
    for (int i = 0; i < argsLen; i++) {
    String cmd = args[i];

    if (cmd.equals(enableFailureOnFly)) {
    connect();

    Util.FATAL(" THIS HAS BEEN DEPRECATED !!! \n");

    // fmp.enableFailure();
    System.exit(0);
    }

    else if (cmd.equals(disableFailureOnFly)) {
    connect();
    // fmp.disableFailure();

    Util.FATAL(" THIS HAS BEEN DEPRECATED !!! \n");

    System.exit(0);
    }

    else {
    Util.ERROR("Unrecognized command arg [" + cmd + "]");
    System.exit(0);
    }
    }
    }

    // ***********************************************
    public static void main(String[] args) {


    System.out.println("FMAdmin.Main: Starting ...");

    parseArguments(args);

    System.out.println("FMAdmin.Main: Stopping ...");
    }

  */
}








