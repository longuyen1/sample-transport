/*
  Copyright (c) 2006 BEA Systems, Inc.
	All rights reserved

	THIS IS UNPUBLISHED PROPRIETARY
	SOURCE CODE OF BEA Systems, Inc.
	The copyright notice above does not
	evidence any actual or intended
	publication of such source code.
*/
package com.bea.alsb.transports.sock;

import com.bea.alsb.transports.socket.SocketTransportMessagesLogger;
import com.bea.wli.sb.transports.TransportEndPoint;
import com.bea.wli.sb.transports.TransportException;
import com.bea.wli.sb.transports.TransportManagerHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.io.*;

import weblogic.security.SubjectUtils;
import weblogic.security.Security;

import javax.security.auth.Subject;

/**
 * This is a receiver thread of the socket transport end point which is
 * responsible for opening a server socket at the configured port and handles
 * start/stop/suspend/resume operations. It creates SocketInboundMessageContext
 * and sends it to the transport SDK.
 */
public class SocketTransportReceiver implements Runnable {
  private SocketTransportEndPoint endPoint;
  private ServerSocket serverSocket;
  private String dispatchPolicy;
  private int timeout;
  private boolean enableNagleAlgorithm;
  private volatile boolean isStopping;
  private int backLog;
  private int bytesize=0;
  private String sockettype="Embedded Length";
  private boolean keepinitbyte=false;
  private boolean keepconnection=false;
  private String welcomemessage="By Line";
  private int welcomemessagecount;
  private int messagelength;
  private String delimiter;

  /**
   * Initializes the ServerSocket with the endpoint configuration.
   *
   * @throws TransportException
   */
  private void init() throws TransportException {
    SocketEndpointConfiguration epc =
      endPoint.getSocketEndpointConfiguration();
    /** Store endpoint configuration variables locally to avoid reading these
     * from xbean again and again.*/
    dispatchPolicy = epc.getDispatchPolicy();
    SocketInboundPropertiesType inboundProperties =
      epc.getInboundProperties();
    timeout = inboundProperties.getTimeout();
    enableNagleAlgorithm = inboundProperties.getEnableNagleAlgorithm();
    backLog = inboundProperties.getBacklog();
	bytesize=inboundProperties.getInitbyte();
	keepinitbyte=inboundProperties.getKeepinitbyte();
	keepconnection=inboundProperties.getKeepconnection();
	sockettype=inboundProperties.getSockettype();
	
	welcomemessage=inboundProperties.getWelcomemessage();
	welcomemessagecount=inboundProperties.getWelcomemessagecount();
	delimiter=inboundProperties.getDelimiter();
	messagelength=inboundProperties.getMessagelength();


  }

  public SocketTransportReceiver(SocketTransportEndPoint endPoint)
    throws TransportException {
    this.endPoint = endPoint;
    init();
  }

  public void run() {
    Socket clientSocket = null;
    URI uri = endPoint.getURI()[0];
    SocketTransportMessagesLogger.uri(uri.toString());
    String address = uri.toString();

    int port =
      Integer.parseInt(address.substring(address.lastIndexOf(':') + 3));
    try {
      serverSocket = new ServerSocket();
      serverSocket.bind(new InetSocketAddress(port), backLog);
    } catch (IOException e) {
      SocketTransportUtil.logger.error(e.getLocalizedMessage(), e);
      return;
    }

    while (true) {
      try {
        SocketTransportMessagesLogger
          .serverSocketAccepting(serverSocket.toString());
        clientSocket = serverSocket.accept();

		
      } catch (IOException e) {
        if (!isStopping) {
          String msg =
            SocketTransportMessagesLogger.serverSocketErrorLoggable()
              .getMessage();
          SocketTransportUtil.logger.error(msg, e);
        } else {
          SocketTransportMessagesLogger.serverSocketStopped(
            serverSocket.toString());
        }
        return;
      }
    
	SocketTransportMessagesLogger.connectionAccepted(clientSocket.toString());

      /** create a worker and schedule it */
      WorkerThread workerThread =
        new WorkerThread(clientSocket, endPoint, timeout, enableNagleAlgorithm, sockettype, bytesize, keepinitbyte, messagelength, delimiter, welcomemessage, welcomemessagecount, keepconnection);

      try {
        TransportManagerHelper.schedule(workerThread, dispatchPolicy);
      } catch (TransportException e) {
        SocketTransportUtil.logger
          .error(SocketTransportMessagesLogger.scheduleFailed(), e);
      }
    }
  }

  public void stopAcceptor() {
    try {
      isStopping = true;
      serverSocket.close();
    } catch (IOException e) {
      SocketTransportUtil.logger.error(e.getLocalizedMessage());
    }
  }

  /**
   * This class represents a single thread of execution of send the data to the
   * transport.
   */
  static class WorkerThread implements Runnable {
    private Socket clientSocket;
    private SocketTransportEndPoint endPoint;
    private int timeout;
    private boolean enableNagleAlgorithm;
	private int bytesize;
	private String sockettype;
	private boolean keepinitbyte;
	private boolean keepconnection;
	private String welcomemessage;
	private int welcomemessagecount;
	private int messagelength;
	private String delimiter;

