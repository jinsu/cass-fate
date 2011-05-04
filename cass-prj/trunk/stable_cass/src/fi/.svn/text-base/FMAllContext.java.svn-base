
package org.fi;

// This is just a class that covers all context


import org.fi.*;
import java.io.*;

public class FMAllContext {
  
  public FMJoinPoint fjp = null;
  public FMContext ctx = null;
  public FMStackTrace fst = null;


  // ********************************************
  public void write(DataOutputStream out) throws IOException {
    fjp.write(out);
    ctx.write(out);
    fst.write(out);
  }

  // ********************************************
  public void readFields(DataInputStream in) throws IOException {
    fjp = new FMJoinPoint();
    fjp.readFields(in);

    ctx = new FMContext();
    ctx.readFields(in);

    fst = new FMStackTrace();
    fst.readFields(in);

  }


  // ********************************************
  public FMAllContext() {

  }
  
  // ********************************************
  public FMAllContext(FMJoinPoint fjp, 
		      FMContext ctx, 
		      FMStackTrace fst) {

    this.fjp = fjp;
    this.ctx = ctx;
    this.fst = fst;
  }
  

  // ********************************************
  public String toString() {

    String buf = "";

    buf += "\nFM JOIN POINT:\n";
    buf += fjp.toString();

    buf += "\nFM CONTEXT:\n";
    buf += ctx.toString();

    buf += "\nFM STACK TRACE:\n";
    buf += fst.toString();

    buf += "\n-----------------------------------\n";

    return buf;

  }

  
}