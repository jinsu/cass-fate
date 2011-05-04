
package org.apache.cassandra;


import java.lang.Thread;
import java.io.*;
import java.lang.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.*;

//JINSU for orderEndpoints function
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.config.DatabaseDescriptor;
//end importing for orderEndpoints function

public final class Util {


  private static PrintStream pps;
  private static MyLong theLastTime = new MyLong(0);
  private static String lastWarning = "";
  static File nodeFlag = new File("/tmp/fi/nodesConnectedFlag");
  static File expFlag = new File("/tmp/fi/experimentRunning");
  static File rebootFlag = new File("/tmp/fi/nodesRebootingFlag");

  // **********************************
  public static class MyLong {
    long l;
    public MyLong() { l = 0; }
    public MyLong(long x) { l = x; }
    public long longValue() { return l;    }
    public void setValue(long x) { l = x; }
  }

  // **********************************
  public static long now() {
    return System.currentTimeMillis();
  }

  // **********************************
  public static String diff() {
    return diff(theLastTime);
  }

  // **********************************
  public static String diff(MyLong lastTime) {
    long currentTime = System.currentTimeMillis();
    double diff = 0.0;
    if (lastTime.longValue() == 0) {
      lastTime.setValue(currentTime);
    }
    else {
      diff = (double)(currentTime - lastTime.longValue()) / 1000;
      lastTime.setValue(currentTime);
    }
    String warning = "";

    if (diff > 1) {
      warning = "*LONG!!*";
    }

    if (diff > 5) {
      warning = "*LONG LONG!!*";

      warning += ("\n" + getStackTrace());
    }

    if (diff > 30) {
      warning = "*LONG LONG LONG!!*";

      warning += ("\n" + getStackTrace());

    }

    lastWarning = warning;

    long ct = (currentTime / 1000) % 1000;

    long tid = (Thread.currentThread().getId()) % 100;

    return String.format("[hd-diff][%5.3f secs] [%d] (t-%d) %s",
                         diff, (int)ct, (int)tid, warning);

  }
//JINSU checks if readRepairFlag exists
	public static boolean checkRRFlag(){
	File f = new File("/tmp/fi/readRepairFlag");
	return f.exists();
}

  // **************************************
  public static void debug(String msg) {

    // only print if experiment is running
    File f = new File("/tmp/fi/experimentRunning");
    if (f.exists()) {
      msg = msg + " [part of workload]";
    }

    debug(pps, msg);
  }

  // **************************************
  public static void debug(PrintStream ps, String msg) {
    boolean debug = true;
    //boolean debug = false;
    if (debug) {
	    if(ps == null) {
		    //JINSU
		    setupPrintStream("/tmp/fi/debug.txt");
	    }
      println(ps, msg);
    }
  }



  // **************************************
  public static void printStackTrace(Exception e) {
    if (e == null)
      print(getStackTrace());
    else
      print(stackTraceToString(e.getStackTrace()));
  }

  // **************************************
  public static void printStackTrace() {
    printStackTrace(null);
  }

  // **************************************
  public static String getStackTrace() {
    Thread t = Thread.currentThread();
    StackTraceElement[] ste = t.getStackTrace();
    return stackTraceToString(ste);
  }

  // **************************************
  public static String stackTraceToString(StackTraceElement[] ste) {
    String str = "";
    for (int i = 0; i < ste.length ; i++) {
      str += String.format("    [%02d] %s \n", i, ste[i].toString());
    }
    return str;
  }

  // ############################################################
  // ##               PRINTING UTILITY                         ##
  // ############################################################
  public static void setupPrintStream(String filename) {

    try {
      FileOutputStream fos = new FileOutputStream(filename);
      pps = new PrintStream(fos);
    } catch (Exception e) {
      EXCEPTION("Can't open " + filename, e);
      System.exit(-1);
    }
  }

  // **************************************
  public static void EXCEPTION(String msg, Exception e) {
	  //pre();
	  print("## EXCEPTION: " + msg + "\n");
	  print("## ---------------------- Exception:\n");
	  print(e.toString() + "\n");
	  if (e.getCause() != null) {
		  print("## ---------------------- Cause: \n");
		  print(e.getCause().toString() + "\n");
	  }
	  print("## ---------------------- Trace: \n");
	  printStackTrace(e);
	  //post();
  }


  // *****************************************
  public PrintStream getPrintStream() {
    return pps;
  }  // **************************************

  public static void setPrintStream(PrintStream ppsArg) {
    pps = ppsArg;
  }


  // **************************************
  public static void print(String msg) {
    print(pps, msg);
  }

  // **************************************
  public static void print(PrintStream ps, String msg) {
    System.out.print(msg);
    System.out.flush(); // ??
    if (ps != null) {
      ps.print(msg);
    }

  }



  // **************************************
  public static void println(PrintStream ps, String msg) {
    print(ps, msg + "\n");
  }
  
  public static void sleepWhileExperimentRunning(String who) {

    boolean sleep = true;
    //boolean sleep = false;

    while (sleep) {
      if (expFlag.exists()) {
        debug("- Experiment running, sleeping ===> " + who);
        sleep(1000);
        //return true;
      } else if (nodeFlag.exists()) {
        debug("- Nodes connected, sleeping ===> " + who);
        sleep(1000);
      } else {
        debug("- Experiment not running, continue ===>" + who);
        break;
        //return false;
      }
    }
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
    return pid;
  }


  // *****************************************
  // a tool to run the command
  public static String runCommand(String cmd) {

    String msg = "";
    try {
      
      String line;
      
      Process p = Runtime.getRuntime().exec(cmd);
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
      System.out.println("runcmd!!! " + e); 
      //System.err.println("runcmd!!!");
    }

    return msg;
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
  public static boolean stringToFileContent(String buf, File f, boolean flush) {

    assertSafeFile(f);

    try {
      FileWriter fw = new FileWriter(f);
      fw.write(buf);
      if (flush)
        fw.flush();
      fw.close();
    } catch (IOException e) {

      return false;
    }
    return true;
  }

  // **************************************
  public static void assertSafeFile(File f) {
    assertSafeFile(f.getAbsolutePath());
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
      System.out.println("Dangerous!!!");
      System.err.println("Dangerous!!!");
      System.out.flush();
      System.exit(-1);
    }
  }


  // **************************************
  public static String getThreadName() {
    Thread t = Thread.currentThread();
    String name = t.getName();
    return name;
  }


  // *****************************************
  public static void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (Exception e) {}

  }

  

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


      for(InetAddress e : endpoints) {
        debug("Util.orderEndpoints ==>" + e);
      }
     
      
      
      return endpoints;
    
  }
  
  private static TreeMap sortTokens(Iterator<Token> iter, TokenMetadata metadata) {
    TreeMap ipMap = new TreeMap();

    while(iter.hasNext()) {
      InetAddress ip = metadata.getEndPoint(iter.next());
      if(ip instanceof Inet4Address) {
        ipMap.put(ip.hashCode(), ip);
      } else {
        Util.debug("Inet6Address not implemented yet.");
        Util.debug("address ::: " + ip);
        Util.debug("Need to be implemented");
      }
    }
    return ipMap;
  }

  private static ArrayList<InetAddress> getOrderedEndpoints(TreeMap tmap, int replicas) {
   Iterator<InetAddress> ipIter = tmap.values().iterator();
   ArrayList<InetAddress> endpoints = new ArrayList<InetAddress>();
   while (endpoints.size() < replicas && ipIter.hasNext()) {
     endpoints.add(ipIter.next());
   }
   return endpoints;

  }
  
}





