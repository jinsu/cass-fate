
package org.fi;

import org.fi.*;
import org.fi.FMServer.FailType;
import org.fi.FMJoinPoint.*;

import java.io.*;
import java.util.Random;





public class ManualFI {

  private static int maxFailCount = 2;

  public ManualFI() {  }
  
  public void resetFailCount(int fc) {
    try {
      File fcFile = new File("/tmp/failCount.txt");
      fcFile.delete();
      FileOutputStream fos = new FileOutputStream(fcFile);    
      DataOutputStream dos = new DataOutputStream(fos);
      dos.writeInt(fc);
      dos.flush();
    } catch (Exception e) {
      Util.EXCEPTION("at resetFailCount", e);
    }
  }
  
  // try to increment, if successful, then it's a go
  public boolean incrementFailCount() {
    try {
      int curFC;
      File fcFile = new File("/tmp/failCount.txt");
      FileInputStream fis = new FileInputStream(fcFile);    
      DataInputStream dis = new DataInputStream(fis);
      curFC = dis.readInt();
      if (curFC < maxFailCount) {
	dis.close(); fis.close(); 
	resetFailCount(curFC+1);
	return true;
      }	
      else {
	return false;
      }
    } catch (Exception e) {
      // Util.EXCEPTION("at incrementFailCount", e);
      return false;
    }
  }

  // *************************************************
  // doFail_01: write to edit log fail, reboot, then read long fail
  public FailType doFail_01(FMJoinPoint fjp, FMContext ctx, FMStackTrace st) {
    
    FailType fail;
    
    fail = f01a_WriteToEditLogInName1(fjp, ctx, st);
    if (fail != FailType.CRASH) return fail;
    
    fail = f01b_ReadLongFstimeInName1(fjp, ctx, st);
    if (fail != FailType.CRASH) return fail;
    
    return FailType.CRASH;
  }


  // *************************************************
  // doFail_02: crash at datanode writes to data block and meta block
  public boolean doFail_02(FMAllContext fac) {

    // have I insert a single crash yet?
    // if not then move on .. if done, then return
    // File firstFailure = new File(FMLogic.FIRST_FAILURE_FLAG);
    // if (firstFailure.exists())
    // return false;



    // just an optimization .. specifically we're interested
    // in client write and datanode failure only 
    // hence we only "filter" data file/meta write
    // [file][/rhh/dfs/data1/tmp/blk_7307724612204181421]

    // and for network stream, node id should not be client (unknown so far)
    boolean passFilter = false;
    //if (FIState.isBlock(fac.ctx.getTargetIO()))
    //passFilter = true;

    
    // I only want to fail a datanode and net IO 
    // if (Util.isNetIOtoDataNode(fac.ctx.getTargetIO()) &&
    // fac.ctx.getNodeId().contains("DataNode"))
    // passFilter = true;
    
    // I only want to fail if this is a datanode
    if (fac.ctx.getNodeId().contains("DataNode"))
      passFilter = true;
    

    if (passFilter == false)
      return false;
    
    
    // build the FIState
    FIState fis = new FIState(fac, FailType.CRASH);
    
    
    // this is just an optimization, because actually
    // we can use the hash file to decide if we want to
    // fail or not
    // boolean isNew = FIState.addIfNew(fis);
    // if (!isNew)
    // return false;
    
    // let's check if we have injected this crash
    // or not in the past
    // if (fis.isFailedBefore()) {
    // System.out.format("_We have injected %d in the past_\n", 
    // fis.getCompleteHashId());
    // return false;
    // }    
    

    // let's inject the crash, 
    // 1) record the single-crash mode
    // 2) we want to remember the failure point
    // 3) by returning true
    // fis.recordToFile();
    // try {
    // boolean rv = firstFailure.createNewFile();
    // } catch (IOException e) { Util.EXCEPTION("weird", e);}
    return true;

    
  }

  // *************************************************
  // doFail_03: crashing at points where a crash in 
  // the pipeline does not cause a client to be dead
  // the cases here are specific ... no need to remember history
  // *************************************************
  public boolean doFail_03(FMAllContext fac) {

    // basically we create a filter for doFail_02 ..
    // that is
    
    if (fac.ctx.getNodeId().equals("DataNode-1") && 
	fac.fjp.contains("call(void java.io.OutputStream.write(byte[], int, int))")
	) {
      return doFail_02(fac);
    }
    

    return false;

    
  }



