package org.fi;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

public class Driver {
  public static boolean TESTING = false;
  //public static boolean TESTING = true;

  // ########################################################################
  // ########################################################################
  // ##                                                                    ##
  // ##                  E X P   P A R A M E T E S                         ##
  // ##                                                                    ##
  // ########################################################################
  // ########################################################################


  public static int NUM_OF_CASS_NODES = 4;

  public static int BREAK_EXP_NUMBER = Parameters.BREAK_EXP_NUMBER;
  //public static int BREAK_EXP_NUMBER = 2;

  public static final int MAX_FSN = Parameters.MAX_FSN;



  public static boolean

               //  enableFailure = false,
                enableFailure = Parameters.enableFailure,

                  //enableOptimizer = true,
                  enableOptimizer = false,

                  enableCoverage = Parameters.enableCoverage,
                  // enableCoverage = false,

                  debug = Parameters.debug,
                  // debug = false,

                  // enableFrog = true,
                  enableFrog = false,

                  junk;
  public static final String FILTER_ID = Parameters.filter;
  public static final String CONSISTENCY = Parameters.clevel;

  // ########################################################################
  // ########################################################################


  // some locations
  public static final String TMPFI           = "/tmp/fi/";

  // dirs
  public static final String CASS_STORAGE_DIR      = TMPFI + "cassandra/";
  public static final String FAIL_HISTORY_DIR      = TMPFI + "failHistory/";
  public static final String RPC_FILES_DIR         = TMPFI + "rpcFiles/";
  public static final String FLAGS_FAILURE_DIR     = TMPFI + "flagsFailure/";
  public static final String EXP_RESULT_DIR        = TMPFI + "expResult/";
  public static final String SOCKET_HISTORY_DIR    = TMPFI + "socketHistory/";
  public static final String CASS_LOGS_DIR         = TMPFI + "logs/";
  public static final String CASS_PIDS_DIR         = TMPFI + "pids/";

  //jinsu for net stuff
  public static final String IP_HISTORY_DIR        = TMPFI + "ipHistory/";
  public static final String TOKENS_DIR        = TMPFI + "tokens/";
  public static final String EXP_PROP_DIR = TMPFI + "expProp/";

  public static final String COVERAGE_COMPLETE_DIR = TMPFI + "coverageComplete/";
  public static final String COVERAGE_STATIC_DIR   = TMPFI + "coverageStatic/";

  // files and flags
  public static final String FROG_OUTPUT_FILE     = TMPFI + "frogOutput.txt";
  public static final String RESET_FROG_FLAG      = TMPFI + "resetFrogFlag";
  public static final String ENABLE_FAILURE_FLAG  = TMPFI + "enableFailureFlag";
  public static final String ENABLE_FROG_FLAG     = TMPFI + "enableFrogFlag";
  public static final String CLIENT_OPTIMIZE_FLAG = TMPFI + "clientOptimizeFlag";
  public static final String ENABLE_COVERAGE_FLAG = TMPFI + "enableCoverageFlag";

  public static final String NODES_CONNECTED_FLAG = TMPFI + "nodesConnectedFlag";

  //public static final String EXPERIMENT_RUN_FLAG  = TMPFI + "experimentRunningblah";
  public static final String EXPERIMENT_RUN_FLAG  = TMPFI + "experimentRunning";
  public static final String NODE_REBOOTING_FLAG = TMPFI + "nodesRebootingFlag";

  //flag for repair workload
  public static final String READ_REPAIR_FLAG = TMPFI + "readRepairFlag";


  // vars
  private static Utility u;
  private static Cass cass;
  private static int expNum = 1;
  private static int wipedOutNum = 1; // # of wiped out experiments

  public Driver() {

    u = new Utility();
    u.setupPrintStream("/tmp/workloadOut.txt");

    // don't forget to run make kill
    printReminder();

    // connect to hdfs ... workload can use this
    cass = new Cass(this);

  }

  // *******************************************
  // 1. prepare everthing
  // 2. decide if we want to enable failure or not
  // 3. run the recursive fsn ...
  // 4. final message or check
  // *******************************************
  public void run() {
    // a big preparation state before we
    // run a set of long experiments
    setUpBeforeEverything();

    // begin recursive fsn
    recursiveFsn(1);


    // final message
    setUpAfterEverything();
  }


  // *******************************************
  // prepare general stuffs .. this takes a while
  // *******************************************
  public void setUpBeforeEverything() {

    createAllDirectories();

    // 0 ... disable failure manager
    disableCoverage();
    disableFailureManager();
    disableClientOptimizer();
    disableFrog();

    // 1. make sure current working directory is correct
    printCwd();


    // ------------------------------ clean up stuffs

    // 2. kill any Cassandra processes (this not always work)
    // so that's why we call "make kill" before running this app
    killCass();

    // clear all
    rmPids();
    rmLogs();
    rmImages();
    rmExpResult();    // .. remove previous experiment result
    rmSocketHistory();    // .. remove previous experiment result
    rmRpcFiles();    // rm all rpc files
    rmCoverageFiles();
    rmFmStat();
    rmFlags();
    //JINSU
    rmIpHistory();
    rmTokens();
    rmExpProp();

    // 5. clear any failure flags
    clearAllFlagsFailure();

    // 6. clear all fail history (the directory that contains
    //    failure hash files
    clearAllFailHistory();

    // ------------------------------ setup stuffs
    // 7. write Experiment configurations/properties to a file.
    recordExpProp();

    // 9. start the failure manager
    startFailureManager();

    //10. start the Cassandra server
    startCass();

    // record max fsn
    recordMaxFsn();

  }

