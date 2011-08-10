
package org.fi;

import org.fi.*;
import org.fi.FMServer.FailType;
import org.fi.FMJoinPoint.*;

import java.io.*;
import java.util.*;



// cassandra specifics


public class FMLogic {


  private static int cachedMaxFsn = 0; // cache, don't update directly

  // public static final String CASS_USERNAME = "hadoop-haryadi";
  public static final String CASS_USERNAME = "cassandra-fi" + System.getenv("USER");

  public static final String TMPFI = "/tmp/fi/";
  public static final String CASS_STORAGE_DIR      = TMPFI + "cassandra/";

  public static final String FAIL_HISTORY_DIR      = TMPFI + "failHistory/";
  public static final String FLAGS_FAILURE_DIR     = TMPFI + "flagsFailure/";
  public static final String EXP_RESULT_DIR        = TMPFI + "expResult/";
  public static final String RPC_FILES_DIR         = TMPFI + "rpcFiles/";
  public static final String SOCKET_HISTORY_DIR    = TMPFI + "socketHistory/";
  public static final String COVERAGE_COMPLETE_DIR = TMPFI + "coverageComplete/";
  public static final String COVERAGE_STATIC_DIR   = TMPFI + "coverageStatic/";
	public static final String CASS_PIDS_DIR         = TMPFI + "pids/";
  public static final String CASS_LOGS_DIR         = TMPFI + "logs/";
	//public static final String CASS_STORAGE_DIR      = TMPFI + "cassandra/";
	public static final String IP_HISTORY_DIR = TMPFI + "ipHistory/";



  public static final String ENABLE_FAILURE_FLAG  = TMPFI + "enableFailureFlag";
  public static final String CLIENT_OPTIMIZE_FLAG = TMPFI + "clientOptimizeFlag";
  public static final String ENABLE_COVERAGE_FLAG = TMPFI + "enableCoverageFlag";

	public static final String NODES_CONNECTED_FLAG = TMPFI + "nodesConnectedFlag";
	public static final String EXPERIMENT_RUN_FLAG = TMPFI + "experimentRunning";



  // ########################################################################
  // ########################################################################
  // ##                                                                    ##
  // ##                            S E T U P S                             ##
  // ##                                                                    ##
  // ########################################################################
  // ########################################################################


  // *********************************************
  public FMLogic() {  }


  //JINSU hack for cass corruption
   // private boolean debug = false;
    private static boolean debug = true;
  private static boolean isDigestReadResponse(FMAllContext fac) {
      if(fac.ctx.getMessageType().equalsIgnoreCase(FMClient.READ_RESPONSE_DIGEST)
              && fac.fjp.contains("sendOneWay")) {

          System.out.println("POW POW kitty");
          //System.out.println("FMLogic can run the corruption!!!");
          return true;
    }
    return false;
  }

  private static boolean isDataReadResponse(FMAllContext fac) {
      if(fac.ctx.getMessageType().equalsIgnoreCase(FMClient.READ_RESPONSE_NORMAL)
              && fac.fjp.contains("sendOneWay") ) {
            return true;
              }
      return false;
  }

  // *********************************************
  // the brain of fm logic begins. have fun!
  // *********************************************
  public static FailType run(FMAllContext fac) {
    /*
      if (isDigestReadResponse(fac)) {
          return FailType.CORRUPTION;
      }
    */


    FailType ft;
      // check if we need to reset anything?
    checkResetExperiment();

   //JINSU
   if(debug) System.out.println("FMLogic run : chkpnt 1");

    // generate all possible failures
    FailType [] failures = listPossibleFailures(fac);
    ft = tryTheseFailures(fac, failures);

    // DEPRECATED ... we are calling this at the client side
    // check if we have persistent failure, see the function's comment
    // ft = checkPersistentFailure(fac, ft);

    return ft;
  }



  // *********************************************
  // do we need to reset anything? signaled by WorkloadDriver
  // *********************************************
  private static void checkResetExperiment() {

  }

