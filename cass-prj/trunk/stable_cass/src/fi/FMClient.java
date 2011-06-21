
package org.fi;

// ************************************ org fi
import org.fi.*;
import org.fi.FMServer.FailType;
import org.fi.FMJoinPoint.*;


// ************************************ jol
import jol.core.JolSystem;
import jol.core.Runtime;
import jol.types.basic.BasicTupleSet;
import jol.types.basic.Tuple;
import jol.types.basic.TupleSet;
import jol.types.exception.JolRuntimeException;
import jol.types.exception.UpdateException;
import jol.types.table.TableName;
import jol.types.table.Table.Callback;
import jol.types.table.Table;

// ************************************ aspect
import org.aspectj.lang.Signature; // include this for Signature, etc!
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.SourceLocation;


// ************************************ java
import java.io.*;
import java.net.InetSocketAddress;
import java.lang.Thread;
import java.lang.StackTraceElement;
import java.net.URL;



// ************************************ XML RPC
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;



// FMClient send to FMServer a list of String
// actually it will go to FMServer first
// the list of String is basically what the event should
// schedule

public class FMClient {


  public static final int FAAS_SYSTEM_EXIT_STATUS = 13;



  // #####################################################################
  // #####################################################################
  // ##                                                                 ##
  // ##              M A I N   E N T R Y   P O I N T S                  ##
  // ##                                                                 ##
  // #####################################################################
  // #####################################################################


  // ******************************************************
  // Each hook has some important properties:
  //  - Return object:
  //      The hook must return an object, this is important
  //      if we want to process corruption. The aspect always
  //      uses this returned value. By default the object is
  //      the object we get from proceed().
  //  - Throws exception:
  //      For exceptions that we want to exercise such as
  //      IOException and FileNotFoundException, then we must
  //      specify explicit hooks for that, so that these special
  //      hooks can return the exceptions as specified.
  //  - FailType.EXCEPTION
  //      Fail type exception is processed here. Other fail types
  //      should be proceed deeper in a centralized location.
  //      FailType.Corruption is alredy processed by now, and
  //      the resulting corruption should be in JoinRov
  //  - FailType.BadDisk
  // ******************************************************
  public static Object fiHookIox(FMJoinPoint fjp)  throws IOException {
    FailType ft = tryFiHook(fjp);
    if (fjp.getIox() != null) throw fjp.getIox();
    return fjp.getJoinRov();
  }

  // ******************************************************
  public static Object fiHookFnfx(FMJoinPoint fjp) throws FileNotFoundException {
    FailType ft = tryFiHook(fjp);
    if (fjp.getFnfx() != null) throw fjp.getFnfx();
    return fjp.getJoinRov();
  }

  // ******************************************************
  public static Object fiHookNox(FMJoinPoint fjp) {
    FailType ft = tryFiHook(fjp);
    return fjp.getJoinRov();
  }



  // #####################################################################
  // #####################################################################
  // ##                                                                 ##
  // ##          S A N I T Y  C H E C K   &   F I L T E R I N G         ##
  // ##                                                                 ##
  // #####################################################################
  // #####################################################################


  // ******************************************************
  private static FailType tryFiHook(FMJoinPoint fjp) {
    return syncedTryFiHook(fjp);
  }


  // ******************************************************
  // first do some sanity checking and filtering
  //  1. check that context is not null
  //  2. check that fm server is connected
  //  3. check that we pass client filter, whatever we define here
  //  4. let's do actual hook
  // ******************************************************
  private static synchronized FailType syncedTryFiHook(FMJoinPoint fjp) {

		//System.out.println("- stf (0)\n");

    if(!isExperimentRunning()) {
			//System.out.println("- stf (1)\n");

			return FailType.NONE;
		}

		// TODO: this is a bit wrong, because for FNFException ..
    // which is thrown by RAF.new FOS.new .. we haven't seen
    // the context so it's still null, but we might want to
    // throw an FNFException ... right now we're throwing FNFException
    // after the call RAF.nww and FOS.new
    if (isNullContext(fjp)) {
			//System.out.println("- stf (2)\n");
      return FailType.NONE;
	}

    if (!passClientFilter(fjp))
      return FailType.NONE;

		//System.out.println("- stf (3)\n");

    FailType ft = doFiHook(fjp);
    return ft;

  }


	private static boolean isExperimentRunning() {
		File f = new File(FMLogic.EXPERIMENT_RUN_FLAG);
		if(f.exists())
			return true;
		return false;
	}


