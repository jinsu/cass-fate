
package org.fi;


import org.fi.*;
import org.fi.FMServer.*;

import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.lang.*;
import java.lang.management.ManagementFactory;


// *************************************** aspectj
import org.aspectj.lang.Signature;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.SourceLocation;

// *************************************** JOl
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


/////////////////////////////////////////
//Cassandra hack related
/////////////////////////////////////////

//JINSU for orderEndpoints function
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.config.DatabaseDescriptor;
//end importing for orderEndpoints function





public class Util {
	//jinsu added for testing purpose
	private static boolean DEBUG = false;
    //private static boolean DEBUG = true;
	private static boolean ENABLE_OLD = true;
	private static boolean ENABLE_FATAL = true;

  private static int dfsDatanodePort = 50010;
  private static int dfsDatanodeIpcPort = 50020;
  private static int dfsNamenodePort = 9000;

	//jinsu cass specific nodes
	private static int cassNodePort = 9000;
	private static int cassStorPort = 7000;

  private static Map<String,String> pidCacheMap = new TreeMap<String, String>();


  public Util() {  }

  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                 G E N E R A L   U T I L I T Y                 ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################

  private static long lastTime = 0;
  public static String diff() {
    long currentTime = System.currentTimeMillis();
    double diff = 0.0;
    if (lastTime == 0) {
      lastTime = currentTime;
    }
    else {
      diff = (double)(currentTime - lastTime) / 1000;
      lastTime = currentTime;
    }

    String warning = "";

    if (diff > 5) {
      warning = "*LONG!!*";
    }

    return String.format("[fi-diff][%5.3f secs] %s", diff, warning);
  }


  private static Random random = new Random();

  public static int r() { return Math.abs(random.nextInt()); }

  // random to 4th digit
  public static int r4() { return (Math.abs(random.nextInt()) % 10000); }

  // random to 8th digit
  public static int r8() { return (Math.abs(random.nextInt()) % 100000000); }



  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                 R P C       U T I L I T Y                     ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################

  public static DataOutputStream getRpcOutputStream(int randId) {
    File f = getRpcFile(randId);
    try {
      FileOutputStream fos = new FileOutputStream(f);
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      DataOutputStream dos = new DataOutputStream(bos);
      return dos;
    } catch (FileNotFoundException e) {
    	if(ENABLE_OLD) {
      Util.FATAL("getRpcOutputStream Can't open: " + f.getAbsolutePath());
      }
      return null;
    }
  }

  public static DataInputStream getRpcInputStream(int randId) {
    File f = getRpcFile(randId);
    try {
      FileInputStream fis = new FileInputStream(f);
      BufferedInputStream bis = new BufferedInputStream(fis);
      DataInputStream dis = new DataInputStream(bis);
      return dis;
    } catch (FileNotFoundException e) {
      Util.FATAL("getRpcInputStream Can't open: " + f.getAbsolutePath());
      return null;
    }
  }

  public static File getRpcFile(int randId) {
    File dir = new File (FMLogic.RPC_FILES_DIR);
    if (!dir.exists()) {
    	if(ENABLE_OLD) {
      Util.FATAL("getRpcFile Not exist: " + dir.getAbsolutePath());
      }
    }

    String fname = String.format("rpc-%08d", randId);
    File f = new File(dir, fname);
    return f;
  }

  public static int failTypeToInt(FailType ft) {
    if      (ft == FailType.CRASH)      return 1;
    else if (ft == FailType.BADDISK)    return 2;
    else if (ft == FailType.EXCEPTION)  return 3;
    else if (ft == FailType.RETFALSE)   return 4;
    else if (ft == FailType.CORRUPTION) return 5;
    else                                return 0;

  }

  public static FailType intToFailType(int i) {
    if      (i == 1) return FailType.CRASH;
    else if (i == 2) return FailType.BADDISK;
    else if (i == 3) return FailType.EXCEPTION;
    else if (i == 4) return FailType.RETFALSE;
    else if (i == 5) return FailType.CORRUPTION;
    else             return FailType.NONE;
  }



  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                      O S       U T I L I T Y                  ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################


  // *****************************************
  // a tool to run the command
  public static String runCommand(String cmd) {


    if (cmd.contains("killall java")) {
    	if(DEBUG) {
    		System.out.println("&&&&&&&&&&KILLALL EXECUTED\n");
    	}
      String buf = getStackTrace();
      File f = new File ("/tmp/killallJavaOut");
      boolean flush = true;
      stringToFileContent(buf, f, flush);
    }


    String msg = "";
    try {
      String line;
      Process p = java.lang.Runtime.getRuntime().exec(cmd);
      BufferedReader input =new BufferedReader
        (new InputStreamReader(p.getInputStream()));
      while ((line = input.readLine()) != null) {
        msg = msg + line + "\n";
      }
      input.close();
      //JINSU HACK
      //JINSU: I was getting TOO MANY FILE OPENED...
      p.getErrorStream().close();
      p.getOutputStream().close();
    } catch (Exception e) {
      EXCEPTION("runCommand", e);
    }

    return msg;
  }