  // *******************************************
  // final
  // *******************************************
  public void setUpAfterEverything() {

    printAllExperimentsFinish();

  }



  // ########################################################################
  // ########################################################################
  // ##                                                                    ##
  // ##                       C O R E    L O G I C                         ##
  // ##                                                                    ##
  // ########################################################################
  // ########################################################################



  // *******************
  // the recursive logic of multiple failures ... not documented right now

  // - unlockFsn: we want to unlock the failure sequence number for this
  //   fsn so we can insert failures
  // - clearFailHistoryOfPostFsns: if we are at failures A-C-?
  //   we want to exercise A-C-D, A-C-E ... but D and E at fsn-3 might
  //   have been exercised before (e.g. A-B-D, A-B-E). so we want to
  //   clear all post fsns so that we can try D and E again at fsn-3.
  // - if (fsn != MAX_FSN) recursiveFsn(fsn + 1)
  //   we only want to run the workload if we get to final fsn .. because
  //   we are indeed trying MAX_FSN of failures.
  // - prepareToRunWorkload .. see there
  // - currentFhf and lastFhf: check if we're at fsn-X, do we see
  //   new failures or not ... if not, break .. so we go back to fsn-X-1
  //   and recursive since then
  // - lock pre-fsns ... say if we are at failures A-B-C, basically
  //   what we want to say is in the next experiment we want to
  //   exercise A-B-D .. so it means we must lock fsn-1 and fsn-2
  //   such that they will go fail A and B
  // *******************************************
  public void recursiveFsn(int fsn) {

    String lastFhf = "LAST";


    // keep doing until there is no new failure for this
    // failure sequence number
    while(true) {

      // see if we can break
      if (expNum > BREAK_EXP_NUMBER)
        break;

      //u.print("\n\n------------------------ fsn-%d \n\n", fsn));

      // ...
      unlockFsn(fsn);
      clearFailHistoryOfPostFsns(fsn);

      // ...
      if (fsn != MAX_FSN) {
        recursiveFsn(fsn+1);
      }
      else {
        prepareToRunWorkload();
      }

      // should we move on ?
      lastFhf = compareAndGetNewFhf(lastFhf, fsn);
      if (lastFhf == null) {

        // haryadi, keep continue on forever
        if(!TESTING) {
        	break;
        }
      }

      // lock pre fsns ..
      lockPreFsns(fsn);

    }
  }


  // *******************************************
  // if current is a new one, returns current
  // else, return null
  // *******************************************
  public String compareAndGetNewFhf(String lastFhf, int fsn) {

    String currentFhf = getLatestFhf(fsn);

    // no failure condition
    if (currentFhf == null)
      return null;

    // last failure is the same as current failure
    // u.print(String.format("- Comparing fsn-%d  last=%s  current=%s \n",
    // fsn, currentFhf, lastFhf));
    if (lastFhf.equals(currentFhf))
      return null;

    // return the new fhf
    return currentFhf;

  }


  // *******************************************
  public void prepareToRunWorkload() {

    // create experiment
    Experiment exp = new Experiment(this, expNum);
    exp.printBegin();

    // prepare to run workload
    setupBeforeEachWorkload(exp);

    // run the actual workload
    runWorkload(exp);

    // setup after
    setupAfterEachWorkload(exp);

  }

  // *******************************************
  public void setupBeforeEachWorkload(Experiment exp) {

    u.print("- Prepare before each workload ... \n");

    recordCurrentExpNumber(exp.getExpNum());
    clearAllInjectedFsn();
    clearAllBadDiskFlags();



    u.print("- check dead nodes (1) ...\n");
    checkDeadNodes();


		restartDeadDataNodes();

    u.print("- check dead nodes (2) ...\n");
    checkDeadNodes();

    resetFrog();

    // must be after restarting dead datanodes !!
    rmAllBlocks();
  }


