package org.fi;

import org.fi.*;
import org.fi.FMServer.*;
import org.fi.FMJoinPoint.*;

import java.io.*;
import java.nio.ByteBuffer;
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
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.TokenMetadata;


public aspect cfiHooks {
    boolean debug = true;

    pointcut cutNaturalEndpoint(Token token, TokenMetadata meta, String table) :
    ( call (* AbstractReplicationStrategy.getNaturalEndpoints(Token, TokenMetadata, String) ) &&
    args(token, meta, table) );

    pointcut cutSendOneWay(Message m, InetAddress t) : 
    ( call (* MessagingService.*(Message, InetAddress) ) && 
    args(m, t) ); 
    
    Object around(Token t, TokenMetadata meta, String tbl) : cutNaturalEndpoint(t, meta, tbl) {
        System.out.println("kitty pow");
        return Util.orderEndpoints(t, meta, tbl);
    }

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
                if(debug) {
                System.out.println("corrupt :: byte array length " + corrupted_digest.length);
                System.out.println("corrupt :: array " + corrupted_digest);
                }
                
                //Corrupting the digest!
                /*
                int cor = corrupted_digest[1] << 2;
                corrupted_digest[1] = new Integer(cor).byteValue();
                int cor2 = ~corrupted_digest[2];
                corrupted_digest[2] = new Integer(cor2).byteValue();
                int cor3 = ~corrupted_digest[3];
                corrupted_digest[3] = new Integer(cor3).byteValue();
                */
                corrupted_digest = ByteBuffer.allocate(16).putInt(52).array();

                if(debug) System.out.println("corrupt :: cor_array " + corrupted_digest);

                //Type mismatch: cannot convert from int to byte
                //corrupted_digest[1] =~ corrupted_digest[1];
                
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

