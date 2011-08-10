


package org.fi;

import org.fi.*;
import org.fi.FMServer.FailType;
import org.fi.FMJoinPoint.*;

import java.io.*;
import java.util.*;


public class FMFilter {

  public FMFilter() {}

  // *************************************************
  // This is the part where "extensible. Given information
  // in fac, and ft.  You can specify which failure that you
  // want to exercise
  // *************************************************
  public static boolean passServerFilter(FMAllContext fac, FailType ft, FIState fis) {

    boolean passFilter = false;
    //boolean passFilter = true;

    //JINSU hack for Cassandra corruption to pass by.
    //passFilter = filterReadRepairTest(fac, ft, fis);
//    passFilter = filterReadRepairTest2(fac, ft, fis);
    passFilter = filterReadRepairTest3(fac, ft, fis);
    //passFilter = filterSimpleRRTest(fac, ft, fis);

    // passFilter = filterWriteBug1(fac, ft, fis);
    // passFilter = filterWriteBug3(fac, ft, fis);
    // passFilter = filterWriteBug5(fac, ft, fis);
    // passFilter = filterWriteBug6(fac, ft, fis);
    // passFilter = filterWriteBug7(fac, ft, fis);

    // passFilter = filterAppendBug2(fac, ft, fis);
    // passFilter = filterAppendBug4(fac, ft, fis);
    // passFilter = filterAppendBug5(fac, ft, fis); Can't reproduce so far
    // passFilter = filterAppendBug6(fac, ft, fis);
    // passFilter = filterAppendBug7(fac, ft, fis);
    // passFilter = filterAppendBug8(fac, ft, fis);

		//JINSU : putting in filter for crash failure
		//I honestly don't know what this is gonna do.
		//please explain...
		//passFilter = filterTest(fac, ft, fis);

    return passFilter;


  }

    //JINSU : Checks if to see if we are dealing with digest messages.
    private static boolean cassDigestTest(FIState fis, String node) {
        FMJoinPoint fjp = fis.fjp;
        FMContext ctx = fis.ctx;
        FailType ft = fis.ft;
        if ( ctx.getMessageType().equalsIgnoreCase(FMClient.READ_RESPONSE_DIGEST)
            && ft == FailType.CORRUPTION
            && cassNodeTest(ctx, node) ) {
            return true;
        }
        return false;
    }

    //JINSU : Checks if the message is the data messages.
    private static boolean cassDataTest(FIState fis, String node) {
        FMJoinPoint fjp = fis.fjp;
        FMContext ctx = fis.ctx;
        FailType ft = fis.ft;
        if ( ctx.getMessageType().equalsIgnoreCase(FMClient.READ_RESPONSE_NORMAL)
             && cassNodeTest(ctx, node) ) {
            return true;
        }
        return false;

    }


    //JINSU : Checks to see if the context contains node.
    private static boolean cassNodeTest(FMContext ctx, String node) {
        return ctx.getNodeId().equalsIgnoreCase(node);
    }

    // ****************
    // JINSU: A simple filter function that injects crashes
    // node1 at phase1 of readrepair.
    private static boolean filterSimpleRRTest(FMAllContext fac, FailType ft, FIState fis) {
     	FMJoinPoint fjp = fac.fjp;
		FMContext ctx = fac.ctx;
		if( FMLogic.getCurrentFsn() == 1 && ft == FailType.CRASH
                && fjp.getJoinPlc() == JoinPlc.BEFORE
                && fjp.getJoinPointStr().contains("DataOutputStream")
                && (cassNodeTest(ctx, "Node1")
                    //|| cassNodeTest(ctx, "Node3")
                    ) ) {
            System.out.println("filter JPS || " + fjp.getJoinPointStr() + "\nfilter ctx || " + ctx);
            return true;

                }
        return false;
    }