  // *******************************************
  public void setupAfterEachWorkload(Experiment exp) {

    // done
    exp.printEnd();


    // check if experiment sees full max_fsn or not
    // if not, then we have seen tested this experiment
    // before in lesser max_fsn, so we'll just
    // delete this experiment folder and does not
    // increment the experiment number

    // it doesn't hurt if we print the fail history and
    // check the fail experiment even though the experiment
    // is wiped out

    // this is a good expeirment that reach

    // now check if this experiment fails
    exp.checkFailExperiment();

    exp.printFailHistorySummary();
    if (debug) {
      exp.printFailHistory();
    }

    // this experiment does not reach the max fsn
    if (!exp.reachMaxFsn()) {

      // must be after checking max fsn ...
      // just delete the content of the experiment dir
      if (!debug) {
        exp.rmExpDirContent();
      }

      exp.wipeOutThisExperiment();
    }
    else {

      // delete the dir content if not fail
      if (!exp.isFail() && !debug) {
        exp.rmExpDirContent();
      }
    }
		  rmSocketHistory();

		if(BREAK_EXP_NUMBER != exp.getExpNum()) {

			if(TESTING) {
				//waiting until all the lagging message queries are completed.
				//u.print("waiting some time til other nodes process the messages already delivered to them...\n");
				//u.sleep(5000);

				killNode("node1");//just kills the node using kill command
				// jinsu : then I need to remove the data because we want the node to start at a clean state
				// sometimes if data isn't deleted before the node is restarted, its commitLog gets corrupted.
				//u.deleteDir(CASS_STORAGE_DIR + "node1");
				//killNode("node2");
				//u.deleteDir(CASS_STORAGE_DIR + "node2");
				u.sleep(60000);
				u.print("node1 has been dead and we waited 60 seconds!\n");
				System.exit(0);
				checkDeadNodes();
				restartDeadDataNodes();
			} else {
				//For Insert Workload I don't need to do anything ...

				//rmPids();
				//rmLogs();
				rmImages();
				rmRpcFiles();
				//rmIpHistory();

			}

		//u.mkDir(CASS_STORAGE_DIR);
		}
		//u.println("checking DeadNodes after worklaod");
		//checkDeadNodes();
    // increment the experiment number
    incrementExpNum();

    // and remove all ports otherwise the directory gets too big !!!
    // and it could contain thousands of files



  }


  // *******************************************
  // check the algorithm below
  // *******************************************
  public void runWorkload(Experiment exp) {

    // the experiment runs here ...
    // here, we can write whatever experiments we want
    // so now, I've created the workload client write class
    // which will run the client write workloads

    //remove this later to enable insert workload.
    //ClientInsertWorkload ciw = new ClientInsertWorkload(this, exp);
    //ciw.run();
    selectWorkloadToRun(FILTER_ID, exp);

    //ClientReadRepairWorkload crrw = new ClientReadRepairWorkload(this, exp);
    //crrw.run();
  }

  //JINSU : for configurable experiments
  public void selectWorkloadToRun(String filter, Experiment exp) {
      String delimiter = "\\d";
      String[] temp;
      temp = filter.split(delimiter);
      String experiment_name = temp[0];
      //System.out.println("######## experiment name = " + experiment_name);
      if(experiment_name.equalsIgnoreCase("readrepair")) {
          ClientReadRepairWorkload crrw = new ClientReadRepairWorkload(this, exp);
          crrw.run();
      } else if(experiment_name.equalsIgnoreCase("insertion")) {
          ClientInsertWorkload ciw = new ClientInsertWorkload(this, exp);
          ciw.run();
      } else { //default case is read repair workload
          ClientReadRepairWorkload crrw = new ClientReadRepairWorkload(this, exp);
          crrw.run();
      }
  }

  // *******************************************
  // unlock this fsn so the hash for this fsn is free
  // unlock this and aosl delete the fsn hash file
  // *******************************************
  public void unlockFsn(int fsn) {

    u.print("- Unlocking fsn-" + fsn + "\n");

    File f = getFsnLockFile(fsn);
    u.deleteFile(f);

    String hashPrefix = "hash-for-fsn-" + fsn;
    u.deleteDirContent(FLAGS_FAILURE_DIR, hashPrefix);
  }

  // *******************************************
  // this is optional if we want to repeat a failure
  // but in difference zone ... try to disable this
  // and see what will happen
  // *******************************************
  public void clearFailHistoryOfPostFsns(int currentFsn) {
    u.print(String.format
            ("- Clearing fail history of post fsns %d - %d \n" ,
             currentFsn+1, MAX_FSN));
    for (int i = currentFsn+1; i <= MAX_FSN; i++) {
      clearFailHistory(i);
    }
  }

  // *******************************************
  // .../failHistory/fsn-1/
  // if we clear fail history we must also clear the latest history
  // of this fsn
  // ../failHistory/latest-for-fsn-1
  // *******************************************
  public void clearFailHistory(int fsn) {
    u.print("- Clearing fail history of fsn-" + fsn + "\n");

    String path = String.format("%s/fsn-%d",
                                FAIL_HISTORY_DIR, fsn);
    if (!u.deleteDir(path)) {
      u.ERROR("Can't delete " + path);
    }

    File f = getLatestHistoryFile(fsn);
    u.deleteFile(f);

  }


  // *******************************************
  // .../flagsFailure/injected-fsn-*
  // *******************************************
  public void clearAllInjectedFsn() {
    u.print("- Clearing all injected fsns ...\n");
    u.deleteDirContent(FLAGS_FAILURE_DIR, "injected-fsn-");
  }

  // *******************************************
  // .../flagsFailure/BadDisk_..
  // *******************************************
  public void clearAllBadDiskFlags() {
    u.print("- Clearing all injected fsns ...\n");
    u.deleteDirContent(FLAGS_FAILURE_DIR, "BadDisk");
  }


