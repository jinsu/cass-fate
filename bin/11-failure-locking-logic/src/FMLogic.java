
package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.LinkedList;



public class FMLogic {

  Driver d;
  Utility u;
  Experiment exp;

  // ********************************************
  public FMLogic(Driver d, Experiment exp) {
    this.d = d;
    this.u = d.getUtility();
    this.exp = exp;
  }


  // ********************************************
  public void run() {

    String [] hashes = { "AA", "BB", "CC", "DD", "EE", "FF", "GG", "HH" };
    //String [] hashes = { "DD", "CC", "BB", "AA" };


    // I'm
    for (int i = 0; i < hashes.length; i++) {

      String curHash = hashes[i];

      // if i have reached max fsn ...
      // just continue ...
      if (hasReachedMaxFsn()) {
        continue;
      }

      // let's get current failure number
      int fsn = getCurrentFsn();


      // check the logic
      if (!shouldFail(fsn, curHash))
        continue;

      recordInjectedFsn(fsn);
      recordFailHistory(fsn, curHash);
      recordLatestHistory(fsn, curHash);

      doFail(fsn, curHash);

      recordFailureToExperiment(fsn, curHash);


    }

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
  private boolean shouldFail(int fsn, String hash) {

    String tmp = String.format("fsn-%d hash-%s", fsn, hash);

    boolean pass;
    if (isFsnLocked(fsn)) {
      if (isFsnAndHashMatched(fsn, hash)) {
        u.print("- shouldFail case 1: " + tmp + "\n");
        pass = true;
      }
      else {
        u.print("- shouldFail case 2: " + tmp + "\n");
        pass = false;
      }
    }
    else {
      if (isInFailHistory(fsn, hash)) {
        u.print("- shouldFail case 3: " + tmp + "\n");
        pass = false;
      }
      else  {
        u.print("- shouldFail case 4: " + tmp + "\n");
        pass = true;
      }
    }
    return pass;
  }

  // ********************************************
  //
  // ********************************************
  private void recordFailHistory(int fsn, String hash) {
    File f = getFailHistoryFile(fsn, hash);
    if (f.exists()) {
      // it's okay that a fail history already there
      // for example, in the case where fail history is locked ..
      return;
    }

    u.createNewFile(f);
    // try {
    // if (!f.createNewFile()) {
    // u.ERROR("Can't create " + f);
    // }
    // } catch(Exception e) {
    // u.EXCEPTION("cant' create " + f, e);
    // }
  }


  // ********************************************
  // we must do this because "ls -t" is not precise
  // ********************************************
  private void recordLatestHistory(int fsn, String hash) {
    u.print("- Recording latest history fsn-" + fsn + " hash-" + hash + "\n");
    File f = getLatestHistoryFile(fsn);
    try {
      FileWriter fw = new FileWriter(f);
      fw.write(hash + "\n");
      fw.close();
    } catch (IOException e) { 
      u.EXCEPTION("weird", e);
      u.ERROR("recordLatestHistory should not happen here");
    }
  }


  // ********************************************
  // return the latest history file for this fsn
  private File getLatestHistoryFile(int fsn) {
    String path = String.format("%s/latest-for-fsn-%d", Driver.FAIL_HISTORY_DIR, fsn);
    File f = new File(path);
    return f;
  }

  // ********************************************
  //
  // ********************************************
  private boolean isInFailHistory(int fsn, String hash) {
    File f = getFailHistoryFile(fsn, hash);
    if (f.exists())
      return true;
    return false;
  }


  // ********************************************
  // fail history file: .../failHistory/fsn-1/h-d989.txt
  // ********************************************
  private File getFailHistoryFile(int fsn, String hash) {

    String dir = String.format("%s/fsn-%d",
                               Driver.FAIL_HISTORY_DIR, fsn);
    File d = new File(dir);
    if (!d.exists()) {
      u.mkDir(d);
      //try { d.mkdir(); }
      //catch (Exception e) { u.EXCEPTION("can't mkdir " + d, e); }
    }

    String file = getHashFileName(hash);
    File f = new File(d, file);
    return f;
  }


  // ********************************************
  private String getHashFileName(String hash) {
    return String.format("h%s.txt", hash);
  }



  // ********************************************
  //
  // ********************************************
  private boolean isFsnAndHashMatched(int fsn, String hash) {
    File f = getFsnAndHashFile(fsn, hash);
    if (f.exists())
      return true;
    return false;
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

  // ********************************************
  // if fsn is locked it means this fsn has a
  // specific hash that we must follow
  // ********************************************
  private boolean isFsnLocked(int fsn) {
    File f = getFsnLockFile(fsn);
    if (f.exists())
      return true;
    return false;
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

  // ********************************************
  // This algorithm is easy ... we're just failing
  // whatever we have
  // ********************************************
  private boolean hasReachedMaxFsn() {
    int maxFsn = Driver.MAX_FSN;
    if (isFsnInjected(maxFsn))
      return true;
    return false;
  }

  // ********************************************
  // get current fsn
  // ********************************************
  private int getCurrentFsn() {
    int fsn = 1;
    while (true) {
      if (!isFsnInjected(fsn))
        return fsn;
      fsn++;
    }
  }

  // *******************************************
  boolean isFsnInjected(int fsn) {
    File f = getInjectedFsnFile(fsn);
    if (f.exists())
      return true;
    return false;
  }

  // *******************************************
  void recordInjectedFsn(int fsn) {
    File f = getInjectedFsnFile(fsn);
    u.createNewFile(f);
    // try {  f.createNewFile();
    // } catch(Exception e) { u.EXCEPTION("can't create " + f, e); }
  }

  // *******************************************
  File getInjectedFsnFile(int fsn) {
    String path = String.format("%s/injected-fsn-%d",
                                Driver.FLAGS_FAILURE_DIR, fsn);
    File f = new File(path);
    return f;
  }

  // ********************************************
  private void doFail(int fsn, String hash) {
    // System.out.format("    [fsn#%d][%s] \n", fsn, hash);
    u.print(String.format("    exp-%02d [fsn#%d][%s] \n", exp.getExpNum(), fsn, hash));

    // just to see 
    exp.addFailure(hash);
    

  }


  // ********************************************
  // ln -s  failHistory/fsn-1/h.txt  expResult/exp-001/h.txt
  private void recordFailureToExperiment(int fsn, String hash) { 
    String path = Driver.FLAGS_FAILURE_DIR + "/currentExpNumber";
    String expNumStr = u.fileContentToString(path);
    if (expNumStr.equals("")) {
      u.FATAL("can't read current exp number");
    }
    expNumStr = expNumStr.replaceAll("\n", "");
    Integer expNum = new Integer(expNumStr);
    
    String expNumDirName = getExpNumDirName(expNum.intValue());
    u.mkDir(expNumDirName);
    
    path = String.format("%s/fsn%d-%s",
			 expNumDirName,
			 fsn, getHashFileName(hash));
    u.stringToFileContent(hash, path);

    
  }

  // ********************************************
  private String getExpNumDirName(int expNum) {
    return String.format("%s/exp-%04d", Driver.EXP_RESULT_DIR, expNum);
  }

}