  // *******************************************
  public static int getIntPid() {
    String pidStr = getPid();
    return (new Integer(pidStr)).intValue();
  }

  // *******************************************
  public static String getPid() {
    String fullPid = ManagementFactory.getRuntimeMXBean().getName();
    String [] split = fullPid.split("@", 2);
    String pid = split[0];
    if(DEBUG) System.out.println("getPid : " + pid);
    return pid;
  }


  // **************************************
  public static String fileContentToString(String path) {
    return fileContentToString(new File(path));
  }

  // **************************************
  public static String fileContentToString(File f) {
    String buf = "";
    if (!f.exists()) {
      ERROR("Not exist " + f);
      return null;
    }
    try {
      BufferedReader in = new BufferedReader(new FileReader(f));
      String line;
      while ((line = in.readLine()) != null) {
        buf = buf + line + "\n";
      }
      in.close();
    } catch (Exception e) {
      EXCEPTION("fileContentToString " + f, e);
      return null;
    }
    return buf;
  }


  // **************************************
  public static boolean stringToFileContent(String buf, String path) {
    return stringToFileContent(buf, new File(path), false);
  }


  // **************************************
  public static boolean stringToFileContent(String buf, File f) {
    return stringToFileContent(buf, f, false);
  }


  // **************************************
  public static void assertSafeFile(String fullPath) {

    boolean safe = false;

    // we only allow removal of anything that is in /tmp/*  !!
    if (fullPath.indexOf("/tmp") == 0) {
      safe = true;
    }
    else  {
      safe = false;
    }

    // if not safe, don't proceed
    if (safe == false) {
      ERROR("You're trying to remove UNSAFE directories!!" + fullPath);
      ERROR("I only allow files under /tmp/** to be removed");
      ERROR("Are you sure?? Exiting");
      if(ENABLE_FATAL) {
      Util.runCommand("killall java");
      }
    }
  }

  // **************************************
  public static void assertSafeFile(File f) {
    assertSafeFile(f.getAbsolutePath());
  }



  // ************************************************
  public static boolean stringToFileContent(String buf, File f, boolean flush) {

    assertSafeFile(f);

    try {
      // boolean rv = f.createNewFile(); ??

      FileWriter fw = new FileWriter(f);
      fw.write(buf);
      if (flush)
        fw.flush();
      fw.close();
    } catch (IOException e) {
      Util.EXCEPTION("stringToFileContent", e);
      Util.ERROR("should not throw exception here");
      return false;
    }
    return true;
  }


  // **************************************
  public static boolean mkDir(String dirname) {
    return mkDir(new File(dirname));
  }

  // **************************************
  public static boolean mkDir(File dir) {
    try {
      dir.mkdir();
    } catch (Exception e) {
      WARNING("can't mkdir " + dir);
      return false;
    }
    return true;
  }

  // **************************************
  public static boolean createNewFile(String path) {
    return createNewFile(new File(path));
  }

  // **************************************
  public static boolean createNewFile(File f) {
    try {
      boolean b = f.createNewFile();
      return b;
    } catch (Exception e) {
      return false;
    }
  }



  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                     H D F S    U T I L I T Y                  ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################


  // *******************************************
  public static String getNodeId() {
    String pid = getPid();
    String nodeId = getNodeIdFromPid(pid);
    return nodeId;
  }


  // *******************************************
  public static String getNetIOContextFromPort(int port) {

    String nodeId  = Util.getNodeIdFromKnownPort(port);
    String ctx = null;

    // good well-known port
    if (!nodeId.contains("Unknown-Port")) {
      ctx = String.format("NetIO-%s:%d", nodeId, port);
    }
    // let's see the socket's parent, the datanode that gives this
    // socket (from ss.accept())
    else {
      nodeId = Util.getNodeIdFromUnknownPort(port);
      if (nodeId.contains("Unknown")) {
        ctx = String.format("NetIO-%s:%d", nodeId, port);
        Util.ERROR("unknown port " + port + " pid is " + Util.getPid());
      }
      else {
        ctx = String.format("NetIO-%s:New:%d", nodeId, port);
      }
    }
    return ctx;
  }


  //********************************************
  public static boolean isMetaFile(String targetIO) {
    if (isBlockFile(targetIO) && targetIO.contains(".meta"))
      return true;
    return false;
  }


  //********************************************
  // .../tmp/blk_
  //********************************************
  public static boolean isTmpMeta(String targetIO) {
    if (isBlockFile(targetIO) && targetIO.contains(".meta") &&
	targetIO.contains("/tmp/blk_"))
      return true;
    return false;
  }

  //********************************************
  // Sometimes I see this:
  //   current/blk_2387194899189267252_1001.meta_tmp1002
  //********************************************
  public static boolean isCurrentTmpMeta(String targetIO) {
    if (isBlockFile(targetIO) && targetIO.contains(".meta") &&
	targetIO.contains("_tmp") && targetIO.contains("current"))
      return true;
    return false;
  }


  //********************************************
  public static boolean isCurrentMeta(String targetIO) {
    if (isBlockFile(targetIO) && targetIO.contains(".meta") &&
	targetIO.contains("/current/"))
      return true;
    return false;
  }