  // *********************************************
  // List all possible failures, given the information about this
  // pointcut. It's up to the FIState model-checker and the
  // server filter to decide which failures that we want to exercise
  // later. All we want to do here is simply list all possible failures.
  // *********************************************
  private static FailType[] listPossibleFailures(FMAllContext fac) {

    List<FailType> list = new ArrayList<FailType>();

    boolean crash = false;
    boolean exception = false;
    boolean corruption = false;
    boolean baddisk = false;
    boolean retfalse = false;


    // throw exception if it's possible (must before)
    if (fac.fjp.getJoinExc() != JoinExc.NONE &&
        fac.fjp.getJoinPlc() == JoinPlc.BEFORE) {
      exception = true;
    }

    // special case, if it's an FNF exception, it's okay
    // that we throw this FNF exception after -- because
    // in 'before' we haven't known the context yet
    if (fac.fjp.getJoinExc() == JoinExc.FNF) {
      exception = true;
    }

    // false bool if operation JoinRbl is yes (must before)
    if (fac.fjp.getJoinRbl() == JoinRbl.YES &&
        fac.fjp.getJoinPlc() == JoinPlc.BEFORE) {
      retfalse = true;
    }

    // corruption only if iot is read (must after)
    /*
    if (fac.fjp.getJoinIot() == JoinIot.READ &&
        fac.fjp.getJoinPlc() == JoinPlc.AFTER) {
      corruption = true;
    }
    */
    //JINSU: change this when we are adding more corruption cases.
    if (isDigestReadResponse(fac) || isDataReadResponse(fac)) {
        corruption = true;
    }

    // crash for read and write (before or after is fine)
    if (fac.fjp.getJoinIot() == JoinIot.READ ||
        fac.fjp.getJoinIot() == JoinIot.WRITE ||
        isDataReadResponse(fac) ) {
      crash = true;
    }

    // baddisk if targetIO is disk (must be before)
    if (fac.ctx.getTargetIO().contains("hadoop") &&
        fac.fjp.getJoinPlc() == JoinPlc.BEFORE) {

      // but we only want to insert baddisk if
      // we haven't failed this disk
      if (!isTargetIOaBadDisk(fac.ctx))  {
        baddisk = true;
      }
    }


    // now let's add possible failures
    if (crash)      list.add(FailType.CRASH);
    if (exception)  list.add(FailType.EXCEPTION);
    if (corruption) list.add(FailType.CORRUPTION);
    if (baddisk)    list.add(FailType.BADDISK);
    if (retfalse)   list.add(FailType.RETFALSE);

    if (list.size() == 0)
      return null;

    FailType[] failures = list.toArray(new FailType[list.size()]);

    if (false) {
      System.out.print("  Possible failures: ");
      for (int i = 0; i < failures.length; i++) {
        System.out.print(failures[i].toString() + ", ");
      }
      System.out.println("\n");
    }

    return failures;

  }


  // *********************************************
  // At this point, we might have a failure to exercise
  // or we don't, we'll just return this to the FMClient
  // but before that, we always need to check for
  // persistent failures (e.g. baddisk), because even
  // though we don't have a failure at this point, we
  // might have exercised a persistent failure (e.g. baddisk)
  // before.  Hence, we want to check if the persistent
  // failure overwrites non-failure.
  // If we should exercise a failure, we just return the failure
  // this function only conversts FailType.NONE to
  // FailType.BADDISK if it's appropriate

  // *********************************************
  public static FailType checkPersistentFailure(FMAllContext fac, FailType ft) {

    if (ft != FailType.NONE) {
      return ft;
    }

    if (isTargetIOaBadDisk(fac.ctx)) {
      return FailType.BADDISK;
    }

    return FailType.NONE;

  }


  // *********************************************
  // from the ctx, is target an already bad disk?
  // if so, return true, else return false
  // *********************************************
  private static boolean isTargetIOaBadDisk(FMContext ctx) {

    // if this is not a disk io return false
    if (!Util.isDiskIO(ctx.getTargetIO())) {
      return false;
    }

    // let's get the nodeId and diskId for this ctx
    String nodeId = ctx.getNodeId();
    String diskId = Util.getDiskIdFromTargetIO(ctx.getTargetIO());

    // something wrong
    if (diskId.equals("DiskUnknown"))
      return false;

    // check the flag file
    File flagFile = getBadDiskFlagFile(nodeId, diskId);
    if (flagFile.exists()) {
      return true;
    }

    return false;
  }




