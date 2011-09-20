
package org.fi;

import java.io.*;
import java.lang.*;


// public class FMStackTraceElement implements Writable {
public class FMStackTraceElement {
  
  private String fileName;
  private String className;
  private String methodName;
  private int lineNumber;
  private int hashCode;
  

  //********************************************
  // writable interface
  // this is serialization (see DatanodeID.java for example)
  // need to decide how to serialize the RPC
  //********************************************
  public void write(DataOutput out) throws IOException {

    // xml rpc
    out.writeUTF(fileName);
    out.writeUTF(className); 
    out.writeUTF(methodName);
    out.writeInt(lineNumber);
    out.writeInt(hashCode);

    // hadoop rpc
    // UTF8.writeString(out, fileName);
    // UTF8.writeString(out, className);
    // UTF8.writeString(out, methodName);
    // out.writeInt(lineNumber);
    // out.writeInt(hashCode);

  }
  
  public void readFields(DataInput in) throws IOException {
    
    // xml rpc
    fileName = in.readUTF();
    className = in.readUTF();
    methodName = in.readUTF(); 
    lineNumber = in.readInt();
    hashCode = in.readInt();

    // hadoop rpc
    // fileName = UTF8.readString(in);
    // className = UTF8.readString(in);
    // methodName = UTF8.readString(in);
    // lineNumber = in.readInt();
    // hashCode = in.readInt();
    
  }

  
  
  //********************************************
  // rest
  //********************************************

  public FMStackTraceElement() {
    
  }
  
  public FMStackTraceElement(StackTraceElement s) { 
    fileName = s.getFileName();
    className = s.getClassName();
    methodName = s.getMethodName();
    lineNumber = s.getLineNumber();
    hashCode = s.hashCode();
  }
  
  public String getFileName() {
    return fileName;
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public int getLineNumber() {
    return lineNumber;
  }
  
  public int getHashCode() {
    return hashCode;
  }

  
}