	// ************************************************************
	// JINSU: A small filter function that only returns true when the failure type is CRASH and the join place is before.
	private static boolean filterReadRepairTest3(FMAllContext fac, FailType ft, FIState fis) {
		FMJoinPoint fjp = fac.fjp;
		FMContext ctx = fac.ctx;
        int fsnNum = FMLogic.getCurrentFsn();

        System.out.println("meow failtye" + ft);
        if ( ( fsnNum == 1  )
                    && cassDigestTest(fis, "Node2") )
            return true;

        if( ( fsnNum == 2 || fsnNum == 3 )
               && (cassDataTest(fis, "Node2") )
               && ft == FailType.CORRUPTION
          )
            return true;

        if( ( fsnNum == 2 || fsnNum == 3 )
                && cassDataTest(fis, "Node3")
                && ft == FailType.CRASH )
            return true;

        System.out.println("meow ch3");
        return false;
    }


	// ************************************************************
	// JINSU: A small filter function that only returns true when the failure type is CRASH and the join place is before.
	private static boolean filterReadRepairTest2(FMAllContext fac, FailType ft, FIState fis) {
		FMJoinPoint fjp = fac.fjp;
		FMContext ctx = fac.ctx;
        int fsnNum = FMLogic.getCurrentFsn();

        System.out.println("meow failtye" + ft);
        if ( ( fsnNum == 1 || fsnNum == 2 || fsnNum == 3 )
                    && cassDataTest(fis, "Node1")
                    && ft == FailType.CORRUPTION )
            return true;
        if( ( fsnNum == 2 || fsnNum == 3 )
                && ft == FailType.CRASH
                && fjp.getJoinPlc() == JoinPlc.BEFORE
                && fjp.getJoinPointStr().contains("DataOutputStream")
                && (cassNodeTest(ctx, "Node1") )
                ) {
            System.out.println("JoinPlc || " + fjp.getJoinPlc() + "\nfilter JPS || " + fjp.getJoinPointStr() + "\nfilter ctx || " + ctx);
            return true;

                   }
        System.out.println("meow ch3");
        return false;
    }


	// ************************************************************
	// JINSU: A small filter function that only returns true when the failure type is CRASH and the join place is before.
	private static boolean filterReadRepairTest(FMAllContext fac, FailType ft, FIState fis) {
		FMJoinPoint fjp = fac.fjp;
		FMContext ctx = fac.ctx;
        int fsnNum = FMLogic.getCurrentFsn();

/*
        if( cassNodeTest(ctx, "Node3")
                && fjp.getJoinPointStr().contains("sendOneWay")
                ) {
                //introduce delay
                //so node1 and node2's msgs get to node0 before node3's.
                System.out.println("forcing Node3 to sleep");
                Util.sleep(200);
                }
*/


        System.out.println("meow failtye" + ft);
		//boolean condition = ctx.getTargetIO().contains("Node3") && ctx.getTargetIO().contains("Node1");
		//boolean containsCondition = //fjp.getJoinPointStr().contains("DataInputStream.readFully");
		//														 ctx.getNodeId().contains("Node0");
//																&& ctx.getTargetIO().contains("Node0");
        if ( fsnNum == 1 && cassDigestTest(fis, "Node2") )
            return true;
        System.out.println("meow ch1");
        if( ( fsnNum == 2
                    || fsnNum == 3 )
                && ft == FailType.CRASH
                && fjp.getJoinPlc() == JoinPlc.BEFORE
                && fjp.getJoinPointStr().contains("DataOutputStream")
                && (cassNodeTest(ctx, "Node1")
                    //|| cassNodeTest(ctx, "Node3")
                   ) ) {
            System.out.println("JoinPlc || " + fjp.getJoinPlc() + "\nfilter JPS || " + fjp.getJoinPointStr() + "\nfilter ctx || " + ctx);
            return true;

                   }
        System.out.println("meow dt curFsn = " + FMLogic.getCurrentFsn());
        System.out.println("meow dt " + cassDataTest(fis, "Node1"));
        if ( ( fsnNum == 2 || fsnNum  == 3 )
                && cassDataTest(fis, "Node1")
                && ft == FailType.CORRUPTION
                ) {
            return true;
                }
        System.out.println("meow ch3");

/*
        if(ft == FailType.CRASH
		&& fjp.getJoinPlc() == JoinPlc.AFTER
//		&& fjp.getJoinIot() == JoinIot.READ
//		&& fjp.getJoinExc() == JoinExc.IO
//		&& fjp.getJoinRbl() == JoinRbl.NO
		&& containsCondition) {

				return true;
			}
            */
		return false;
	}