  // *********************************************
  // for each possible failure, we want to try if we
  // so do the failure or not.
  // if ft is approved, then we should break
  // if not, we should continue to the next failure
  // *********************************************
  private static FailType tryTheseFailures(FMAllContext fac, FailType [] failures) {
    FailType ft = FailType.NONE;
    if (failures == null)
      return ft;

    //JINSU
    if(debug) System.out.println("FMLogic tTF : chkpt 1");

    for (int i = 0; i < failures.length; i++) {
      ft = tryThisFailure(fac, failures[i]);
      if (ft != FailType.NONE) {
        break;
      }
    }
    return ft;
  }


  // *********************************************
  // Before we insert the failure, we want to filter this
  // first.  So check the filter.
  // *********************************************
  private static FailType tryThisFailure(FMAllContext fac, FailType ft) {

    // let's build the FIState based on the failure
    // build the FIState
    FIState fis = new FIState(fac, ft);



    if (FMFilter.passServerFilter(fac, ft, fis)) {
        //JINSU
        if(debug) System.out.println("FMLogic tThisF : chkpt 1");
      // if pass the server filter, we want to measure the stats
      // that have been filtered ..
      Coverage.recordStatAfterFilter(fac, ft, fis);


      // for the sake of recording stat, we're done ..
      // so no need to continue ...
      // just check if
      if (isEnableFailureFlagExist()) {

        if(debug) System.out.println("FMLogic tThisF : chkpt 2");

        FailType retFt = runFailLogic(fac, ft, fis);
        return retFt;

      }
    }
    if(debug) System.out.println("FMLogic tThisF : chkpt 3");
    return FailType.NONE;
  }


  // ########################################################################
  // ########################################################################
  // ##                                                                    ##
  // ##                       C O R E    L O G I C                         ##
  // ##                                                                    ##
  // ########################################################################
  // ########################################################################




  // *************************************************
  // This is the fail logic: logics for single crash,
  // multiple crashes, remembering failure history
  // should all go in this place
  // *************************************************
  private static FailType runFailLogic(FMAllContext fac, FailType ft, FIState fis) {

    System.out.println("corrupt :: rfl1 :: show me FailType => " + ft);
    // if i have reached max fsn ...
    // just continue ...
    if (hasReachedMaxFsn()) {
      return FailType.NONE;
    }
     System.out.println("corrupt :: rfl2 :: checkpoint");

    // let's get current failure number
    int fsn = getCurrentFsn();

    // check the logic
    if (!shouldFail(fsn, fis)) {
      return FailType.NONE;
    }

     System.out.println("corrupt :: rfl3 :: checkpoint");
    // if we reach this point we're doing failure ..
    recordFailure(fac, ft, fis, fsn);


    return ft;

  }



  // ********************************************
  // Given a fsn and a hash, this is the logic:
  //  - first we check if the fsn is locked or not
  //    if it is locked then the hash must match
  //    with the hash specified by the locked fsn.
  //    otherwise, we shouldn't fail this.
  //  - else, if fsn is not locked, then we go
  //    to normal mode, where we check if we fail
  //    this failure before or not
  // ********************************************
  private static boolean shouldFail(int fsn, FIState fis) {

    // String tmp = String.format("fsn-%d hash-%s", fsn, hash);

    boolean shouldFail;
    if (isFsnLocked(fsn)) {
      if (isFsnAndHashMatched(fsn, fis.getHashId())) {
        shouldFail = true;
        System.out.println("corrupt :: sf1 checkpoint");
      }
      else {
        shouldFail = false;
        System.out.println("corrupt :: sf2 checkpoint");
      }
    }
    else {
      if (isInFailHistory(fsn, fis.getHashId())) {
        // System.out.format("_We have injected %d in the past_\n",
        // fis.getHashId());
        shouldFail = false;
        System.out.println("corrupt :: sf3 checkpoint");
      }
      else  {
        shouldFail = true;
        System.out.println("corrupt :: sf4 checkpoint");

      }
    }
    return shouldFail;
  }


  // ********************************************
  // record the failure
  // ********************************************
  private static void recordFailure(FMAllContext fac, FailType ft,
                                    FIState fis, int fsn) {

    recordInjectedFsn(fsn);
    recordFailHistory(fac, ft, fis, fsn);
    recordFailureToExperiment(fac, ft, fis, fsn);
    recordLatestHistory(fsn, fis);

    // special treatment for bad disk
    recordBadDisk(fac, ft);

  }



