package org.fi;

import java.io.*;
import java.lang.*;


import java.util.Map;
import java.util.TreeMap;


import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.fi.FMServer.*;
import org.fi.FMJoinPoint.*;

public class Coverage {

  // for raw coverage
  private static SortedMap<String,Integer> map = new TreeMap<String,Integer>();
  private static final String RAW_COVERAGE_FILE = "/tmp/fmRawCoverage.txt";
  private static BufferedWriter out = null;


  // for statistics
  private static final String STAT_FILE = "/tmp/fmStat.txt";
  private static PrintStream statps;

  //********************************************
  public Coverage() { }


  //********************************************
  private static void setup() {

    if (out == null) {
      try {
	out = new BufferedWriter(new FileWriter(RAW_COVERAGE_FILE));

      } catch (IOException e) {
	Util.EXCEPTION(" can't open " + RAW_COVERAGE_FILE, e);
	Util.ERROR    (" can't open " + RAW_COVERAGE_FILE);
      }
    }

    if (statps == null) {
      try {
        FileOutputStream fos = new FileOutputStream(STAT_FILE);
        statps = new PrintStream(fos);
      } catch (Exception e) {
        Util.EXCEPTION(" can't open " + STAT_FILE, e);
      }
    }
  }


  // ********************************************
  private static boolean isEnableCoverageFlagExist() {
    File f = new File(FMLogic.ENABLE_COVERAGE_FLAG);
    if (f.exists())
      return true;
    return false;
  }


  // ********************************************
  // called before do fail
  // ********************************************
  public static void recordBeforeFilter(FMAllContext fac) {



    setup(); // !!!


    String key = String.format("%s:%d",
                               fac.fjp.getFileName(),
                               fac.fjp.getLine());

    if (map.containsKey(key)) {
      // have seen this before, just report once
      return;
    }

    map.put(key, new Integer(0));


    String line =
      String.format("\n") +
      String.format("     [iajc] warning at %s \n", fac.fjp.getJoinPointStr()) +
      String.format("     [iajc] ^^^^^^^^^^    \n") +
      String.format("     [iajc] %s:%d:0:0 warningHooks:javaIO ...\n",
                    fac.fjp.getFileName(),
                    fac.fjp.getLine()) +
      //String.format("   %s \n", fac.fjp.getJoinPointStr());

      //JINSU HACK
      String.format("     [messageType] %s\n", fac.ctx.getMessageType());

      String.format("\n");


    System.out.println(line);

    try {
      out.write(line);
      out.flush();
    } catch (IOException e) {
      Util.EXCEPTION(" can't write " + RAW_COVERAGE_FILE, e);
      Util.ERROR    (" can't write " + RAW_COVERAGE_FILE);
    }


  }


  // ********************************************
  // temporary stuffs
  static FMJoinPoint fjp;
  static String sl;
  static int slHash;
  static String jp;
  static int slJpHash;

  // ********************************************
  private static void prepare(FMAllContext fac, FailType ft) {

    fjp = fac.fjp;
    sl = fjp.getSourceLoc();
    slHash = fjp.getSourceLocHash();
    jp = fjp.getJoinPointStr();
    slJpHash = fjp.getSlJpHash(); // unique of each join point
  }

  // ********************************************
  // This recording records all the failures points
  // that have been filtered, and all the possible failure types
  // Keyword:
  // KeyLoc: source location
  // KeyJp:  hash of source location and join point
  // ********************************************
  public static void recordStatAfterFilter(FMAllContext fac, FailType ft, FIState fis) {

    if (!isEnableCoverageFlagExist())
      return;

    // I always want to do this!!
    recordCompleteHashId(fac, fis);

    recordStaticHashId(fac, fis);


    // just disable this, take too much storage ...
    // recordToTmpFmStat(fac, ft, fis);


  }

  // *************************************************
  public static void recordToTmpFmStat(FMAllContext fac, FailType ft, FIState fis) {

    if (!FMServer.debug)
      return;

    setup(); // !!!

    prepare(fac, ft);
    recordSourceLoc(fac, ft);
    recordJoinPoint(fac, ft);
    recordJoinIot(fac, ft);
    recordFailType(fac, ft);
    recordIoContext(fac, ft);
  }

  // *************************************************
  private static void recordSourceLoc(FMAllContext fac, FailType ft) {
    String buf = String.format("%-10s %12d.sh %s \n", "KeySourceLoc", slHash, sl);
    statps.print(buf);
  }

  // *************************************************
  private static void recordJoinPoint(FMAllContext fac, FailType ft) {
    String buf = String.format("%-10s %12d.sh %s \n", "KeyJoinPoint", slHash, jp);
    statps.print(buf);
  }

