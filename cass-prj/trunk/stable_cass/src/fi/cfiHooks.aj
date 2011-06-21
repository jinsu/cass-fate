package org.fi;

import org.fi.*;
import org.fi.FMServer.*;
import org.fi.FMJoinPoint.*;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.Thread;
import java.lang.StackTraceElement;


import org.aspectj.lang.Signature; // include this for Signature, etc!
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.SourceLocation;

import org.apache.cassandra.net.*;
import org.apache.cassandra.db.ReadResponse;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.io.util.DataOutputBuffer;


public aspect cfiHooks {
    boolean debug = false;

    pointcut cutSendOneWay(Message m, InetAddress t) : 
    ( call (* MessagingService.*(Message, InetAddress) ) && 
    args(m, t) ); 
    
    Object around(Message message, InetAddress addr) : cutSendOneWay(message, addr) {
        Context c = new Context();
        byte[] body = message.getMessageBody();
        ByteArrayInputStream bufIn = new ByteArrayInputStream(body);
        Message response = null;
        try {
            ReadResponse result = ReadResponse.serializer().deserialize(new DataInputStream(bufIn));
            if (result.isDigestQuery()) {
                c.setMessageType("Digest");
            }
            
            FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint, null, null,
                    JoinPlc.AFTER , JoinIot.READ,
                    JoinExc.IO     , JoinRbl.NO);
            //returns targetIO
            String IoNode = Util.getNetIOContextFromInetAddr(message.getFrom());
            c.setTargetIO(IoNode);
            if(debug) System.out.println("cutSendOneWay targetIO : " + c.getTargetIO());
            
            FailType ft = FMClient.doCfiHook(fjp, c);
            //Why is FailType none?? it shouldn't be. It should be corruption

            System.out.println("corrupt :: failType returned :: " + ft);
            //add a check for FailType
            //if (c.getMessageType().equalsIgnoreCase("Digest")) {
            if (ft == FailType.CORRUPTION) {
                byte[] corrupted_digest = result.digest();
                if(debug) System.out.println("corrupt :: byte array length " + corrupted_digest.length);
                //Corrupting the digest!
                int cor = ~corrupted_digest[1];
                corrupted_digest[1] = new Integer(cor).byteValue();
                
                //Make a new digest message
                ReadResponse corrupted = new ReadResponse(corrupted_digest);
        
                //Set digest query to be true
                corrupted.setIsDigestQuery(true);
                
                //Serialize the data
                DataOutputBuffer bufOut = new DataOutputBuffer();
                ReadResponse.serializer().serialize(corrupted, bufOut);
                byte[] bytes = new byte[bufOut.getLength()];
                System.arraycopy(bufOut.getData(), 0, bytes, 0, bytes.length);

                //Form the message
                response = message.getReply(message.getFrom(), bytes);
                if(debug) System.out.println("corrupt :: response created");
            }

        } catch(IOException e) {
            System.out.println("BAD Message : IOException thrown");
        }
        /*
                */
        if(response != null) {
            return proceed(response, addr);
        } else {
            return proceed(message, addr);
        }
    }
}