  // *******************************************
  // check if pre fsns are not locked then we want to
  // lock them
  // *******************************************
  public void lockPreFsns(int currentFsn) {
    u.print(String.format("- Locking pre fsns %d - %d \n", 1, currentFsn));
    for (int i = 1; i < currentFsn; i++) {
      lockFsn(i);
    }
  }

  // *******************************************
  // lockFsn comprises of adding the flag,
  // and also adding the latest failure hash id
  // *******************************************
  public void lockFsn(int fsn) {
    u.print("- Locking fsn-" + fsn + "\n");

    File f = getFsnLockFile(fsn);
    u.createNewFile(f);

    String latestFhf = getLatestFhf(fsn);
    if (latestFhf == null) {
      u.FATAL("lockFsn logic error");
    }

    f = getFsnAndHashFile(fsn, latestFhf);
    u.createNewFile(f);
  }


  // *******************************************
  private void recordCurrentExpNumber(int expNum) {
    String path = FLAGS_FAILURE_DIR + "/currentExpNumber";
    String tmp = String.format("%d", expNum);
    u.stringToFileContent(tmp, path);
  }




  // ########################################################################
  // ########################################################################
  // ##                                                                    ##
  // ##                        U T I L I T Y                               ##
  // ##                                                                    ##
  // ########################################################################
  // ########################################################################


  // *******************************************
  private static void incrementExpNum() {
    expNum++;
  }

  // *******************************************
  public static int getWipedOutNum() {
    return wipedOutNum;
  }

  // *******************************************
  public static void incrementWipedOutNum() {
    wipedOutNum++;
  }

  // ********************************************
  // filename: locked-fsn-#
  // ********************************************
  private File getFsnLockFile(int fsn) {
    String path = String.format("%s/locked-fsn-%d",
                                FLAGS_FAILURE_DIR,
                                fsn);
    File f = new File(path);
    return f;
  }

  // ********************************************
  // return the latest history file for this fsn
  private File getLatestHistoryFile(int fsn) {
    String path = String.format("%s/latest-for-fsn-%d", FAIL_HISTORY_DIR, fsn);
    File f = new File(path);
    return f;
  }


  // ***************************************************
  public String getLatestFhf(int fsn) {

    File f = getLatestHistoryFile(fsn);
    if (!f.exists()) {
      return null;
    }
    String tmp = u.fileContentToString(f);
    if (tmp == null)
      return tmp;
    tmp = tmp.replaceAll("\n", "");
    return tmp;
  }


  // ********************************************
  // filename: hash-for-fsn-%d-is-
  // ********************************************
  private File getFsnAndHashFile(int fsn, String hash) {
    String path = String.format("%s/hash-for-fsn-%d-is-h%s.txt",
                                FLAGS_FAILURE_DIR,
                                fsn, hash);
    File f = new File(path);
    return f;
  }

  public void printCwd() {
    String curDir = System.getProperty("user.dir");
    u.println(String.format("- Current directory is " + curDir));
  }


  // *******************************************
  public void rmCoverageFiles() {
    u.print("- Removing Coverage files ...\n");
    if (!u.deleteDirContent(COVERAGE_COMPLETE_DIR)) {
      u.ERROR("Can't delete " + COVERAGE_COMPLETE_DIR);
    }
    if (!u.deleteDirContent(COVERAGE_STATIC_DIR)) {
      u.ERROR("Can't delete " + COVERAGE_COMPLETE_DIR);
    }
  }


  // *******************************************
  // remove all files recursively starting from CASS_STORAGE_DIR
  // *******************************************
  public void rmImages() {
    u.print("- Removing images ...\n");
    if (!u.deleteDir(CASS_STORAGE_DIR)) {
      u.ERROR("Can't delete " + CASS_STORAGE_DIR);
    }
  }

  // *******************************************
  // remove all files recursively
  // *******************************************
  public void rmExpResult() {
    u.print("- Removing previous experiment results ...\n");
    if (!u.deleteDirContent(EXP_RESULT_DIR)) {
      u.ERROR("Can't delete " + EXP_RESULT_DIR);
    }
  }

  // *******************************************
  // remove sockethistory
  // *******************************************
  public void rmSocketHistory() {
    u.print("- Removing socket history ...\n");
    if (!u.deleteDirContent(SOCKET_HISTORY_DIR)) {
      u.ERROR("Can't delete " + SOCKET_HISTORY_DIR);
    }
  }

  // *******************************************
  // remove rpc files
  // *******************************************
  public void rmRpcFiles() {
    u.print("- Removing RPC files ...\n");
    if (!u.deleteDirContent(RPC_FILES_DIR)) {
      u.ERROR("Can't delete " + RPC_FILES_DIR);
    }
  }



  // *******************************************
  // clear failure flags flags
  // *******************************************
  public void clearAllFlagsFailure() {
    u.print("- Clearing all failure flags ...\n");
    if (!u.deleteDirContent(FLAGS_FAILURE_DIR)) {
      u.ERROR("Can't delete " + FLAGS_FAILURE_DIR);
    }
  }

  // *******************************************
  // reset frog
  // this is a stupid but fast way to reset frog
  // just create a new flag /tmp
  // *******************************************
  public void resetFrog() {
    u.print("- Resetting frog ...\n");
    File f = new File(RESET_FROG_FLAG);
    try {
      f.createNewFile();
    } catch (Exception e) {
      u.ERROR("can't create " + f.getAbsolutePath());
    }

  }