  // ******************************************************
  // context is okay to be null here because we're not weaving
  // this aspect.aj to all files .. so ClassWC objects generated in
  // non-weaved files will not have context
  // ******************************************************
  private static boolean isNullContext(FMJoinPoint fjp) {

    if (fjp.getClassWC() == null) {
      Util.WARNING(fjp.getJoinPoint(), "null class WC at failure hook (FMClient)");
      return true;
    }

    if (fjp.getClassWC().getContext() == null) {

      // FIXME, hack:
      // check fi cwc is an instance of file or not
      // if so let's get the absolute path ..
      // the reason why File doesn't have context is because
      // sometimes File is obtained from File.listFile()
      ClassWC cwc = fjp.getClassWC();
      if (cwc instanceof File) {
	File f = (File)cwc;
	f.context = new Context(f.getAbsolutePath());
	return false;
      }

      //
      Util.WARNING(fjp.getJoinPoint(), "null context at failure hook (FMClient)");
      return true;
    }

    return false;
  }


  // ******************************************************
  // some filtering we could do at client
  private static boolean passClientFilter(FMJoinPoint fjp) {

    boolean pass = false;

    // we're interested in after join place only
    // if (fjp.getJoinPlc() == JoinPlc.AFTER) {
    // pass = true;
    // }

    // DEPRECATED policy
    // A little logic in insertFiHook, because depending on the
    // context, we want to do the failure before/after.
    // So far, here's a little policy:
    //   - Context = Disk, crash after
    //   - Context = NetIO, crash before and after

    pass = true;

    return pass;

  }




  // #####################################################################
  // #####################################################################
  // ##                                                                 ##
  // ##                  F M C L I E N T     L O G I C                  ##
  // ##                                                                 ##
  // #####################################################################
  // #####################################################################


  // ******************************************************
  private static FailType doFiHook(FMJoinPoint fjp) {

    // Util.WARNING(jp, "intercepted!!!");


    // ****************************
    // 1. prepare
    // prepare FST, FMC, FJP, id (all Writable)
    // ****************************
    Thread t = Thread.currentThread();
    FMStackTrace fst = new FMStackTrace(t.getStackTrace());

    FMContext ctx = new FMContext(fjp.getClassWC().getContext().getTargetIO());
    ctx.setCutpointRandomId();

    // ****************************
    // 2. let's get failure
    // REMEMBER: remember that if this hangs, then this means
    // that some of the args that are passed here do not have
    // a correct Writable read and write implementation!
    // So, do check each argument
    // ****************************
    FMAllContext fac = new FMAllContext(fjp, ctx, fst);


    FailType ft = sendContextOptimizer(fac);



    // ****************************
    // 3. now let's check if there is any persistent failure
    // ****************************
    ft = FMLogic.checkPersistentFailure(fac, ft);


    // ****************************
    // 4. let's process the failure
    // ****************************

    printFailType(fjp, fst, ctx, ft);
    processFailure(fjp, fst, ctx, ft);

    // some FailTypes (e.g. exception might not have been processsed
    // here, so we need to pass this on)

    return ft;

  }

  // ******************************************************
  // Do more optimization at the client, so that we reduce
  // communication to the fm server
  // ******************************************************
  private static FailType sendContextOptimizer(FMAllContext fac) {


    // if we have reached the max fsn .. then there is no point
    // we're checking this to the fm server logic
    // but remember we still need to check for persistent failures
    if (isClientOptimizeFlagExist()) {

      if (!FMLogic.isEnableFailureFlagExist()) {
	return FailType.NONE;
      }

      if (FMLogic.hasReachedMaxFsn()) {
        return FailType.NONE;
      }
    }

    FailType ft = sendContextViaXmlRpc(fac);

    return ft;

  }


  // ***********************************************************
  private static boolean isClientOptimizeFlagExist() {
    File f = new File(FMLogic.CLIENT_OPTIMIZE_FLAG);
    if (f.exists())
      return true;
    return false;
  }



  // ******************************************************
  private static void printFailType(FMJoinPoint fjp,
                                    FMStackTrace fst,
                                    FMContext ctx,
                                    FailType ft) {


    if (ft != FailType.NONE)
      return;

    if (!FMServer.debug)
      return;

    Util.MESSAGE("I'm failing this (see below) with FailType: " + ft);
    System.out.println(ctx);
    System.out.println(fjp);
    System.out.println(fst);
    System.out.println("");

  }


