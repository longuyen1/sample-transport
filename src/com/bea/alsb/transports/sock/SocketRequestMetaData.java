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

import com.bea.wli.sb.transports.DefaultRequestMetaData;
import com.bea.wli.sb.transports.RequestHeaders;
import com.bea.wli.sb.transports.RequestHeadersXML;
import com.bea.wli.sb.transports.TransportException;
import com.bea.wli.sb.transports.TransportProvider;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlException;

import java.io.IOException;

/**
 * POJO class for Request metadata of the socket transport.
 */
public class SocketRequestMetaData
  extends DefaultRequestMetaData<SocketRequestMetaDataXML> {
  private int port = Integer.MIN_VALUE;
  private String hostAddress;

  public SocketRequestMetaData(SocketRequestMetaDataXML rmdXML)
    throws TransportException {
    super(SocketTransportProvider.getInstance(), rmdXML);
    if(rmdXML != null) {
      if(rmdXML.isSetClientHost()) {
        setClientHost(rmdXML.getClientHost());
      }
      if(rmdXML.isSetClientPort()) {
        setClientPort(rmdXML.getClientPort());
      }
    }
  }

  public SocketRequestMetaData(String requestEncoding) throws TransportException {
    /*not calling super.(TransportProvider provider, RequestHeaders hdr,
    String enc) because it does not create new headers if hdr is null.*/
    super(SocketTransportProvider.getInstance());
    setCharacterEncoding(requestEncoding);
  }

  protected RequestHeaders createHeaders(TransportProvider provider,
                                         RequestHeadersXML hdrXML)
    throws TransportException {
    return new SocketRequestHeaders(hdrXML);
  }

  /**
   * Returns RequestMetaDataXML, XML bean which is created by setting the values
   * of this POJO instace.
   *
   * @return
   * @throws TransportException
   */
  public SocketRequestMetaDataXML toXML() throws TransportException {
    SocketRequestMetaDataXML requestMetaData = super.toXML();
    // set socket transport specific metadata.
    if (hostAddress != null) {
      requestMetaData.setClientHost(hostAddress);
    }
    if (port != Integer.MIN_VALUE) {
      requestMetaData.setClientPort(port);
    }
    return requestMetaData;
  }

  public void setClientHost(String hostAddress) {
    this.hostAddress = hostAddress;
  }

  public void setClientPort(int port) {
    this.port = port;
  }

  /**
   * Validates and Parses the given XmlObject to SocketRequestMetaDataXML.
   * @param xbean
   * @return SocketRequestMetaDataXML of the given XmlObject.
   * @throws TransportException
   */
  public static SocketRequestMetaDataXML getSocketRequestMetaData(
    XmlObject xbean) throws TransportException {
    if (xbean == null) {
      return null;
    } else if (xbean instanceof SocketRequestMetaDataXML) {
      return (SocketRequestMetaDataXML) xbean;
    } else {
      try {
        return SocketRequestMetaDataXML.Factory.parse(xbean.newInputStream());
      } catch (XmlException e) {
        throw new TransportException(e.getMessage(), e);
      } catch (IOException e) {
        throw new TransportException(e.getMessage(), e);
      }
    }
  }
}