  //********************************************
  public static boolean isBlockFile(String targetIO) {
    if (targetIO.contains("/dfs/data") && targetIO.contains("blk_"))
      return true;
    return false;
  }

  //********************************************
  public static boolean isDataFile(String targetIO) {
    if (isBlockFile(targetIO) && !isMetaFile(targetIO))
      return true;
    return false;
  }


  //********************************************
  public static boolean isTmpData(String targetIO) {
    if (isBlockFile(targetIO) && !isMetaFile(targetIO) &&
	targetIO.contains("/tmp/blk_"))
      return true;
    return false;
  }


  //********************************************
  public static boolean isCurrentData(String targetIO) {
    if (isBlockFile(targetIO) && !isMetaFile(targetIO) &&
	targetIO.contains("/current/"))
      return true;
    return false;
  }

  //********************************************
  public static boolean isNetIO(String targetIO) {
    if (targetIO.contains("NetIO"))
      return true;
    return false;
  }

	//##############################################################################
	//Jungmin change made

	//********************************************
	public static boolean isLogFile(String targetIO) {
    if (targetIO.contains("/commitLogs/CommitLog") && targetIO.contains(".log"))
      return true;
    return false;
  }

	//********************************************
  public static boolean isDiskIO(String targetIO) {
    if (targetIO.contains("/tmp/fi/cassandra/node"))
      return true;
    return false;
  }

	//##############################################################################


	//##########################################################
	//JINSU : changes
	//*********************************************************
	public static boolean deleteNodeConnectedFlag() {
		File f = new File(FMLogic.NODES_CONNECTED_FLAG);
		if(f.exists()) {
			return f.delete();
		}
		return true;
	}

	//#########################################################

  //********************************************
  // return disk1 or disk2 etc.
  // only support upto four disks
  public static String getDiskIdFromTargetIO(String targetIO) {
    if (targetIO.contains("dfs/dataOne"))        return "Disk1";
    else if (targetIO.contains("dfs/dataTwo"))   return "Disk2";
    else if (targetIO.contains("dfs/dataThree")) return "Disk3";
    else if (targetIO.contains("dfs/dataFour"))  return "Disk4";
    else if (targetIO.contains("dfs/data"))      return "DiskMain";
    return "DiskUnknown";
  }

  //********************************************
  public static boolean isNetIOtoDataNode(String targetIO) {
    if (targetIO.contains("NetIO") && targetIO.contains("DataNode"))
      return true;
    return false;
  }


  // ***************************************************
  public static String getDatanodeStringIdFromLocName(String locName) {
    int dnId = getDnIdFromLocName(locName);
    return String.format("DataNode-%d", dnId);
  }

  // ***************************************************
  private static int getDnIdFromLocName(String locName) {
    // 127.0.0.1:50013
    String [] split = locName.split(":", 2);
    String portStr = split[1];
    Integer port = new Integer(portStr);
    int dnId = port.intValue() - dfsDatanodePort;
    return dnId;
  }

  // ***************************************************
  public static String getNodeIdFromKnownPort(int port) {

    int support = 10;
    int dnId;

		//jinsu dfsNamenodePort and cassNodePort are the same so i want to do this first.
		// cassandra node
		//if (port >= cassNodePort && port < cassNodePort + support) {
		//	int cnode = getCnodeIdFromPort(port);
		//	return String.format("Node%d", cnode);
		//}

    // namenode
    if (port == dfsNamenodePort) {
      return String.format("NameNode", port);
    }

    // fmserver
    if (port == FMServer.PORT) {
      return String.format("FMServer", port);
    }

    // datanodes: only support up to 9 datanodes
    if (port >= dfsDatanodePort && port < dfsDatanodePort + support) {
      dnId = getDnIdFromPort(port);
      return String.format("DataNode-%d", dnId, port);
    }

    if (port >= dfsDatanodeIpcPort && port < dfsDatanodeIpcPort + support) {
      dnId = getDnIdFromIpcPort(port);
      return String.format("DataNode-%d:IPC", dnId, port);
    }



    // return the port whatever it is
    return String.format("Unknown-Port-%d", port);

  }
  // jinsu
  // comebine getNetIOContextFromPort and getNetIOContextFromInetAddr
  public static String getNetIOContext(InetAddress iaddr, int port) {
    String ctx = null;
    ctx = getNetIOContextFromInetAddr(iaddr);
    if(ctx != null) {
    	ctx = String.format("%s:%d", ctx, port);
    }
    return ctx;
  }


  // jinsu
  // *******************************************
  public static String getNetIOContextFromInetAddr(InetAddress iaddr) {

    String nodeId  = Util.getNodeIdFromINetAddr(iaddr);
    String ctx = null;

    // good well-known port
    if (!nodeId.contains("Unknown-Ip")) {
      ctx = String.format("NetIO-%s:%s", nodeId, iaddr.getHostAddress());
    }
    // let's see the socket's parent, the datanode that gives this
    // socket (from ss.accept())
    else {
    	System.out.println("&&&Unknown IP " + iaddr.getHostAddress() + " pid is " + Util.getPid());
    	/*
      nodeId = Util.getNodeIdFromUnknownPort(port);
      if (nodeId.contains("Unknown")) {
        ctx = String.format("NetIO-%s:%d", nodeId, port);
        Util.ERROR("unknown port " + port + " pid is " + Util.getPid());
      }
      else {
        ctx = String.format("NetIO-%s:New:%d", nodeId, port);
      }
      */
    }
    return ctx;
  }