  // *****************************************************
  private static void processFailure(FMJoinPoint fjp,
                                     FMStackTrace fst,
                                     FMContext ctx,
                                     FailType ft)  {
    if (ft == FailType.NONE)
      return;

    else if (ft == FailType.CRASH)
      processCrash(fjp, fst, ctx);

    else if (ft == FailType.CORRUPTION)
      processCorruption(fjp, fst, ctx);

    else if (ft == FailType.RETFALSE)
      processReturnFalse(fjp, fst, ctx);

    else if (ft == FailType.EXCEPTION)
      processException(fjp, fst, ctx);

    else if (ft == FailType.BADDISK)
      processBadDisk(fjp, fst, ctx);

  }


  // ******************************************************
  private static void processCrash(FMJoinPoint fjp,
                                   FMStackTrace fst,
                                   FMContext ctx) {

    String pidToCrash = Util.getPid();

    Util.WARNING("I'm crahing here, and should see no more output");

    // 1) let's do the forceful way, use kill
    // String cmd = String.format("kill -s KILL %5s", pidToCrash);
    // String cmdout = Util.runCommand(cmd);

		//#################################
		//#################################
		//JINSU: We are crashing a node so all nodes aren't connected anymore.
		//IMPORTANT HACK
		//#################################
		//#################################
		org.apache.cassandra.Util.debug("In FMClient processCrash(), deleting nodeConnectedFlag");
		Util.deleteNodeConnectedFlag();

    // )2 or, let's do the normal way
    System.exit(FAAS_SYSTEM_EXIT_STATUS);
    Util.ERROR("if you see this, we are not crashing properly");

    // if we ever see this file, we're not failing properly
    File f = new File(FMLogic.TMPFI + "CRASH-FAILED");
    try { f.createNewFile(); } catch (Exception e) { }
  }


  // ******************************************************
  private static void processCorruption(FMJoinPoint fjp,
                                        FMStackTrace fst,
                                        FMContext ctx) {

    Object jrov = fjp.getJoinRov();

    // something is wrong, for corruption jrov should not be null
    if (jrov == null) {
      Util.FATAL("Corrupting a null Join ROV");
      return;
    }

    if (jrov instanceof java.lang.Long) {
      long tmp = ((Long)jrov).longValue();
      long tmp2;
      tmp2 = tmp - (tmp % 100000);
      tmp2 += (2 * 3600 * 1000);
      Long newJrov = new Long(tmp2);
      fjp.setJoinRov(newJrov);

      Util.MESSAGE("Corrupting read long from " +
                   tmp + " to " +
                   newJrov.longValue());
    }

  }

  // ******************************************************
  private static void processReturnFalse(FMJoinPoint fjp,
                                         FMStackTrace fst,
                                         FMContext ctx) {

    Object jrov = fjp.getJoinRov();

    // something is wrong, for ret false, jrov must be null!
    if (jrov != null) {
      // if we get to this point, there must be some race condition
      // where at JoinPlc.BEFORE, this join point does not see
      // the failure, so it doesn't return a false, but then
      // at JoinPlc.AFTER, some other join point in other threads
      // is being failed (E.g. baddisk), and hence, suddently
      // JoinPlc.AFTER for this point "Sees" the failure, but
      // we have run proceed() for this joinpoint. So in this case,
      // we should just say an error rather than a fatal
      Util.ERROR("Returning false, but Join ROV is not null");
      return;
    }

    Boolean newJrov = new Boolean(false);
    fjp.setJoinRov(newJrov);

    Util.MESSAGE("Returning false now ... ");
  }


  // ******************************************************
  // We set the exception that we should throw later in
  // FMJoinPoint
  // ******************************************************
  private static void processException(FMJoinPoint fjp,
                                       FMStackTrace fst,
                                       FMContext ctx) {

    if (fjp.getJoinExc() == JoinExc.IO) {
      fjp.setIox(new IOException("Intentional IOException from " + fjp.toString()));
    }
    else if (fjp.getJoinExc() == JoinExc.FNF) {
      fjp.setFnfx(new FileNotFoundException("Intentional IOException from FM"));
    }
  }


  // ******************************************************
  // If a bad disk, then we want to see what's the
  // join point is about. If it's exception then baddisk
  // will manifest to an exception. If it's a boolean return
  // value, it will manifest to a false return value
  // ******************************************************
  private static void processBadDisk(FMJoinPoint fjp,
                                     FMStackTrace fst,
                                     FMContext ctx) {
    if (fjp.getJoinExc() != JoinExc.NONE)
      processException(fjp, fst, ctx);
    else if (fjp.getJoinRbl() == JoinRbl.YES)
      processReturnFalse(fjp, fst, ctx);
  }


