
package org.fi;


import org.fi.*;

import org.apache.hadoop.io.*;

// *************************************************** JOL
import jol.core.JolSystem;
import jol.core.Runtime;
import jol.types.basic.BasicTupleSet;
import jol.types.basic.Tuple;
import jol.types.basic.TupleSet;
import jol.types.exception.JolRuntimeException;
import jol.types.exception.UpdateException;
import jol.types.table.TableName;
import jol.types.table.Table.Callback;
import jol.types.table.Table;

// ************************************ XML RPC
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

// *************************************************** Java
import java.io.*;
import java.net.URL;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.lang.Thread;
import java.lang.StackTraceElement;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;

// *************************************************** Axpect
import org.aspectj.lang.JoinPoint;


// *************************************************** HDFS
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.ClientDatanodeProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.DFSClient.DFSOutputStream;

import org.apache.hadoop.hdfs.protocol.DataTransferProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.DatanodeID;




// FrogClient send to FrogServer a list of String
// actually it will go to FMServer first
// the list of String is basically what the event should
// schedule

public class FrogClient {


  // place of call interpose
  public static enum Plc { BEFORE, RETURNING, EXCEPTION; }


  public static final String ENABLE_FROG_FLAG  = FMLogic.TMPFI + "enableFrogFlag";

  private static PrintStream mps; // from DFSClient from WorkloadDriver

  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                         G E N E R A L                         ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################


  // ***********************************************************
  public static boolean isEnableFrogFlagExist() {
    File f = new File(ENABLE_FROG_FLAG);
    if (f.exists())
      return true;
    return false;
  }

  // ***********************************************************
  public static void print(String buf) {
    if (mps == null)
      mps = DFSClient.getMyPrintStream();
    Util.print(mps, buf);
  }

  // ***********************************************************
  public static void println(String buf) {
    print(buf+"\n");
  }



  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                 H A D O O P    R P C                          ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################

  /*
    private static FMProtocol fmp = null;
    private static Configuration conf = new Configuration();
    private static InetSocketAddress addr =
    new InetSocketAddress(FMServer.bindAddr, FMServer.port);


    // *********************************
    public static void sendToFrogServer(FMJoinPoint fjp, FrogEvent fev) {



    try {

    // connect
    if (fmp == null) {
    try {
    fmp = (FMProtocol)
    RPC.getProxy(FMProtocol.class, FMProtocol.versionID, addr, conf);
    } catch (IOException e) {
    Util.WARNING("cannot contact FM from FrogClient");
    return;
    }
    if (fmp == null)
    return;
    }

    Thread t = Thread.currentThread();
    FMStackTrace fst = new FMStackTrace(t.getStackTrace());

    fmp.sendFrogEvent(fjp, fst, fev);

    } catch (Exception e) {
    Util.EXCEPTION("sendFrogEvent", e);
    }
    }
  */


  // #######################################################################
  // #######################################################################
  // ####                                                               ####
  // ####                    X M L       R P C                          ####
  // ####                                                               ####
  // #######################################################################
  // #######################################################################

  private static XmlRpcClient frogClient;

  // ******************************************************
  private static boolean cannotConnectToServer() {
    if (frogClient != null)
      return false;
    connectToFMServer();
    if (frogClient == null)
      return true;
    return false;
  }

  // ******************************************************
  private static void connectToFMServer() {
    try {
      XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
      String httpAddr = "http://127.0.0.1:" + FMServer.PORT + "/xmlrpc";
      config.setServerURL(new URL(httpAddr));
      frogClient = new XmlRpcClient();
      frogClient.setConfig(config);
    } catch (Exception e) {
      frogClient = null;
    }
  }

  // ******************************************************
  public static void sendToFrogServer(FMJoinPoint fjp, FrogEvent fev) {
    Thread t = Thread.currentThread();
    FMStackTrace fst = new FMStackTrace(t.getStackTrace());

    sendFrogEventViaXmlRpc(fjp, fst, fev);
  }


