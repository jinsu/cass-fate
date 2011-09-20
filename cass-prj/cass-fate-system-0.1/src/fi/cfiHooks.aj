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
import org.apache.cassandra.db.Row;
import org.apache.cassandra.db.ColumnFamily;
import org.apache.cassandra.db.Column;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.utils.FBUtilities;


public aspect cfiHooks {
    boolean debug = true;

    pointcut cutNaturalEndpoint(Token token, TokenMetadata meta, String table) :
    ( call (* AbstractReplicationStrategy.getNaturalEndpoints(Token, TokenMetadata, String) ) &&
    args(token, meta, table) );

    pointcut cutSendOneWay(Message m, InetAddress t) : 
    ( call (* MessagingService.*(Message, InetAddress) ) && 
    args(m, t) ); 

    Object around(Token t, TokenMetadata meta, String tbl) : cutNaturalEndpoint(t, meta, tbl) {
        return Util.orderEndpoints(t, meta, tbl);
    }

    Object around(Message message, InetAddress addr) : cutSendOneWay(message, addr) {
        Context c = new Context();
        byte[] body = message.getMessageBody();
        ByteArrayInputStream bufIn = new ByteArrayInputStream(body);
        Message response = null;
        ReadResponse corrupted = null;
        try {
            ReadResponse result = ReadResponse.serializer().deserialize(new DataInputStream(bufIn));
            if (result.isDigestQuery()) {
                c.setMessageType(FMClient.READ_RESPONSE_DIGEST);
            } else {
                c.setMessageType(FMClient.READ_RESPONSE_NORMAL);
            }
            
            FMJoinPoint fjp = new FMJoinPoint(thisJoinPoint, null, null,
                    JoinPlc.BEFORE , JoinIot.READ,
                    JoinExc.IO     , JoinRbl.NO);
            //returns targetIO
            String IoNode = Util.getNetIOContextFromInetAddr(addr);
            c.setTargetIO(IoNode);

            if(debug) System.out.println("cutSendOneWay targetIO : " + c.getTargetIO());
            
            FailType ft = FMClient.doCfiHook(fjp, c);
            
            if(debug) System.out.println("corrupt :: failType returned :: " + ft);

            //add a check for FailType
            if (ft == FailType.CORRUPTION) {
                if( c.getMessageType().equals(FMClient.READ_RESPONSE_NORMAL) ) {
                    Row cleanRow = result.row();
                    
                    //NEW CHANGE : trying to corrupt the column values in column Family.
                    ColumnFamily newCf = cleanRow.cf;
                    newCf.addColumn(new Column("corrupted".getBytes(), "crrpt_val".getBytes(), System.currentTimeMillis()));
                    Row crrpt_row = new Row(cleanRow.key, newCf);
                    
                    //need to convert to string value ...crrpt_row.key = crrpt_row.key.hashCode();
                    //crrpt_row.key = "corrupted_key_value";
                    if(debug) System.out.println("corrupt :: old cf = " + cleanRow.cf + "\nnew cf = " + newCf);
                    corrupted = new ReadResponse(crrpt_row);
                    
                } else { 
                    byte[] original_digest = result.digest();
                    byte[] corrupted_digest;
                    if(debug) {
                        System.out.println("corrupt :: og byte array length " + original_digest.length);
                        System.out.println("corrupt :: display og array " + original_digest);
                    }

                    //Corrupting the digest!
                    int og = FBUtilities.byteArrayToInt(original_digest);
                    corrupted_digest = FBUtilities.toByteArray(~og);

                    //corrupted_digest = ByteBuffer.allocate(16).putInt(52).array();

                    if(debug) System.out.println("corrupt :: cor_array " + corrupted_digest);

                    //Make a new digest message
                    corrupted = new ReadResponse(corrupted_digest);

                    //Set digest query to be true
                    corrupted.setIsDigestQuery(true);
                }


                //Serialize the data
                DataOutputBuffer bufOut = new DataOutputBuffer();
                ReadResponse.serializer().serialize(corrupted, bufOut);
                byte[] bytes = new byte[bufOut.getLength()];
                System.arraycopy(bufOut.getData(), 0, bytes, 0, bytes.length);

                //Form the message
                response = message.getReply(message.getFrom(), bytes);
                

            }
            else if(ft == FailType.CRASH) {
                FMClient.callProcessCrash();
            }
        } catch(IOException e) {
            System.out.println("BAD Message : IOException thrown");
        }
        /*
                */
        if(response != null) {
            if(debug) System.out.println("corrupt proceed:: response created");

            return proceed(response, addr);
        } else {
            if(debug) System.out.println("corrupt proceed:: message unchanged");

            return proceed(message, addr);
        }
    }
}