  // *******************************************
  // remove all files inside the FAIL_HISTORY_DIR dir
  // *******************************************
  public void clearAllFailHistory() {
    u.print("- Removing all fail history ...\n");
    if (!u.deleteDirContent(FAIL_HISTORY_DIR)) {
      u.ERROR("Can't delete " + FAIL_HISTORY_DIR);
    }
  }


  // *******************************************
  // rm all logs file ..
  // *******************************************
  public void rmLogs() {
    u.print("- Removing logs ...\n");
    if (!u.deleteDirContent(CASS_LOGS_DIR)) {
      u.ERROR("Can't delete " + CASS_LOGS_DIR);
    }
  }


  // *******************************************
  // rm all pid file ..
  // *******************************************
  public void rmPids() {
    u.print("- Removing logs ...\n");
    if (!u.deleteDirContent(CASS_PIDS_DIR)) {
      u.ERROR("Can't delete " + CASS_PIDS_DIR);
    }
  }

  // *******************************************
  // rm all ipHistory file ..
  // *******************************************
  public void rmIpHistory() {
    u.print("- Removing ipHistory ...\n");
    if (!u.deleteDirContent(IP_HISTORY_DIR)) {
      u.ERROR("Can't delete " + IP_HISTORY_DIR);
    }
  }

  // *******************************************
  // rm all tokens file ..
  // *******************************************
  public void rmTokens() {
    u.print("- Removing Tokens ...\n");
    if (!u.deleteDirContent(TOKENS_DIR)) {
      u.ERROR("Can't delete " + TOKENS_DIR);
    }
  }

  public void rmExpProp() {
    u.print("- Removing ExpProps ...\n");
    if (!u.deleteDirContent(EXP_PROP_DIR)) {
      u.ERROR("Can't delete " + EXP_PROP_DIR);
    }

  }

  // *******************************************
  // start failure manager
  // *******************************************
  public void startFailureManager() {

    u.print("- Starting Failure Manager ...\n");
    String cmdout = u.runCommand("bin/cfi");

    u.print(cmdout);
    u.print("\n\n");
  }

  // *******************************************
  // record max fsn, so the fm knows the max fsn
  // *******************************************
  public void recordMaxFsn() {
    String path = FLAGS_FAILURE_DIR + "/maxFsn";
    String tmp = String.format("%d", MAX_FSN);
    u.stringToFileContent(tmp, path);
  }

  // *******************************************
  // enable failure manager via the fmadmin command
  // see my bin/hadoop to find what this is
  public static void enableFailureManager() {

    if (!Driver.enableFailure)
      return;

    u.print("- Enabling Failure Manager ...\n");
    u.createNewFile(ENABLE_FAILURE_FLAG);
  }


  // *******************************************
  public static void enableFrog() {

    if (!Driver.enableFrog)
      return;

    u.print("- Enabling Frog ...\n");
    u.createNewFile(ENABLE_FROG_FLAG);
  }


  // *******************************************
  public static void disableFrog() {
    u.print("- Disabling Frog ...\n");
    u.deleteFile(ENABLE_FROG_FLAG);
  }


  // *******************************************
  public static void enableCoverage() {

    if (!Driver.enableCoverage)
      return;

    u.print("- Enabling Coverage ...\n");
    u.createNewFile(ENABLE_COVERAGE_FLAG);
  }

  // *******************************************
  public static void disableCoverage() {
    u.print("- Disabling Coverage...\n");
    u.deleteFile(ENABLE_COVERAGE_FLAG);
  }


  // *******************************************
  public static void enableClientOptimizer() {

    if (!Driver.enableOptimizer)
      return;

    u.print("- Optimizing FM Client ...\n");
    u.createNewFile(CLIENT_OPTIMIZE_FLAG);
  }


  // *******************************************
  public static void disableClientOptimizer() {
    u.print("- unOptimizing FM Client ...\n");
    u.deleteFile(CLIENT_OPTIMIZE_FLAG);
  }

  // *******************************************
  public static void disableFailureManager() {
    u.print("- Disabling Failure Manager ...\n");
    u.deleteFile(ENABLE_FAILURE_FLAG);
  }

  // *******************************************
  public void rmFmStat() {
    u.deleteFile("/tmp/fmStat.txt");
  }

  // *******************************************
  public void rmFlags() {
    u.print("- Deleting flags ...\n");
    u.deleteFile(Driver.NODES_CONNECTED_FLAG);
    //u.deleteFile(Driver.EXPERIMENT_RUN_FLAG);
  }


  // *******************************************
  // just call start-cass
  public void startCass() {
    u.print("- Starting Cassandra ...\n");
		u.print(new Date(System.currentTimeMillis()) + " - sC(1) " + u.diff() +"\n");
		String cmdout = u.runCommand("bin/allCnode " + NUM_OF_CASS_NODES);
		u.print(new Date(System.currentTimeMillis()) + " - sC(2) " + u.diff() +"\n");
		u.print(cmdout);
		u.print("\n\n");
		/*
    String cmdout = u.runCommand("bin/cassandra -p 0");
    u.print(cmdout);
    u.print("\n\n");

    for (int i = 1; i < Driver.NUM_OF_CASS_NODES; i++) {
      cmdout = u.runCommand("bin/cnode " + i);
      u.print(cmdout);
      u.print("\n\n");
    }
		*/

    cass.assertConnection();
  }




