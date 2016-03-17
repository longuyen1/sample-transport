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

import com.bea.wli.config.Ref;
import com.bea.wli.sb.transports.TransportEndPoint;
import com.bea.wli.sb.transports.TransportException;
import com.bea.wli.sb.transports.EndPointConfiguration;
import com.bea.wli.sb.transports.URIType;
import com.bea.wli.sb.transports.TransportProvider;
import com.bea.wli.sb.transports.RequestMetaData;
import org.apache.xmlbeans.XmlObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class represents End point abstraction for socket transport.
 */
public class SocketTransportEndPoint implements TransportEndPoint {
  private Ref ref;
  private EndPointConfiguration config;
  private SocketTransportProvider socketTransportProvider;
  private volatile SocketTransportReceiver socketTransportReceiver;
  private String dispatchPolicy;
  private String requestEncoding;
  private String responseEncoding;
  private TransportEndPoint.MessagePatternEnum messagePattern;
  private URI[] uri;
  private SocketEndpointConfiguration epc;

  public SocketTransportEndPoint(Ref ref, EndPointConfiguration config,
                                 SocketTransportProvider socketTransportProvider)
    throws TransportException {
    this.ref = ref;
    this.config = config;
    this.socketTransportProvider = socketTransportProvider;
    epc = SocketTransportUtil.getConfig(config);
    dispatchPolicy = epc.getDispatchPolicy();
    requestEncoding = epc.getRequestEncoding();
    responseEncoding = epc.getResponseEncoding();
    if (config.getInbound()) {
      socketTransportReceiver = new SocketTransportReceiver(this);
    }
  }


  /**
   * @return the reference to the service representing this endpoint
   */
  public Ref getServiceRef() {
    return ref;
  }


  /**
   * @return the array of URIs for this endpoint. Inbound endpoints will have a
   *         single URI, whereas outbound endpoints will be associated with
   *         multiple URIs
   */
  public URI[] getURI() {
    if (uri == null) {
      try {
        URIType[] uristr = config.getURIArray();
        uri = new URI[uristr.length];
        for (int i = 0; i < uristr.length; i++) {
          uri[i] = new URI(uristr[i].getValue());
        }
        return uri;
      }
      catch (URISyntaxException e) {
        SocketTransportUtil.logger.error(e.getLocalizedMessage(), e);
        return null;
      }
    }

    return uri;
  }

  /**
   * @return true if this is an inbound endpoint
   */
  public boolean isInbound() {
    return config.getInbound();
  }


  /**
   * @return an XML Bean that describes the configuration of this endpoint.
   *         Configuration properties are specific to the provider type for this
   *         endpoint.
   */
  public EndPointConfiguration getConfiguration() {
    return config;
  }


  /**
   * @return the transport provider for this endpoint
   */
  public TransportProvider getProvider() {
    return socketTransportProvider;
  }


  /**
   * @return empty (new) meta-data for the request part of the outbound message,
   *         e.g. headers, etc.
   */
  public RequestMetaData createRequestMetaData() throws TransportException {
    SocketRequestMetaData requestMetaDataPojo =
      new SocketRequestMetaData(requestEncoding);
    return requestMetaDataPojo;
  }

  /**
   * @return encoding of the request.
   */
  public String getRequestEncoding() {
    return requestEncoding;
  }

  public String getResponseEncoding() {
    return responseEncoding;
  }


  /**
   * @return return meta-data for the request part of the outbound message
   *         initialized according to provider-defined XmlBean representation.
   */
  public RequestMetaData createRequestMetaData(XmlObject rmdXML)
    throws TransportException {
    SocketRequestMetaDataXML socketRequestMetaDataXML =
      SocketRequestMetaData.getSocketRequestMetaData(rmdXML);
    if (socketRequestMetaDataXML != null) {
      return new SocketRequestMetaData(socketRequestMetaDataXML);
    }
    return null;
  }

  /**
   * @return returns false always becuase this transport does not support
   *         transactions.
   * @throws TransportException
   */
  public boolean isTransactional() throws TransportException {
    return false;
  }


  /**
   * @return returns the type of messaging pattern for this endpoint
   * @throws TransportException
   */
  public MessagePatternEnum getMessagePattern() throws TransportException {
    if (messagePattern == null) {
      messagePattern =
        epc.getRequestResponse() ?
          MessagePatternEnum.SYNCHRONOUS :
          MessagePatternEnum.ONE_WAY;
    }

    return messagePattern;
  }

  public SocketEndpointConfiguration getSocketEndpointConfiguration() {
    return epc;
  }

  public void start() {
    if (isInbound()) {

        /** Schedule a receiver to receive requests from clients. */
        /** This is a long running thread it exists for the life time of
         * this endpoint. Generally thread work should be assigned to work-
         * managers of WL Server. */
        Thread thread = new Thread(socketTransportReceiver);
        thread.start();

    }
  }

  public void stop() {
    if (isInbound() && socketTransportReceiver != null) {
      socketTransportReceiver.stopAcceptor();
    }
  }

  public void suspend() {
    stop();
  }

  public void resume() {
    start();
  }

}
