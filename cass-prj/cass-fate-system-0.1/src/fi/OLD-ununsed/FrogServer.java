
package org.fi;

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

import org.fi.*;
import java.io.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


public class FrogServer {

  public static final int JOL_LOAD_PORT = 6501;
  private JolSystem system;
  private static String fnAd = FrogServer.getAddress();

  private int violationId = 0;
  
  public final static String sdir 
    = FMLogic.HADOOP_STORAGE_DIR + "dfs/name1/current/";
  
  public final static String imageFile     = sdir + "fsimage";
  public final static String editsFile     = sdir + "edits";
  public final static String editsNewFile  = sdir + "edits.new";
  public final static String imageCkptFile = sdir + "fsimage.ckpt";


  public final static String frogOutputFile = FMLogic.TMPFI + "frogOutput.txt";
  
  public final static String RESET_FROG_FLAG_FILE = FMLogic.TMPFI + "resetFrogFlag";

  private static PrintStream ps;


  BufferedReader stdin = new BufferedReader
    (new InputStreamReader(System.in));

  // ##################################################################
  // ##################################################################
  // ##                                                              ##
  // ##                S E T U P  S T U F F                          ##
  // ##                                                              ##
  // ##################################################################
  // ##################################################################

  // *********************************
  public FrogServer() {

    try {

      // startup core stuffs
      this.system = Runtime.create(Runtime.DEBUG_WATCH, System.out, JOL_LOAD_PORT);





      install("model.olg");
      install("spec.olg");
      this.system.start();


      // setup print stream
      resetFrog();


      // print initial
      PrintTable.start(system);

      // testManual();

      Thread.sleep(500);
      Util.MESSAGE(ps, "FrogServer is ready (after sleeping for 0.5 second) !!!");

    } catch (Exception e) {
      Util.EXCEPTION(ps, "FrogServer.new()", e);
      System.exit(0);
    }

  }



  // *********************************
  public void processFrogEvent(FMJoinPoint fjp, FMStackTrace fst, FrogEvent fev) {

    // processFrogEvent is the only main entrance to the
    // JOL spec, so we reset everything here
    checkResetFrog();


    printProcessHeader(fev);


    Util.scheduleEvent(fjp, fst, ps, system, fev.pn, fev.tn, fev.tuple);

    Util.scheduleDirectEvent(ps, system, "spec", "manual_eval", Util.r());

    // then I need to print all tables here
    PrintTable.printAllTables();


    checkAllViolations(fjp, fst, fev);
  }


  // **************************************
  public void printProcessHeader(FrogEvent fev) {

    String buf = String.format("# [new frog event][" + fev + "] \n");

    Util.print(ps, "\n");
    Util.print(ps, "\n");
    Util.print(ps, "# ########################################################\n");
    Util.print(ps, "# ########################################################\n");
    Util.print(ps, buf);
    Util.print(ps, "# ########################################################\n");
    Util.print(ps, "# ########################################################\n");
    Util.print(ps, "\n");
    Util.print(ps, "\n");

  }


  // *********************************
  // This is a stupid but fast way to check if we need to
  // reset frog or not.  The "good" way is to use FMAdmin
  // so that we can send a reset command from the command line
  // but the problem is that, if we want to do that we must
  // use bin/hadoop FMAdmin which is slow and must use RPC
  // to the FI server. So let's just use file existence to do
  // this
  private void checkResetFrog() {
    File f = new File(RESET_FROG_FLAG_FILE);
    if (!f.exists())
      return;

    // if this flag exists, then someone has told me to
    resetFrog();

    // delete the file
    try { f.delete(); } catch (Exception e) {
      Util.ERROR(ps, "problem in reset frog");
    }

  }

  // *********************************
  private void resetFrog() {
    // clear some stuffs up, so let's do that
    resetPrintStream();

    // clear frog tables
    resetFrogTables();

    // then print let's see what we have
    PrintTable.printAllTables();
  }



  // *********************************
  private void resetPrintStream() {

    try {

      if (ps != null) {
        ps.flush();
        ps.close();
      }

      FileOutputStream fos = new FileOutputStream(frogOutputFile);
      ps = new PrintStream(fos);

      Util.MESSAGE(ps, "new frog output");


    } catch (Exception e) {
      Util.EXCEPTION("Can't open " + frogOutputFile, e);
      System.exit(-1);
    }
  }


  // *********************************
  private void resetFrogTables() {

    // delete all ...
    Util.scheduleDirectEvent(ps, system, "model", "reset", Util.r4());

    // then run init again ...
    Util.scheduleDirectEvent(ps, system, "spec", "spec_init", "Fn");

  }

  // *********************************
  public static PrintStream getPs() {
    return ps;
  }

  // *********************************
  private void install(String olgPath) throws JolRuntimeException {
    this.system.install("model", ClassLoader.getSystemResource(olgPath));
    this.system.evaluate();
  }


  // *********************************
  private static String getAddress() {
    return "tcp:localhost:" + JOL_LOAD_PORT;
  }

  // *********************************
  private void checkAllViolations(FMJoinPoint fjp, FMStackTrace fst, FrogEvent fev) {


    // manual
    checkLostState(fjp, fst, fev);

    // automatic (per table in spec)
    checkViolation(fjp, fst, fev, "spec", "failover_violation");

  }

