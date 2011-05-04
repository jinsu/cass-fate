

package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

public class Hdfs {

  FileSystem fs = null;
  Configuration conf = null;

  //static final int FILE_SIZE = 16;
  static final int FILE_SIZE = 512;                                                    

  static final long blockSize = 4096;
  Utility u;
  short dfsReplication;
  int ioFileBufferSize;
  int ioBytesPerChecksum;

  byte buffer[];

  // *******************************************
  public Hdfs(Driver d) {


    this.u = d.getUtility();

    buffer = new byte[FILE_SIZE];
    for (int i = 0; i < FILE_SIZE; i ++) {
      buffer[i] = '-';
    }

    setupConfiguration();


    DFSClient.setMyPrintStream(Utility.ps);


  }

  // *******************************************
  private void setupConfiguration() {

    u.print("- in Hdfs.setupConfiguration ... \n");

    conf = new Configuration();
    conf.setInt("dfs.datanode.handler.count", 50);
    conf.setBoolean("dfs.support.append", true);

    // we can load these ones properly, because these default
    // configurations xml are put in hadoop-dir/build/classes/
    // which is part of our classpath in the build.xml
    Configuration.addDefaultResource("core-default.xml");
    Configuration.addDefaultResource("hdfs-default.xml");

    // for these two files we must add hadoop-dir/conf/
    // to our classpath so that we can load it as is
    Configuration.addDefaultResource("hdfs-site.xml");
    Configuration.addDefaultResource("core-site.xml");

    dfsReplication = (short)conf.getInt("dfs.replication", 100);
    ioFileBufferSize = conf.getInt("io.file.buffer.size", 4096);
    ioBytesPerChecksum = conf.getInt("io.bytes.per.checksum", 512);

    u.print(String.format("   dfs.replication is %d \n", dfsReplication));

    u.print(String.format("   fs.default.name is %s \n",
                          conf.get("fs.default.name", "abc")));

  }

  // *******************************************
  // we only want to do this if fs is null
  // we cannot create start this in HDFS construct
  // because we don't know when namenode is up or down
  private void reconnectToHDFS() {
    try {

      u.print("- Reconnecting to HDFS ... \n");

      // note that if the server is not waken up yet,
      // this will wait forever, because I hacked
      // the proxy a while ago
      fs = FileSystem.get(conf);


      u.print(String.format("- Connected to fs %s ... \n", fs.getName()));


    } catch (Exception e) {
      u.EXCEPTION(" In HDFS construct", e);
    }
  }

  // *******************************************
  private void assertConnection() {
    if (fs == null)
      reconnectToHDFS();
  }

  // *******************************************
  public void mkdirViaShell() {

    u.print("- Mkdir files ...\n");
    String cmdout = u.runCommand("bin/hadoop fs -mkdir files");
    u.print(cmdout);
    u.print("\n\n");

  }

  // *******************************************
  public void putFileViaShell(String dest) {


    String s = String.format("- Put file to %s ... \n", dest);
    u.print(s);
    String cmdout = u.runCommand("bin/hadoop fs -put file4KB files/" + dest);
    // String cmdout = u.runCommand("bin/hadoop fs -put file4KB " + dest);
    u.print(cmdout);
    u.print("\n\n");


  }


  // *******************************************
  public void lsViaShell() {
    assertConnection();

    u.print("- Ls ...\n");
    String cmdout = u.runCommand("bin/hadoop fs -ls files");
    // String cmdout = u.runCommand("bin/hadoop fs -ls .");
    u.print(cmdout);
    u.print("\n\n");

  }


  // *******************************************
  public void testClientNameNode() {
    u.print("- testClientNameNode ... \n");
    try {
      DFSClient.haryadiTestClientNameNode();
    } catch(IOException e) {
      u.EXCEPTION("test", e);
    }
  }


