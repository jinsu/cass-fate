
package org.fi;

import java.io.*;
import java.lang.*;


// this context must be pure and have no dependence at all

public class Context {

  private String targetIO = "";

  private Object extraContext = "";

  private int port = 0;

  private final String TMPFI = "/tmp/fi/";
  // private final String HADOOP_USERNAME = "hadoop-haryadi";
  private final String HADOOP_USERNAME = "hadoop-" + System.getenv("USER") ;
  private final String HADOOP_STORAGE_DIR = TMPFI + HADOOP_USERNAME + "/";

  private String messageType = "Unidentified";

  //********************************************
  // rest
  //********************************************

  public Context() {

  }

  public Context(String s) {
    targetIO = new String(s);
  }

  public Context(int port) {
    this.port = port;
  }

  public Context(String s, String mtype) {
    this(s);
    setMessageType(mtype);
  }

  public String getMessageType() {
      return messageType;
  }

  public void setMessageType(String t) {
      messageType = new String(t);
  }

  public int getPort() {
    return port;
  }

  public String getTargetIO() {
    return targetIO;
  }

  public void setExtraContext(Object extra) {
    this.extraContext = extra;
  }

  public Object getExtraContext() {
    return extraContext;
  }

  public String toString() {
    String tmp = targetIO;
    if (targetIO.contains("/tmp/" + HADOOP_USERNAME))
      tmp = tmp.replaceFirst("/tmp/" + HADOOP_USERNAME, "/thh/");
    if (targetIO.contains(HADOOP_STORAGE_DIR))
      tmp = tmp.replaceFirst(HADOOP_STORAGE_DIR, "/rhh/");
    return tmp;
  }

  public void setTargetIO(String f) {
    targetIO = new String (f);
  }

}