  // *******************************************
  // killall cass processes (not always work)
  public void killCass() {

    u.print("- Killing Cassandra nodes ...\n");


		//jinsu change made
		cass.client = null;
		u.deleteFile(NODES_CONNECTED_FLAG);

    //u.MESSAGE(" Killing cassandra, FIXME \n");
    NodeProcess[] nps = getNodeProcesses();
    if (nps == null) return;
    for (int i = 0; i < nps.length; i++) {
      String cmd = String.format("kill -s KILL %5s", nps[i].getPid());
      u.print(String.format("   %s, %s \n", cmd, nps[i].getName()));
      String cmdout = u.runCommand(cmd);
    }
    u.print("\n\n");
  }

	// **********************************************
	// jinsu kill one node using kill command.
	// you should and clean up data, pidfile
	public void killNode(String nodeId) {
		NodeProcess[] nps = getNodeProcesses();
		String killPid = "";
		for(NodeProcess node : nps) {
			if(node.getName().equals(nodeId)) {

				killPid = u.getPidFromTmpPid(new File(node.getTmpPidFile()));
				String cmd = String.format("kill -s KILL %5s", killPid);
				String cmdOut = u.runCommand(cmd);
				u.print(cmdOut+"\n");
			}
		}
		if(nodeId.equals("node0")) {
					cass.client = null;
		}
		u.deleteFile(NODES_CONNECTED_FLAG);
	}

  // *******************************************
  // a bit stupid method to find out if a datanode is dead or not
  // just do ps -p pid .. then search if there is the word java in it or not
  public void checkDeadNodes() {
    u.print("- Checking dead nodes ...\n");
    NodeProcess[] nps = getNodeProcesses();
    if (nps == null) return;
    for (int i = 0; i < nps.length; i++) {
      boolean isAlive = u.isPidAlive(nps[i].getPid());
      u.print(String.format("   %-5s  %-15s ", nps[i].getPid(), nps[i].getName()));
      if (isAlive) { u.print("ok   \n"); }
      else         { u.print("DEAD \n"); }
    }
    u.print("\n\n");
  }


  // *******************************************
  // restart dead datanodes ...
  // go through each pid in /tmp/hadoop..pid
  // and find which pid is dead
  // FIXME
  // *******************************************
  public void restartDeadDataNodes() {
    u.print("- Restarting dead nodes ...\n");

    //u.MESSAGE("FIXME: restart dead datanodes \n");

    NodeProcess[] nps = getNodeProcesses();
    if (nps == null) return;
    for (int i = 0; i < nps.length; i++) {
      boolean isAlive = u.isPidAlive(nps[i].getPid());
      if (isAlive)
        continue; // continue if it's alive

      String s = String.format("    Restarting %-15s %s \n",
                               nps[i].getName(), nps[i].getPid());
      u.print(s);

      // before restarting, make sure we remove
      // all stuffs that relate to this dead datanode
      // such as the pid file, and log files

      // first I need to remove the tmp pid file
      u.deleteFile(nps[i].getTmpPidFile());

      // then I need to remove the logs
      rmNodeLogFile(nps[i].getName());

			//JINSU
			//TODO: Sometimes, we need to clear the commitLogs or data in the dead node because commitLogs get corrupted at times.
			//need to call u.deleteDirContent(CASS_STORAGE_DIR+nps[i].getName()) but it shouldn't be called for rebootWorkload.

			u.createNewFile(NODE_REBOOTING_FLAG);
			//u.stringToFileContent(nps[i].get);

      // let's resetart the datanode
      restartNode(nps[i].getName());

      u.deleteFile(NODE_REBOOTING_FLAG);

      // okay so we must wait until that datanode is registered
    	u.print("    Waiting for registration ...\n");
			waitForNodeRegistration(nps[i].getName());
    }

    u.createNewFile(NODES_CONNECTED_FLAG);
    u.sleep(1000);
    u.print("\n\n");
  }



  // *******************************************
  // restart the datanode with dnId
  // in my case I could do this by callng
  // e.g. "pdatanode -3" .. in thanh's case you
  // must set the conf folder properly
  public void restartNode(String nodeName) {
    // or, bin/hd.sh --config conf start pdatanode -1
    String nodeNum = nodeName.replace("node", "");

    if (nodeNum.equals("0")) {
    	cass.client = null; //TODO : generalize this because later on, node0 might not be the only one the client connects to.
      String cmd = String.format("bin/cassandra -p %s \n", nodeNum);
      String cmdout = u.runCommand(cmd);
    } else {
      String cmd = String.format("bin/cnode %s \n", nodeNum);
      String cmdout = u.runCommand(cmd);
    }

  }


