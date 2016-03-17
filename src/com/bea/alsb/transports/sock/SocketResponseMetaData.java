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

import com.bea.wli.sb.transports.DefaultResponseMetaData;
import com.bea.wli.sb.transports.TransportException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlException;

import java.io.IOException;

/**
 * POJO class for ResponseMetaData of the socket transport, which has two
 * attrbutes called endPointHost and endPointIP.
 */
public class SocketResponseMetaData
  extends DefaultResponseMetaData<SocketResponseMetaDataXML> {
  /**
   * Host name of the endpoint of the external system.
   */
  private String endPointHost;
  /**
   * IP address of the endpoint of the external system.
   */
  private String endPointIP;

  public SocketResponseMetaData(SocketResponseMetaDataXML rmdXML)
    throws TransportException {
    super(SocketTransportProvider.getInstance(), rmdXML);
    if(rmdXML != null) {
      if(rmdXML.isSetServiceEndpointHost()) {
        setEndPointHost(rmdXML.getServiceEndpointHost());
      }
      if(rmdXML.isSetServiceEndpointIp()) {
        setEndPointIP(rmdXML.getServiceEndpointIp());
      }
    }
  }

  public SocketResponseMetaData(String encoding) throws TransportException {
    super(SocketTransportProvider.getInstance());
    setCharacterEncoding(encoding);
  }

  public String getEndPointHost() {
    return endPointHost;
  }

  public void setEndPointHost(String endPointHost) {
    this.endPointHost = endPointHost;
  }

  public String getEndPointIP() {
    return endPointIP;
  }

  public void setEndPointIP(String endPointIP) {
    this.endPointIP = endPointIP;
  }

  /**
   * Returns ResponseMetaDataXML, XML bean which is created by setting the
   * values of this POJO instace.
   *
   * @return
   * @throws TransportException
   */
  public SocketResponseMetaDataXML toXML() throws TransportException {
    SocketResponseMetaDataXML responseMetaData =super.toXML();

    if (endPointHost != null) {
      responseMetaData.setServiceEndpointHost(endPointHost);
    }
    if (endPointIP != null) {
      responseMetaData.setServiceEndpointIp(endPointIP);
    }
    return responseMetaData;
  }

  /**
   * Validates and Parses the given XmlObject to SocketResponseMetaDataXML.
   * @param xbean
   * @return SocketResponseMetaDataXML of given XmlObject.
   * @throws TransportException
   */
  public static SocketResponseMetaDataXML getSocketResponseMetaData(
    XmlObject xbean) throws TransportException {
    if (xbean == null) {
      return null;
    } else if (xbean instanceof SocketResponseMetaDataXML) {
      return (SocketResponseMetaDataXML) xbean;
    } else {
      try {
        return (SocketResponseMetaDataXML.Factory.parse(xbean.newInputStream()));
      } catch (XmlException e) {
        throw new TransportException(e.getMessage(), e);
      } catch (IOException e) {
        throw new TransportException(e.getMessage(), e);
      }
    }
  }
}
