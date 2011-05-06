
package org.fi;


import org.fi.*;
import org.fi.FMServer.FailType;
import org.fi.FrogClient.Plc;

import java.io.*;
import java.util.AbstractList;
import java.net.InetSocketAddress;
import java.lang.Thread;
import java.lang.StackTraceElement;
import java.nio.channels.FileChannel;

import org.aspectj.lang.Signature; // include this for Signature, etc!
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.SourceLocation;

// hdfs specifics (okay, for frogHooks because this should be specific)
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.FSImage;
import org.apache.hadoop.fs.permission.FsPermission;

import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.DFSClient.DFSOutputStream;
import org.apache.hadoop.hdfs.protocol.ClientDatanodeProtocol;
import org.apache.hadoop.conf.Configuration;


import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;



public aspect frogHooks {


  // #####################################################################
  // #####################################################################
  // #####                                                            ####
  // #####                  D I S K   H O O K S                       ####
  // #####                                                            ####
  // #####################################################################
  // #####################################################################


  static final boolean hookFrogDisk = false;

  // Modeling: files added to editLog
  // intercept ByteArrayOutputStream.writeTo(outputstream)
  static final boolean hookDisk01 = false;
  after(ByteArrayOutputStream buf, OutputStream out) returning
    : ((if(hookFrogDisk) && !within(org.fi.*) && args(out) && target(buf)) &&
       (call (* ByteArrayOutputStream+.*(OutputStream+) throws IOException))
       ) {
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    Util.MESSAGE(" frogHooks 1 ");
    FrogClient.scanEditBuffer(fjp, buf, out);
  }

  // Modeling: files added to fsImage
  // intercept DataOutputStream.write(byte[] b, int off, int len) in saveFSImage
  // Object around (byte[] b, int off, int len, OutputStream out) throws IOException
  after(byte[] b, int off, int len, OutputStream out) returning
    : ((if(hookFrogDisk) && !within(org.fi.*) && args(b, off, len) && target(out) && within(FSImage)) &&
       (call (* DataOutputStream.write(byte[], int, int) throws IOException))
       ) {
    Util.MESSAGE(" frogHooks 2 ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    FrogClient.scanFSImageEntry(fjp, new String(b, off, len), out);
  }

  // Modeling: file truncation
  // Object around (FileChannel fc, long size) throws IOException
  after(FileChannel fc, long sz) returning
    : ((if(hookFrogDisk) && target(fc) && !within(org.fi.*) && args(sz)) &&
       (call(* FileChannel.truncate(long)))
       ) {
    Util.MESSAGE(" frogHooks 3 ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    if (Util.assertCtx(thisJoinPoint, fc.getContext()) && sz == 0)
      FrogClient.truncateStorageFile(fjp, fc.getContext().getTargetIO());
  }

  // Modeling: user's view (via Hadoop API)
  // if you intercept HDFS class, must use execution
  after(String p, FsPermission m) returning (boolean rv)
    : ((if(hookFrogDisk) && !within(org.fi.*) && args(p, m)) &&
       (execution (boolean NameNode.mkdirs(String, FsPermission)))
       ) {
    Util.MESSAGE(" frogHooks 4 ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    if (rv) FrogClient.addPathUser(fjp, p);
  }


  // Modeling: file creation(via RAF 1st way, e.g. fsimage.ckpt)
  after(String f, String m) returning
    : ((if(hookFrogDisk) && !within(org.fi.*) && args(f, m)) &&
       (call (RandomAccessFile.new(String, String)))
       ) {
    Util.MESSAGE(" frogHooks 5a ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    FrogClient.addStorageFile(fjp, f);
  }


  // Modeling: file creation(via RAF 2nd way, e.g. edits.new)
  after(File f, String m) returning
    : ((if(hookFrogDisk) && !within(org.fi.*) && args(f, m)) &&
       (call (RandomAccessFile.new(File, String)))
       ) {
    Util.MESSAGE(" frogHooks 5b ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    if (Util.assertCtx(thisJoinPoint, f.getContext()))
      FrogClient.addStorageFile(fjp, f.getContext().getTargetIO());
  }

  // Modeling: file creation(via FOS)
  after(File f) returning
    : ((if(hookFrogDisk) && !within(org.fi.*) && args(f)) &&
       (call (FileOutputStream.new(File)))
       ) {
    Util.MESSAGE(" frogHooks 6 ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    FrogClient.addStorageFile(fjp, f.getContext().getTargetIO());
  }

  // Modeling: file renaming
  after(File s, File d) returning (boolean rv)
    : ((if(hookFrogDisk) && !within(org.fi.*) && args(d) && target(s)) &&
       (call (boolean File.renameTo(File)))
       ) {
    Util.MESSAGE(" frogHooks 7 ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    FrogClient.renameStorageFile(fjp, s.getContext().getTargetIO(),
                                 d.getContext().getTargetIO());
  }


  // #####################################################################
  // #####################################################################
  // #####                                                            ####
  // #####              N E T W O R K     H O O K S                   ####
  // #####                                                            ####
  // #####################################################################
  // #####################################################################



  static final boolean hookFrogNet = false;

  // List of files that we must intercept:
  //  - core/net/SocketIOWithTimeout.java
  //  - DFSClient.java
  //  - ??

  // Modeling: network failure
  after(SocketChannel sc) throwing (IOException e)
    : ((if (hookFrogNet) && target(sc)) &&
       (call (boolean SocketChannel.finishConnect()))
       ) {
    Util.MESSAGE(" frogNetHooks 1 ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    FrogClient.failedConnection(fjp, sc);
  }


  // #####################################################################
  // #####################################################################
  // #####                                                            ####
  // #####             P R O T O C O L     H O O K S                  ####
  // #####                                                            ####
  // #####################################################################
  // #####################################################################


  static final boolean hookFrogProtocol = false;

  // Modeling: client protocol
  after() returning (LocatedBlock lb)
    : ((if (hookFrogProtocol) && !within(org.fi.*)) &&
       (call (LocatedBlock ClientProtocol.addBlock(String,String)))
       ) {
    Util.MESSAGE(" frogNetHooks 2 ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    FrogClient.returnedNodes(fjp, lb);
  }

  Object around(String s, long o, long l)
    : ((if (hookFrogProtocol) && !within(org.fi.*) && args(s,o,l)) &&
       (call (LocatedBlocks ClientProtocol.getBlockLocations(String,long,long)
              throws IOException))
       ) {
    System.out.println("CP.getBlockLocations()");
    return proceed(s,o,l);
  }


  // #####################################################################
  // #####################################################################
  // #####                                                            ####
  // #####             ???     H O O K S                              ####
  // #####                                                            ####
  // #####################################################################
  // #####################################################################


  // MAYBE DEPRECATED ........... FMServer should notify
  // Modeling: dead nodes
  // NOTE: I must be careful with this one, or otherwise, we'll hit
  // an infinite loop
  before(int exitStatus)
    : ((if (hookFrogProtocol) && !within(org.fi.*) && args(exitStatus)) &&
       (call (void System.exit(int)))
       ) {
    Util.MESSAGE(" frogNetHooks 3 ");
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint);
    FrogClient.deadNode(fjp, exitStatus);
  }



  // #####################################################################
  // #####################################################################
  // #####                                                            ####
  // #####        C L I E N T    P R O T O C O L     H O O K S        ####
  // #####                                                            ####
  // #####################################################################
  // #####################################################################
  
  
  // ******************************************************
  // public int haryadiTest(int x) throws IOException;
  // ******************************************************
  static final boolean hookCp01 = false;
  Object around(int x) throws IOException
    : ((if (hookCp01) && !within(org.fi.*) && args(x)) &&
       (call (int ClientProtocol.haryadiTest(int) throws IOException))
       ) {
    try {
      FrogClient.cpHaryadiTest(thisJoinPoint, Plc.BEFORE, null, x);
      Object obj = proceed(x);
      FrogClient.cpHaryadiTest(thisJoinPoint, Plc.RETURNING, obj, x);
      return obj;
    } catch(IOException e) {
      FrogClient.cpHaryadiTest(thisJoinPoint, Plc.EXCEPTION, e, x);
      throw e;
    }
  }

  // ******************************************************
  //  public LocatedBlocks getBlockLocations(String src, long offset,
  //                                         long length) throws IOException;
  // ******************************************************
  static final boolean hookCp02 = false;
  Object around(String s, long o, long l) throws IOException
    : ((if (hookCp02) && !within(org.fi.*) && args(s,o,l)) &&
       (call (LocatedBlocks ClientProtocol.getBlockLocations(String,long,long)
              throws IOException))
       ) {
    try {
      FrogClient.cpGetBlockLocations(thisJoinPoint, Plc.BEFORE, null, s, o, l);
      Object obj = proceed(s, o, l);
      FrogClient.cpGetBlockLocations(thisJoinPoint, Plc.RETURNING, obj, s, o, l);
      return obj;
    } catch(IOException e) {
      FrogClient.cpGetBlockLocations(thisJoinPoint, Plc.EXCEPTION, e, s, o, l);
      throw e;
    }
  }

  // ******************************************************
  // public void create(String src, FsPermission masked, String clientName,
  //                    boolean overwrite, short replication, long blockSize
  //                    ) throws IOException;
  // ******************************************************
  static final boolean hookCp03 = false;
  Object around(String s, FsPermission m, String cn,
                boolean o, short r, long bs) throws IOException
    : ((if (hookCp02) && !within(org.fi.*) && args(s,m,cn,o,r,bs)) &&
       (call (void ClientProtocol.create(String,FsPermission,String,
                                         boolean,short,long)
              throws IOException))
       ) {
    try {
      FrogClient.cpCreate(thisJoinPoint, Plc.BEFORE, null, s, m, cn, o, r, bs);
      Object obj = proceed(s, m, cn, o, r, bs);
      FrogClient.cpCreate(thisJoinPoint, Plc.RETURNING, obj, s, m, cn, o, r, bs);
      return obj;
    } catch(IOException e) {
      FrogClient.cpCreate(thisJoinPoint, Plc.EXCEPTION, e, s, m, cn, o, r, bs);
      throw e;
    }
  }

  // ******************************************************
  // public LocatedBlock append(String src, String clientName) throws IOException;
  // ******************************************************
  static final boolean hookCp04 = false;
  Object around(String s, String cn) throws IOException
    : ((if (hookCp04) && !within(org.fi.*) && args(s,cn)) &&
       (call (LocatedBlock ClientProtocol.append(String,String)
              throws IOException))
       ) {
    try {
      FrogClient.cpAppend(thisJoinPoint, Plc.BEFORE, null, s, cn);
      Object obj = proceed(s, cn);
      FrogClient.cpAppend(thisJoinPoint, Plc.RETURNING, obj, s, cn);
      return obj;
    } catch(IOException e) {
      FrogClient.cpAppend(thisJoinPoint, Plc.EXCEPTION, e, s, cn);
      throw e;
    }
  }


  // ******************************************************
  // public void abandonBlock(Block b, String src, String holder) throws IOException;
  // ******************************************************
  static final boolean hookCp05 = false;
  Object around(Block b, String s, String h) throws IOException
    : ((if (hookCp05) && !within(org.fi.*) && args(b,s,h)) &&
       (call (void ClientProtocol.abandonBlock(Block,String,String)
              throws IOException))
       ) {
    try {
      FrogClient.cpAbandonBlock(thisJoinPoint, Plc.BEFORE, null, b, s, h);
      Object obj = proceed(b, s, h);
      FrogClient.cpAbandonBlock(thisJoinPoint, Plc.RETURNING, obj, b, s , h);
      return obj;
    } catch(IOException e) {
      FrogClient.cpAbandonBlock(thisJoinPoint, Plc.EXCEPTION, e, b, s, h);
      throw e;
    }
  }

  // ******************************************************
  // public LocatedBlock addBlock(String src, String clientName) throws IOException;
  // ******************************************************
  static final boolean hookCp06 = false;
  Object around(String s, String cn) throws IOException
    : ((if (hookCp06) && !within(org.fi.*) && args(s,cn)) &&
       (call (LocatedBlock ClientProtocol.addBlock(String,String)
              throws IOException))
       ) {
    try {
      FrogClient.cpAddBlock(thisJoinPoint, Plc.BEFORE, null, s, cn);
      Object obj = proceed(s, cn);
      FrogClient.cpAddBlock(thisJoinPoint, Plc.RETURNING, obj, s, cn);
      return obj;
    } catch(IOException e) {
      FrogClient.cpAddBlock(thisJoinPoint, Plc.EXCEPTION, e, s, cn);
      throw e;
    }
  }

  // ******************************************************
  // public boolean complete(String src, String clientName) throws IOException;
  // ******************************************************
  static final boolean hookCp07 = false;
  Object around(String s, String cn) throws IOException
    : ((if (hookCp07) && !within(org.fi.*) && args(s,cn)) &&
       (call (boolean ClientProtocol.complete(String,String)
              throws IOException))
       ) {
    try {
      FrogClient.cpComplete(thisJoinPoint, Plc.BEFORE, null, s, cn);
      Object obj = proceed(s, cn);
      FrogClient.cpComplete(thisJoinPoint, Plc.RETURNING, obj, s, cn);
      return obj;
    } catch(IOException e) {
      FrogClient.cpComplete(thisJoinPoint, Plc.EXCEPTION, e, s, cn);
      throw e;
    }
  }


  // ******************************************************
  // public void reportBadBlocks(LocatedBlock[] blocks) throws IOException;
  // ******************************************************
  static final boolean hookCp08 = false;
  Object around(LocatedBlock[] blks) throws IOException
    : ((if (hookCp08) && !within(org.fi.*) && args(blks)) &&
       (call (void ClientProtocol.reportBadBlocks(LocatedBlock[])
              throws IOException))
       ) {
    try {
      FrogClient.cpReportBadBlocks(thisJoinPoint, Plc.BEFORE, null, blks);
      Object obj = proceed(blks);
      FrogClient.cpReportBadBlocks(thisJoinPoint, Plc.RETURNING, obj, blks);
      return obj;
    } catch(IOException e) {
      FrogClient.cpReportBadBlocks(thisJoinPoint, Plc.EXCEPTION, e, blks);
      throw e;
    }
  }


  // ******************************************************
  // public boolean rename(String src, String dst) throws IOException;
  // ******************************************************
  static final boolean hookCp09 = false;
  Object around(String s, String d) throws IOException
    : ((if (hookCp09) && !within(org.fi.*) && args(s,d)) &&
       (call (boolean ClientProtocol.rename(String,String)
              throws IOException))
       ) {
    try {
      FrogClient.cpRename(thisJoinPoint, Plc.BEFORE, null, s, d);
      Object obj = proceed(s, d);
      FrogClient.cpRename(thisJoinPoint, Plc.RETURNING, obj, s, d);
      return obj;
    } catch(IOException e) {
      FrogClient.cpRename(thisJoinPoint, Plc.EXCEPTION, e, s, d);
      throw e;
    }
  }


  // ******************************************************
  // public boolean delete(String src) throws IOException;
  // ******************************************************
  static final boolean hookCp10 = false;
  Object around(String s) throws IOException
    : ((if (hookCp10) && !within(org.fi.*) && args(s)) &&
       (call (boolean ClientProtocol.delete(String)
              throws IOException))
       ) {
    try {
      FrogClient.cpDelete(thisJoinPoint, Plc.BEFORE, null, s);
      Object obj = proceed(s);
      FrogClient.cpDelete(thisJoinPoint, Plc.RETURNING, obj, s);
      return obj;
    } catch(IOException e) {
      FrogClient.cpDelete(thisJoinPoint, Plc.EXCEPTION, e, s);
      throw e;
    }
  }


  // ******************************************************
  // public boolean delete(String src, boolean recursive) throws IOException;
  // ******************************************************
  static final boolean hookCp11 = false;
  Object around(String s, boolean r) throws IOException
    : ((if (hookCp11) && !within(org.fi.*) && args(s,r)) &&
       (call (boolean ClientProtocol.delete(String,boolean)
              throws IOException))
       ) {
    try {
      FrogClient.cpDelete(thisJoinPoint, Plc.BEFORE, null, s, r);
      Object obj = proceed(s, r);
      FrogClient.cpDelete(thisJoinPoint, Plc.RETURNING, obj, s, r);
      return obj;
    } catch(IOException e) {
      FrogClient.cpDelete(thisJoinPoint, Plc.EXCEPTION, e, s, r);
      throw e;
    }
  }


  // ******************************************************
  // public void renewLease(String clientName) throws IOException;
  // ******************************************************
  static final boolean hookCp12 = false;
  Object around(String cn) throws IOException
    : ((if (hookCp12) && !within(org.fi.*) && args(cn)) &&
       (call (void ClientProtocol.renewLease(String)
              throws IOException))
       ) {
    try {
      FrogClient.cpRenewLease(thisJoinPoint, Plc.BEFORE, null, cn);
      Object obj = proceed(cn);
      FrogClient.cpRenewLease(thisJoinPoint, Plc.RETURNING, obj, cn);
      return obj;
    } catch(IOException e) {
      FrogClient.cpRenewLease(thisJoinPoint, Plc.EXCEPTION, e, cn);
      throw e;
    }
  }

  // ******************************************************
  // public void fsync(String src, String client) throws IOException;
  // ******************************************************
  static final boolean hookCp13 = false;
  Object around(String s, String c) throws IOException
    : ((if (hookCp13) && !within(org.fi.*) && args(s,c)) &&
       (call (void ClientProtocol.fsync(String,String)
              throws IOException))
       ) {
    try {
      FrogClient.cpFsync(thisJoinPoint, Plc.BEFORE, null, s, c);
      Object obj = proceed(s, c);
      FrogClient.cpFsync(thisJoinPoint, Plc.RETURNING, obj, s, c);
      return obj;
    } catch(IOException e) {
      FrogClient.cpFsync(thisJoinPoint, Plc.EXCEPTION, e, s, c);
      throw e;
    }
  }


  // ******************************************************

  // NOT SUPPORTED YET:

  // public boolean setReplication(String src, short replication) throws IOException;
  // public void setPermission(String src, FsPermission permission) throws IOException;
  // public void setOwner(String src, String username, String groupname)
  //                     throws IOException;

  // public boolean mkdirs(String src, FsPermission masked) throws IOException;
  // public FileStatus[] getListing(String src) throws IOException;
  // public long[] getStats() throws IOException;

  //  public DatanodeInfo[] getDatanodeReport(FSConstants.DatanodeReportType type)
  // throws IOException;
  // public long getPreferredBlockSize(String filename) throws IOException;
  // public void finalizeUpgrade() throws IOException;
  // public boolean setSafeMode(FSConstants.SafeModeAction action) throws IOException;
  // public void saveNamespace() throws IOException;
  // public void refreshNodes() throws IOException;
  // public UpgradeStatusReport distributedUpgradeProgress(UpgradeAction action)
  // throws IOException;
  // public void metaSave(String filename) throws IOException;
  // public FileStatus getFileInfo(String src) throws IOException;
  // public ContentSummary getContentSummary(String path) throws IOException;
  // public void setQuota(String path, long namespaceQuota, long diskspaceQuota)
  // throws IOException;

  // public void setTimes(String src, long mtime, long atime) throws IOException;

  // ******************************************************



  // #####################################################################
  // #####################################################################
  // #####                                                            ####
  // #####    C L I E N T     D A T A N O D E     P R O T O C O L     ####
  // #####                                                            ####
  // #####################################################################
  // #####################################################################


  
  // ******************************************************
  // LocatedBlock recoverBlock(Block block, boolean keepLength,
  //                           DatanodeInfo[] targets) throws IOException;
  // ******************************************************
  static final boolean hookCdp01 = false;
  Object around(ClientDatanodeProtocol cdp, Block b, boolean kl, DatanodeInfo[] ts)
    throws IOException
    : ((if (hookCdp01) && !within(org.fi.*) && args(b,kl,ts) && target(cdp)) &&
       (call (void ClientDatanodeProtocol.recoverBlock(Block,boolean,DatanodeInfo[])
              throws IOException))
       ) {
    try {
      FrogClient.cdpRecoverBlock(thisJoinPoint, Plc.BEFORE, null, cdp, b, kl, ts);
      Object obj = proceed(cdp, b, kl, ts);
      FrogClient.cdpRecoverBlock(thisJoinPoint, Plc.RETURNING, obj, cdp, b, kl, ts);
      return obj;
    } catch(IOException e) {
      FrogClient.cdpRecoverBlock(thisJoinPoint, Plc.EXCEPTION, e, cdp, b, kl, ts);
      throw e;
    }
  }


  // **************************************************
  // helper, because we don't know the actual datanode from 
  // ClientDatanodeProtocol
  // **************************************************
  static final boolean hookCdp02 = false;
  Object around(DatanodeInfo dnid, Configuration c) throws IOException
    : ((if (hookCdp02) && !within(org.fi.*) && args(dnid,c)) &&
       (call (void DFSClient.createClientDatanodeProtocolProxy
	      (DatanodeID,Configuration)))
       ) {
    FrogClient.cdpProxy(thisJoinPoint, Plc.BEFORE, null, dnid);
    Object obj = proceed(dnid, c);
    FrogClient.cdpProxy(thisJoinPoint, Plc.RETURNING, obj, dnid);
    return obj;
  }



  // ####################################################################
  // ####################################################################
  // ####                                                            ####
  // ####         D A T A    T R A N S F E R     P R O T O C O L     ####
  // ####                                                            ####
  // ####################################################################
  // ####################################################################
  
  
  // ******************************************************
  // out.flush()
  // ******************************************************
  static final boolean hookDtp02 = false;
  Object around(DataOutputStream dos) throws IOException
    : ((if (hookDtp02) && !within(org.fi.*) && target(dos)) &&
       (call (void DataOutputStream.flush() throws IOException))
       ) {
    Context ctx = dos.getContext();
    if (ctx == null) return proceed(dos);
    if (!Util.isNetIO(ctx.getTargetIO())) return proceed(dos);
    FrogClient.dtpFlushOp(thisJoinPoint, Plc.BEFORE, null, ctx);
    try {
      Object obj = proceed(dos);
      FrogClient.dtpFlushOp(thisJoinPoint, Plc.RETURNING, obj, ctx);
      return obj;
    } catch(IOException e) {
      FrogClient.dtpFlushOp(thisJoinPoint, Plc.EXCEPTION, e, ctx);
      throw e;
    }
  }  

  // ******************************************************
  // from DataTransferProtocol.java, in.readShort();
  // ******************************************************
  static final boolean hookDtp03 = false;
  Object around(DataInputStream dis) throws IOException
    : ((if (hookDtp03) && !within(org.fi.*) && target(dis)) &&
       (call (short DataInputStream.readShort() throws IOException))
       ) {
    Context ctx = dis.getContext();
    if (ctx == null || !Util.isNetIO(ctx.getTargetIO())) return proceed(dis);
    try {
      FrogClient.dtpReadStatus(thisJoinPoint, Plc.BEFORE, null, ctx);
      Object obj = proceed(dis);
      FrogClient.dtpReadStatus(thisJoinPoint, Plc.RETURNING, obj, ctx);
      return obj;
    } catch(IOException e) {
      FrogClient.dtpReadStatus(thisJoinPoint, Plc.EXCEPTION, e, ctx);
      throw e;
    }
  }  
  

  // ******************************************************
  // from DataTransferProtocol.java, DataInput.readX(..)
  // We can't use DataInputStream because some wrappers
  // e.g. Text.readString(DataInput) wraps DIS into DI
  // ******************************************************
  static final boolean hookDtp04 = false;
  Object around(ClassWC cwc) throws IOException
    : ((if (hookDtp04) && !within(org.fi.*) && target(cwc)) &&
       (call (* DataInput.read*(..)       throws IOException) ||
	call (* DataInputStream.read*(..) throws IOException))
       ) {
    Context ctx = cwc.getContext();
    if (ctx == null) return proceed(cwc);
    if (!Util.isNetIO(ctx.getTargetIO())) return proceed(cwc);
    try {
      FrogClient.dtpReadAny(thisJoinPoint, Plc.BEFORE, null, ctx);
      Object obj = proceed(cwc);
      FrogClient.dtpReadAny(thisJoinPoint, Plc.RETURNING, obj, ctx);
      return obj;
    } catch(IOException e) {
      FrogClient.dtpReadAny(thisJoinPoint, Plc.EXCEPTION, e, ctx);
      throw e;
    }
  }  
  

  // ******************************************************
  // from DataTransferProtocol.java, out.write(byte) // overwritten by flush()
  // ******************************************************
  /*
  static final boolean hookDtp01 = false;
  Object around(DataOutputStream dos, int b) throws IOException
    : ((if (hookDtp01) && !within(org.fi.*) && args(b) && target(dos)) &&
       (call (void DataOutputStream.write(int) throws IOException))
       ) {
    Context ctx = dos.getContext();
    if (ctx == null || !Util.isNetIO(ctx.getTargetIO())) return proceed(dos, b);
    try {
      FrogClient.dtpWriteOp(thisJoinPoint, Plc.BEFORE, null, ctx, b);
      Object obj = proceed(dos, b);
      FrogClient.dtpWriteOp(thisJoinPoint, Plc.RETURNING, obj, ctx, b);
      return obj;
    } catch(IOException e) {
      FrogClient.dtpWriteOp(thisJoinPoint, Plc.EXCEPTION, e, ctx, b);
      throw e;
    }
  }  
  */
  


  // ####################################################################
  // ####################################################################
  // ####                                                            ####
  // ####             C L I E N T    A P I    P R O T O C O L        ####
  // ####                                                            ####
  // ####################################################################
  // ####################################################################

  // **************************************************
  // note: for client api, must be around EXECUTION (not call)
  // **************************************************
  static final boolean hookApi01 = false;
  Object around(DFSOutputStream dfsos) throws IOException
    : ((if (hookApi01) && !within(org.fi.*) && target(dfsos)) &&
       (execution (void DFSOutputStream.close() throws IOException))
       ) {
    try {
      FrogClient.apiClose(thisJoinPoint, Plc.BEFORE, null, dfsos);
      Object obj = proceed(dfsos);
      FrogClient.apiClose(thisJoinPoint, Plc.RETURNING, obj, dfsos);
      return obj;
    } catch(IOException e) {
      FrogClient.apiClose(thisJoinPoint, Plc.EXCEPTION, e, dfsos);
      throw e;
    }
  }



  
}