  // **************************************************
  private static boolean filterTransientDisk(FMAllContext fac,
                                             FailType ft, FIState fis) {

    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String nodeId = ctx.getNodeId();
    boolean isDiskIO = Util.isDiskIO(ctx.getTargetIO());
    boolean transientFailure = (ft == FailType.EXCEPTION || ft == FailType.RETFALSE);
    boolean writeIO = (fjp.getJoinIot() == JoinIot.WRITE);
    boolean inDatanode = nodeId.contains("DataNode");

    if (transientFailure &&
        isDiskIO && inDatanode && writeIO) {
      return true;
    }
    return false;

  }


  // **************************************************
  // Append-Bug 2:
  // http://hdfswiki.pbworks.com/Block-lost-when-primary-crashes-in-recoverBlock
  // Block is lost if primary datanode crashes in the middle tryUpdateBlock.
  // # available datanode = 2
  // # replica = 2
  // # disks / datanode = 1
  // # failures = 1
  // # failure type = crash
  // **************************************************
  private static boolean filterAppendBug2(FMAllContext fac,
                                          FailType ft, FIState fis) {
    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    JoinPlc joinPlace = fjp.getJoinPlc();

    if (ft == FailType.CRASH &&
        joinPlace == JoinPlc.AFTER &&
        ctx.getNodeId().equals("DataNode-1") &&
        fst.toString().contains("tryUpdateBlock") &&
        joinPoint.contains("renameTo") &&
        !ctx.getTargetIO().contains(".meta_tmp") && // rename metafile to tmpfile
        FMLogic.getMaxFsn() == 1)
      return true;
    return false;
  }



  // **************************************************
  // append-bug #4:
  //  http://hdfswiki.pbworks.com/UpdateBlock-fails-due-to-unmatched-file-length
  // # available datanodes = 3
  // # replicas = 3
  // # disks / datanode = 1
  // # failures = 1
  // failure type = bad disk
  //     When/where failure happens = (see below)
  //     This bug is non-deterministic, to reproduce it, add a sufficient sleep
  // before out.write() in BlockReceiver.receivePacket() in dn1 and dn2 but not dn3
  //   ALSO: go to datanode/BlockReceiver.java, and enable "enableAddDelay"
  // **************************************************
  private static boolean filterAppendBug4(FMAllContext fac,
                                          FailType ft, FIState fis) {
    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    JoinPlc joinPlace = fjp.getJoinPlc();
    boolean isDataFile = Util.isDataFile(ctx.getTargetIO());
    int fsn = FMLogic.getCurrentFsn();
    if (
        ft == FailType.BADDISK &&
        //joinPlace == JoinPlc.AFTER &&
        ctx.getNodeId().equals("DataNode-3") &&
        fst.toString().contains("receivePacket") &&
        joinPoint.contains("write") &&
        isDataFile &&
        FMLogic.getMaxFsn() == 1)

      return true;
    return false;

  }



  // **************************************************
  // append-bug #5:
  // http://hdfswiki.pbworks.com/
  // CRC-does-not-match-when-retrying-appending-a-partial-block
  // # available datanodes = 2
  // # replicas = 2
  // # ALSO: change FILE_SIZE to 16 in workload-driver Driver/Hdfs.java
  // # disks / datanode = 1
  // # failures = 1
  // failure type = bad disk
  // NOTE: I couldn't reproduce this so far
  // **************************************************
  private static boolean filterAppendBug5(FMAllContext fac,
                                          FailType ft, FIState fis) {
    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    JoinPlc joinPlace = fjp.getJoinPlc();
    boolean isDataFile = Util.isDataFile(ctx.getTargetIO());

    if (ft == FailType.BADDISK &&
        //        joinPlace == JoinPlc.AFTER &&
        ctx.getNodeId().equals("DataNode-2") &&
        fst.toString().contains("receivePacket") &&
        joinPoint.contains("write") &&
        isDataFile &&
        FMLogic.getMaxFsn() == 1)
      return true;
    return false;

  }




