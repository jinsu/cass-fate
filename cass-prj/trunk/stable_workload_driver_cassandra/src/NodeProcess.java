

package org.fi;


// A NodeProcess is built from the tmp-pid files
public class NodeProcess {

  private String name;   // the node's name (e.g. namenode, pdatanode-1, fi)
  private String pid;    // the pid
  private String tmpPidFile; // the tmp-pid file name
  private boolean isDn;  // is this a datanode?
  private String dnId;   // the datanode Id if applicable


  public NodeProcess(String tmpPidFile, String pid, String nodeName) {

    this.pid = pid;
    this.tmpPidFile = tmpPidFile;
    

    // now let's create the name
    // remove the hadoop prefix and .pid 
    String tmp = nodeName;
    // tmpPidFile.replaceAll(Driver.HADOOP_USERNAME + "-", "");
    //tmp = tmp.replaceAll(".pid", "");
    name = tmp;
    
    // now let's see if it's a datanode,
    // if so, cut the filename so we get the dnId
    if (name.contains("datanode")) {
      isDn = true;
      dnId = name.replaceAll("pdatanode", "");
    }
    else {
      isDn = false;
      dnId = "-X";
    }
  }
  
  public String toString() {
    return String.format("%-5d  %-12s  %2s  \n", pid, name, dnId);
  }

  public String getName() {
    return name;
  }

  public String getPid() {
    return pid;
  }

  public String getTmpPidFile() {
    return tmpPidFile;
  }

  public String getDnId() {
    return dnId;
  }
  
  public boolean isDataNode() {
    return isDn;
  }

}