  // ******************************************************
  private static void sendFrogEventViaXmlRpc(FMJoinPoint fjp,
                                             FMStackTrace fst,
                                             FrogEvent fev) {

    // this is okay, because we don't always want to
    // run hdfs with fm server
    if (cannotConnectToServer())
      return;

    int randId = Util.r4();
    File f = Util.getRpcFile(randId);
    DataOutputStream dos = Util.getRpcOutputStream(randId);

    try {

      fjp.write(dos);
      fst.write(dos);
      fev.write(dos);
      dos.close();

      System.out.format("- Sending %d \n", randId);

      Object[] params = new Object[]{new Integer(randId)};
      Integer result = (Integer) frogClient.execute("FMServer.sendFrogEvent", params);

      if (result.intValue() != 1) {
        Util.FATAL("frog server returns something wrong");
      }

      System.out.format("- Received %d %s \n", result);
      f.delete();

    } catch (Exception e) {
      f.delete();
      Util.EXCEPTION("RPC frog client error", e);
      Util.FATAL("RPC frog client error");
    }
  }



  // ##################################################################
  // ##################################################################
  // ##                                                              ##
  // ##                D I S K    M O D E L I N G                    ##
  // ##                                                              ##
  // ##################################################################
  // ##################################################################




  // *********************************
  public static void scanEditBuffer(FMJoinPoint fjp,
                                    ByteArrayOutputStream buf,
                                    OutputStream out) {

    if (out == null)
      return;
    if (out.getContext() == null)
      return;

    System.out.format(">>   scanEditBuffer: [%s] \n", out.getContext().getTargetIO());

    byte [] ba = buf.toByteArray();
    ByteArrayInputStream bais = new ByteArrayInputStream(ba);
    DataInputStream in = new DataInputStream(bais);

    final byte OP_MKDIR = 3;
    try {
      try {
        while (true) {
          byte opcode = in.readByte();
          if (opcode == OP_MKDIR) {
            int length = in.readInt();
            String pathServer = in.readUTF();
            long timestamp = Long.parseLong(in.readUTF());
            long atime = Long.parseLong(in.readUTF());
            PermissionStatus permissions = PermissionStatus.read(in);

            System.out.format("    OP_MKDIR: %d [%s] \n", length, pathServer);
            addPathServer(fjp, pathServer, out.getContext().getTargetIO());

          }
          else {
            // System.out.println("    It's not make dir");
            break;
          }
        }
      } catch (EOFException e) {
        in.close();
      }
    } catch (IOException e) {
      Util.EXCEPTION("scaneditbuffer", e);
    }

  }

  // *********************************
  public static void addStorageFile(FMJoinPoint fjp, String storageFile) {
    System.out.format(">>   addStorageFile [%s] \n", storageFile);
    FrogEvent fev = new FrogEvent("model", "add_storage_file", storageFile);
    sendToFrogServer(fjp, fev);
  }

  // *********************************
  public static void renameStorageFile(FMJoinPoint fjp, String src, String dst) {
    System.out.format(">>  renameStorageFile [%s] to [%s] \n", src, dst);
    FrogEvent fev = new FrogEvent("model", "rename_storage_file", src, dst);
    sendToFrogServer(fjp, fev);
  }


  // *********************************
  public static void scanFSImageEntry(FMJoinPoint fjp, String pathServer,
                                      OutputStream out) {
    addPathServer(fjp, pathServer, out.getContext().getTargetIO());
  }

  // *********************************
  public static void addPathServer(FMJoinPoint fjp, String pathServer,
                                   String storageFile) {

    // the root .. ignore
    if (pathServer.equals("")) {
      pathServer = "/";
    }

    System.out.format(">>   addPathServer [%s] to storage file [%s] \n",
                      pathServer, storageFile);
    FrogEvent fev = new FrogEvent("model", "add_path_server",
                                  pathServer, storageFile, "InFS");
    sendToFrogServer(fjp, fev);
  }

  // *********************************
  public static void truncateStorageFile(FMJoinPoint fjp, String storageFile) {
    System.out.format(">>   truncateStorage [%s] \n", storageFile);
    FrogEvent fev = new FrogEvent("model", "truncate_storage_file", storageFile);
    sendToFrogServer(fjp, fev);
  }

