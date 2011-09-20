
package org.fi;


import org.fi.*;
import org.fi.FMServer.FailType;

import java.io.*;
// import java.util.*;
import java.net.InetSocketAddress;
import java.lang.Thread;
import java.lang.StackTraceElement;


import org.aspectj.lang.Signature; // include this for Signature, etc!
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.SourceLocation;


// ***********************************************************
// "file" stuffs that HDFS imports
// ***********************************************************
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter; // only used in namenode, metasave
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
//import java.nio.channels.OverlappingFileLockException;
//import java.util.zip.ZipFile;
//import java.io.FileNotFoundException;
//import java.io.FilenameFilter;


// ***********************************************************
// "buffer" stuffs that HDFS uses
// ***********************************************************
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.LongBuffer;
//import java.nio.BufferOverflowException;


// ***********************************************************
// "data" stuffs that HDFS uses
// ***********************************************************
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.zip.DataFormatException;


// ***********************************************************
// "byte" stuffs that HDFS uses
// ***********************************************************
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;



// *********************************************************************
// Warning
// *********************************************************************
public aspect warningHooks {
  
  // #####################################################################
  // #####                     P O I N T C U T S                      ####
  // #####################################################################
  
  // for or-ing
  pointcut STE() : call (* StackTraceElement.*(..));
  

  // ****************************************************************  FILE
  pointcut F()   : call (* File.*(..)             throws IOException+);
  pointcut FIS() : call (* FileInputStream.*(..)  throws IOException+);
  pointcut FOS() : call (* FileOutputStream.*(..) throws IOException+);
  pointcut FR()  : call (* FileReader.*(..)       throws IOException+);
  pointcut FW()  : call (* FileWriter.*(..)       throws IOException+);
  pointcut RAF() : call (* RandomAccessFile.*(..) throws IOException+);
  pointcut FC()  : call (* FileChannel.*(..)      throws IOException+);
  pointcut FL()  : call (* FileLock.*(..)         throws IOException+);
  pointcut FD()  : call (* FileDescriptor.*(..)   throws IOException+);

  pointcut b_F()   : call (boolean File.*(..)             );
  pointcut b_FIS() : call (boolean FileInputStream.*(..)  );
  pointcut b_FOS() : call (boolean FileOutputStream.*(..) );
  pointcut b_FR()  : call (boolean FileReader.*(..)       );
  pointcut b_FW()  : call (boolean FileWriter.*(..)       );
  pointcut b_RAF() : call (boolean RandomAccessFile.*(..) );
  pointcut b_FC()  : call (boolean FileChannel.*(..)      );
  pointcut b_FL()  : call (boolean FileLock.*(..)         );
  pointcut b_FD()  : call (boolean FileDescriptor.*(..)   );

  pointcut n_F()   : call (File.new(..)             );
  pointcut n_FIS() : call (FileInputStream.new(..)  );
  pointcut n_FOS() : call (FileOutputStream.new(..) );
  pointcut n_FR()  : call (FileReader.new(..)       );
  pointcut n_FW()  : call (FileWriter.new(..)       );
  pointcut n_RAF() : call (RandomAccessFile.new(..) );
  pointcut n_FC()  : call (FileChannel.new(..)      );
  pointcut n_FL()  : call (FileLock.new(..)         );
  pointcut n_FD()  : call (FileDescriptor.new(..)   );
  
  // ****************************************************************  BUFFER
  pointcut BIS()  : call (* BufferedInputStream.*(..)   throws IOException+);
  pointcut BOS()  : call (* BufferedOutputStream.*(..)  throws IOException+);
  pointcut BR()   : call (* BufferedReader.*(..)        throws IOException+);
  pointcut BW()   : call (* BufferedWriter.*(..)        throws IOException+);
  pointcut B()    : call (* Buffer.*(..)                throws IOException+);  
  pointcut BB()   : call (* ByteBuffer.*(..)            throws IOException+);  
  pointcut CB()   : call (* CharBuffer.*(..)            throws IOException+);  
  pointcut LB()   : call (* LongBuffer.*(..)            throws IOException+);  
  
  pointcut b_BIS()  : call (boolean BufferedInputStream.*(..)   );
  pointcut b_BOS()  : call (boolean BufferedOutputStream.*(..)  );
  pointcut b_BR()   : call (boolean BufferedReader.*(..)        );
  pointcut b_BW()   : call (boolean BufferedWriter.*(..)        );
  pointcut b_B()    : call (boolean Buffer.*(..)                );
  pointcut b_BB()   : call (boolean ByteBuffer.*(..)            );
  pointcut b_CB()   : call (boolean CharBuffer.*(..)            );
  pointcut b_LB()   : call (boolean LongBuffer.*(..)            );

  pointcut n_BIS()  : call (BufferedInputStream.new(..)   );
  pointcut n_BOS()  : call (BufferedOutputStream.new(..)  );
  pointcut n_BR()   : call (BufferedReader.new(..)        );
  pointcut n_BW()   : call (BufferedWriter.new(..)        );
  pointcut n_B()    : call (Buffer.new(..)                );
  pointcut n_BB()   : call (ByteBuffer.new(..)            );
  pointcut n_CB()   : call (CharBuffer.new(..)            );
  pointcut n_LB()   : call (LongBuffer.new(..)            );

  // ****************************************************************  DATA
  pointcut DI( )  : call (* DataInput.*(..)        throws IOException+);
  pointcut DIS()  : call (* DataInputStream.*(..)  throws IOException+);
  pointcut DO( )  : call (* DataOutput.*(..)       throws IOException+);
  pointcut DOS()  : call (* DataOutputStream.*(..) throws IOException+);

  pointcut b_DI()  : call (boolean DataInput.*(..)        );
  pointcut b_DIS() : call (boolean DataInputStream.*(..)  );
  pointcut b_DO()  : call (boolean DataOutput.*(..)       );
  pointcut b_DOS() : call (boolean DataOutputStream.*(..) );

  pointcut n_DI()  : call (DataInput.new(..)        );
  pointcut n_DIS() : call (DataInputStream.new(..)  );
  pointcut n_DO()  : call (DataOutput.new(..)       );
  pointcut n_DOS() : call (DataOutputStream.new(..) );
  
  
  // ****************************************************************  BYTE
  pointcut BAIS() : call (* ByteArrayInputStream.*(..)  throws IOException+);
  pointcut BAOS() : call (* ByteArrayOutputStream.*(..) throws IOException+);
  pointcut RBC()  : call (* ReadableByteChannel.*(..)   throws IOException+);
  pointcut WBC()  : call (* WritableByteChannel.*(..)   throws IOException+);

  pointcut b_BAIS() : call (boolean ByteArrayInputStream.*(..)  );
  pointcut b_BAOS() : call (boolean ByteArrayOutputStream.*(..) );
  pointcut b_RBC()  : call (boolean ReadableByteChannel.*(..)   );
  pointcut b_WBC()  : call (boolean WritableByteChannel.*(..)   );

  pointcut n_BAIS() : call (ByteArrayInputStream.new(..)  );
  pointcut n_BAOS() : call (ByteArrayOutputStream.new(..) );
  pointcut n_RBC()  : call (ReadableByteChannel.new(..)   );
  pointcut n_WBC()  : call (WritableByteChannel.new(..)   );
  




  // #####################################################################
  // #####           A G G R E G A T E   P O I N T C U T S            ####
  // #####################################################################

  
  // ************************************************************** ALL.ALL

  // ********************
  pointcut javaIOAllAll()
    : (!within(org.fi.*) &&
       (// File Exception
	F()     || FIS()   || FOS()   || FR()   || FW()   || 
	RAF()   || FC()    || FL()    || FD()   ||
	// File boolean
	b_F()   || b_FIS() || b_FOS() || b_FR() || b_FW() || 
	b_RAF() || b_FC()  || b_FL()  || b_FD() ||	
	// File new
	n_F()   || n_FIS() || n_FOS() || n_FR() || n_FW() || 
	n_RAF() || n_FC()  || n_FL()  || n_FD() || 
        // 
	// Buffer Exception
	BIS()   || BOS()   || BR()   || BW()   || 
	B()     || BB()    || CB()   || LB()   ||
	// Buffer boolean
	b_BIS() || b_BOS() || b_BR() || b_BW() || 
	b_B()   || b_BB()  || b_CB() || b_LB() ||
	// Buffer new
	n_BIS() || n_BOS() || n_BR() || n_BW() || 
	n_B()   || n_BB()  || n_CB() || n_LB() ||
	//
	// Data Exception
	DI()   || DIS()   || DO()   || DOS()   ||
	// Data boolean
	b_DI() || b_DIS() || b_DO() || b_DOS() ||
	// Data new
	n_DI() || n_DIS() || n_DO() || n_DOS() ||
	//
	// Byte Exception
	BAIS()   || BAOS()   || RBC()   || WBC()   ||
	// Byte boolean
	b_BAIS() || b_BAOS() || b_RBC() || b_WBC() ||
	// Byte new
	n_BAIS() || n_BAOS() || n_RBC() || n_WBC() ||	
	//
	//
	STE())
       );


  // ************************************************************** ALL.*

  // ****************************
  pointcut javaIOAllException()
    : (!within(org.fi.*) &&
       (// File Exception
	F()     || FIS()   || FOS()   || FR()   || FW()   || 
	RAF()   || FC()    || FL()    || FD()   ||
	// Buffer Exception
	BIS()   || BOS()   || BR()   || BW()   || 
	B()     || BB()    || CB()   || LB()   ||
	// Data Exception
	DI()   || DIS()   || DO()   || DOS()   ||
	// Byte Exception
	BAIS()   || BAOS()   || RBC()   || WBC()   ||
	//
	STE())
       );


  // ************************
  pointcut javaIOAllBoolean()
    : (!within(org.fi.*) &&
       (// File boolean
	b_F()   || b_FIS() || b_FOS() || b_FR() || b_FW() || 
	b_RAF() || b_FC()  || b_FL()  || b_FD() ||	
	// Buffer boolean
	b_BIS() || b_BOS() || b_BR() || b_BW() || 
	b_B()   || b_BB()  || b_CB() || b_LB() ||
	// Data boolean
	b_DI() || b_DIS() || b_DO() || b_DOS() ||
	// Byte boolean
	b_BAIS() || b_BAOS() || b_RBC() || b_WBC() ||
	//
	STE())
       );

  // ********************
  pointcut javaIOAllNew()
    : (!within(org.fi.*) &&
       (// File new
	n_F()   || n_FIS() || n_FOS() || n_FR() || n_FW() || 
	n_RAF() || n_FC()  || n_FL()  || n_FD() || 
	// Buffer new
	n_BIS() || n_BOS() || n_BR() || n_BW() || 
	n_B()   || n_BB()  || n_CB() || n_LB() ||
	// Data new
	n_DI() || n_DIS() || n_DO() || n_DOS() ||
	// Byte new
	n_BAIS() || n_BAOS() || n_RBC() || n_WBC() ||	
	//
	STE())
       );

  
  // ************************************************************** *.ALL

  // *********************
  pointcut javaIOFileAll()
    : (!within(org.fi.*) &&
       (// File Exception
	F()     || FIS()   || FOS()   || FR()   || FW()   || 
	RAF()   || FC()    || FL()    || FD()   ||
	// File boolean
	b_F()   || b_FIS() || b_FOS() || b_FR() || b_FW() || 
	b_RAF() || b_FC()  || b_FL()  || b_FD() ||	
	// File new
	n_F()   || n_FIS() || n_FOS() || n_FR() || n_FW() || 
	n_RAF() || n_FC()  || n_FL()  || n_FD() || 
	//
	STE())
       );


  // **********************
  pointcut javaIOBufferAll()
    : (!within(org.fi.*) &&
       (// Buffer Exception
	BIS()   || BOS()   || BR()   || BW()   || 
	B()     || BB()    || CB()   || LB()   ||
	// Buffer boolean
	b_BIS() || b_BOS() || b_BR() || b_BW() || 
	b_B()   || b_BB()  || b_CB() || b_LB() ||
	// Buffer new
	n_BIS() || n_BOS() || n_BR() || n_BW() || 
	n_B()   || n_BB()  || n_CB() || n_LB() ||
	//
	STE())
       );


  // *********************
  pointcut javaIODataAll()
    : (!within(org.fi.*) &&
       (// Data Exception
	DI()   || DIS()   || DO()   || DOS()   ||
	// Data boolean
	b_DI() || b_DIS() || b_DO() || b_DOS() ||
	// Data new
	n_DI() || n_DIS() || n_DO() || n_DOS() ||
	//
	STE())
       );


  // ******************
  pointcut javaIOByteAll()
    : (!within(org.fi.*) &&
       (// Byte Exception
	BAIS()   || BAOS()   || RBC()   || WBC()   ||
	// Byte boolean
	b_BAIS() || b_BAOS() || b_RBC() || b_WBC() ||
	// Byte new
	n_BAIS() || n_BAOS() || n_RBC() || n_WBC() ||	
	//
	STE())
       );


  // ************************************************************** File.*


  // ********************
  pointcut javaIOFileException()
    : (!within(org.fi.*) &&
       (
	// File Exception
	F()     || FIS()   || FOS()   || FR()   || FW()   || 
	RAF()   || FC()    || FL()    || FD()   ||
	//
	STE())
       );

  // ********************
  pointcut javaIOFileBoolean()
    : (!within(org.fi.*) &&
       (
	// File boolean
	b_F()   || b_FIS() || b_FOS() || b_FR() || b_FW() || 
	b_RAF() || b_FC()  || b_FL()  || b_FD() ||	
	//
	STE())
       );

  // ********************
  pointcut javaIOFileNew()
    : (!within(org.fi.*) &&
       (
	// File new
	n_F()   || n_FIS() || n_FOS() || n_FR() || n_FW() || 
	n_RAF() || n_FC()  || n_FL()  || n_FD() || 
	//
	STE())
       );

  // ************************************************************** Buffer.*

  // ********************
  pointcut javaIOBufferException()
    : (!within(org.fi.*) &&
       (
	// Buffer Exception
	BIS()   || BOS()   || BR()   || BW()   || 
	B()     || BB()    || CB()   || LB()   ||
	//
	STE())
       );

  // ********************
  pointcut javaIOBufferBoolean()
    : (!within(org.fi.*) &&
       (
	// Buffer boolean
	b_BIS() || b_BOS() || b_BR() || b_BW() || 
	b_B()   || b_BB()  || b_CB() || b_LB() ||
	//
	STE())
       );

  // ********************
  pointcut javaIOBufferNew()
    : (!within(org.fi.*) &&
       (
	// Buffer new
	n_BIS() || n_BOS() || n_BR() || n_BW() || 
	n_B()   || n_BB()  || n_CB() || n_LB() ||
	//
	STE())
       );

  
  // ************************************************************** Data.*
  
  // ********************
  pointcut javaIODataException()
    : (!within(org.fi.*) &&
       (
	// Data Exception
	DI()   || DIS()   || DO()   || DOS()   ||
	//
	STE())
       );

  // ********************
  pointcut javaIODataBoolean()
    : (!within(org.fi.*) &&
       (
	// Data boolean
	b_DI() || b_DIS() || b_DO() || b_DOS() ||
	//
	STE())
       );

  // ********************
  pointcut javaIODataNew()
    : (!within(org.fi.*) &&
       (
	// Data new
	n_DI() || n_DIS() || n_DO() || n_DOS() ||
	//
	STE())
       );
  
  // ************************************************************** Byte.*
  
  // ********************
  pointcut javaIOByteException()
    : (!within(org.fi.*) &&
       (
	// Byte Exception
	BAIS()   || BAOS()   || RBC()   || WBC()   ||
	//
	STE())
       );  

  // ********************
  pointcut javaIOByteBoolean()
    : (!within(org.fi.*) &&
       (
	// Byte boolean
	b_BAIS() || b_BAOS() || b_RBC() || b_WBC() ||
	//
	STE())
       );  

  // ********************
  pointcut javaIOByteNew()
    : (!within(org.fi.*) &&
       (
	// Byte new
	n_BAIS() || n_BAOS() || n_RBC() || n_WBC() ||	
	//
	STE())
       );  


  // #####################################################################
  // #####             W R I T E   P O I N T C U T S                  ####
  // #####################################################################


  // ****************************************************************  BYTE
  pointcut javaIOWriteCalls() 
    : (!within(org.fi.*) &&
       (STE()
	|| call (* DataOutputStream+.*(..))
	|| call (* FileOutputStream+.*(..))
	|| call (* File.createNewFile(..))
	|| call (* File.mkdir(..))
	|| call (* File.delete(..))
	|| call (* File.renameTo(..))
	|| call (* FileChannel.force(..))
	|| call (* FileChannel.truncate(..))
	|| call (* RandomAccessFile.setLength(..))
	|| call (FileOutputStream.new(..))
	|| call (RandomAccessFile.new(..))
	)
       );



  
  // #####################################################################
  // #####                    W  A  R  N  I  N  G                     ####
  // #####################################################################
  
  // WRITE
  // declare warning : javaIOWriteCalls() : "warningHooks:javaIO-write-calls";
  
  // ALL.ALL = 695
  // declare warning : javaIOAllAll() : "warningHooks:javaIO-all-all";
  
  // ALL.* [ ex = 439, bool = 129, new = 135, total = 703 ]
  // declare warning : javaIOAllException() : "warningHooks:javaIO-all-exception";
  // declare warning : javaIOAllBoolean()   : "warningHooks:javaIO-all-boolean";
  // declare warning : javaIOAllNew()       : "warningHooks:javaIO-all-new";
  
  // *.ALL [ file = 320, buffer = 29, data = 351, byte = 1, total = 701 ]
  // declare warning : javaIOFileAll()   : "warningHooks:javaIO-File-All";
  // declare warning : javaIOBufferAll() : "warningHooks:javaIO-Buffer-All";
  // declare warning : javaIODataAll()   : "warningHooks:javaIO-Data-All";
  // declare warning : javaIOByteAll()   : "warningHooks:javaIO-Byte-All";
  
  
  // File.* [ exception = 117, boolean = 124, new = 82 ]
  // declare warning : javaIOFileException() : "warningHooks:javaIO-File-Exception";
  // declare warning : javaIOFileBoolean()   : "warningHooks:javaIO-File-Boolean";
  // declare warning : javaIOFileNew()   : "warningHooks:javaIO-File-New";
  
  // Buffer.* [ exception = 6, boolean = 0, new = 23 ]
  // declare warning : javaIOBufferException() : "warningHooks:javaIO-Buffer-Exception";
  // declare warning : javaIOBufferBoolean()   : "warningHooks:javaIO-Buffer-Boolean";
  // declare warning : javaIOBufferNew()   : "warningHooks:javaIO-Buffer-New";
  
  // Data.* [ exception = 321, boolean = 5, new = 30 ]
  // declare warning : javaIODataException() : "warningHooks:javaIO-Data-Exception";
  // declare warning : javaIODataBoolean()   : "warningHooks:javaIO-Data-Boolean";
  // declare warning : javaIODataNew()   : "warningHooks:javaIO-Data-New";

  // Byte.* [ exception = 1, boolean = 0, new = 0 ]
  // declare warning : javaIOByteException() : "warningHooks:javaIO-Byte-Exception";
  // declare warning : javaIOByteBoolean()   : "warningHooks:javaIO-Byte-Boolean";
  // declare warning : javaIOByteNew()   : "warningHooks:javaIO-Byte-New";
  

  // TODO:
  // HOW ABOUT CORRUPTION???? 


}