  // *********************************
  // check violation automatically, given progName pn and tableName tn
  private void checkViolation(FMJoinPoint fjp, FMStackTrace fst,
                              FrogEvent fev, String pn, String tn) {

    SortedMap<String,String> sm_ss = null;


    // if table is empty, no violation, return
    if (Util.isTableEmpty(system, pn, tn))
      return;

    // check each table that represents violation
    if (tn.equals("failover_violation"))
      sm_ss = PrintTable.getTableMap(pn, tn);

    if (sm_ss == null)
      return;

    reportViolation(fjp, fst, fev, tn, sm_ss);
  }


  // *********************************
  // check consistency manually here
  private void checkLostState(FMJoinPoint fjp, FMStackTrace fst, FrogEvent fev) {

    // let's pull server state first
    Table tbl;
    SortedMap<String,String> server_state = new TreeMap<String, String>();
    SortedMap<String,String> user_state = new TreeMap<String, String>();
    SortedMap<String,String> lost_state = new TreeMap<String, String>();


    tbl = Util.getTable(system, "spec", "server_state");
    for (Tuple t : tbl.tuples())
      server_state.put((String) t.value(0), (String) t.value(1));

    tbl = Util.getTable(system, "model", "user_state");
    for (Tuple t : tbl.tuples())
      user_state.put((String) t.value(0), (String) "");


    Set s = user_state.entrySet();
    Iterator itr = s.iterator();
    while(itr.hasNext()) {
      String path = (String) ((Map.Entry)itr.next()).getKey();
      if (!server_state.containsKey(path)) {
        lost_state.put(path, "");
      }
    }

    if (lost_state.size() != 0) {
      reportViolation(fjp, fst, fev, "lost_state", lost_state);
    }

  }


  // *********************************
  private void reportViolation(FMJoinPoint fjp, FMStackTrace fst, FrogEvent fev,
                               String tableName, SortedMap<String,String> tableMap) {

    String buf = "";

    buf += String.format("\n\n");
    buf += String.format("## ################################################\n");
    buf += String.format("##   Failed Specification : %s \n", tableName);
    buf += String.format("## ################################################\n");
    buf += String.format("## \n");
    buf += String.format("## Violated state (table map): \n");
    int i = 0;
    Set s = tableMap.entrySet();
    Iterator itr = s.iterator();
    while(itr.hasNext()) {
      buf += String.format("    [%02d] %s \n", i++, ((Map.Entry)itr.next()).getValue());
    }
    buf += String.format("## \n");
    buf += String.format("## FMJoinPoint: \n   %s \n", fjp);
    buf += String.format("## \n");
    buf += String.format("## FrogEvent: \n   %s \n", fev);
    buf += String.format("## \n");
    buf += String.format("## FMStackTrace: \n%s  \n", fst);
    buf += String.format("## \n");
    buf += String.format("## ################################################ END\n\n");

    Util.print(ps, buf);

  }


  // *********************************
  private void testManualNiceExampleForNeil()
    throws InterruptedException, JolRuntimeException, IOException {


    // Util.scheduleEvent(ps, system, "model", "user_state", "/dir1");
    // Util.scheduleEvent(ps, system, "model", "user_state", "/dir2");
    // Util.scheduleEvent(ps, system, "spec", "server_state", "/dir1", "bla");

    // PrintTable.printAllTables();

    // stdin.readLine();

    // I have to manually run eval here ..
    // Util.scheduleEvent(ps, system, "spec", "manual_eval", Util.r());
    // PrintTable.printAllTables();

    // stdin.readLine();
    // Util.scheduleEvent(ps, system, "spec", "server_state", "/dir2", "bla");
    // PrintTable.printAllTables();


    // stdin.readLine();

    // but this time it doesn't work. lost_state is still 0
    // Util.scheduleEvent(ps, system, "spec", "manual_eval", Util.r());
    // PrintTable.printAllTables();

    // stdin.readLine();

    // stop();
    // System.exit(0);

  }


  // *********************************
  private void testManual()
    throws InterruptedException, JolRuntimeException, IOException {


    // Util.scheduleEvent(ps, system, "model", "user_state", "/dir1");
    // Util.scheduleEvent(ps, system, "model", "user_state", "/dir2");
    // Util.scheduleEvent(ps, system, "spec", "server_state", "/dir1", "bla");
    // Util.scheduleEvent(ps, system, "spec", "manual_eval", Util.r());
    // PrintTable.printAllTables();

    // stdin.readLine();


    // Util.scheduleEvent(ps, system, "model", "user_state", "/dir3");
    // Util.scheduleEvent(ps, system, "model", "user_state", "/dir4");
    // Util.scheduleEvent(ps, system, "spec", "server_state", "/dir2", "bla");
    // Util.scheduleEvent(ps, system, "spec", "manual_eval", Util.r());
    // PrintTable.printAllTables();

    // I have to manually run eval here ..

    // stdin.readLine();
    // Util.scheduleEvent(ps, system, "spec", "server_state", "/dir2", "bla");
    // PrintTable.printAllTables();


    // stdin.readLine();

    // but this time it doesn't work. lost_state is still 0
    // Util.scheduleEvent(ps, system, "spec", "manual_eval", Util.r());
    // PrintTable.printAllTables();

    // stdin.readLine();

    // stop();
    // System.exit(0);

  }


  // *********************************
  private void stop() {
    this.system.shutdown();
  }

}