  // **************************************************
  // Append-Bug 6:
  //  http://hdfswiki.pbworks.com/
  //  DFSClient-incorrectly-asks-for-new-block-if-primary-
  //  crashes-during-first-recoverBlock
  // this filter is exactly the same as that of append bug 2
  // all we need to do is the primary data node crash during recoverBlock
  // # available datanodes = 2
  // # replicas = 2
  // # disks / datanode = 1
  // # failures = 1
  // failure type = crash
  // When/where failure happens = during primary's recoverBlock
  // **************************************************
  private static boolean filterAppendBug6(FMAllContext fac,
                                          FailType ft, FIState fis) {
    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    JoinPlc joinPlace = fjp.getJoinPlc();

    if (ft == FailType.CRASH &&
        joinPlace == JoinPlc.AFTER &&
        ctx.getNodeId().equals("DataNode-1") &&
        fst.toString().contains("tryUpdateBlock") &&
        joinPoint.contains("renameTo") &&
        !ctx.getTargetIO().contains(".meta_tmp") && // rename metafile to tmpfile
        FMLogic.getMaxFsn() == 1)
      return true;
    return false;

  }

  // **************************************************
  // append Bug #7
  // http://hdfswiki.pbworks.com/Generation-Stamp-mismatches%2C-leading-to-failed-append
  // # available datanodes = 3
  // # replicas = 3
  // # disks / datanode = 1
  // # failures = 2
  // failure type = crash
  // **************************************************
  private static boolean filterAppendBug7(FMAllContext fac,
                                          FailType ft, FIState fis) {
    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    JoinPlc joinPlace = fjp.getJoinPlc();
    boolean isDataFile = Util.isDataFile(ctx.getTargetIO());
    int fsn = FMLogic.getCurrentFsn();

    if (
        (fsn == 1 &&
         ft == FailType.CRASH &&
         joinPlace == JoinPlc.AFTER &&
         ctx.getNodeId().equals("DataNode-3") &&
         fst.toString().contains("receivePacket") &&
         joinPoint.contains("write") &&
         isDataFile &&
         FMLogic.getMaxFsn() == 2)
        ||
        (fsn == 2 &&
         ft == FailType.CRASH &&
         joinPlace == JoinPlc.AFTER &&
         ctx.getNodeId().equals("DataNode-1") &&
         fst.toString().contains("tryUpdateBlock") &&
         joinPoint.contains("renameTo") &&
         !ctx.getTargetIO().contains(".meta_tmp") &&
         FMLogic.getMaxFsn() == 2)
        )
      return true;

    return false;
  }



  // **************************************************
  // append Bug #8
  // http://hdfswiki.pbworks.com/
  // Corrupted-block-if-a-crash-happens-before-writing-to-
  // checksumOut-but-after-writing-to-dataOut
  // # available datanodes = 1
  // # replicas = 1
  // # disks / datanode = 1
  // # failures = 1
  // failure type = crash
  // **************************************************
    private static boolean filterAppendBug8(FMAllContext fac,
                                            FailType ft, FIState fis) {
    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    JoinPlc joinPlace = fjp.getJoinPlc();
    boolean isDataFile = Util.isDataFile(ctx.getTargetIO());

    if (ft == FailType.CRASH &&
        joinPlace == JoinPlc.AFTER &&
        ctx.getNodeId().equals("DataNode-1") &&
        fst.toString().contains("receivePacket") &&
        joinPoint.contains("write") &&
        isDataFile &&
        FMLogic.getMaxFsn() == 1)
      return true;
    return false;

  }