  //jinsu get nodeId using ip address
  //***************************************************
  public static String getNodeIdFromINetAddr(InetAddress iaddr) {
		//look in IP_HISTORY_DIR
		String cnode = getCnodeIdFromIp(iaddr);
		if(cnode != null) {
			return cnode;
		}
		return String.format("Unknown-Ip");
		}

  //jinsu
  //consults ip history dir and returns "Node0, Node1... or null if it cant find the file."
  private static String getCnodeIdFromIp(InetAddress iaddr) {
  	String nodeId = null;
  	int addr_hash = iaddr.hashCode();
  	nodeId = getNodeIDFromTmpIpFile(FMLogic.IP_HISTORY_DIR + addr_hash);
  	if(nodeId.equals("0")) {
  		nodeId = null;
  	}
  	return nodeId;
  }
	// ***************************************************
  private static String getNodeIDFromTmpIpFile(String fname) {

    File f = new File(fname);
    if (!f.exists()) {
      Util.WARNING("not exist: " + fname);
      return "0";
    }

    // else read the file and return the pid
    try {
      BufferedReader in = new BufferedReader(new FileReader(fname));
      String str = in.readLine();
      if (str == null) {
        Util.WARNING("str is null");
        return "0";
      }
      in.close();
      return str; // successful one
    } catch (IOException e) {
      Util.EXCEPTION("getnodeidfromipfile", e);
      return "0";
    }
  }

	//jinsu for cassandra to be compatible with getNodeIdFromKnownPort function
	private static int getCnodeIdFromPort(int port) {
	//look inside sockethistory and get it from there
		int cnode = port - cassNodePort;
		return cnode;
	}

  // ***************************************************
  private static int getDnIdFromPort(int port) {
    // see dfs.datanode.address in conf

    int dnId = port - dfsDatanodePort;
    return dnId;
  }


  // ***************************************************
  private static int getDnIdFromIpcPort(int port) {
    // see dfs.datanode.address in con

    int dnId = port - dfsDatanodeIpcPort;
    return dnId;
  }


  // ***************************************************
  // given the form "NetIO-....:...." node, return the filename
  public static String getNodeIdFromNetIO(String filename) {
    String [] tmp1 = filename.split("-", 2);
    String [] tmp2 = tmp1[1].split(":", 2);
    String nodeId = tmp2[0];
    return nodeId;
  }

  // ***************************************************
  // given the form "NetIO-....:...." node, return the filename
  public static String getPidFromNodeId(String nodeId) {

    String tmpPid = "/tmp/" + FMLogic.CASS_USERNAME + "-";
    if (nodeId.contains("DataNode"))
      tmpPid += nodeId.replace("DataNode", "pdatanode");
    if (nodeId.contains("NameNode"))
      tmpPid += nodeId.replace("NameNode", "namenode");

    //jinsu making changes appropriate for cassandra node naming
    if (nodeId.contains("Node"))
    	tmpPid = FMLogic.TMPFI + "pids/" + nodeId.toLowerCase();
    tmpPid += ".pid";

    String pid = getPidFromTmpPidFile(tmpPid);
    return pid;
  }


  // ***************************************************
  public static String getNodeIdFromPid(String pid) {

    String nodeId = "UnknownNodeId";
    String fname;

    // check the cache key first
    // key = String pid, value = nodeId
    if (pidCacheMap.containsKey(pid)) {
      nodeId = (String) pidCacheMap.get(pid);
      return nodeId;
    }

		//jinsu making changes because we just store pids inside /tmp/fi/pids
		// check if it is a node
		int cnode = getCnodeIdFromPid(pid);
		if(cnode >= 0) {
			nodeId = String.format("Node%d", cnode);
			pidCacheMap.put(pid, nodeId);
			return nodeId;
		}

		//jinsu it prints too many lines...
		if(ENABLE_OLD) {
		/*
    // check if datanode
    int nid = getDnIdFromPid(pid);
    if (nid != 0) {
      nodeId = String.format("DataNode-%d", nid);
      pidCacheMap.put(pid, nodeId);
      return nodeId;
    }

    // check if namenode
    fname = "/tmp/" + FMLogic.CASS_USERNAME+ "-namenode.pid";
    if (isPidMatch(fname, pid)) {
      nodeId = "NameNode";
      pidCacheMap.put(pid, nodeId);
      return nodeId;
    }

    // check if secondary namenode
    fname = "/tmp/" + FMLogic.CASS_USERNAME + "-namenode.pid";
    if (isPidMatch(fname, pid)) {
      nodeId = "SecondaryNameNode";
      pidCacheMap.put(pid, nodeId);
      return nodeId;
    }

    // check if client
    fname = "/tmp/" + FMLogic.CASS_USERNAME + "-client.pid";
    if (isPidMatch(fname, pid)) {
      nodeId = "Client";
      pidCacheMap.put(pid, nodeId);
      return nodeId;
    }
    */
		}
    return nodeId;

  }