  //*********************************************
  // a helper function to get the total number of
  // alive Nodes for waitForNodeRegistration
  //*********************************************
  public int getNumAliveNodes(NodeProcess[] nps) {
    int num = 0;

    for (int i=0; i < nps.length; i++) {
      boolean isAlive = u.isPidAlive(nps[i].getPid());
      if (isAlive) num++;
    }
    return num;
  }

	//JINSU: maybe more general than getNumAliveNodes
	public NodeProcess[] getLiveNodes(NodeProcess[] nps) {
		LinkedList<NodeProcess> liveNps = new LinkedList<NodeProcess>();

    for (int i=0; i < nps.length; i++) {
      boolean isAlive = u.isPidAlive(nps[i].getPid());
      if (isAlive) liveNps.add(nps[i]);
    }
    return (NodeProcess[]) liveNps.toArray(new NodeProcess[liveNps.size()]);
  }

  // *******************************************
  // a stupid but working method to find out if
  // a datanode has been registered or not.
  // we can detect successful registration by checking that
  // "is now part of the cluster." exists in the log file
  // ex. waitForNodeRegistration("node0") => wait for node0 to see other live nodes.
  public void waitForNodeRegistration(String nodeName) {
    //u.print("-wFNR(1)" + u.diff() + "\n");
    int nodeNum = Integer.parseInt(nodeName.replace("node", ""));
    //u.print("-wFNR(2)" + u.diff() + "\n");
    //String cmd = String.format("grep -a %s %s", "cluster", CASS_LOGS_DIR + "node" + nodeNum + ".log");
    String cmd = String.format("grep -a %s %s", "cassReady", CASS_LOGS_DIR + "node" + nodeNum + ".log");
    //cmd[2] = cmd[2].replace("_", " ");
    //u.print("size of the cmd is " + cmd.length + "\n");

    boolean connecting = true;
    String cmdOut = "";
    String pattern = "127.0.0.1";
    NodeProcess[] nps = getNodeProcesses();
    int numAlive = getNumAliveNodes(nps);
    //u.print("-wFNR(3)" + u.diff() + "\n");
    while(connecting) {
      //u.print("-wFNR(4)" + u.diff() + "\n");
      String logFile = getLogFileFromNodeName(nodeName);
      if(logFile != null) {
        //u.print("-wFNR(5)" + u.diff() + "\n");
        cmdOut = u.runCommand(cmd);
        //u.print("cmdOut\n-- " + cmdOut);
        int contains = 0;
        //u.print("-wFNR(6)" + u.diff() + "\n");
        for(int i = 0; i < NUM_OF_CASS_NODES; i++) {
          //u.print("-wFNR(6.5)" + u.diff() + "\n");
          //if (!u.isPidAlive(nps[i].getPid())) {
          //  continue;
          //}
          //u.print("-wFNR(6.6)" + u.diff() + "\n");
          if(i == nodeNum) continue;
          //i == nodeNum, no need to check because i'm checking whether myself is up
          //and we are checking whehter other nodes are up...
          if(i == 0) { //special case for node0...127.0.0.1
            if(cmdOut.contains(pattern)) {
              contains++;
            } else {
              break;
            }
          } else {
            if(cmdOut.contains(pattern + i)) {
              contains++;
            } else {
              break;
            }
          }
        }
        //u.print("-wFNR(7)" + u.diff() + "\n");
        //u.print("wFNR comparison :::: getNumAliveNodes = " + getNumAliveNodes(nps) + "... numAlive = " + numAlive + "... contains = " + contains + "\n");
        if(contains == (numAlive - 1)) {
          //NUM_OF_CASS_NODES - 1 because you don't check yourself
          connecting = false;
        }
        //u.print("-wFNR(8)" + u.diff() + "\n");
      } else {
        u.print(nodeName + " log doesn't exist...\n");
      }
      //u.print(cmdOut + "\n");
      u.print("- (Waking up) Waiting for Node" + nodeNum + " to see all other nodes...\n");
      u.sleep(1000);
    }
    //Checking for Token size to be correct

    //TODO : put this token check back in.
    //Taking it temporarily for easy porting for now
    //Jin-Su Oct/6/2010
    //Jin-Su Nov/22/2010

    String tokenCmd = String.format("grep -a %s %s", "TokenSizeTest", TOKENS_DIR + "nodeToken" + nodeNum);
    //boolean waiting = true;
    while(true) {
      String tokenCmdOut = u.runCommand(tokenCmd);
      u.print(":::: checking token size ::::");
      u.print(tokenCmdOut+"\n");
      //tokenCmdOut.
      if(tokenCmdOut.contains(new Integer(numAlive).toString())) {
        break;
      }
      u.print("- (Waiting for TokenSizeTest)...\n");
      u.sleep(1000);
    }


    u.print("- Node"+nodeNum+" is alive\n");
  }

  // *******************************************
  // rm all log files related to this datanode
  public void rmNodeLogFile(String nodeName) {
    String logFile = getLogFileFromNodeName(nodeName);
    if (logFile != null)
      u.deleteFile(CASS_LOGS_DIR, logFile);
    String outFile = getOutFileFromNodeName(nodeName);
    if (outFile != null)
      u.deleteFile(CASS_LOGS_DIR, outFile);
  }