  // ******************************************************
  // DEPRECATED -- never crash the recipient of we get
  // a weird dead RPC to the FM server --- because when we
  // crash the other datanode, the other datanode could
  // be inside the FM server, and the FM server cannot return
  // properly, so then other RPC is dead ...
  private static void crashRecipient(FMContext ctx) {
    // String nodeId = Util.getNodeIdFromNetIO(ctx.getTargetIO());
    // String pidToCrash = Util.getPidFromNodeId(nodeId);
    // if (pidToCrash.equals("0")) { bad
  }



  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                    X M L       R P C                          ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################

  private static XmlRpcClient fmClient;


  // ******************************************************
  private static boolean cannotConnectToServer() {
    if (fmClient != null)
      return false;
    connectToFMServer();
    if (fmClient == null)
      return true;
    return false;
  }



  // ******************************************************
  private static void connectToFMServer() {
    try {
      XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
      String httpAddr = "http://127.0.0.1:" + FMServer.PORT + "/xmlrpc";
      config.setServerURL(new URL(httpAddr));
      fmClient = new XmlRpcClient();
      fmClient.setConfig(config);
    } catch (Exception e) {
      fmClient = null;
    }
  }


  // ******************************************************
  private static FailType sendContextViaXmlRpc(FMAllContext fac) {

    FailType ft = FailType.NONE;

    // this is okay, because we don't always want to
    // run hdfs with fm server
    if (cannotConnectToServer())
      return ft;

    int randId = fac.ctx.getCutpointRandomId();
    File f = Util.getRpcFile(randId);
    DataOutputStream dos = Util.getRpcOutputStream(randId);

    try {

      fac.write(dos);
      dos.close();

      // System.out.format("- Sending %d \n", randId);

      Object[] params = new Object[]{new Integer(randId)};
      Integer result = 0;
      result = (Integer) fmClient.execute("FMServer.sendContext", params);

      ft = Util.intToFailType(result);

      // System.out.format("- Received %d %s \n", result, ft);

      f.delete();

    } catch (Exception e) {
      f.delete();

      Util.EXCEPTION("RPC client error", e);
      // Util.FATAL("RPC client error"); // it's okay that if we cannot connect
    }

    return ft;

  }



  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                 H A D O O P    R P C                          ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################

  /*

    private static FMProtocol fmp = null;
    private static Configuration conf = new Configuration();
    private static InetSocketAddress addr =
    new InetSocketAddress(FMServer.bindAddr, FMServer.port);


    // ******************************************************
    private static FailType sendContextViaHadoopRPC(FMAllContext fac) {

    if (isNullFMServer())
    return FailType.NONE;

    FailType ft = FailType.NONE;
    ft = fmp.sendContext(fac.fjp, fac.ctx, fac.fst);
    return ft;
    }

    // ******************************************************
    private static boolean isNullFMServer() {
    if (fmp != null)
    return false;
    if (connectToFMServer() != null)
    return false;
    return true;
    }

    // ******************************************************
    private static FMProtocol connectToFMServer() {

    if (fmp != null)
    return fmp;

    try {
    // this shouldn't be wait for proxy, if fm server is not there, continue
    fmp = (FMProtocol)
    RPC.getProxy(FMProtocol.class, FMProtocol.versionID, addr, conf);
    // RPC.waitForProxy(FMProtocol.class, FMProtocol.versionID, addr, conf);
    } catch (IOException e) {
    Util.WARNING("cannot contact FM");
    return null;
    }
    return fmp;
    }

  */



  /////////////////////////////////////////////////
  /////////////////////////////////////////////////
  //////////// Jinsu's Cassandra Utility //////////
  /////////////////////////////////////////////////
  /////////////////////////////////////////////////

  public static FailType doCfiHook(FMJoinPoint fjp, Context c) {
        Thread t = Thread.currentThread();
        FMStackTrace fst = new FMStackTrace(t.getStackTrace());
        FMContext ctx = new FMContext(c.getTargetIO(), c.getMessageType());
        ctx.setCutpointRandomId();
        FMAllContext fac = new FMAllContext(fjp, ctx, fst);
        //System.out.println(fac);

        FailType ft = sendContextViaXmlRpc(fac);

        return ft;
  }
}