  //jinsu making a function to get how many nodes are running.

  private static int getNumCassNodes() {
  String fname = String.format(FMLogic.TMPFI + "pids/");
  File f = new File(fname);
  if(f.isDirectory()) {
  	String[] children = f.list();
  	if(DEBUG) {
  		System.out.print("In " + FMLogic.TMPFI + "pids/... [ ");
  		if(children != null)
 				System.out.print(children);
  		System.out.print(" ] \n");
  	}
  	int rtNum = 0;
  	if(children != null) {
			for(String child: children) {
				if (child.contains("node")) {
					rtNum++;
				}
			}
  	}
  	return rtNum;

	}
	return -1;
	}

  //jinsu making a function to check all node#.pid files.
  private static int getCnodeIdFromPid(String pid) {
  	//start from 0!!
  	int max_nodes = getNumCassNodes();
  	if(DEBUG) {
  		System.out.println(max_nodes + " Cass Nodes running...");
  	}
  	//supporting up to 10 nodes for now...
  	for (int i = 0; i < max_nodes; i++) {
  		String fname = String.format(FMLogic.TMPFI + "pids/" + "node%d.pid", i);
  		if (isPidMatch(fname, pid))
  			return i;
  	}
  	return -1;
  }

  // ***************************************************
  // don't use this from outside
  private static int getDnIdFromPid(String pid) {

    // start from 1!!
    // just support upto 9 datanodes
    for (int i = 1; i < 10; i++) {
      String fname = String.format("/tmp/%s-pdatanode-%d.pid",
                                   FMLogic.CASS_USERNAME, i);
      if (isPidMatch(fname, pid))
        return i;
    }
    return 0;
  }

  // ***************************************************
  private static boolean isPidMatch(String fname, String pid) {
    String tmp = getPidFromTmpPidFile(fname);
    if (tmp.equals(pid)) {
      return true; // match!
    }
    return false;
  }

  // ***************************************************
  private static String getPidFromTmpPidFile(String fname) {

    File f = new File(fname);
    if (!f.exists()) {
      Util.WARNING("not exist: " + fname);
      return "0";
    }

    // else read the file and return the pid
    try {
      BufferedReader in = new BufferedReader(new FileReader(fname));
      String str = in.readLine();
      if (str == null) {
        Util.WARNING("str is null");
        return "0";
      }
      in.close();
      return str; // successful one
    } catch (IOException e) {
      Util.EXCEPTION("getdatanodefrompid", e);
      return "0";
    }
  }


  //********************************************
  //only supports upto 9 datanodes
  //JINSU making changes : changed DataNode -> Node.
  public static String getDnIdFromNetTargetIO(String targetIO) {
    /*
    if (targetIO.contains("DataNode-1")) return "DataNode-1";
    if (targetIO.contains("DataNode-2")) return "DataNode-2";
    if (targetIO.contains("DataNode-3")) return "DataNode-3";
    if (targetIO.contains("DataNode-4")) return "DataNode-4";
    if (targetIO.contains("DataNode-5")) return "DataNode-5";
    if (targetIO.contains("DataNode-6")) return "DataNode-6";
    if (targetIO.contains("DataNode-7")) return "DataNode-7";
    if (targetIO.contains("DataNode-8")) return "DataNode-8";
    if (targetIO.contains("DataNode-9")) return "DataNode-9";
    // Util.ERROR("DataNode-Unknown");
    return "DataNode-Unknown";
    */
    if (targetIO.contains("Node0")) return "Node0";
    if (targetIO.contains("Node1")) return "Node1";
    if (targetIO.contains("Node2")) return "Node2";
    if (targetIO.contains("Node3")) return "Node3";
    if (targetIO.contains("Node4")) return "Node4";
    if (targetIO.contains("Node5")) return "Node5";
    if (targetIO.contains("Node6")) return "Node6";
    if (targetIO.contains("Node7")) return "Node7";
    if (targetIO.contains("Node8")) return "Node8";
    if (targetIO.contains("Node9")) return "Node9";
    return "Node-Unknown";
  }

  //********************************************
  public static void addSocketHistory(Socket s) {
    try {
      String nodeId = getNodeId();
      int port = s.getLocalPort();
      System.out.println("&&&Added socket : " + s.getLocalPort());
      String fname = String.format("%s/%d", FMLogic.SOCKET_HISTORY_DIR, port);
      FileOutputStream fos = new FileOutputStream(fname);
      PrintStream p = new PrintStream(fos);
      p.print(nodeId + "\n");
      p.close();
      //jinsu debugging
      if(DEBUG) {
      File f = new File(fname);
      System.out.println("&&&Checking if file got created... file " + port + " f.exists? == " + f.exists());
      }
    } catch (Exception e) {}
  }

