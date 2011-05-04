

package org.fi;


import java.io.*;
import java.util.*;

public class BufferedOutputStreamWrapper extends BufferedOutputStream {
  
  
  public BufferedOutputStreamWrapper(OutputStream os) {
    super(os);
  }

  public BufferedOutputStreamWrapper(OutputStream os, int size) {
    super(os, size);
  }
  
  public byte[] getBuffer() {
    return buf;
  }

  public int getCount() {
    return count;
  }

}


