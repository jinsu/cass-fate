
package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.LinkedList;


public class Driver {

  //public static final int MAX_FSN = 3;
  public static final int MAX_FSN = 2;


  // set this to true or false, if you want to enable
  // new failure or not
  public static boolean enableFailure = true;
  // public static boolean enableFailure = false;

  // some locations
  public static String FAIL_HISTORY_DIR = "/tmp/fi/failHistory/";
  public static String FLAGS_FAILURE_DIR = "/tmp/fi/flagsFailure/";
  public static String FIRST_FAILURE_FLAG = "firstFailureInjected";
  public static String EXP_RESULT_DIR = "/tmp/fi/expResult/";

  public static int expNum = 1;


  private int BREAK_EXP_NUMBER = 10000;
  // private int BREAK_EXP_NUMBER = 2;


  Utility u;


  // *******************************************
  // general setup
  public Driver() {
    u = new Utility();
    u.setupPrintStream("/tmp/workloadOut.txt");
  }


  // *******************************************
  public void run() {

    // a big preparation state before we
    // run a set of long experiments
    prepareEverything();

    // begin recursive fsn with 1
    recursiveFsn(1);

    // here, we can write whatever experiments we want
    // so now, I've created the workload client write class
    // which will run the client write workloads
    // Workload w = new Workload(this);
    // w.run();


  }


  // *******************************************
  public void recursiveFsn(int fsn) {

    String lastFhf = "LAST";
    String currentFhf = "CURRENT";

    // clearLatestHistory();


    while(true) {

      if (expNum > BREAK_EXP_NUMBER) break;





      u.print(String.format
              ("\n\n------------------------ fsn-%d -----------------------\n\n", fsn));

      unlockFsn(fsn);
      clearFailHistoryOfPostFsns(fsn);

      if (fsn != MAX_FSN) {
        recursiveFsn(fsn+1);
      }
      else {
        clearAllInjectedFsn();
        runWorkload();
      }
      
      // check if we should proceed or not ....
      currentFhf = getLatestFhf(fsn);

      // no failure condition OR
      // last failure is the same as current failure
      u.print(String.format("- Comparing fsn-%d  last=%s  current=%s \n",
                            fsn, currentFhf, lastFhf));
      if (currentFhf == null ||
          lastFhf.equals(currentFhf))
        break;

      // remember
      lastFhf = currentFhf;

      // lock pre fsns ..
      lockPreFsns(fsn);

    }
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


  // ********************************************
  // filename: hash-for-fsn-%d-is-
  // ********************************************
  private File getFsnAndHashFile(int fsn, String hash) {
    String path = String.format("%s/hash-for-fsn-%d-is-h%s.txt",
                                Driver.FLAGS_FAILURE_DIR,
                                fsn, hash);
    File f = new File(path);
    return f;
  }

  // ***************************************************
  public String getLatestFhf(int fsn) {

    File f = getLatestHistoryFile(fsn);
    if (!f.exists()) {
      return null;
    }

    try {
      BufferedReader in = new BufferedReader(new FileReader(f));
      String str = in.readLine();
      if (str == null) {
        return null;
      }
      in.close();
      return str; // successful one
    } catch (IOException e) {
      u.EXCEPTION("getLatestFhf", e);
      return null;
    }
  }


  // ********************************************
  // return the latest history file for this fsn
  private File getLatestHistoryFile(int fsn) {
    String path = String.format("%s/latest-for-fsn-%d", FAIL_HISTORY_DIR, fsn);
    File f = new File(path);
    return f;
  }


  // *******************************************
  // .../flagsFailure/injected-fsn-*
  // *******************************************
  public void clearAllInjectedFsn() {
    u.print("- Clearing all injected fsns ...\n");
    u.deleteDirContent(FLAGS_FAILURE_DIR, "injected-fsn-");
  }

  // *******************************************
  // .../flagsFailure/latest-for-fsn-*
  // *******************************************
  //public void clearLatestHistory() {
  //u.print("- Clearing all latest history ... \n");
  //u.deleteDirContent(FAIL_HISTORY_DIR, "latest-for-fsn");
  //}

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

  // ********************************************
  // filename: locked-fsn-#
  // ********************************************
  private File getFsnLockFile(int fsn) {
    String path = String.format("%s/locked-fsn-%d",
                                Driver.FLAGS_FAILURE_DIR,
                                fsn);
    File f = new File(path);
    return f;
  }

  // *******************************************
  public void runWorkload() {


    u.print("- Starting workload ... \n");

    Experiment exp = new Experiment(this, expNum++);
    exp.setType("Workload");
    recordCurrentExpNumber(exp.getExpNum());

    // print header
    exp.printBegin();

    // the experiment runs here ...
    FMLogic fml = new FMLogic(this, exp);
    fml.run();

    exp.printEnd();

    u.print("- Ending workload ... \n");

  }

  // *******************************************
  private void recordCurrentExpNumber(int expNum) {
    String path = FLAGS_FAILURE_DIR + "/currentExpNumber";
    String tmp = String.format("%d", expNum);
    u.stringToFileContent(tmp, path);
  }

  // *******************************************
  // prepare general stuffs .. this takes a while
  public void prepareEverything() {
    u.print("- Prepare everything ... \n");
    resetFailureFlags();
    clearAllFailHistory();
    rmExpResult();
  }

  // *******************************************
  // reset failure flags flags
  public void resetFailureFlags() {
    u.print("- Resetting failure flags ...\n");
    if (!u.deleteDirContent(FLAGS_FAILURE_DIR)) {
      u.ERROR("Can't delete " + FLAGS_FAILURE_DIR);
    }
  }


  // *******************************************
  // remove all files recursively
  public void rmExpResult() {
    u.print("- Removing previous experiment results ...\n");
    if (!u.deleteDirContent(EXP_RESULT_DIR)) {
      u.ERROR("Can't delete " + EXP_RESULT_DIR);
    }
  }


  // *******************************************
  // remove all files inside the FAIL_HISTORY_DIR dir
  public void clearAllFailHistory() {
    u.print("- Removing all fail history ...\n");
    if (!u.deleteDirContent(FAIL_HISTORY_DIR)) {
      u.ERROR("Can't delete " + FAIL_HISTORY_DIR);
    }
  }

  // *******************************************
  // print what ever inside the latest hash file
  public void printCurrentFailureHashFile() {
    String latest = getCurrentFailureHashFile();

    u.print(String.format
            ("Latest hashed failure is [[%s]] \n", latest));
    u.print("\n\n");

    if (latest == null)
      return;

    printFailureHashFile(latest);
  }

  // *******************************************
  // print the failure hash file
  public void printFailureHashFile(String fname) {

    File f = new File(FAIL_HISTORY_DIR, fname);

    u.print(String.format("Content of '%s' is: \n\n", fname));
    try {
      BufferedReader in = new BufferedReader(new FileReader(f));
      while (true) {
        String buf = in.readLine();
        if (buf == null)
          break;
        u.print(buf + "\n");
      }
      in.close();
    } catch (Exception e) {
      u.EXCEPTION("printFailureHashFile", e);
    }
    u.print("\n\n");

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
  Utility getUtility() {
    return u;
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


}