    public WorkerThread(Socket clientSocket, SocketTransportEndPoint endPoint,
                        int timeout, boolean enableNagleAlgorithm, String sockettype, int bytesize, boolean keepinitbyte, int messagelength, String delimiter, String welcomemessage, int welcomemessagecount, boolean keepconnection) {
      this.clientSocket = clientSocket;
      this.endPoint = endPoint;
      this.timeout = timeout;
      this.enableNagleAlgorithm = enableNagleAlgorithm;
	  this.bytesize=bytesize;
	  this.keepinitbyte=keepinitbyte;
	  this.keepconnection=keepconnection;
	  this.sockettype=sockettype;
	  this.messagelength=messagelength;
	  this.delimiter=delimiter;
	  this.welcomemessage=welcomemessage;
	  this.welcomemessagecount=welcomemessagecount;
    }

    public void run() {
      
		
		try {
		SocketEndpointConfiguration epc =endPoint.getSocketEndpointConfiguration();
		SocketInboundPropertiesType inboundProperties = epc.getInboundProperties();
        /** set the socket properties. */
        
		clientSocket.setSoTimeout(timeout);
        clientSocket.setTcpNoDelay(!enableNagleAlgorithm);
        String msgId = new Random().nextInt() + "." + System.nanoTime();
        InputStream inputStream = clientSocket.getInputStream();

        /** read the incoming message */
        String encoding = endPoint.getRequestEncoding();
        if(encoding == null) {
          encoding = "utf-8";
        }

		InputStreamReader inputStreamReader = new InputStreamReader(inputStream, encoding);
		BufferedInputStream bis=new BufferedInputStream(inputStream);
		
		System.out.println("Transport Receiver >> Run >> Socket Type : "+sockettype);
		System.out.println("Transport Receiver >> Run >> Keep initbyte : "+keepinitbyte);

		do{	
		try{  
		keepconnection=inboundProperties.getKeepconnection();

		if(sockettype.equals("Fixed Length"))
		{
			messagelength=inboundProperties.getMessagelength();
			byte [] msg=new byte [messagelength];
			int i= inputStream.read(msg);
		

		final SocketInboundMessageContext inboundMessageContext = new SocketInboundMessageContext(endPoint, clientSocket, msgId, msg);

			/** send inbound cotext to SDK which sends it to the pipeline,
			 * invoke the pipeline in anonymous subject.*/
		Subject subject = SubjectUtils.getAnonymousUser();
			if (subject != null) {
			  try {
				Security.runAs(subject,
				  new PrivilegedExceptionAction<Void>() {
					public Void run() throws TransportException {
					  TransportManagerHelper.getTransportManager()
						 .receiveMessage(inboundMessageContext, null);
					   return null;
					}
				  });
			  } catch (PrivilegedActionException e) {
				throw (TransportException) e.getException();
			  }
			} else {
			  TransportManagerHelper.getTransportManager()
			  .receiveMessage(inboundMessageContext, null);
			}

		}
		else if(sockettype.equals("New Line")){
			int i = -1;
			StringBuilder sb = new StringBuilder();
			char[] buff = new char[512];
			while (true) {
				i = inputStreamReader.read(buff);
				if (i == -1) {
				 break;
				}
				sb.append(buff, 0, i);
				/** if it ends with double CRLF, come out. We can read the content
			   * after  \r\n\r\n becuase we are expecting only one message per
			   * connection i.e we are closing the connection after processing a
			   * single message.
			   */
				if ((i = sb.indexOf(SocketTransportUtil.D_CRLF)) != -1) {
				break;
				}
			}

        String msg;
        if (i != -1) {
          msg = sb.substring(0, i);
        } else {
          throw new MessageFormatException(
            SocketTransportMessagesLogger.invalidMessage());
        }

        /** if its one way close the connection. */
        if (endPoint.getMessagePattern()
          .equals(TransportEndPoint.MessagePatternEnum.ONE_WAY)) {
          try {
            // closing the input stream only because we didn't open any output
            // stream.
            clientSocket.getInputStream().close();
          } catch (IOException e) {
            SocketTransportUtil.logger.error(e.getLocalizedMessage());
          }
        }
        final SocketInboundMessageContext inboundMessageContext =
          new SocketInboundMessageContext(endPoint, clientSocket, msgId, msg.getBytes());

        /** send inbound cotext to SDK which sends it to the pipeline,
         * invoke the pipeline in anonymous subject.*/
        Subject subject = SubjectUtils.getAnonymousUser();
        if (subject != null) {
          try {
            Security.runAs(subject,
              new PrivilegedExceptionAction<Void>() {
                public Void run() throws TransportException {
                  TransportManagerHelper.getTransportManager()
                     .receiveMessage(inboundMessageContext, null);
                   return null;
                }
              });
          } catch (PrivilegedActionException e) {
            throw (TransportException) e.getException();
          }
        } else {
          TransportManagerHelper.getTransportManager()
          .receiveMessage(inboundMessageContext, null);
        }

	}

		else if(sockettype.equals("Delimited")){
			int i = -1;
			StringBuilder sb = new StringBuilder();
			char[] buff = new char[512];
			while (true) {
				i = inputStreamReader.read(buff);
				if (i == -1) {
				 break;
				}
				sb.append(buff, 0, i);
				/** if it ends with double CRLF, come out. We can read the content
			   * after  \r\n\r\n becuase we are expecting only one message per
			   * connection i.e we are closing the connection after processing a
			   * single message.
			   */
				if ((i = sb.indexOf(delimiter)) != -1) {
				break;
				}
			}

        String msg;
        if (i != -1) {
          msg = sb.substring(0, i);
        } else {
          throw new MessageFormatException(
            SocketTransportMessagesLogger.invalidMessage());
        }

        /** if its one way close the connection. */
        if (endPoint.getMessagePattern()
          .equals(TransportEndPoint.MessagePatternEnum.ONE_WAY)) {
          try {
            // closing the input stream only because we didn't open any output
            // stream.
            clientSocket.getInputStream().close();
          } catch (IOException e) {
            SocketTransportUtil.logger.error(e.getLocalizedMessage());
          }
        }
        final SocketInboundMessageContext inboundMessageContext =
          new SocketInboundMessageContext(endPoint, clientSocket, msgId, msg.getBytes());

        /** send inbound cotext to SDK which sends it to the pipeline,
         * invoke the pipeline in anonymous subject.*/
        Subject subject = SubjectUtils.getAnonymousUser();
        if (subject != null) {
          try {
            Security.runAs(subject,
              new PrivilegedExceptionAction<Void>() {
                public Void run() throws TransportException {
                  TransportManagerHelper.getTransportManager()
                     .receiveMessage(inboundMessageContext, null);
                   return null;
                }
              });
          } catch (PrivilegedActionException e) {
            throw (TransportException) e.getException();
          }
        } else {
          TransportManagerHelper.getTransportManager()
          .receiveMessage(inboundMessageContext, null);
        }

	}
	else{

	/* CWH */

	byte[] lenStr = new byte[bytesize];
	bis.read(lenStr);
	int len = Integer.parseInt(new String(lenStr));
	
	
	byte[] input = new byte[len];
	bis.read(input);

	byte[] msg=null;
	
	if(keepinitbyte){
		  msg=new byte[bytesize+input.length];
		  for (int i=0;i<bytesize;i++ )
			  msg[i]=lenStr[i];
		  for (int i=0;i<input.length;i++ )
			  msg[i+bytesize]=input[i];
	}
	else{ 
		msg=input;
	}
	
	//String temp_message=new String(msg);
	System.out.println("Transport Receiver >> Run >> Transport layer received a message: " + new String(msg) + ". Len: " + (len));
   /** if its one way close the connection. */
     
	 if (endPoint.getMessagePattern().equals(TransportEndPoint.MessagePatternEnum.ONE_WAY)) {
		try {
            // closing the input stream only because we didn't open any output
            // stream.
		clientSocket.getInputStream().close();
		} catch (IOException e) {
         SocketTransportUtil.logger.error(e.getLocalizedMessage());
		}
     }


     final SocketInboundMessageContext inboundMessageContext = new SocketInboundMessageContext(endPoint, clientSocket, msgId, msg);

        /** send inbound cotext to SDK which sends it to the pipeline,
         * invoke the pipeline in anonymous subject.*/
     Subject subject = SubjectUtils.getAnonymousUser();
     if (subject != null) {
		try {
            Security.runAs(subject, new PrivilegedExceptionAction<Void>() {
				public Void run() throws TransportException {
                  TransportManagerHelper.getTransportManager().receiveMessage(inboundMessageContext, null);
                   return null;
                }
              });
          } catch (PrivilegedActionException e) {
            throw (TransportException) e.getException();
          }
        } else {
          TransportManagerHelper.getTransportManager()
          .receiveMessage(inboundMessageContext, null);
        }
	}
	}
    
	catch (TransportException e) {
        SocketTransportUtil.logger.error(getErrorMsg(), e);	
		System.out.println("****************************** Exception in Transport ******************************** "+e.getCause());
		clientSocket.getInputStream().close();
      } catch (IOException e) {
        SocketTransportUtil.logger.error(getErrorMsg(), e);
		System.out.println("****************************** Exception in IO******************************** "+e.getCause());
		clientSocket.getInputStream().close();
	  } catch (NumberFormatException e) {
		System.out.println("****************************** Exception in Number format ***************************** "+e.getCause()); 
		clientSocket.getInputStream().close();
		SocketTransportUtil.logger.error(getErrorMsg(), e);
      } catch (MessageFormatException e) {
        SocketTransportUtil.logger.error(getErrorMsg(), e);
		clientSocket.getInputStream().close();
		System.out.println("****************************** Exception in Mesage Format ******************************** "+e.getCause());
      }
	}while(keepconnection);

   } catch(Exception e){
		System.out.println(e.getMessage());
	  }  
    }

    private String getErrorMsg() {
      return SocketTransportMessagesLogger.socketReceiverFailedLoggable()
        .getMessage();
    }
  }

}