	//jinsu adds IP address and to identify nodes
	//********************************************
  public static void addIpHistory(InetAddress iaddr) {
    try {
      String nodeId = getNodeId();
      //int port = s.getLocalPort();
      String ip_addr = iaddr.getHostAddress();
      String fname = String.format("%s/%d", FMLogic.IP_HISTORY_DIR, iaddr.hashCode());
      //jinsu
      File ipf = new File(fname);


      boolean exists_but_dif_node = ipf.exists() && !nodeId.equalsIgnoreCase(fileContentToString(fname).trim());
			if(DEBUG) {
				System.out.println("ipf exists? " + ipf.exists());
				System.out.println("exists_but_dif_node?? " + exists_but_dif_node);
			}
      if(!ipf.exists() || exists_but_dif_node) {
		    FileOutputStream fos = new FileOutputStream(fname);
		    PrintStream p = new PrintStream(fos);
		    p.print(nodeId + "\n");
		    p.close();
		    if(DEBUG) {
		    System.out.println("&&&Added IP : " + iaddr.getHostAddress() + " hashCode : " + iaddr.hashCode());
		    //jinsu debugging

				  File f = new File(fname);
				  System.out.println("&&&Checking if file got created... file " + fname + " f.exists? == " + f.exists());
		    }
      }
    } catch (Exception e) {
    	System.out.println("Exception..." + e);
    }
  }



  //********************************************
  // consult socket history here
  public static String getNodeIdFromUnknownPort(int port) {

    String fname = String.format("%s/%d", FMLogic.SOCKET_HISTORY_DIR, port);
    File f = new File(fname);
    if (!f.exists()) {
    	//jinsu what the heck??
    	System.out.println("&&&Couldn't find file for port : " + port);
      return "UnknownNodeId";
    }
    String nodeId = fileContentToString(f);
    nodeId = nodeId.replace("\n","");
    return nodeId;
  }




  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####             E R R O R    M E S S A G E   U T I L              ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################


  // *******************************
  public static void pre(PrintStream ps) {
    print(ps, "\n");
    print(ps, "## ############################################\n");
  }

  // *******************************
  public static void post(PrintStream ps) {
    print(ps, "## ############################################\n");
    print(ps, "\n");
  }

  // *******************************
  public static void MESSAGE(PrintStream ps, String msg) {
    pre(ps);
    print(ps, "## MESSAGE: " + msg + "\n");
    post(ps);
  }

  // *******************************
  public static void ERROR(PrintStream ps, String msg) {
    pre(ps);
    print(ps, "## ERROR: " + msg + "\n");
    printStackTrace(ps);
    post(ps);
  }

  // *******************************
  public static void FATAL(PrintStream ps, String msg) {
    System.err.println("## FATAL: " + msg + "\n");
    pre(ps);
    print(ps, "## FATAL: " + msg + "\n");
    printStackTrace(ps);
    post(ps);
    if (ps != null) {
      ps.flush();
      ps.close();
    }
    System.out.flush();
    System.err.flush();
    if(ENABLE_FATAL) {
    Util.runCommand("killall java");
    }
  }

  // *******************************
  public static void WARNING(PrintStream ps, String msg) {
    pre(ps);
    print(ps, "## WARNING: " + msg + "\n");
    post(ps);
  }

  // *******************************
  public static void WARNING_ONELINE(PrintStream ps, String msg) {
    print(ps, "## WARNING: " + msg + "\n");
  }

  // *******************************
  public static void EXCEPTION(PrintStream ps, String msg, Exception e) {
    pre(ps);
    print(ps, "## EXCEPTION: " + msg + "\n");
    print(ps, "## ---------------------- Exception: \n");
    print(ps, e.toString() + "\n");
    print(ps, "## ---------------------- Cause: \n");
    print(ps, e.getCause() + "\n");
    print(ps, "## ---------------------- Trace: " + "\n");
    printStackTrace(ps, e);
    post(ps);
  }

  // *******************************
  public static void MESSAGE(String msg) {
    PrintStream ps = null;
    MESSAGE(ps, msg);
  }

  // *******************************
  public static void ERROR(String msg) {
    PrintStream ps = null;
    ERROR(ps, msg);
  }

  // *******************************
  public static void FATAL(String msg) {
    PrintStream ps = null;
    FATAL(ps, msg);
  }

  // *******************************
  public static void WARNING(String msg) {
    PrintStream ps = null;
    WARNING(ps, msg);
  }

  // *******************************
  public static void WARNING_ONELINE(String msg) {
    PrintStream ps = null;
    WARNING_ONELINE(ps, msg);
  }

  // *******************************
  public static void EXCEPTION(String msg, Exception e) {
    PrintStream ps = null;
    EXCEPTION(ps, msg, e);
  }

  // *******************************
  public static void printStackTrace(PrintStream ps, Exception e) {
    if (e == null)
      print(ps, getStackTrace());
    else
      print(ps, stackTraceToString(e.getStackTrace()));
  }

  // *******************************
  public static void printStackTrace(PrintStream ps) {
    printStackTrace(ps, null);
  }

  // *******************************
  public static void printStackTrace() {

    System.out.print(getStackTrace());
  }

