
package org.fi;


import java.io.*;
import java.util.*;
import java.nio.*;

import java.lang.Thread;
import java.lang.StackTraceElement;
import java.lang.Math;


public class Utility {
  
  // private static Map<String,String> pidCacheMap = new TreeMap<String, String>();
  
  public static PrintStream ps = null;
  
  public Utility() {  
    
  }


  // ############################################################
  // ##                 GENERAL UTILITY                        ##
  // ############################################################

  private Random random = new Random();

  public int r() { return Math.abs(random.nextInt()); }
  
  // random to 4th digit
  public int r4() { return (Math.abs(random.nextInt()) % 10000); }

  // random to 8th digit
  public int r8() { return (Math.abs(random.nextInt()) % 100000000); }
  

  // ############################################################
  // ##               PRINTING UTILITY                         ##
  // ############################################################
  public void setupPrintStream(String filename) {

    try {
      FileOutputStream fos = new FileOutputStream(filename);
      ps = new PrintStream(fos);
    } catch (Exception e) {
      EXCEPTION("Can't open " + filename, e);
      System.exit(-1);
    }
  }

  // ***************************************** 
  // print to both output shell and to the print stream
  public void print(String buf) {
    System.out.print(buf);
    ps.print(buf);
  }


  // ############################################################
  // ##                   HDFS UTILITY                         ##
  // ############################################################


  // *****************************************
  // given an tmp-pid file, get the pid by reading the file
  public String getPidFromTmpPid(File f) {
    String pid = null;
    if (!f.exists())
      return pid;
    try {
      BufferedReader in = new BufferedReader(new FileReader(f));
      pid = in.readLine();
      if (pid == null) {
	WARNING("pid is null");
	return pid;
      }
      in.close();
    } catch (Exception e) {
      EXCEPTION("getPidFromTmpPid ", e);
    }
    return pid;
  }


  // ############################################################
  // ##                   OS UTILITY                           ##
  // ############################################################