  // **************************************************
  // WRITE BUG # 1:
  // http://hdfswiki.pbworks.com/FrontPage
  //   dfs.replication = 1
  //   #datanodes      = 1
  //   #disks/datanode = 2  (see hdfs-site.xml, dfs.data.dir, enable two data disks)
  //   MAX_FSN         = 1
  //   Workload        = putfile
  //   FailTpe         = BadDisk
  // **************************************************
  private static boolean filterWriteBug1(FMAllContext fac,
                                      FailType ft, FIState fis) {

    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    String diskId = Util.getDiskIdFromTargetIO(ctx.getTargetIO());
    boolean firstDisk = diskId.equals("Disk1");
    boolean secondPhase = joinPoint.contains("renameTo");
    boolean isDataFile = Util.isDataFile(ctx.getTargetIO());
    boolean badDiskFailure = (ft == FailType.BADDISK);

    if (firstDisk && secondPhase && isDataFile && badDiskFailure) {
      return true;
    }

    return false;
  }




  // **************************************************
  // WRITE BUG # 3:
  //   http://hdfswiki.pbworks.com/Namenode-returning-the-same-Datanode-to-client%2C-due-to-infrequent-heartbeat
  //   There is _one_ bad-disk failure during the first phase.
  // Setup:
  //   dfs.replication = 2 (in "hdfs-site.xml")
  //   #datanodes      = 3 (in "slaves")
  //   MAX_FSN         = 1 (in Driver.java)
  //   Workload        = putfile
  //   FailType        = BadDisk
  // **************************************************
  private static boolean filterWriteBug3(FMAllContext fac,
                                      FailType ft, FIState fis) {
    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    boolean isDataFile = Util.isDataFile(ctx.getTargetIO());
    boolean firstPhase = joinPoint.contains("RandomAccessFile(File, String)");

    if (ft == FailType.BADDISK &&
        ctx.getNodeId().equals("DataNode-1") &&
        firstPhase &&
        isDataFile &&
        FMLogic.getMaxFsn() == 1) {
      return true;
    }

    return false;
  }


  // **************************************************
  // WRITE BUG # 5:
  //   http://hdfswiki.pbworks.com/Client-uselessly-retries-recoverBlock-5-times
  //   There are two bad-disk failures.
  //   The first one happens in the 2nd phase at DN-2
  //   This will force the client to create another pipeline
  //   just for DN-1.  But DN-1 fails again.
  //   The client gives up, without contacting the namenode again
  // Setup:
  //   dfs.replication = 2 (in "hdfs-site.xml")
  //   #datanodes      = 4 (in "slaves")
  //   MAX_FSN         = 2 (in Driver.java)
  //   Workload        = putfile
  //   FailType        = BadDisk
  // **************************************************
  private static boolean filterWriteBug5(FMAllContext fac,
                                      FailType ft, FIState fis) {
    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    boolean secondPhase = joinPoint.contains("renameTo");
    boolean isDataFile = Util.isDataFile(ctx.getTargetIO());

    if (ft == FailType.BADDISK &&
        secondPhase &&
        isDataFile &&
        FMLogic.getMaxFsn() == 2  &&
        fst.contains("PacketResponder", "lastDataNodeRun")
        ) {
      return true;
    }

    return false;
  }