  // *******************************
  public static String getStackTrace() {
    Thread t = Thread.currentThread();
    // FMStackTrace fst = new FMStackTrace(t.getStackTrace());
    // return fst.toString();
    StackTraceElement[] ste = t.getStackTrace();
    return stackTraceToString(ste);
  }

  // *******************************
  public static String stackTraceToString(StackTraceElement[] ste) {
    String str = "";
    for (int i = 0; i < ste.length ; i++) {
      str += String.format("    [%02d] %s \n", i, ste[i].toString());
    }
    return str;
  }


  // ********************************
  // print to both
  public static void print(PrintStream ps, String buf) {
    System.out.print(buf);
    if (ps != null) {
      ps.print(buf);
    }
  }



  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####             A S P E C T       U T I L I T Y                   ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################


  // ************************************************
  public static boolean assertCtx(JoinPoint jp, Context ctx) {
    if (ctx == null) {
      Util.WARNING(jp, "context is null");
      return false;
    }
    return true;
  }

  // ************************************************
  public static void WARNING(JoinPoint jp, String msg) {
    pre(null);
    System.out.println("## WARNING: " + msg);
    System.out.println("##    File: " + jp.getSourceLocation().getFileName());
    System.out.println("##    Line: " + jp.getSourceLocation().getLine());
    System.out.println("## JoinPnt: " + jp.toString());
    post(null);
    printStackTrace(null);
    post(null);
  }

  // ************************************************
  public static void WARNING_ONELINE(JoinPoint jp, String msg) {
    System.out.println("## WARNING: " + msg);
  }

  // ************************************************
  public static void MESSAGE(JoinPoint jp, String msg) {

    System.out.println();
    System.out.println("## ############################################");
    System.out.println("## MESSAGE: " + msg);
    System.out.println("##    File: " + jp.getSourceLocation().getFileName());
    System.out.println("##    Line: " + jp.getSourceLocation().getLine());
    System.out.println("## JoinPnt: " + jp.toString());
    System.out.println("## ############################################");
    System.out.println();

  }


  // ************************************************
  public static void ERROR(JoinPoint jp, String msg) {

    System.out.println();
    System.out.println("## ############################################");
    System.out.println("##   ERROR: " + msg);
    System.out.println("##    File: " + jp.getSourceLocation().getFileName());
    System.out.println("##    Line: " + jp.getSourceLocation().getLine());
    System.out.println("## JoinPnt: " + jp.toString());
    System.out.println("## ############################################");
    System.out.println();
    printStackTrace();
  }


  // ************************************************
  public static void FATAL(JoinPoint jp, String msg) {

    System.out.println();
    System.out.println("## ############################################");
    System.out.println("##   FATAL: " + msg);
    System.out.println("##    File: " + jp.getSourceLocation().getFileName());
    System.out.println("##    Line: " + jp.getSourceLocation().getLine());
    System.out.println("## JoinPnt: " + jp.toString());
    System.out.println("## ############################################");
    System.out.println();
    printStackTrace();
    if(ENABLE_FATAL) {
    Util.runCommand("killall java");
    }
  }


  // ************************************************
  public static String getClassName(JoinPoint jp) {
    Signature sig = jp.getSignature();
    Class c = sig.getDeclaringType();
    String tmp = c.getName();
    Package p = c.getPackage();
    return tmp.replaceFirst(p.getName()+".", "");
  }

  // ************************************************
  public static String getMethodName(JoinPoint jp) {
    Signature sig = jp.getSignature();
    return sig.getName();
  }

  // ************************************************
  public static String getPackageName(JoinPoint jp) {
    Signature sig = jp.getSignature();
    Class c = sig.getDeclaringType();
    Package p = c.getPackage();
    return p.getName();
  }


  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                J O L          U T I L I T Y                   ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################


  // ************************************************
  public static Table getTable(JolSystem system, String progName, String tableName) {
    Table t = system.catalog().table(new TableName(progName, tableName));
    if (t == null) {
      Util.ERROR("Table for " + progName + "::" + tableName + " does not exist");
      if(ENABLE_FATAL) {
      Util.runCommand("killall java");
      }
      return null;
    }
    return t;
  }


  // ************************************************
  public static void printEvent(FMJoinPoint fjp,
                                FMStackTrace fst,
                                PrintStream ps,
                                String pn,
                                String tn,
                                Object... values)  {
    print(ps, "\n");
    printEventTuples(ps, pn, tn, values);
    printEventJoinPoint(ps, fjp);
    printEventStack(ps, fst);
    print(ps, "\n");
  }

  // ************************************************
  public static void printEventTuples(PrintStream ps,
                                      String pn,
                                      String tn,
                                      Object... values)  {
    print(ps, "## Begin Frog Event Tuples: \n");
    print(ps, "    Program Name : " + pn + "\n");
    print(ps, "    Table Name   : " + tn + "\n");
    print(ps, "    Tuple Length : " + values.length + "\n");
    for (int i = 0; i < values.length; i++) {
      String type;
      if (values[i] instanceof String)       type = "String";
      else if (values[i] instanceof Integer) type = "Integer";
      else                                   type = "Unknown";

      String s = String.format("%2d : %-8s : %s \n", i, type, values[i].toString());
      print(ps, "      " + s);
    }
    print(ps, "## End Frog Event Tuples: \n");
  }

