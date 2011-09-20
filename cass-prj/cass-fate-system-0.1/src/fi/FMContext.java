
package org.fi;


// ************************************ java
import java.io.*;
import java.lang.*;
import java.lang.management.ManagementFactory;





//public class FMContext implements Writable {
public class FMContext {

  // to test memory, if there is any leaakge, we'll hit an exception
  // private char [] big = new char [1024*1024];

  private int cutpointRandomId = 0;
  private String targetIO = "";
  private String nodeId = "Unknown";
  private int pid = 0;
  private String messageType = "Undecided";


  //********************************************
  // writable interface
  // this is serialization (see DatanodeID.java for example)
  // need to decide how to serialize the RPC
  //********************************************
  public void write(DataOutput out) throws IOException {

    // xml rpc
    out.writeInt(cutpointRandomId);
    out.writeUTF(targetIO);
    out.writeUTF(nodeId);
    out.writeInt(pid);
    out.writeUTF(messageType);

    // hadoop rpc
    // out.writeInt(cutpointRandomId);
    // UTF8.writeString(out, targetIO);
    // UTF8.writeString(out, nodeId);
    // out.writeInt(pid);
  }

  public void readFields(DataInput in) throws IOException {

    // xml rpc
    cutpointRandomId = in.readInt();
    targetIO = in.readUTF();
    nodeId = in.readUTF();
    pid = in.readInt();
    messageType = in.readUTF();

    // hadoop rpc
    // cutpointRandomId = in.readInt();
    // targetIO = UTF8.readString(in);
    // nodeId = UTF8.readString(in);
    // pid = in.readInt();

  }



  //********************************************
  // rest
  //********************************************

  public FMContext() {
    setAttributes();
  }

  public FMContext(String s) {
    targetIO = new String(s);
    setAttributes();
  }

  public FMContext(String s, String mtype) {
   this(s);
   setMessageType(mtype);
  }

  private void setAttributes() {
    setPidAndNodeId();
  }

  public void setPidAndNodeId() {

    pid = Util.getIntPid();

    String pidStr = Util.getPid();
    nodeId = Util.getNodeIdFromPid(pidStr);

  }

  public int getPid() {
    return pid;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setCutpointRandomId() {
    cutpointRandomId = Util.r8();
  }

  public int getCutpointRandomId() {
    return cutpointRandomId;
  }

  public String getTargetIO() {
    return targetIO;
  }

  private String getTargetIOPrint(String f) {

    String forPrint = "  [targetIO][";
    int width = 58;
    int i;
    for (i = 0; i+width < f.length() ; i += width) {
      forPrint += f.substring(i, i+width);
      forPrint += "\n     ";
    }
    forPrint += f.substring(i, f.length());
    forPrint += "]";
    return forPrint;
  }


  public String toString() {
    String tmp = targetIO;
    if (targetIO.contains("/tmp/" + FMLogic.CASS_USERNAME))
      tmp = tmp.replaceFirst("/tmp/" + FMLogic.CASS_USERNAME, "/thh/");
    if (targetIO.contains(FMLogic.CASS_STORAGE_DIR))
      tmp = tmp.replaceFirst(FMLogic.CASS_STORAGE_DIR, "/rhh/");
    String buf = String.format("  [targetIO][%s] at *%s* :: [%s] message \n", tmp, nodeId, messageType);
    return buf;
  }


  public void setTargetIO(String f) {
    targetIO = new String (f);
  }

  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String t) {
    messageType = new String (t);
  }


}
