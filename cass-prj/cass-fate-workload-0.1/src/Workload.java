
package org.fi;

import java.io.*;
import java.nio.channels.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;



public interface Workload {
  
  public void run();

  public void runWithFailure();
  
  public void preSetup();
  
  public void postSetup();
  
}