  // ************************************************
  public static void printEventStack(PrintStream ps, FMStackTrace fst) {
    print(ps, "## Begin Frog Event Stack: \n");
    if (fst == null) {
      // printStackTrace(ps);
      print(ps, "    Direct call \n");
    }
    else {
      print(ps, fst.toString());
    }
    print(ps, "## End Frog Event Stack: \n");
  }

  // ************************************************
  public static void printEventJoinPoint(PrintStream ps, FMJoinPoint fjp) {
    print(ps, "## Begin Frog Event Join Point: \n");
    if (fjp == null) {
      print(ps, "    null \n");
    }
    else {
      print(ps, fjp.toString() + "\n");
    }
    print(ps, "## End Frog Event Join Point: \n");
  }


  // ************************************************
  public static void scheduleDirectEvent(PrintStream ps,
                                         JolSystem system,
                                         String pn, String tn,
                                         Object... values)  {
    scheduleEvent(null, null, ps, system, pn, tn, values);
  }

  // ************************************************
  // this is an entry point to schedule event
  public static void scheduleEvent(FMJoinPoint fjp,
                                   FMStackTrace fst,
                                   PrintStream ps,
                                   JolSystem system,
                                   String pn, String tn,
                                   Object... values)  {

    printEvent(fjp, fst, ps, pn, tn, values);

    TupleSet ts = new BasicTupleSet();
    ts.add(new Tuple(values));
    scheduleEvent(ps, system, pn, tn, ts);
  }


  // ************************************************
  public static void scheduleEvent(PrintStream ps,
                                   JolSystem system,
                                   String pn, String tn,
                                   TupleSet ts) {
    if (system == null) {
      Util.ERROR(ps, " null system at Util ");
      if(ENABLE_FATAL) {
      Util.runCommand("killall java");
      }
    }

    try {
      system.schedule(pn, new TableName(pn, tn), ts, null);
      system.evaluate();
    } catch (JolRuntimeException e) {
      Util.EXCEPTION(ps, "schedule event", e);
      System.out.println(e);
      if(ENABLE_FATAL) {
      Util.runCommand("killall java");
      }
    }
  }

  // ************************************************
  public static void install(JolSystem system, String olgPath)
    throws JolRuntimeException {
    system.install("model", ClassLoader.getSystemResource(olgPath));
    system.evaluate();
  }


  // ************************************************
  public static boolean isTableEmpty(JolSystem system, String pn, String tn) {
    Table tbl = Util.getTable(system, pn, tn);
    for (Tuple t : tbl.tuples())  {
      return false;
    }
    return true;
  }


  /////////////////////////////////////////////////
  ////////////Cassandra Utility///////////////////
  ////////////////////////////////////////////////
  //JINSU : I haven't tested this out thoroughly.
  //tested this for naturalEndpoints list that is not empty.
  public static ArrayList<InetAddress> orderEndpoints(Token token, TokenMetadata metadata, String table) {
      int replicas = DatabaseDescriptor.getReplicationFactor(table);
      List<Token> tokens = metadata.sortedTokens();
      ArrayList<InetAddress> endpoints = new ArrayList<InetAddress>(replicas);

      if (tokens.isEmpty())
          return endpoints;

      // Add the token at the index by default
      Iterator<Token> iter = TokenMetadata.ringIterator(tokens, token);

      TreeMap ipMap = sortTokens(iter, metadata);

      endpoints = getOrderedEndpoints(ipMap, replicas);

      /* JINSU for testing purpose
         String elements = "";
         for(InetAddress e : sorted) {
         elements = elements + e.toString() + " ::: ";
         }
         System.out.println(elements);
         */

/*
      for(InetAddress e : endpoints) {
          debug("Util.orderEndpoints ==>" + e);
      }
*/


      return endpoints;

  }

  private static TreeMap sortTokens(Iterator<Token> iter, TokenMetadata metadata) {
      TreeMap ipMap = new TreeMap();

      while(iter.hasNext()) {
          InetAddress ip = metadata.getEndPoint(iter.next());
          if(ip instanceof Inet4Address) {
              ipMap.put(ip.hashCode(), ip);
          } else {

               System.out.println("Inet6Address not implemented yet.");
              System.out.println("address ::: " + ip);
              System.out.println("Need to be implemented");
          }
      }
      return ipMap;
  }

  private static ArrayList<InetAddress> getOrderedEndpoints(TreeMap tmap, int replicas) {
      Iterator<InetAddress> ipIter = tmap.values().iterator();
      ArrayList<InetAddress> endpoints = new ArrayList<InetAddress>();

      /* This change made so that seed0 is always the client.
       * And seed1, 2, and 3 always stores the data.
       * Assuming that the first item in the iterator is seed0.
       */
      if(ipIter.hasNext()) {
          //burn off the first item which is seed0
          ipIter.next();
      }

      while (endpoints.size() < replicas && ipIter.hasNext()) {
          endpoints.add(ipIter.next());
      }
      return endpoints;

  }

}
