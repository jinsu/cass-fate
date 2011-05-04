
// used to be FMCore

package org.fi;

import org.fi.*;

import java.io.*;
import java.util.Random;




import org.fi.FMJoinPoint.*;


import org.apache.xmlrpc.common.TypeConverterFactoryImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

//JINSU
import org.apache.xmlrpc.server.*;
//import org.apache.cassandra.Util;


// ******************************************************************
// Important note on FMServer usage:
// FMServer should be just a server that passes decision to its
// members.  For example, to decide whether we should do a fault
// injection or not, it just calls ManualFI .. later we want to do
// AutoFI for example.
// And also, to do modeling, we will use the Frog class
// ******************************************************************

// public class FMServer implements FMProtocol {
public class FMServer {

  
  public static boolean debug = false;
  //public static boolean debug = true;
  


  // private FrogServer frogServer;

  // the failure types
  // transient = exception
  // persistent = baddisk
  public static enum FailType {
    CRASH, BADDISK, EXCEPTION, RETFALSE, CORRUPTION, NONE;
  }

  // ***********************************************************
  public FMServer() {
    // also check frogHooks.aj is enabled!!
    // frogServer = new FrogServer();
  }


  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                F M    S E R V E R   S E R V I C E             ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################


  // ***********************************************************
  private static synchronized FailType syncedSendContext(FMAllContext fac) {



    // 1. print all context
    if (debug) printAllContext(fac);

    // 2. mark that we "cover" this, although we haven't excercised this
    //    (only if we want to compare, but normally, let's jsut comment this out
    //     otherwise to many output in tmp-fi-out)
    Coverage.recordBeforeFilter(fac);

    // 3. we always go to the failure logic, but check if we should
    //    fail or not later, so that we can get the coverage point
    FailType ft = doFail(fac);

    // print and return
    if (debug || true) printFailure(fac, ft);
    
    return ft;
    
  }


  // ***********************************************************
  private static void printFailure(FMAllContext fac, FailType ft) {
    if (ft == FailType.NONE)
      return;
    Util.MESSAGE("Server: I'm failing this (see above) [ft:" + ft + "]");
  }

  // ***********************************************************
  private static void printAllContext(FMAllContext fac) {


    // print context
    System.out.println("Receive sendContext: [" + fac.ctx.getCutpointRandomId() + "]\n");


    System.out.println(fac.ctx);

    // print joinpoint
    System.out.println(fac.fjp);

    // print stack
    System.out.println(fac.fst);

  }


  // ***********************************************************
  // doFail should consult the model check,
  // but if we want to drive the model checker with specific
  // manual stuffs, then we also consult the doFail
  // ***********************************************************
  private static FailType doFail(FMAllContext fac) {

    FailType ft = FailType.NONE;
    
    // By default we want to call the fmlogic
    ft = FMLogic.run(fac);
    

    // or do manual stuffs
    // boolean isFail = manualFI.doFail_02(fac);

    // fail03: fail03 with more filters, very specific
    // boolean isFail = manualFI.doFail_03(fac);

    // this is sufficient for doFail 01
    // ft =  manualFI.doFail_01(fjp, ctx, fst);

    return ft;
  }

  // ***********************************************************
  /*
  public void sendFrogEvent(FMJoinPoint fjp, FMStackTrace fst, FrogEvent fev) {

    if (frogServer == null) {
      Util.WARNING("Please instantiate FrogServer first, see FMServer");
      return;
    }

    System.out.println("Receive sendFrogEvent: ");

    // print joinpoint
    System.out.println(fjp);

    // print stack
    System.out.println(fst);



    // System.out.format("++ haha %s \n", fev);
    frogServer.processFrogEvent(fjp, fst, fev);
  }
  */



  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                    X M L       R P C                          ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################
  
  public final static int PORT = 16000;


  // ***********************************************************
  public void start() {

    try {
      System.out.println("- Starting XML-RPC Server...");
      WebServer webServer = new WebServer(PORT);

      XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
      PropertyHandlerMapping phm = new PropertyHandlerMapping();
      phm.addHandler("FMServer", org.fi.FMServer.class);
      xmlRpcServer.setHandlerMapping(phm);

      XmlRpcServerConfigImpl serverConfig =
        (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
      serverConfig.setEnabledForExtensions(true);
      serverConfig.setContentLengthOptional(false);

      System.out.println("- Waiting for incoming ...");

      webServer.start();

      System.out.println("- After webserver.start ...");

    } catch (Exception e) {
      Util.EXCEPTION("Can't start server", e);
      Util.FATAL("Can't start server");
    }
  }





  // ************************************
  // THis is the send context via XML RPC
  // ************************************
  public int sendContext(int randId) {

    // System.out.format("- Receveived %d \n", randId);
    return syncedSendContext(randId);
  }

  // ************************************
  // It is important for send context to be synchronized AND static !!
  // the reason is that web xml services always create a new FMServer
  // to process a request. So if we don't create this as static,
  // the synchronization is useless!
  // ************************************
  private static synchronized int syncedSendContext(int randId) {

    DataInputStream dis = Util.getRpcInputStream(randId);
    FMAllContext fac = new FMAllContext();

    try {
      fac.readFields(dis);
    } catch (Exception e) {
      Util.EXCEPTION("fac.readFields", e);
      Util.FATAL("fac.readFields");
    }

    FailType ft = syncedSendContext(fac);
    int rv = Util.failTypeToInt(ft);
    return rv;

  }




  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                 H A D O O P    R P C                          ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################


  /*

  public final static String bindAddr = "localhost";

  // ***********************************************************
  public long getProtocolVersion(String protocol, long clientVersion)
  throws IOException {
  return FMProtocol.versionID;
  }

  // ***********************************************************
  public void initialize() throws InterruptedException {
  System.out.println("FMServer: Init ...");
  Configuration conf = new Configuration();
  Server server;
  try {
  server = RPC.getServer(this, bindAddr, port, conf);
  // this runs forever ...
  server.start();
  server.join();
  System.out.println("FMServer: After server join ...");
  } catch (IOException e) {
  System.out.println("FMServer: Exception ...");
  e.printStackTrace();
  }
  }


  // ***********************************************************
  public FailType sendContext(FMJoinPoint fjp,
  FMContext ctx, FMStackTrace fst) {
  FMAllContext fac = new FMAllContext(fjp, ctx, fst);
  return syncedSendContext(fac);
  }

  */

}
