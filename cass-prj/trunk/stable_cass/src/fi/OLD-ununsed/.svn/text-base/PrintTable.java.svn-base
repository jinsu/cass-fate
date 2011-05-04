
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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


public class PrintTable {

  private static int inodeNum = 100;
  private static int freeBlockNum = 30000;
  private static int count = 0;

  public PrintTable() { }
  private static JolSystem system;

  public static void start(JolSystem systemArg) {
    system = systemArg;
  }

  private static SortedMap<String,String> sm_ss;
  private static Table tbl;

  // *********************************
  // also returns table
  public static void prepare(String pn, String tn) {
    tbl = Util.getTable(system, pn, tn);
    sm_ss = new TreeMap<String, String>();
    Util.print(FrogServer.getPs(),
	       String.format("\n  PRINT TABLE [%s][%s] \n", pn, tn));
  }

  // *********************************
  // dfs/data/current/VERSION
  // dfs/data/in_use.lock
  // dfs/data/storage
  // dfs/name1/image/fsimage
  // dfs/name1/in_use.lock
  // dfs/namesecondary/in_use.lock
  
  public static String trimPath(String fp) {
    String t1 = fp.replaceFirst(FMLogic.HADOOP_STORAGE_DIR + "dfs/name1/", "/name1/");
    String t2 = t1.replaceFirst(FMLogic.HADOOP_STORAGE_DIR + "dfs/namesecondary/", "/nmsec/");
    String t3 = t2.replaceFirst(FMLogic.HADOOP_STORAGE_DIR + "dfs/data/", "/ddata/");
    return t3;
  }


  // *********************************
  public static void printSM_SS() {
    int i = 0;
    Set s = sm_ss.entrySet();
    Iterator itr = s.iterator();
    while(itr.hasNext()) {
      String t = trimPath((String)((Map.Entry)itr.next()).getValue());
      Util.print(FrogServer.getPs(),
		 String.format("    [%02d] %s \n", i++, t));
    }
  }


  // *********************************
  public static void server_storage() throws JolRuntimeException {
    prepare("model", "server_storage");
    int i = 0;
    for (Tuple t : tbl.tuples())  {
      sm_ss.put((String) t.value(0) + (String) t.value(1),
                String.format("%-10s  %-10s  %-10s ",
                              (String) t.value(0), // Path
                              (String) t.value(1), // File
                              (String) t.value(2)  // Status
                              ));
      i++;
    }
    printSM_SS();
  }


  // *********************************
  public static void user_state() throws JolRuntimeException {
    prepare("model", "user_state");
    int i = 0;
    for (Tuple t : tbl.tuples())  {
      sm_ss.put((String) t.value(0),
                String.format("%-10s  ",
                              (String) t.value(0)  // Path
                              ));
      i++;
    }
    printSM_SS();
  }


  // *********************************
  public static void server_state() throws JolRuntimeException {
    prepare("spec", "server_state");
    int i = 0;
    for (Tuple t : tbl.tuples())  {
      sm_ss.put((String) t.value(0),
                String.format("%-10s  %-10s",
                              (String) t.value(0), // Path
                              (String) t.value(1)  // StorageFile
                              ));
      i++;
    }
    printSM_SS();
  }

  // *********************************
  public static void lost_state() throws JolRuntimeException {
    prepare("spec", "lost_state");
    int i = 0;
    for (Tuple t : tbl.tuples())  {
      sm_ss.put((String) t.value(0),
                String.format("%-10s  ",
                              (String) t.value(0)  // Path
                              ));
      i++;
    }
    printSM_SS();
  }
  

  // *********************************
  public static void storage_file() throws JolRuntimeException {
    prepare("model", "storage_file");
    int i = 0;
    for (Tuple t : tbl.tuples())  {
      sm_ss.put((String) t.value(0),
                String.format("%-10s  ",
                              (String) t.value(0)  // StorageFile
                              ));
      i++;
    }
    printSM_SS();
  }



  // *********************************
  public static void fill_failover_violation_map() throws JolRuntimeException {
    int i = 0;
    for (Tuple t : tbl.tuples())  {
      sm_ss.put((String) t.value(0) + ((Integer) t.value(1)).toString(),
                String.format("%s  %8d",
                              (String)  t.value(0), // NodeId
                              (Integer) t.value(1)  // RandId
                              ));
      i++;
    }
  }
  
  
  // *********************************
  public static void fill_dead_nodes_map() throws JolRuntimeException {
    int i = 0;
    for (Tuple t : tbl.tuples())  {
      sm_ss.put((String) t.value(1) + (String) t.value(0),
                String.format("%-12s  %-10s",
                              (String) t.value(0), // NodeId
                              (String) t.value(1)  // ObservedBy
                              ));
      i++;
    }
  }
  

  // ##################################################################
  // ##################################################################
  // ##################################################################

  // **********************************************
  public static void fillTableMap(String pn, String tn) throws JolRuntimeException {
    prepare(pn, tn);
    if (tn.equals("failover_violation")) 
      fill_failover_violation_map();    
    else if (tn.equals("dead_nodes")) 
      fill_dead_nodes_map();
  }

  // **********************************************
  public static SortedMap<String,String> getTableMap(String pn, String tn)  {
    
    try {
      fillTableMap(pn, tn);
      return sm_ss;
    } catch (Exception e) {
      Util.EXCEPTION(FrogServer.getPs(), "getTableMap", e);
      Util.ERROR(FrogServer.getPs(), "getTableMap " + pn + " " + tn);
      return null;
    }
  }
  
  // *********************************
  public static void printTableMap(String pn, String tn) throws JolRuntimeException {
    fillTableMap(pn, tn);
    printSM_SS();
  }


  // ************************************
  public static void printAllTables() {


    try {

      Util.print(FrogServer.getPs(),
		 "\n---------------------------- PRINT \n\n");

    // ------------------- for storage)
    /* 
    storage_file();
    server_storage();
    server_state();
    user_state();
    lost_state();
    */


    // ------------------- for model)
    printTableMap("spec", "failover_violation");
    printTableMap("model", "dead_nodes");
    
    
    Util.print(FrogServer.getPs(), "\n");

    // just make sure the screen is not still if
    // we repeat printing the same stuff
    if (count++%2==0)     Util.print(FrogServer.getPs(), "\n");
    
    
    } catch(Exception e) {
      Util.EXCEPTION(FrogServer.getPs(), "printAllTables", e);
    }

  }


}