  // **************************************************
  // WRITE BUG # 6:
  //   http://hdfswiki.pbworks.com/A-block-is-stuck-in-ongoingRecovery-due-to-exception-not-propagated
  // Setup:
  //   dfs.replication = 2 (in "hdfs-site.xml")
  //   #datanodes      = 4 (in "slaves")
  //   MAX_FSN         = 2 (in Driver.java)
  //   Workload        = putfile
  //   FailType        = Exception/Retfalse
  // **************************************************
  private static boolean filterWriteBug6(FMAllContext fac,
                                      FailType ft, FIState fis) {

    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    boolean secondPhase = joinPoint.contains("renameTo");
    boolean isMetaFile = Util.isMetaFile(ctx.getTargetIO());
    boolean isCurrentTmpMeta = Util.isCurrentTmpMeta(ctx.getTargetIO());
    int fsn = FMLogic.getCurrentFsn();
    String nodeId = ctx.getNodeId();

    // the 1st failure
    if (fsn == 1 &&
        ft == FailType.EXCEPTION &&
        isMetaFile &&
        nodeId.equals("DataNode-1") &&
        fst.contains("BlockReceiver", "flush") &&
        joinPoint.contains("DataOutputStream.flush")) {
      return true;
    }

    // the 2nd failure
    if (fsn == 2 &&
        ft == FailType.RETFALSE &&
        isCurrentTmpMeta &&
        nodeId.equals("DataNode-2") &&
        fst.contains("FSDataset", "tryUpdateBlock") &&
        joinPoint.contains("File.renameTo")) {
      return true;
    }

    return false;

  }


  // **************************************************
  // WRITE BUG # 7:
  //   http://hdfswiki.pbworks.com
  // Setup:
  //   dfs.replication = 2 (in "hdfs-site.xml")
  //   #datanodes      = 4 (in "slaves")
  //   MAX_FSN         = 2 (in Driver.java)
  //   Workload        = putfile
  //   FailType        = Exception/Retfalse
  // **************************************************
  private static boolean filterWriteBug7(FMAllContext fac,
                                      FailType ft, FIState fis) {

    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    String joinPoint = fjp.getJoinPointStr();
    boolean secondPhase =
      joinPoint.contains("renameTo") && fst.contains("PacketResponder", "run");
    boolean transientFailure = (ft == FailType.EXCEPTION || ft == FailType.RETFALSE);
    boolean isMetaFile = Util.isMetaFile(ctx.getTargetIO());
    int fsn = FMLogic.getCurrentFsn();
    String nodeId = ctx.getNodeId();

    if (transientFailure && isMetaFile && secondPhase) {

      // Note: This is concurrent failures!
      if (fsn == 1 && nodeId.equals("DataNode-1"))
        return true;
      if (fsn == 2 && nodeId.equals("DataNode-2"))
        return true;
    }

    return false;

  }



  // ********************************
  // Playing around with filters here
  // *********************************
  private static boolean passServerFilterRepository
    (FMAllContext fac, FailType ft, FIState fis) {

    FMJoinPoint fjp = fac.fjp;
    FMContext ctx = fac.ctx;
    FMStackTrace fst = fac.fst;
    int fsn = FMLogic.getCurrentFsn();
    String nodeId = ctx.getNodeId();
    boolean isBlockFile = Util.isBlockFile(ctx.getTargetIO());
    boolean isMetaFile = Util.isMetaFile(ctx.getTargetIO());
    boolean isDataFile = Util.isDataFile(ctx.getTargetIO());
    boolean isNetIoToDn = Util.isNetIOtoDataNode(fac.ctx.getTargetIO());
    JoinIot ioType = fjp.getJoinIot();
    String joinPoint = fjp.getJoinPointStr();
    String diskId = Util.getDiskIdFromTargetIO(ctx.getTargetIO());
    JoinPlc jplc = fjp.getJoinPlc();
    // nodeId.equals("DataNode-2") && diskId.equals("Disk1"))

    boolean firstPhase = joinPoint.contains("RandomAccessFile(File, String)");
    boolean secondPhase =
      joinPoint.contains("renameTo") ||
      joinPoint.contains("flush");


    // just an optimization .. specifically we're interested
    // in client write and datanode failure only
    // hence we only "filter" data file/meta write
    // [file][/rhh/dfs/data1/tmp/blk_7307724612204181421]

    // and for network stream, node id should not be client (unknown so far)
    boolean passFilter = false;

    // abc
    if (nodeId.contains("DataNode") &&
        ft == FailType.EXCEPTION || ft == FailType.RETFALSE) {
      passFilter = true;
    }

    // System.out.println("_passFilter_ " + passFilter);


    return passFilter;
  }





}