  // ########################################################################
  // ########################################################################
  // ##                                                                    ##
  // ##                        U T I L I T Y                               ##
  // ##                                                                    ##
  // ########################################################################
  // ########################################################################



  // ********************************************
  // This algorithm is easy ... we're just failing
  // whatever we have
  // ********************************************
  public static boolean hasReachedMaxFsn() {

    int maxFsn = getMaxFsn();
    if (isFsnInjected(maxFsn)) {
        System.out.println("corrupt :: hr1 : checkpoint");
      return true;
    }
    return false;
  }

  // *******************************************
  public static int getMaxFsn() {
    if (cachedMaxFsn != 0)
      return cachedMaxFsn;
    String path = FLAGS_FAILURE_DIR + "/maxFsn";
    String tmp1 = Util.fileContentToString(path);

    if (tmp1 == null) {
      Util.FATAL("maxFsn is unknown");
      return 0;
    }
    tmp1 = tmp1.replaceAll("\n", "");

    Integer tmp;
    try {
      tmp = new Integer(tmp1);
    } catch(NumberFormatException nfe) {
      Util.FATAL("There is no maxFsn file?");
      return 0;
    }

    int maxFsn = tmp.intValue();
    if (maxFsn < 1 || maxFsn > 100) {
      Util.FATAL("weird maxFsn " + maxFsn);
    }
    cachedMaxFsn = maxFsn;
    return cachedMaxFsn;
  }

  // *******************************************
  private static boolean isFsnInjected(int fsn) {
    File f = getInjectedFsnFile(fsn);
    if (f.exists())
      return true;
    return false;
  }

  // *******************************************
  private static File getInjectedFsnFile(int fsn) {
    String path = String.format("%s/injected-fsn-%d", FLAGS_FAILURE_DIR, fsn);
    File f = new File(path);
    return f;
  }

  // ********************************************
  public static int getCurrentFsn() {
    int fsn = 1;
    while (true) {
      if (!isFsnInjected(fsn))
        return fsn;
      fsn++;
    }
  }

  // ********************************************
  // if fsn is locked it means this fsn has a
  // specific hash that we must follow
  // ********************************************
  private static boolean isFsnLocked(int fsn) {
    File f = getFsnLockFile(fsn);
    if (f.exists())
      return true;
    return false;
  }

  // ********************************************
  // filename: locked-fsn-#
  // ********************************************
  private static File getFsnLockFile(int fsn) {
    String path = String.format("%s/locked-fsn-%d", FLAGS_FAILURE_DIR, fsn);
    File f = new File(path);
    return f;
  }


  // ********************************************
  //
  // ********************************************
  private static boolean isFsnAndHashMatched(int fsn, int hashId) {
    File f = getFsnAndHashFile(fsn, hashId);
    if (f.exists())
      return true;
    return false;
  }

  // ********************************************
  // filename: hash-for-fsn-%d-is-
  // ********************************************
  private static File getFsnAndHashFile(int fsn, int hashId) {
    String path = String.format("%s/hash-for-fsn-%d-is-h%s.txt",
                                FLAGS_FAILURE_DIR,
                                fsn, hashId);
    File f = new File(path);
    return f;
  }


  // ********************************************
  //
  // ********************************************
  private static boolean isInFailHistory(int fsn, int hashId) {
    File f = getFailHistoryFile(fsn, hashId);
    if (f.exists())
      return true;
    return false;
  }


  // ********************************************
  // fail history file: .../failHistory/fsn-1/h-d989.txt
  // ********************************************
  private static File getFailHistoryFile(int fsn, int hashId) {

    String dir = String.format("%s/fsn-%d", FAIL_HISTORY_DIR, fsn);
    File d = new File(dir);
    if (!d.exists()) {
      Util.mkDir(d);
    }

    String file = getHashFileName(hashId);
    File f = new File(d, file);
    return f;
  }


  // ********************************************
  public static String getHashFileName(int hashId) {
    return String.format("h%d.txt", hashId);
  }

  // *******************************************
  private static void recordInjectedFsn(int fsn) {
    File f = getInjectedFsnFile(fsn);
    Util.createNewFile(f);
  }