  // *****************************************
  public void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (Exception e) {}

  }


  // *****************************************
  // find out if pid is a java alive, by calling the shell
  // ps -c -p pid , and then grep the output and see
  // if "java" is in there or not
  public boolean isPidAlive(String pid) {
    String cmdout = runCommand ("ps -c -p " +  pid);
    if (cmdout.contains("java")) 
      return true;
    return false;
  }


  // *****************************************
  // a tool to run the command 
  public String runCommand(String cmd) {

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
    } catch (Exception e) {
      EXCEPTION("runCommand", e);
    }
    
    return msg;
  }



  // ############################################################
  // ##                   FILE UTILITY                         ##
  // ############################################################

  // **************************************
  public void copyFile(String from, String to) {
    File ffrom = new File(from);
    File fto = new File(to);

    if (!ffrom.exists())
      WARNING("in copyFile: from does not exist: " + from);
    if (fto.exists())
      WARNING("in copyFile: to exist: " + to);

    String cmdout = runCommand ("cp " +  from + " " + to);
    print(cmdout + "\n");
  }
  

  // **************************************
  // mkDir
  public boolean mkDir(String dirname) { 
    return mkDir(new File(dirname));
  }

  // **************************************
  // mkDir
  public boolean mkDir(File dir) {
    try {
      print(String.format("    Creating new dir  '%s' \n", dir.getAbsolutePath()));
      dir.mkdir();
    } catch (Exception e) {
      WARNING("can't mkdir " + dir);
      return false;
    }
    return true;
  }

  // **************************************
  public boolean createNewFile(String path) {
    return createNewFile(new File(path));
  }
  
  // **************************************
  public boolean createNewFile(File f) {
    try {
      boolean b = f.createNewFile();
      if (b) {
	print(String.format("    Creating new file '%s' \n", f.getAbsolutePath()));
      }
      return b;
    } catch (Exception e) {
      return false;
    }
  }

  // **************************************
  // convert string to file, and delete
  public boolean deleteDir(String dirname) { 
    return deleteDir(new File(dirname));
  }


  // **************************************
  public void assertSafeFile(String fullPath) {
    
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
      System.exit(-1);
    }
  }

  // **************************************
  public void assertSafeFile(File f) {
    assertSafeFile(f.getAbsolutePath());
  }

  // **************************************
  // do the actual deletion, CAREFUL !!!
  // I only allow deletion if the file is under /tmp/
  // if your files are under different folder
  // just change the flags here
  public boolean doDelete(File f) { 
    
    assertSafeFile(f);
    
    // if not exist, we're fine, just return true
    if (!f.exists())
      return true;

    // do the actual deletion
    if (f.isDirectory()) {
      print(String.format("    Deleting dir  '%s' \n", f.getAbsolutePath()));
    }
    else {
      print(String.format("    Deleting file '%s' \n", f.getAbsolutePath()));
    }
    return f.delete();
  }
  

  // **************************************
  // delete the file
  public boolean deleteFile(String path) { 
    return doDelete(new File(path));
  }

  // **************************************
  // delete the file
  public boolean deleteFile(File f) { 
    return doDelete(f);
  }


  // **************************************
  public boolean deleteFile(String parent, String fname) { 
    return doDelete(new File(parent, fname));
  }


  // **************************************
  // recursively delete the content of a directory AND this directory
  public boolean deleteDir(File dir) { 
    

    if (dir.isDirectory()) { 
      String[] children = dir.list(); 
      for (int i=0; i<children.length; i++) { 
	boolean success = deleteDir(new File(dir, children[i])); 
	if (!success) { 
	  return false; 
	} 
      } 
    } 
        // The directory is now empty so delete it     
    return doDelete(dir); 
  } 
  
  
  // **************************************
  // just delete the content of the directory BUT NOT
  // the directory
  public boolean deleteDirContent(String dirname) { 
    
    File dir = new File(dirname);
    if (dir.isDirectory()) { 
      String[] children = dir.list(); 
      for (int i=0; i< children.length; i++) { 

	if (dirname.contains("exp")) {
	  System.out.format("  [%d][%s] \n", i, children[i]);
	}

	boolean success = deleteDir(new File(dir, children[i])); 
	if (!success) { 
	  return false; 
	} 
      } 
    } 
    return true;
  }

  
  // **************************************
  // just delete the content of the directory THAT 
  // CONTAINS the substring 
  public boolean deleteDirContent(String dirname, String substring) { 
    File dir = new File(dirname);
    if (dir.isDirectory()) { 
      String[] children = dir.list(); 
      for (int i=0; i< children.length; i++) { 
	if (children[i].contains(substring)) {
	  File f = new File(dir, children[i]); 
	  boolean success = doDelete(f);
	  if (!success) { 
	    return false; 
	  } 
	}
      } 
    } 
    return true;
  }


  // **************************************
  public String fileContentToString(String path) {
    File f = new File(path);
    return fileContentToString(f);
  }


  // **************************************
  public String fileContentToString(File f) {
    String buf = "";

    if (!f.exists()) {
      buf = String.format("EMPTY FILE CONTENT, FILE %s NOT EXIST", 
			  f.getAbsolutePath());
      return buf;
    }
    
    // read the file
    try {
      BufferedReader in = new BufferedReader(new FileReader(f));
      String line;
      while ((line = in.readLine()) != null) {
	buf = buf + line + "\n";
      }
      in.close();
    } catch (Exception e) {
      EXCEPTION("fileContentToString", e);
      return String.format("EXCEPTION READING FILE %s ", 
			   f.getAbsolutePath());
    }
    return buf;
  }


  // **************************************
  public boolean stringToFileContent(String s, String fname) {
    return stringToFileContent(s, new File(fname));
  }

  // **************************************
  public boolean stringToFileContent(String s, File f) {
    return stringToFileContent(s, f, false);
  }

  // **************************************
  public boolean stringToFileContent(String s, File f, boolean flush) {
    assertSafeFile(f);
    try {
      // boolean rv = f.createNewFile(); ?? 
      FileWriter fw = new FileWriter(f);
      fw.write(s);
      if (flush)
        fw.flush();
      fw.close();
    } catch (IOException e) {
      EXCEPTION("stringToFileContent", e);
      ERROR("should not throw exception here");
      return false;
    }
    return true;
  }
  

  
  // ############################################################
  // ##               ERROR MESSAGING UTILITY                  ##
  // ############################################################


  // **************************************
  public void pre() {
    print("\n");
    print("## ############################################\n");
  }


  // **************************************
  public void post() {
    print("## ############################################\n");
    print("\n");
  }
  

  // **************************************
  public void ERROR(String msg) {
    pre();
    print("## ERROR: " + msg + "\n");
    printStackTrace();
    post();
  }

  // **************************************
  public void FATAL(String msg) {
    pre();
    print("## FATAL: " + msg + "\n");
    printStackTrace();
    post();
    System.exit(-1);
  }


  // **************************************
  public void killAllJava() {
    print("should kill all java now\n");
  }


  // **************************************
  public void WARNING(String msg) {
    pre();
    print("## WARNING: " + msg + "\n");
    post();
  }
  

  // **************************************
  public void WARNING_ONELINE(String msg) {
    print("## WARNING: " + msg);
  }
  

  // **************************************
  public void EXCEPTION(String msg, Exception e) {
    pre();
    print("## EXCEPTION: " + msg + "\n");
    print("## ---------------------- Exception:\n");
    print(e.toString() + "\n");
    if (e.getCause() != null) {
      print("## ---------------------- Cause: \n");
      print(e.getCause().toString() + "\n");
    }
    print("## ---------------------- Trace: \n");
    printStackTrace(e);
    post();
  }
  

  // **************************************
  public void printStackTrace(Exception e) {
    if (e == null)
      print(getStackTrace());
    else 
      print(stackTraceToString(e.getStackTrace()));
  }

  // **************************************
  public void printStackTrace() {
    printStackTrace(null);
  }
  
  // **************************************
  public String getStackTrace() {
    Thread t = Thread.currentThread();
    StackTraceElement[] ste = t.getStackTrace();
    return stackTraceToString(ste);
  }

  // **************************************
  public String stackTraceToString(StackTraceElement[] ste) {
    String str = "";
    for (int i = 0; i < ste.length ; i++) {
      str += String.format("    [%02d] %s \n", i, ste[i].toString());
    }
    return str;
  }

  // **************************************
  public void MESSAGE(String msg) {
    pre();
    print("## MESSAGE: " + msg);
    post();
  }



}