
package org.fi;

import org.fi.FMServer.*;

import java.io.*;
import java.lang.*;


import java.util.Map;
import java.util.TreeMap;


public class FIState {

  public FMJoinPoint fjp;
  public FMContext ctx;
  public FMStackTrace fst;
  public FailType ft;
  
  String targetIoType   = "Unknown";
  String diskId   = "Unknown";
  String fileType = "Unknown";
  String targetNode = "Unknown";
  
  int completeHashId;
  String completeHashIdStr;

  int staticHashId;
  String staticHashIdStr;
  
  
  //********************************************
  // public FIState() { }

  //********************************************
  public FIState(FMAllContext fac, FailType ft) {
    this.fjp = fac.fjp;
    this.ctx = fac.ctx;
    this.fst = fac.fst;
    this.ft  = ft;


    setIoProperties(ctx);

    

    // then let's build the completeHashId
    generateCompleteHashId();
    generateStaticHashId();

  }


  //********************************************
  // here is how we convert the context into a file type.
  // For metadata file, we can just say the metadata file is
  // the file type. but for data file, there is too many,
  // so let's do this.
  // Must also include: disk number
  //********************************************
  public void setIoProperties(FMContext ctx) {
    
    String targetIO = ctx.getTargetIO();
    
    // net io
    if (Util.isNetIO(targetIO)) {
      targetIoType = "NetIO";
      targetNode = Util.getDnIdFromNetTargetIO(targetIO);
   
      //Jinsu changes : fileType => socketStream, diskId => noDisk
      diskId = "noDisk(net)";
      fileType = "SocketStream";
    }
    
    // disk io
    else if (Util.isDiskIO(targetIO)) {
      
      targetIoType = "DiskIO";
      diskId = "Unused";
      
      if (Util.isLogFile(targetIO)) {
				fileType = "LogFile";
	
				//if (Util.isTmpMeta(targetIO)) 
				//	fileType = "MetaFile-Tmp";
				//if (Util.isCurrentMeta(targetIO))
				//	fileType = "MetaFile-Current";
				//if (Util.isCurrentTmpMeta(targetIO))
				//	fileType = "MetaFile-CurrentTmp";
			}
      else {//if (Util.isDataFile(targetIO)) {
				fileType = "Unknown";
				//fileType = "DataFile-???";
				//if (Util.isTmpData(targetIO)) 
				//	fileType = "DataFile-Tmp";
				//if (Util.isCurrentData(targetIO))
				//	fileType = "DataFile-Current";
      }
    }

  }



  

  //********************************************
  // this is our policy of hash id of the state,
  // i.e the abstract representation of the state
  void generateCompleteHashId() {

    // IMPORTANT NOTE:
    // MAKE SURE the experiment is repeatable ... i.e.
    // if completeHashIdStr contains something undeterminism
    // (e.g. block generation stamp), then it's hard to repeat
    // the experiment!!

    // what is a state,
    // a state is a combination of:
    //   a. the node id (the least # of failures)
    //   b. file type (or file transfer to)
    //   c. the join point (important because in one line, many things can happen)
    //   d. join point 
    //   e. file type (rather than filename, because not good for datablocks)
    
    int hashOption = 100;
    
    switch (hashOption) {
      

    case 0:
      // option 0: my trials      
      completeHashIdStr = String.format("%s:%s:%s",
				fjp.toString(),
				ctx.getNodeId(),
				diskId);
      break;
    case 1:
      // option 1: just the fail type (5 cases)
      completeHashIdStr = ft.toString();
      break;
    case 2:
      // option 2: just the diskId (2 cases)
      completeHashIdStr = diskId;
      break;
    case 3:
      // option 3: just the node id (2 cases)
      completeHashIdStr = ctx.getNodeId();
      break;
    case 4:
      // option 4: just the file type (2 cases)
      completeHashIdStr = targetIoType;
      break;      
    case 5:
      // option 5: file type (2 cases)
      completeHashIdStr = fileType;
      break;
    case 6:
      // option 6: just the target node (2 cases)
      completeHashIdStr = targetNode;
      break;
    case 7:
      // option 7: just the join point (120 / 2 cases -- include read)
      completeHashIdStr = fjp.toString();
      break;
    case 8:
      // option 8: stack trace (66 cases)
      completeHashIdStr = fst.toString();
      break;

    case 100:
      // all options
      completeHashIdStr = 
	"\n" + 
	ft.toString()     + "\n" + 
	diskId            + "\n" +
	ctx.getNodeId()   + "\n" +
	targetIoType            + "\n" +
	fileType          + "\n" + 
	targetNode        + "\n" + 
	fjp.toString()    + "\n" +
	fst.toString()    + "\n" +
	"\n";
      break;
      


    case 11:
      // option 11: the joint point and nodeId
      completeHashIdStr = ctx.getNodeId() + "\n" + fjp.toString();
      break;
    case 12:
      // option 12: the joint point, nodeId, targetIoType
      completeHashIdStr = ctx.getNodeId() + "\n" + fjp.toString() + "\n" + targetIoType;
      break;
    case 13:
      // option 13: the joint point, nodeId, targetIoType, stack trace
      completeHashIdStr = 
	ctx.getNodeId() + "\n" + 
	fjp.toString()  + "\n" + 
	targetIoType          + "\n" + 
	fst.toString();
      break;

    default:
      completeHashIdStr = "WrongOption";
      break;
    }

    // option 1: this is the full flow
    completeHashId = completeHashIdStr.hashCode();

  }

  //********************************************
  public int getCompleteHashId() {
    return completeHashId;
  }

  //********************************************
  public int getHashId() {
    return getCompleteHashId();
  }

  //********************************************
  public String getCompleteHashIdStr() {
    return completeHashIdStr;
  }
  
  //********************************************
  public String getHashIdStr() {
    return getCompleteHashIdStr();
  }

  //********************************************
  public String getDiskId() {
    return diskId;
  }


  
  //********************************************
  // this is just a metric for coverage, which is mostly
  // static (sourceloc, etc.) and dynamic (stack, ), excluding the fail type
  //********************************************
  private void generateStaticHashId() {
     
    staticHashIdStr = 
      "\n" + 
      targetIoType      + "\n" +
      // diskId            + "\n" +
      fileType          + "\n" + 
      // targetNode        + "\n" + 
      // ctx.getNodeId()   + "\n" +
      fjp.toString()    + "\n" +
      fst.toString()    + "\n" +
      "\n";
    staticHashId = staticHashIdStr.hashCode();
  }
  
  // ***************  
  public int getStaticHashId() {
    return staticHashId;
  }
  
  // ***************
  public String getStaticHashIdStr() {  
    return staticHashIdStr;
  }

}