  // *********************************
  public static void addPathUser(FMJoinPoint fjp, String pathUser) {
    System.out.format(">>   addPathUser [%s] \n", pathUser);
    FrogEvent fev = new FrogEvent("model", "add_path_user", pathUser);
    sendToFrogServer(fjp, fev);
  }



  // ##################################################################
  // ##################################################################
  // ##                                                              ##
  // ##               N E T W O R K    M O D E L I N G               ##
  // ##                                                              ##
  // ##################################################################
  // ##################################################################



  // *********************************
  public static void failedConnection(FMJoinPoint fjp, SocketChannel sc) {
    Socket s = sc.socket();
    int port = sc.context.getPort();
    String dnId = Util.getNodeIdFromKnownPort(port);
    System.out.format(">>   failedConnection [%d][%s] \n", port, dnId);
    FrogEvent fev = new FrogEvent("model", "failed_connection", dnId, "Client");
    sendToFrogServer(fjp, fev);
  }


  // *********************************
  public static void returnedNodes(FMJoinPoint fjp, LocatedBlock lb) {
    DatanodeInfo [] locs = lb.getLocations();
    for (int i = 0; i < locs.length; i++) {
      String dnId = Util.getDatanodeStringIdFromLocName(locs[i].getName());
      System.out.format(">>   returnedNodes [%s] \n", dnId);
      FrogEvent fev = new FrogEvent("model", "returned_node", dnId);
      sendToFrogServer(fjp, fev);
    }
  }


  // *********************************
  public static void deadNode(FMJoinPoint fjp, int exitStatus) {

    // not what we want
    if (exitStatus != FMClient.FAAS_SYSTEM_EXIT_STATUS)
      return;

    String pid = Util.getPid();
    String dnId = Util.getNodeIdFromPid(pid);
    System.out.format(">>   deadNode [%s][%s] \n", pid, dnId);
    FrogEvent fev = new FrogEvent("model", "dead_nodes", dnId, "FAAS");
    sendToFrogServer(fjp, fev);
  }




  // #####################################################################
  // #####################################################################
  // #####                                                            ####
  // #####        C L I E N T    P R O T O C O L     H O O K S        ####
  // #####                                                            ####
  // #####################################################################
  // #####################################################################