  // *************************************************
  private static void recordJoinIot(FMAllContext fac, FailType ft) {
    String key;
    if      (fac.fjp.getJoinIot() == JoinIot.READ)  key = "KeyJoinIotRead";
    else if (fac.fjp.getJoinIot() == JoinIot.WRITE) key = "KeyJoinIotWrite";
    else                                            key = "KeyJoinIotNone";

    String buf = String.format
      ("%-10s %12d.sjh %s %s \n", key, slJpHash, sl, jp);
    statps.print(buf);
  }


  // *************************************************
  private static void recordIoContext(FMAllContext fac, FailType ft) {

    String targetIO = fac.ctx.getTargetIO();

    String key = "IOContext-";

    if      (Util.isNetIO(targetIO))  key += "NetIO-";
    else if (Util.isDiskIO(targetIO)) key += "DiskIO-";
    else                              key += "Unknown-";

    if      (fac.fjp.getJoinIot() == JoinIot.READ)  key += "Read";
    else if (fac.fjp.getJoinIot() == JoinIot.WRITE) key += "Write";
    else                                            key += "None";

    String buf = String.format
      ("%-10s %12d.sjh %s %s \n", key, slJpHash, sl, jp);
    statps.print(buf);

  }


  // *************************************************
  private static void recordFailType(FMAllContext fac, FailType ft) {
    String key = "";

    if      (ft == FailType.CRASH)      key = "KeyFailTypeCrash";
    else if (ft == FailType.BADDISK)    key = "KeyFailTypeBadDisk";
    else if (ft == FailType.EXCEPTION)  key = "KeyFailTypeException";
    else if (ft == FailType.RETFALSE)   key = "KeyFailTypeRetFalse";
    else if (ft == FailType.CORRUPTION) key = "KeyFailTypeCorruption";

    String buf = String.format("%s %12d.sjh %s %s \n", key, slJpHash,
                               sl, jp);
    statps.print(buf);


  }

  // *************************************************
  private static void recordCompleteHashId(FMAllContext fac, FIState fis) {

    File f = getCompleteHashIdFile(fis.getCompleteHashId());
    if (f.exists()) {
      return;
    }

    // just create this first, in case, we're doing crashes
    Util.createNewFile(f);

    String buf = getCompleteHashIdContent(fac, fis);
    Util.stringToFileContent(buf, f);
  }

  // ********************************************
  private static String getCompleteHashIdContent(FMAllContext fac, FIState fis) {

    String buf = "";


    buf += "\n";
    buf += "# ctx / fsj / fst \n";
    buf += "# ------------------------------------------\n";
    buf += fac.ctx + "\n";
    buf += fac.fjp + "\n";
    buf += fac.fst + "\n";
    buf += "\n";

    buf += "\n";
    buf += "# Complete Hash Id Str: \n";
    buf += "# ------------------------------------------\n";
    buf += fis.getCompleteHashIdStr();
    buf += "\n";

    buf += "\n";
    buf += "# Cross-info: \n";
    buf += "# ------------------------------------------\n";
    buf += "\n";

    buf += "The completeHashId is  : [[ " + fis.getCompleteHashId() + " ]] \n";
    buf += "The staticHashId       : [[ " + fis.getStaticHashId()   + " ]] \n";

    return buf;
  }


  // ********************************************
  private static File getCompleteHashIdFile(int hashId) {
    String file = FMLogic.getHashFileName(hashId);
    File f = new File(FMLogic.COVERAGE_COMPLETE_DIR, file);
    return f;
  }




  // *************************************************
  private static void recordStaticHashId(FMAllContext fac, FIState fis) {

    File f = getStaticHashIdFile(fis.getStaticHashId());
    if (f.exists()) {
      return;
    }

    // just create this first, in case, we're doing crashes
    Util.createNewFile(f);

    String buf = getStaticHashIdContent(fac, fis);
    Util.stringToFileContent(buf, f);
  }


  // ********************************************
  private static String getStaticHashIdContent(FMAllContext fac, FIState fis) {

    String buf = "";

    buf += "\n";
    buf += "# fsj / fst \n";
    buf += "# ------------------------------------------\n";
    buf += fac.ctx + "\n";
    buf += fac.fjp + "\n";
    buf += fac.fst + "\n";
    buf += "\n";

    buf += "\n";
    buf += "# Static Hash Id Str: \n";
    buf += "# ------------------------------------------\n";
    buf += fis.getStaticHashIdStr();
    buf += "\n";

    buf += "\n";
    buf += "# Cross-info: \n";
    buf += "# ------------------------------------------\n";
    buf += "\n";

    buf += "The completeHashId is  : [[ " + fis.getCompleteHashId() + " ]] \n";
    buf += "The staticHashId       : [[ " + fis.getStaticHashId()   + " ]] \n";

    return buf;
  }


  // ********************************************
  private static File getStaticHashIdFile(int hashId) {
    String file = FMLogic.getHashFileName(hashId);
    File f = new File(FMLogic.COVERAGE_STATIC_DIR, file);
    return f;
  }





}
