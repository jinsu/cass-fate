


package org.fi;

import java.io.*;
import java.lang.*;

//import org.apache.hadoop.io.UTF8;
//import org.apache.hadoop.io.Writable;


// public class FrogEvent implements Writable {
public class FrogEvent {

  public String pn;
  public String tn;
  
  // public so one can set this up directly
  public String [] tuple;
  
  public void write(DataOutput out) throws IOException {

    // xml rpc
    out.writeUTF(pn);
    out.writeUTF(tn);
    out.writeInt(tuple.length);
    for (int i = 0; i < tuple.length; i++) 
      out.writeUTF(tuple[i]);

    // hadoop rpc
    // UTF8.writeString(out, pn);
    // UTF8.writeString(out, tn);
    // out.writeInt(tuple.length);
    // for (int i = 0; i < tuple.length; i++) 
    // UTF8.writeString(out, tuple[i]);

  }

  public void readFields(DataInput in) throws IOException {
    
    // xml rpc
    pn = in.readUTF();
    tn = in.readUTF();
    int length = in.readInt();
    tuple = new String[length];
    for (int i = 0; i < length; i++) {
      tuple[i] = in.readUTF();
    }
    
    // hadoop rpc
    // pn = UTF8.readString(in);
    // tn = UTF8.readString(in);
    // int length = in.readInt();
    // tuple = new String[length];
    // for (int i = 0; i < length; i++) {
    // tuple[i] = UTF8.readString(in);
    // }
    
  }

  // this must exist!!!!!
  public FrogEvent() {

  }


  public FrogEvent(String pnArg, String tnArg, String... args) {
    pn = pnArg;
    tn = tnArg;
    tuple = args;
  }
  
  public String toString() {
    String s = String.format("%s:%s <", pn, tn);
    for (int i = 0; i < tuple.length; i++) {
      if (i == tuple.length - 1) s += String.format("%s>", tuple[i]);
      else                       s += String.format("%s, ", tuple[i]);
    }    
    return s;
  }
  
}