  // *********************************
  public static void cpHaryadiTest(JoinPoint jp, Plc plc, Object rv,
                                   int x) {

    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "cpHaryadiTest");
  }


  // *********************************
  public static void cpGetBlockLocations(JoinPoint jp, Plc plc, Object rv,
                                         String src, long offset, long length) {
    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "cpGetBlockLocations");
  }

  // *********************************
  public static void cpCreate(JoinPoint jp, Plc plc, Object rv,
                              String src, FsPermission masked,
                              String clientName, boolean overwrite,
                              short replication, long blockSize) {
    if (!isEnableFrogFlagExist()) return;
    String msg = String.format
      ("*cpCreate* src=%s rep=%d cn=%s", src, replication, clientName);
    printProtocolMessage(plc, msg);
  }

  // *********************************
  public static void cpAppend(JoinPoint jp, Plc plc, Object rv,
                              String src, String clientName) {
    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "*cpAppend*");
  }

  // *********************************
  public static void cpAbandonBlock(JoinPoint jp, Plc plc, Object rv,
                                    Block b, String src, String holder) {
    if (!isEnableFrogFlagExist()) return;

    long blockId = b.getBlockId();
    long gs = b.getGenerationStamp();
    String msg = String.format
      ("*cpAbandonBlock* bid=%d gs=%d src=%s hd=%s", 
       blockId, gs, src, holder);
    printProtocolMessage(plc, msg);
  }

  // *********************************
  // supported
  public static void cpAddBlock(JoinPoint jp, Plc plc, Object rv,
                                String src, String clientName) {
    if (!isEnableFrogFlagExist())
      return;

    String msg = String.format
      ("*cpAddBlock* src=%s cn=%s", src, clientName);
    
    printProtocolMessage(plc, msg);
    
    if (plc == Plc.RETURNING) {
      LocatedBlock lb = (LocatedBlock) rv;
      Block b = lb.getBlock();
      DatanodeInfo[] dns = lb.getLocations();
      
      String blockName = b.getBlockName(); // just blk_blockId
      long blockId = b.getBlockId();
      long generationStamp = b.getGenerationStamp();

      msg = String.format("  bid=%d gs=%d", blockId, generationStamp);
      printProtocolMessage(plc, msg);
    
      if (dns != null) {
	for (int i = 0; i < dns.length; i++) {
	  String locName = dns[i].getName();
	  String nodeName = Util.getDatanodeStringIdFromLocName(locName);
	  String storageId = dns[i].getStorageID();

	  msg = String.format("  dn[%d] dn=%s sid=%s ",
			      i, nodeName, storageId);
	  printProtocolMessage(plc, msg);
	}
      }
      
    }
  }
  
  
  // *********************************
  public static void cpComplete(JoinPoint jp, Plc plc, Object rv,
                                String src, String clientName) {
    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "*cpComplete*");
  }

  // *********************************
  public static void cpReportBadBlocks(JoinPoint jp, Plc plc, Object rv,
                                       LocatedBlock[] blocks) {

    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "*cpReportBadBlocks*");
  }

  // *********************************
  public static void cpRename(JoinPoint jp, Plc plc, Object rv,
                              String src, String dst) {
    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "*cpRename*");
  }

  // *********************************
  public static void cpDelete(JoinPoint jp, Plc plc, Object rv,
                              String src) {
    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "*cpDelete1*");
  }

  // *********************************
  public static void cpDelete(JoinPoint jp, Plc plc, Object rv,
                              String src, boolean recursive) {
    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "*cpDelete2*");
  }

  // *********************************
  // not supported right now ....
  // *********************************
  public static void cpRenewLease(JoinPoint jp, Plc plc, Object rv,
                                  String clientName) {
    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "*cpRenewLease*");
  }

  // *********************************
  public static void cpFsync(JoinPoint jp, Plc plc, Object rv,
                             String src, String client) {
    if (!isEnableFrogFlagExist()) return;
    printProtocolMessage(plc, "*cpFsync*");
  }


  // ####################################################################
  // ####################################################################
  // ####                                                            ####
  // ####    C L I E N T     D A T A N O D E     P R O T O C O L     ####
  // ####                                                            ####
  // ####################################################################
  // ####################################################################

  // remember it here ..
  private static String latestDnProxy = "";


  // *********************************
  public static void cdpRecoverBlock(JoinPoint jp, Plc plc, Object rv,
				     ClientDatanodeProtocol cdp,
                                     Block block, boolean keepLength,
                                     DatanodeInfo[] targets) {
    if (!isEnableFrogFlagExist()) return;
    
    String src = Util.getNodeId();
    String tgt = latestDnProxy;
    String msg = String.format("*cdpRecoverBlock* (%s --> %s)", src, tgt);
    printProtocolMessage(plc, msg);
  }


  // *********************************
  // we cannot get the datanode target from ClientDatanodeProtocol
  // because it's an interface, so we need a helper here
  // *********************************
  public static void cdpProxy(JoinPoint jp, Plc plc, Object rv,
			      DatanodeID dnid) {
    if (!isEnableFrogFlagExist()) return;
    String locName = dnid.getName();
    String nodeId = Util.getDatanodeStringIdFromLocName(locName);
    latestDnProxy = nodeId;
  }


  // ####################################################################
  // ####################################################################
  // ####                                                            ####
  // ####         D A T A    T R A N S F E R     P R O T O C O L     ####
  // ####                                                            ####
  // ####################################################################
  // ####################################################################



  // *********************************
  public static void dtpFlushOp(JoinPoint jp, Plc plc, Object rv, Context ctx) {


    if (!isEnableFrogFlagExist()) return;
    
    
    String src = Util.getNodeId();
    String tgt = Util.getNodeIdFromNetIO(ctx.getTargetIO());    
    String msg = String.format("*dtpFlushOp* (%s --> %s)", src, tgt);
    printProtocolMessage(plc, msg);
    
    
    Object obj = ctx.getExtraContext();
    if (obj != null &&
	obj instanceof BufferedOutputStreamWrapper) {
      BufferedOutputStreamWrapper bosw =
	(BufferedOutputStreamWrapper) obj;      
      msg = getProtocolMessage(bosw);
    }
    else {
      msg = "Unknown-NoBOSW";
    }

    if (plc == Plc.BEFORE) {
      println(msg);

      if (msg.equals("Unknown-NoBOSW")) {
	FMJoinPoint fjp = new FMJoinPoint(jp);
	println(fjp.toString());
      }
    }
    
  }


  // *********************************
  public static void dtpReadStatus(JoinPoint jp, Plc plc, Object rv, Context ctx) {

    if (!isEnableFrogFlagExist()) return;

    String src = Util.getNodeId();
    String tgt = Util.getNodeIdFromNetIO(ctx.getTargetIO());
    String msg = String.format("*dtpReadStatus* (%s <-- %s)", src, tgt);
    
    if (plc == Plc.RETURNING) {
      String status = dtpOpStatusShortToString(((Short)rv).shortValue());
      msg += ("returns " + status);
    }
    
    printProtocolMessage(plc, msg);
  }

  // *********************************
  public static void dtpReadAny(JoinPoint jp, Plc plc, Object rv, Context ctx) {

    if (!isEnableFrogFlagExist()) return;
    
    String src = Util.getNodeId();
    String tgt = Util.getNodeIdFromNetIO(ctx.getTargetIO());
    String msg = String.format("*dtpReadAny* (%s <-- %s)", src, tgt);
    
    printProtocolMessage(plc, msg);
  }


  // ####################################################################
  // ####################################################################
  // ####                                                            ####
  // ####           C L I E N T     A P I    P R O T O C O L         ####
  // ####                                                            ####
  // ####################################################################
  // ####################################################################
  
  
  // *********************************
  public static void apiClose(JoinPoint jp, Plc plc, Object rv, 
			      DFSOutputStream dfsos) {
    if (!isEnableFrogFlagExist()) return;
    String src = dfsos.getSrc();
    String msg = String.format("*apiClose* %s", src);
    printProtocolMessage(plc, msg);    
  }



  // ####################################################################
  // ####################################################################
  // ####                                                            ####
  // ####                    U  T  I  L  I  T  Y                     ####
  // ####                                                            ####
  // ####################################################################
  // ####################################################################


  // *********************************
  public static void printProtocolMessage(Plc plc, String msg) {
    if (plc == Plc.BEFORE) {
      println(" [FC][Call] " + msg);
    }
    else if (plc == Plc.RETURNING) {
      println(" [FC][Rets] " + msg);
    }
    else {
      println(" [FC][Excp] " + msg);
    }
  }

  // *********************************
  private static String dtpOpIntToString(int b) {
    byte op = (byte)b;
    if      (op == DataTransferProtocol.OP_WRITE_BLOCK)    return "WriteBlock";
    else if (op == DataTransferProtocol.OP_READ_BLOCK)     return "ReadBlock";
    else if (op == DataTransferProtocol.OP_READ_METADATA)  return "WriteMeta";
    else if (op == DataTransferProtocol.OP_REPLACE_BLOCK)  return "ReplaceBlock";
    else if (op == DataTransferProtocol.OP_COPY_BLOCK)     return "CopyBlock";
    else if (op == DataTransferProtocol.OP_BLOCK_CHECKSUM) return "BlockChecksum";
    else    return null;
  }

  // *********************************
  private static String dtpOpStatusShortToString(short s) {
    if      (s == DataTransferProtocol.OP_STATUS_SUCCESS)        return "Success";
    else if (s == DataTransferProtocol.OP_STATUS_ERROR)          return "Error";
    else if (s == DataTransferProtocol.OP_STATUS_ERROR_CHECKSUM) return "ErrorChecksum";
    else if (s == DataTransferProtocol.OP_STATUS_ERROR_INVALID)  return "ErrorInvalid";
    else if (s == DataTransferProtocol.OP_STATUS_ERROR_EXISTS)   return "ErrorExists";
    else if (s == DataTransferProtocol.OP_STATUS_CHECKSUM_OK)    return "ChecksumOk";
    else                                                         return "UnknownStatus";
  }

  // *********************************
  private static String getProtocolMessage(BufferedOutputStreamWrapper bosw) {
    String buf = "";

    if (bosw.getCount() == 0) {
      return "Unknown-ZeroSizeBuffer";
    }
    
    byte op = (byte)0;

    byte[] ba = bosw.getBuffer();
    ByteArrayInputStream bais = new ByteArrayInputStream(ba);
    DataInputStream in = new DataInputStream(bais);
    
    try {

      // from DataXceiver.java
      short version = in.readShort();
      if ( version != DataTransferProtocol.DATA_TRANSFER_VERSION ) {
        return "Unknown-BufSize-" + bosw.getCount();
      }

      op = in.readByte();

    } catch (Exception e) {
      return "GotException";
    }

    switch ( op ) {
    case DataTransferProtocol.OP_READ_BLOCK:
      buf += "Op:ReadBlock";
      break;
    case DataTransferProtocol.OP_WRITE_BLOCK:
      buf = writeBlock(in);
      break;
    case DataTransferProtocol.OP_READ_METADATA:
      buf += "Op:ReadMetadata";
      break;
    case DataTransferProtocol.OP_REPLACE_BLOCK:
      buf += "Op:ReplaceBlock";
      break;
    case DataTransferProtocol.OP_COPY_BLOCK:
      buf += "Op:CopyBlock";
      break;
    case DataTransferProtocol.OP_BLOCK_CHECKSUM:
      buf += "Op:BlockChecksum";
      break;
    default:
      return "UnknownOpCode";
    }
    return buf;
  }


  // *********************************
  private static String writeBlock(DataInputStream dis) {

    String buf = "";

    try {

      buf += ("   OpCode : WriteBlock \n");

      long blockId = dis.readLong();
      buf += ("   BlkId  : " +  blockId + "\n");

      long genTimeStamp = dis.readLong();
      buf += ("   GenTs  : " + genTimeStamp + "\n");

      int pipelineSize = dis.readInt();
      buf += ("   PipeSz : " + pipelineSize + "\n");

      boolean isRecovery = dis.readBoolean();
      buf += ("   isRec  : " + isRecovery + "\n");

      String client = Text.readString(dis);
      buf += ("   Client : " + client + "\n");

      boolean hasSrcDataNode = dis.readBoolean(); // is src node info present
      buf += ("   srcDn  : " + hasSrcDataNode + "\n");

      if (hasSrcDataNode) {
	DatanodeInfo srcDataNode = null;
        srcDataNode = new DatanodeInfo();
        srcDataNode.readFields(dis);
	buf += datanodeInfoToString(srcDataNode);
      }

      int numTargets = dis.readInt();
      if (numTargets < 0) {
        throw new IOException("Mislabelled incoming datastream.");
      }
      buf += ("   numTgt : " + numTargets + "\n");

      DatanodeInfo targets[] = new DatanodeInfo[numTargets];
      for (int i = 0; i < targets.length; i++) {
        DatanodeInfo tmp = new DatanodeInfo();
        tmp.readFields(dis);
        targets[i] = tmp;

	buf += ("   Target #" + i + "\n");	
	buf += datanodeInfoToString(tmp);
	
      }

      return buf;

    } catch (IOException e) {
      // return whatever we have
      return buf;
    }

  }

  // ****************************************
  public static String datanodeInfoToString(DatanodeInfo tmp) {
    
    String buf = "";
    buf += ("    Name      : " + tmp.getName()            + "\n");
    buf += ("    StorageId : " + tmp.getStorageID()       + "\n");
    buf += ("    InfoPort  : " + tmp.getInfoPort()        + "\n");
    buf += ("    IpcPort   : " + tmp.getIpcPort()         + "\n");
    buf += ("    Capacity  : " + tmp.getCapacity()        + "\n");
    buf += ("    DfsUsed   : " + tmp.getDfsUsed()         + "\n");
    buf += ("    Remaining : " + tmp.getRemaining()       + "\n");
    buf += ("    XceiverCn : " + tmp.getXceiverCount()    + "\n");
    buf += ("    Location  : " + tmp.getNetworkLocation() + "\n");
    buf += ("    HostName  : " + tmp.getHostName()        + "\n");
    buf += ("    AdminSt   : " + tmp.getAdminState()      + "\n");
    return buf;

  }

}

