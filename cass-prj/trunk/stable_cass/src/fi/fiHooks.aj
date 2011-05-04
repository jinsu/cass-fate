
package org.fi;


import org.fi.*;
import org.fi.FMServer.*;
import org.fi.FMJoinPoint.*;


import java.io.*;
// import java.util.*;
import java.net.InetSocketAddress;
import java.lang.Thread;
import java.lang.StackTraceElement;


import org.aspectj.lang.Signature; // include this for Signature, etc!
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.SourceLocation;



// ***************************************************
// SOME IMPORTANT NOTES:
//
//   1. For constructors:
//
//      ClassWC context can only be gotten after we run proceed()
//      So, ther eis no point to insert fiHook before the proceed()
//
// ***************************************************
public aspect fiHooks {



  // #####################################################################
  // #####################################################################
  // ##                                                                 ##
  // ##                 W R I T E       A D V I C E S                   ##
  // ##                                                                 ##
  // #####################################################################
  // #####################################################################


  // *********************************************************
  // 1. target(c), Write, IOException, NonBoolean
  // *********************************************************
  static final boolean hookWrite01 = true;
  Object around(ClassWC c) throws IOException
    : ((if(hookWrite01) && !within(org.fi.*) && target(c)) &&
       (// buffered writes:
	call (* OutputStream.*(..)             throws IOException) ||
	call (* DataOutputStream+.*(..)        throws IOException) ||
	call (* FileOutputStream+.*(..)        throws IOException) ||
	// non-buffered writes:
	call (* OutputStream+.flush(..)     throws IOException) ||
	call (* OutputStream+.close(..)     throws IOException) ||
        call (* FileChannel.force(..)          throws IOException) ||
        call (* FileChannel.truncate(..)       throws IOException) ||
        call (* FileChannel.write*(..)         throws IOException) ||
        call (* RandomAccessFile+.write*(..)    throws IOException) ||
        call (* RandomAccessFile+.setLength(..) throws IOException)
        )
       ) {
		FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint  , c, null,
                                      JoinPlc.BEFORE , JoinIot.WRITE,
                                      JoinExc.IO     , JoinRbl.NO);
    //System.out.println("FIHOOK!!!!!!!!!!!!!!!!!!!!!" + fjp);
		
		Object obj = FMClient.fiHookIox(fjp);
    if (obj != null) return obj;
    obj = proceed(c);
    fjp.setAfter(obj);
    return FMClient.fiHookIox(fjp);
  }



  // *********************************************************
  // 2. target(c), Write, IOException, Boolean
  // *********************************************************
  static final boolean hookWrite02 = true;
  Object around(ClassWC c) throws IOException
    : ((if(hookWrite02) && !within(org.fi.*) && target(c)) &&
       (call (* File.createNewFile(..))
        )
       ) {
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint  , c, null,
                                      JoinPlc.BEFORE , JoinIot.WRITE,
                                      JoinExc.IO     , JoinRbl.YES);
    Object obj = FMClient.fiHookIox(fjp);
    if (obj != null) return obj;
    obj = proceed(c);
    fjp.setAfter(obj);
    return FMClient.fiHookIox(fjp);
  }


  // *********************************************************
  // 3. target(c), Write, NonException, Boolean
  // *********************************************************
  static final boolean hookWrite03 = true;
  Object around(ClassWC c) 
    : ((if(hookWrite03) && !within(org.fi.*) && target(c)) &&
       (call (* File.mkdir(..))     ||
        call (* File.delete(..))    ||
        call (* File.renameTo(..))
        )
       ) {
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint  , c, null,
                                      JoinPlc.BEFORE , JoinIot.WRITE,
                                      JoinExc.NONE   , JoinRbl.YES);
    Object obj = FMClient.fiHookNox(fjp);
    if (obj != null) return obj;
    obj = proceed(c);
    fjp.setAfter(obj);
    return FMClient.fiHookNox(fjp);
  }


  // *********************************************************
  // 4. USELESS constructor, Iot.NONE, NonException, NonBoolean
  //    FOS(FD) is special, nothing affect the FS.
  //    Important: for constructor, don't use target(c)
  //    because we get cwc from the obj return value
  // *********************************************************
  static final boolean hookWrite04 = true;
  Object around() 
    : ((if(hookWrite04) && !within(org.fi.*)) &&
       (call (FileOutputStream.new(FileDescriptor))
        )
       ) {
    Object obj = proceed();
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint  , (ClassWC)obj, obj,
                                      JoinPlc.BEFORE , JoinIot.NONE,
                                      JoinExc.NONE   , JoinRbl.NO);
    return FMClient.fiHookNox(fjp);
  }

  
  


  // *********************************************************
  // 5. constructor, Write, FNFException, NonBoolean
  //    Important: for constructor, don't use target(c)
  //    because we get cwc from the obj return value
  // *********************************************************
  static final boolean hookWrite05a = true;
  Object around() throws FileNotFoundException
    : ((if(hookWrite05a) && !within(org.fi.*)) &&
       call (FileOutputStream.new(..)            throws FileNotFoundException) 
       ) {
    Object obj = proceed();
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint  , (ClassWC)obj, obj,
                                      JoinPlc.BEFORE , JoinIot.WRITE,
                                      JoinExc.FNF    , JoinRbl.NO);
    return FMClient.fiHookFnfx(fjp);
  }
  
  static final boolean hookWrite05b = true;
  Object around(String p, String m) throws FileNotFoundException
    : ((if(hookWrite05b) && !within(org.fi.*) && args(p, m)) &&
       call (RandomAccessFile.new(String,String) throws FileNotFoundException)
       ) {
    Object obj = proceed(p, m);
    if (!m.contains("w"))
      return obj;
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint  , (ClassWC)obj, obj,
                                      JoinPlc.BEFORE , JoinIot.WRITE,
                                      JoinExc.FNF    , JoinRbl.NO);
    return FMClient.fiHookFnfx(fjp);
  }
  

  static final boolean hookWrite05c = true;
  Object around(File f, String m) throws FileNotFoundException
    : ((if(hookWrite05c) && !within(org.fi.*) && args(f, m)) &&
       call (RandomAccessFile.new(File,String)   throws FileNotFoundException)
       ) {
    Object obj = proceed(f, m);
    if (!m.contains("w"))
      return obj;
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint  , (ClassWC)obj, obj,
                                      JoinPlc.BEFORE , JoinIot.WRITE,
                                      JoinExc.FNF    , JoinRbl.NO);
    return FMClient.fiHookFnfx(fjp);
  }


  // *********************************************************
  // 6. args(c), Write, IOException, NonBoolean
  // *********************************************************
  static final boolean hookWrite06 = true;
  Object around (ClassWC c) throws IOException
    : ((if(hookWrite06) && !within(org.fi.*) && args(c)) &&
       (call (* ByteArrayOutputStream+.writeTo(ClassWC) throws IOException)
        )
       ) {
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint  , c, null,
                                      JoinPlc.BEFORE , JoinIot.WRITE,
                                      JoinExc.IO     , JoinRbl.NO);
    Object obj = FMClient.fiHookIox(fjp);
    if (obj != null) return obj;
    obj = proceed(c);
    fjp.setAfter(obj);
    return FMClient.fiHookIox(fjp);
  }



  // #####################################################################
  // #####################################################################
  // ##                                                                 ##
  // ##                    R E A D       A D V I C E S                  ##
  // ##                                                                 ##
  // #####################################################################
  // #####################################################################


  // *********************************************************
  // target(c), Read, IOException, NonBoolean
  static final boolean hookRead01 = true;
  Object around(ClassWC c) throws IOException
    : ((if(hookRead01) && !within(org.fi.*) && target(c)) &&
       (call (* InputStream+.*(..)       throws IOException) ||
        call (* DataInputStream+.*(..)  throws IOException) ||
        call (* FileInputStream+.*(..)  throws IOException)
        )
       ) {
    FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint  , c, null,
                                      JoinPlc.BEFORE , JoinIot.READ,
				      JoinExc.IO     , JoinRbl.NO);
    Object obj = FMClient.fiHookIox(fjp);
    if (obj != null) return obj;
    obj = proceed(c);
    fjp.setAfter(obj);
    return FMClient.fiHookIox(fjp);
  }



}
