

package org.fi;


import org.fi.*;


import java.io.*;
import java.util.*;
import java.lang.*;
import java.net.*;
import java.nio.channels.*;

public aspect weavert {

  declare parents: File implements ClassWC;
  declare parents: DataInput implements ClassWC;
  declare parents: DataOutput implements ClassWC;
  declare parents: InputStream implements ClassWC;
  declare parents: OutputStream implements ClassWC;
  declare parents: RandomAccessFile implements ClassWC;
  declare parents: FileChannel implements ClassWC;
  declare parents: Reader implements ClassWC;  
  declare parents: Writer implements ClassWC;  
  declare parents: FileLock implements ClassWC;
  
  // WARNING: you should declare something that implements
  // ClassWc ONLY IF you want to magically transfer the context.
  // But for sockets, we want to somewhat manually transfer the
  // context for now.
  // For example: for, DataOutputStream.new(Socket)
  // we want to transfer them ourselves .. 
  // declare parents: SocketChannel implements ClassWC;
  // declare parents: Socket implements ClassWC;
  
  public Context File.context = null;
  public Context File.getContext() { return context; }
  public void File.setContext(Context x) { context = x; }

  public Context InputStream.context = null;
  public Context InputStream.getContext() { return context; }
  public void InputStream.setContext(Context x) { context = x; }

  public Context OutputStream.context = null;
  public Context OutputStream.getContext() { return context; }
  public void OutputStream.setContext(Context x) { context = x; }
  
  public Context RandomAccessFile.context = null;
  public Context RandomAccessFile.getContext() { return context; }
  public void RandomAccessFile.setContext(Context x) { context = x; }

  public Context FileChannel.context = null;
  public Context FileChannel.getContext() { return context; }
  public void FileChannel.setContext(Context x) { context = x; }
  
  public Context Reader.context = null;
  public Context Reader.getContext() { return context; }
  public void Reader.setContext(Context x) { context = x; }  

  public Context Writer.context = null;
  public Context Writer.getContext() { return context; }
  public void Writer.setContext(Context x) { context = x; }  

  public Context FileLock.context = null;
  public Context FileLock.getContext() { return context; }
  public void FileLock.setContext(Context x) { context = x; }  


  public Context SocketChannel.context = null;
  public Context SocketChannel.getContext() { return context; }
  public void SocketChannel.setContext(Context x) { context = x; }  
  
  public Context Socket.context = null;
  public Context Socket.getContext() { return context; }
  public void Socket.setContext(Context x) { context = x; }  
  
  
}