  // **********************************************
  public FailType f01a_WriteToEditLogInName1(FMJoinPoint fjp, 
					     FMContext ctx, 
					     FMStackTrace st) {
    
    boolean cond1, cond2, cond3, cond4;
    cond1 = cond2 = cond3 = cond4 = false;

    // cond1 .. f01a should not exist
    File f1 = new File("/tmp/fail01a");
    if (!f1.exists()) cond1 = true;
    
    // cond2 .. f01b should not exist
    File f2 = new File("/tmp/fail01b");
    if (!f2.exists()) cond2 = true;
    
    // second it must be the edit log under name1
    // [.../dfs/name1/current/edits.new] or edits
    if (ctx.getTargetIO().contains("dfs/name1/current/edits"))
      cond3 = true;

    // first it must be under the context of logSync
    // [0] io.DataOutputBuffer (writeTo:113)
    // [1] namenode.EditLogOutputStream (flush:89)
    // [2] namenode.FSEditLog (logSync:994)
    // [3] namenode.FSNamesystem (mkdirs:1732)
    // [4] namenode.NameNode (mkdirs:553)
    if (st.contains("DataOutputBuffer", "writeTo") &&
	st.contains("EditLogOutputStream", "flush") &&
	st.contains("FSEditLog", "logSync") &&
	st.contains("FSNamesystem", "mkdirs") &&
	st.contains("NameNode", "mkdirs")) {
      cond4 = true;
    }

    // let's fail! now I need to remember this failure has happened
    if (cond1 && cond2 && cond3 && cond4)  {
      File f3 = new File("/tmp/fail01a");
      try { f3.createNewFile(); } catch (Exception e) { Util.ERROR(" ex 1"); };
      return FailType.EXCEPTION;
    }
    
    return FailType.CRASH;
  }

  public FailType f01b_ReadLongFstimeInName1(FMJoinPoint fjp, 
					    FMContext ctx, 
					    FMStackTrace st) {

    boolean cond1, cond2, cond3, cond4, cond5;
    cond1 = cond2 = cond3 = cond4 = cond5 = false;
   
    // second it must be the edit log under name1
    // [.../dfs/name1/current/fstime]
    if (ctx.getTargetIO().contains("dfs/name1/current/fstime"))
      cond1 = true;

    if (fjp.contains("DataInputStream.readLong()"))
      cond2 = true;
    
    // and readLong during startup only !!
    // SourceLoc: [575] [FSImage.java]
    // call(long java.io.DataInputStream.readLong())
    // [0] namenode.FSImage (readCheckpointTime:575)
    // [1] namenode.FSImage (loadFSImage:777)
    // [2] namenode.FSImage (recoverTransitionRead:369)
    // [3] namenode.FSDirectory (loadFSImage:95)
    // [4] namenode.FSNamesystem (initialize:315)
    // [5] namenode.FSNamesystem (<init>:292)
    // [6] namenode.NameNode (initialize:204)
    // [7] namenode.NameNode (<init>:288)
    // [8] namenode.NameNode (createNameNode:967)
    // [9] namenode.NameNode (main:976)
    // [T] TOTAL HASH CODE: [4345152]
    if (st.contains("FSImage"        , "readCheckpointTime") &&
	st.contains("FSImage"        , "loadFSImage") &&
	st.contains("FSDirectory"    , "loadFSImage") &&
	st.contains("FSNamesystem"   , "initialize") &&
	st.contains("NameNode"       , "initialize") &&
	st.contains("NameNode"       , "createNameNode") &&
	st.contains("NameNode"       , "main"))  {
      cond3 = true;
    }
    
    // condition 4, f01a must happen first, but only if cond1-3 is true
    File f1 = new File("/tmp/fail01a");
    if (f1.exists()) cond4 = true;
    
    File f2 = new File("/tmp/fail01b");
    if (!f2.exists()) cond5 = true;
    
    // let's fail and rememer this
    if (cond1 && cond2 && cond3 && cond4 && cond5) {
      File f3 = new File("/tmp/fail01b");
      try { f3.createNewFile(); } catch (Exception e) { Util.ERROR(" ex 2");};
      return FailType.CORRUPTION;
    }
    
    return FailType.CRASH;
    
  }
}