  // *******************************************
  public void putFile(String dest, Experiment exp) {

    u.print("- Hdfs.putfile " + dest + "...\n");

    assertConnection();


    try {

      Path dPath = new Path("files/" + dest);
      // Path dPath = new Path(dest);
      if (fs.exists(dPath)) {
        u.ERROR("something is wrong with the experiment, multiple files exist");
      }

      u.print("- Creating FSDOS stm \n");

      FSDataOutputStream stm = fs.create(dPath, false, ioFileBufferSize,
                                         dfsReplication, blockSize);

      u.print("- Writing FSDOS \n");
      stm.write(buffer, 0, FILE_SIZE);

      u.print("- Closing FSDOS .. all being done here\n");
      stm.close();


    } catch (IOException e) {

      u.EXCEPTION("hdfs.putfile fails", e);

      u.ERROR("hdfs.putfile fails");

      // if we get here, the experiment has failed
      exp.markFailFromNonFrog();
      exp.addNonFrogReport("hdfs.putFile(" + dest +") FAILS!");
      exp.addExceptionToNonFrogReport(e);


    }
    u.print("- End of hdfs.putfile\n");

  }

  // *******************************************
  public void getFile(String dest, Experiment exp) {

    u.print("- Hdfs.getfile " + dest + "...\n");


    // if the experiment already fails .. no need to move on
    if (exp.isFail()) {
      return;
    }

    Exception ex = null;
    byte [] tmpbuf = new byte[FILE_SIZE];
    Path dPath = new Path("files/" + dest);
    FSDataInputStream in;
    int rv = 0;
    boolean fail = false;

    try {
      in = fs.open(dPath);
      rv = in.read(tmpbuf, 0, FILE_SIZE);
      in.close();
    }  catch (IOException e) {
      fail = true;
      ex = e;
    }

    if (rv == 0)
      fail = true;
    if (tmpbuf[0] != '-')
      fail = true;

    if (fail == false)
      return;

    exp.markFailFromNonFrog();
    exp.addNonFrogReport("hdfs.getFile(" + dest + ") FAILS!");
    if (ex != null)
      exp.addExceptionToNonFrogReport(ex);

  }



  // *******************************************
  // append to a file, file must exist before
  public void appendFile(String dest, Experiment exp) {

    u.print("- Hdfs.appendfile " + dest + "...\n");

    assertConnection();
    FSDataOutputStream stm = null;
    try {

      Path dPath = new Path("files/" + dest);
      // Path dPath = new Path(dest);
      if (!fs.exists(dPath)) {
        u.ERROR("something is wrong with the experiment," +
                " append to non-existing file");
      }

      u.print("- Creating FSDOS stm for append \n");

      //      FSDataOutputStream stm = fs.append(dPath);
      stm = fs.append(dPath);
      u.print("- Append FSDOS \n");
      stm.write(buffer, 0, FILE_SIZE);

      u.print("- Closing FSDOS .. all being done here\n");
      stm.close();

    } catch (IOException e) {
      u.EXCEPTION("hdfs.appendfile fails", e);
      u.ERROR("hdfs.appendfile fails");

      // if we get here, the experiment has failed
      exp.markFailFromNonFrog();
      exp.addNonFrogReport("hdfs.appendFile(" + dest +") FAILS!");
      exp.addExceptionToNonFrogReport(e);

      if (stm != null) {
        try {
          stm.close();
        }
        catch (IOException e1) {
          u.EXCEPTION("stm.close failed", e1);
        }
      }

    }

    u.print("- End of hdfs.appendfile\n");

  }







  // *******************************************
  public void delete(String dest, Experiment exp) {
    try {
      Path dPath = new Path("files/" + dest);
      fs.delete(dPath);
    } catch (IOException e) {
      u.EXCEPTION("hdfs.delete fails", e);
    }
  }

  // *******************************************
  public void clearVirtualWaitTime() {
    DFSClient.clearVirtualWaitTime();
  }

  // *******************************************
  public int getVirtualWaitTime() {
    return DFSClient.getVirtualWaitTime();
  }

}