  // *************************************************
  private static void recordBadDisk(FMAllContext fac, FailType ft) {

    // no need to do anything if it's not a baddisk
    if (ft != FailType.BADDISK)
      return;

    // if it's a bad disk .... need to remember what node and what disk ..
    String nodeId = fac.ctx.getNodeId();
    String diskId = Util.getDiskIdFromTargetIO(fac.ctx.getTargetIO());
    File flagFile = getBadDiskFlagFile(nodeId, diskId);
    Util.createNewFile(flagFile);
  }

  // *******************************************
  private static File getBadDiskFlagFile(String nodeId, String diskId) {
    String fname =
      String.format("%s/BadDisk_%s_%s",
                    FLAGS_FAILURE_DIR, nodeId,  diskId);
    File f = new File(fname);
    return f;
  }

  // ********************************************
  private static void recordFailHistory(FMAllContext fac, FailType ft,
                                        FIState fis, int fsn) {

    File f = getFailHistoryFile(fsn, fis.getHashId());
    if (f.exists()) {
      // it's okay that a fail history already there
      // for example, in the case where fail history is locked ..
      return;
    }


    String buf = getFailHistoryContent(fac, ft, fis, fsn);
    Util.stringToFileContent(buf, f, true);
  }


  // ********************************************
  public static String getFailHistoryContent(FMAllContext fac, FailType ft,
                                             FIState fis, int fsn) {

    String buf = "";

    // print hash id first
    buf += "\n";
    buf += "The hash ID string is: \n";
    buf += "## [" + fis.getHashIdStr() + "] \n";
    buf += "\n";
    buf += "The hash ID is: \n";
    buf += "[[" + fis.getHashId() + "]] \n";
    buf += "\n";

    // print all context
    buf += String.format("Receive sendContext: [" +
                         fac.ctx.getCutpointRandomId() + "]\n");

    buf += "\n";


    if (ft != null) {
      buf += "FailType: **" + ft.toString() + "**\n\n";
    }


    buf += fac.ctx + "\n";
    buf += fac.fjp + "\n";
    buf += fac.fst + "\n";

    buf += "\n";

    return buf;
  }


  // ********************************************
  // we must do this because "ls -t" is not precise
  // ********************************************
  private static void recordLatestHistory(int fsn, FIState fis) {
    File f = getLatestHistoryFile(fsn);
    Util.stringToFileContent(fis.getHashId() + "\n", f);
  }

  // ********************************************
  // return the latest history file for this fsn
  // ********************************************
  private static File getLatestHistoryFile(int fsn) {
    String path = String.format("%s/latest-for-fsn-%d", FAIL_HISTORY_DIR, fsn);
    File f = new File(path);
    return f;
  }


  // ********************************************
  // this is the function that is dependent on workload driver
  // however, if expNumStr
  // ********************************************
  private static void recordFailureToExperiment(FMAllContext fac, FailType ft,
                                                FIState fis, int fsn) {

    String path = FLAGS_FAILURE_DIR + "/currentExpNumber";
    String expNumStr = Util.fileContentToString(path);
    if (expNumStr == null) {
      Util.WARNING("No info on experiment number, continuing");
      return;
    }
    expNumStr = expNumStr.replaceAll("\n", "");
    Integer expNum;
    try {
      expNum = new Integer(expNumStr);
    } catch(NumberFormatException nfe) {
      Util.EXCEPTION("Can't convert exp# ", nfe);
      return;
    }


    String expNumDirName = getExpNumDirName(expNum.intValue());
    Util.mkDir(expNumDirName);

    path = String.format("%s/fsn%d-%s",
                         expNumDirName,
                         fsn, getHashFileName(fis.getHashId()));

    String buf = getFailHistoryContent(fac, ft, fis, fsn);

    Util.stringToFileContent(buf, path);

  }


  // ********************************************
  private static String getExpNumDirName(int expNum) {
    return String.format("%s/exp-%05d", EXP_RESULT_DIR, expNum);
  }


  // ***********************************************************
  public static boolean isEnableFailureFlagExist() {
    File f = new File(ENABLE_FAILURE_FLAG);
    if (f.exists())
      return true;
    return false;
  }


}