  // *******************************************
  // get the log file for this datanode
  public String getLogFileFromNodeName(String nodeName) {
    File dir = new File(CASS_LOGS_DIR);
    String[] c = dir.list();
    for (int i=0; i< c.length; i++) {
      if (c[i].contains(nodeName + ".log"))
        return c[i];
    }
    return null;
  }

  // *******************************************
  // get the output file for this datanode
  public String getOutFileFromNodeName(String nodeName) {
    File dir = new File(CASS_LOGS_DIR);
    String[] c = dir.list();
    for (int i=0; i< c.length; i++) {
      if (c[i].contains(nodeName + ".out"))
        return c[i];
    }
    return null;
  }

  // *******************************************
  // get a list of node processes from tmp-pids
  // see the NodeProcess class
  // *******************************************
  public NodeProcess[] getNodeProcesses() {

    LinkedList<NodeProcess> list = new LinkedList<NodeProcess>();

    for (int i=0; i<NUM_OF_CASS_NODES; i++) {
      String tmpPidFile = String.format("%s/node%d.pid", CASS_PIDS_DIR, i);
      String name = String.format("node%d", i);
      File f = new File(tmpPidFile);
      String pid = u.getPidFromTmpPid(f);
      NodeProcess np = new NodeProcess(tmpPidFile, pid, name);
      list.add(np);
    }
    return (NodeProcess[]) list.toArray(new NodeProcess[list.size()]);
  }



  // *******************************************
  // is this a tmp-pid file?
  public boolean isTmpPid(String fname) {
    u.MESSAGE("FIXME isTmpPid \n");
    return false;
    //if (fname.contains(CASS_USERNAME) && fname.contains(".pid"))
    //return true;
    //return false;
  }



  // *******************************************
  // current failure hash file is the most recent
  // fail history (use "ls -t" to grep the file)
  public String getCurrentFailureHashFile() {

    String cmd = String.format("ls -t %s", FAIL_HISTORY_DIR);
    String cmdout = u.runCommand(cmd);

    String [] split = cmdout.split("\n", 2);
    String latest = split[0];
    String dotTxt = ".txt";

    // sanity check
    if (latest.indexOf("h") == 0 &&
        latest.indexOf(dotTxt) == latest.length() - dotTxt.length()) {
      return latest;
    }

    // it's possible that if we don't inject failure
    // there is no has failure, let's just put this to something
    u.WARNING("getLatestHashedFailure returns " +  latest);

    return null;

  }




  // *******************************************
  // IMPORTANT !!!!
  // the reason we want to remove all blocks is because
  // we don't want to have other background traffics
  // this is  a bad hack .. but let's do it for now
  public void rmAllBlocks() {


    u.print("- Removing all blocks ...(OLD HDFS stuff, FIX ME if needed)\n");
  }

  // *******************************************
  Utility getUtility() {
    return u;
  }

  // *******************************************
  Cass getCass() {
    return cass;
  }


  // *******************************************
  // print done ...
  public void printAllExperimentsFinish() {

    String full =
      ("## ################################################# ##\n");
    String side =
      ("##                                                   ##\n");
    String middle = String.format
      ("## A L L    E X P E R I M E N T S    F I N I S H !!! ##\n");

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

  public static void waiting (int n){

    long t0, t1;

    t0 =  System.currentTimeMillis();

    do{
      t1 = System.currentTimeMillis();
    }
    while ((t1 - t0) < (n * 1000));
  }

  // *******************************************
  public void createAllDirectories() {
    u.mkDir(TMPFI);
    u.mkDir(EXP_RESULT_DIR);
    u.mkDir(FAIL_HISTORY_DIR);
    u.mkDir(COVERAGE_COMPLETE_DIR);
    u.mkDir(COVERAGE_STATIC_DIR);
    u.mkDir(FLAGS_FAILURE_DIR);;
    u.mkDir(CASS_LOGS_DIR);
    u.mkDir(RPC_FILES_DIR);
    u.mkDir(SOCKET_HISTORY_DIR);
    u.mkDir(CASS_PIDS_DIR);

    //jinsu for net contextPassing
    u.mkDir(IP_HISTORY_DIR);
    //jinsu for checking tokenSize
    u.mkDir(TOKENS_DIR);
    //jinsu for experiment configurations
    u.mkDir(EXP_PROP_DIR);
  }




  // *******************************************
  public void printReminder() {


    u.print("## ############################################# ## \n");
    u.print("## ############################################# ## \n");
    u.print("##                                               ## \n");
    u.print("##    DON'T FORGET TO RUN: make kill             ## \n");
    u.print("##                                               ## \n");
    u.print("## ############################################# ## \n");
    u.print("## ############################################# ## \n");

    // don't forget to run make kill (just make sure no java process
    // before this)


  }
  public void recordExpProp() {
      //writing filter value to a file.
      recordFilterId();
  }
  // *******************************************
  public void recordFilterId() {
    String fpath = EXP_PROP_DIR + "FILTERID";
    boolean succ = u.stringToFileContent(FILTER_ID, fpath);
    if(!succ) {
        u.ERROR("Can't create " + fpath);
    }
  }
  public static void main(String[] args) {
  }

}
