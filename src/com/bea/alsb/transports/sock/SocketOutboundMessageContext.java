package com.bea.alsb.transports.sock;

import com.bea.alsb.transports.socket.SocketTransportMessagesLogger;
import com.bea.wli.sb.sources.Source;
import com.bea.wli.sb.sources.StreamSource;
import com.bea.wli.sb.sources.TransformException;
import com.bea.wli.sb.sources.TransformOptions;
import com.bea.wli.sb.transports.TransportSender;
import com.bea.wli.sb.transports.EndPointConfiguration;
import com.bea.wli.sb.transports.TransportOptions;
import com.bea.wli.sb.transports.OutboundTransportMessageContext;
import com.bea.wli.sb.transports.ServiceTransportSender;
import com.bea.wli.sb.transports.TransportException;
import com.bea.wli.sb.transports.NoServiceTransportSender;
import com.bea.wli.sb.transports.ResponseMetaData;
import com.bea.wli.sb.transports.TransportSendListener;
import com.bea.wli.sb.transports.TransportManagerHelper;
import com.bea.wli.sb.transports.TransportManager;

import java.util.Random;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.io.*;

/**
 * This class provides transport level message context abstraction for an
 * outgoing message.
 */
public class SocketOutboundMessageContext
  implements OutboundTransportMessageContext {
  private TransportSender sender;
  private TransportOptions options;
  private String msgId;
  private EndPointConfiguration config;
  private InputStream responseIS;
  private SocketResponseMetaData responseMetadata;
  

  /**
   * Initializes the field variables. sender should be either {@link
   * com.bea.wli.sb.transports.ServiceTransportSender} or {@link
   * com.bea.wli.sb.transports.NoServiceTransportSender}.
   *
   * @param sender
   * @param options
   * @throws com.bea.wli.sb.transports.TransportException if the given sender type is neither
   *         ServiceTransportSender nor NoServiceTransportSender.
   */
  public SocketOutboundMessageContext(TransportSender sender,
                                      TransportOptions options)
    throws TransportException {
    this.msgId = new Random().nextInt() + "." + System.nanoTime();
    this.sender = sender;
    this.options = options;
    if (sender instanceof ServiceTransportSender) {
      this.config =
        ((ServiceTransportSender) sender).getEndPoint().getConfiguration();
    } else if (sender instanceof NoServiceTransportSender) {
      this.config =
        ((NoServiceTransportSender) sender).getEndPointConfiguration();
    } else {
      throw new TransportException(
        SocketTransportMessagesLogger.illegalTransportSender());
    }
  }
	

  /**
   * @return the meta-data for the response part of the message, e.g. headers,
   *         etc. Returns null if there is no response meta-data
   */
  public ResponseMetaData getResponseMetaData() throws TransportException {
    return responseMetadata;
  }

  /**
   * Returns the Source of the response stream.
   *
   * @return
   * @throws TransportException
   */
  public Source getResponsePayload() throws TransportException {
    return responseIS == null ? null : new StreamSource(responseIS);
  }

  /**
     * @return the base uri for to which the message was sent for an outbound
     * message or from which the message was sent on an inbound message
     */
  public URI getURI() {
    return options.getURI();
  }

  /**
   * @return returns transport provider-specific message identifier. Ideally it
   *         should uniquely identify the message among other messages going
   *         through the ALSB runtime, However, ALSB does not depend on the
   *         message Id being unique. The message Id will be added to the
   *         message context and thus visible in the pipeline.
   */
  public String getMessageId() {
    return msgId;
  }

  /**
   * Sends the message to the external service, schedules a Runnable which sets
   * the response metadata and reads the response from the external service.
   *
   * @param listener
   */
  public void send(final TransportSendListener listener)
    throws TransportException {
    String address = options.getURI().toString();
    try {
      String host = null;
      int port = 0;
      try {
        URI uri = new URI(address);
        host = uri.getHost();
        port = uri.getPort();
      } catch (URISyntaxException e) {
        new TransportException(e.getMessage(), e);
      }
		SocketTransportMessagesLogger.ipAddress(host, port);
		final Socket clientSocket = new Socket(host, port);

		SocketEndpointConfiguration socketEndpointConfiguration =
        SocketTransportUtil.getConfig(config);
		SocketOutboundPropertiesType outboundProperties =
        socketEndpointConfiguration.getOutboundProperties();

		String sockettype=outboundProperties.getSockettype();
		String welcomemessage=outboundProperties.getWelcomemessage();
		int welcomemessagecount=outboundProperties.getWelcomemessagecount();
		boolean keepinitbyte=outboundProperties.getKeepinitbyte();

		InputStreamReader inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
        int i = -1;
        StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(inputStreamReader);
		
		if(welcomemessage.equals("By Line")){
		//System.out.println("******** Welcome Message ********");
			String sr="";
			for(int counter=0;counter<welcomemessagecount; counter++)
				sr=br.readLine();
		} else
			inputStreamReader.skip(welcomemessagecount);

		clientSocket.setTcpNoDelay(!outboundProperties.getEnableNagleAlgorithm());
		clientSocket.setSoTimeout(outboundProperties.getTimeout());


        String reqEnc = socketEndpointConfiguration.getRequestEncoding();
        if (reqEnc == null) {
          reqEnc = "utf-8";
        }

      // Send the message to the external service.

		OutputStream outputStream = clientSocket.getOutputStream();
		TransformOptions transformOptions = new TransformOptions();
		transformOptions.setCharacterEncoding(reqEnc);

		if(sockettype.equals("Embedded Length"))
		{
			com.bea.wli.sb.sources.TransformOptions option=new com.bea.wli.sb.sources.TransformOptions();
			option.setCharacterEncoding(reqEnc);
			int bytesize=outboundProperties.getInitbyte();
			

			/*if(!keepinitbyte){
				
				int len=sender.getPayload().getInputStream(option).available();
				outputStream.write(String.format("%0" + bytesize + "d",len).getBytes());	
			
			}
		sender.getPayload().writeTo(outputStream, transformOptions);
		*/

			if(!keepinitbyte){
				int len=sender.getPayload().getInputStream(option).available();
				
				byte [] initbyte=String.format("%0" + bytesize + "d",len).getBytes();
				byte [] message=new byte[bytesize];
				int x= sender.getPayload().getInputStream(option).read(message);
				
				byte [] finalmessage=new byte[len+4];
				System.arraycopy(initbyte, 0, finalmessage, 0, bytesize);
				System.arraycopy(message, 0, finalmessage, bytesize, len);
				
				outputStream.write(finalmessage);
			
			}
			else{
				sender.getPayload().writeTo(outputStream, transformOptions);
			}

		}

		else if (sockettype.equals("New Line")){ 	
		  sender.getPayload().writeTo(outputStream, transformOptions);
		  outputStream.write(SocketTransportUtil.D_CRLF.getBytes(reqEnc));
		 }

		 else if(sockettype.equals("Fixed Length")){
		 sender.getPayload().writeTo(outputStream, transformOptions);
		 }
		 else{
			 String delimiter=outboundProperties.getDelimiter();
			 sender.getPayload().writeTo(outputStream, transformOptions);
		     outputStream.write(delimiter.getBytes());
		 }

		outputStream.flush();
		SocketTransportMessagesLogger.flushed();

		PipelineAcknowledgementTask task = new PipelineAcknowledgementTask(listener, clientSocket, socketEndpointConfiguration);
		TransportManagerHelper.schedule(task, socketEndpointConfiguration.getDispatchPolicy());
   
	} catch (UnknownHostException e) {
      SocketTransportUtil.logger.error(e.getLocalizedMessage());
      throw new TransportException(e.getMessage(), e);
    } catch (IOException e) {
      SocketTransportUtil.logger.error(e.getLocalizedMessage());
      throw new TransportException(e.getMessage(), e);
    } catch (TransformException e) {
      SocketTransportUtil.logger.error(e.getLocalizedMessage());
      throw new TransportException(e.getMessage(), e);
    } catch (TransportException e) {
      SocketTransportUtil.logger.error(e.getLocalizedMessage());
      throw e;
    }
  }

  /**
   * This task does the acknowledgement work of the outbound to the pipeline.
   */
  class PipelineAcknowledgementTask implements Runnable {
    private TransportSendListener listener;
    private Socket clientSocket;
    private SocketEndpointConfiguration epc;

    public PipelineAcknowledgementTask(TransportSendListener listener,
                                       Socket clientSocket,
                                       SocketEndpointConfiguration epc) {
      this.listener = listener;
      this.clientSocket = clientSocket;
      this.epc = epc;
    }

    /**
     * It reads the response sent from the external service, sets the headers
     * and invokes the pipeline.
     */
    public void run() {
      try {


        // if the end-point is one-way, don't read the response.
        if (!epc.getRequestResponse()) {
          SocketTransportMessagesLogger.oneWayEndpoint();
          listener.onReceiveResponse(SocketOutboundMessageContext.this);
          return;
        }
		
		SocketOutboundPropertiesType outboundProperties = epc.getOutboundProperties();
		


        String resEnc = getResponseEncoding();
        responseMetadata = new SocketResponseMetaData(resEnc);
        InetAddress inetAddress = clientSocket.getInetAddress();
        responseMetadata.setEndPointHost(inetAddress.getHostName());
        responseMetadata.setEndPointIP(inetAddress.getHostAddress());
		
		
		
        // Reading the response from the external service.
        InputStream inputStream = clientSocket.getInputStream();
		String sockettype=outboundProperties.getSockettype();	
		
		if(sockettype.equals("Embedded Length")){
			int bytesize=outboundProperties.getInitbyte();
			boolean keepinitbyte=outboundProperties.getKeepinitbyte();
			boolean keepconnection=outboundProperties.getKeepconnection();	

			java.io.BufferedInputStream bis = new java.io.BufferedInputStream(inputStream);
			
			System.out.println("********  Reading message from external service ********");
		
			byte[] lenStr = new byte[bytesize];
			bis.read(lenStr);
			System.out.println("Message Length >>>>  " + new String(lenStr));
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

			System.out.println("Message >>>> "+ new String(input));
			responseIS = new ByteArrayInputStream(msg);
			listener.onReceiveResponse(SocketOutboundMessageContext.this);
	  
		}
		else if(sockettype.equals("New Line")){
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, resEnc);
			BufferedReader br = new BufferedReader(inputStreamReader);
			System.out.println("******** Reading message from external service ********");

			String msg=br.readLine();
	 
			System.out.println("Message >>>> " + msg);
			  responseIS = new ByteArrayInputStream(msg.getBytes(resEnc));
			  listener.onReceiveResponse(SocketOutboundMessageContext.this);
        
		}
		else if(sockettype.equals("Fixed Length")){
		
		java.io.BufferedInputStream bis = new java.io.BufferedInputStream(inputStream);
		int messagelength=outboundProperties.getMessagelength();
		byte [] msg=new byte[messagelength];
	    bis.read(msg);

		System.out.println("Message >>>> " + new String(msg));
		responseIS = new ByteArrayInputStream(msg);
		listener.onReceiveResponse(SocketOutboundMessageContext.this);
		}
		else{
			String delimiter=outboundProperties.getDelimiter();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, resEnc);
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
        if (i != -1) {
          // strip delimiter from the message.
          String msg = sb.substring(0, i);
          responseIS = new ByteArrayInputStream(msg.getBytes(resEnc));
          listener.onReceiveResponse(SocketOutboundMessageContext.this);
        } else {
          // Message format is wrong, it should end with \r\n\r\n
          listener.onError(SocketOutboundMessageContext.this,
            TransportManager.TRANSPORT_ERROR_GENERIC,
            SocketTransportMessagesLogger.invalidMessage());
        }

		
		}

      } catch (IOException e) {
        SocketTransportUtil.logger.error(e.getLocalizedMessage(), e);
        listener.onError(SocketOutboundMessageContext.this,
          TransportManager.TRANSPORT_ERROR_GENERIC, e.getLocalizedMessage());
      } catch (TransportException trex) {
        SocketTransportUtil.logger.error(trex.getLocalizedMessage(), trex);
        listener.onError(SocketOutboundMessageContext.this,
          TransportManager.TRANSPORT_ERROR_GENERIC, trex.getLocalizedMessage());
      } finally {
        try {

			
			if(!epc.getOutboundProperties().getKeepconnection())
				clientSocket.close();
        } catch (IOException e) {
          SocketTransportUtil.logger.error(e.getLocalizedMessage(), e);
        }
      }
    }

    private String getResponseEncoding() {
      String resEnc = epc.getResponseEncoding();
      if (resEnc == null) {
        resEnc = "utf-8";
      }
      return resEnc;
    }

  }
}
