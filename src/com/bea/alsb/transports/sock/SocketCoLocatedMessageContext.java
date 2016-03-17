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

import org.apache.xmlbeans.XmlObject;
import com.bea.wli.sb.transports.ResponseMetaData;
import com.bea.wli.sb.transports.ServiceTransportSender;
import com.bea.wli.sb.transports.TransportOptions;
import com.bea.wli.sb.transports.TransportException;
import com.bea.wli.sb.transports.CoLocatedMessageContext;

/**
 * This clas provides implementation for co-located out-bound  invocations.
 */
public class SocketCoLocatedMessageContext extends CoLocatedMessageContext {

  /**
   * Constructor.
   *
   * @param sender
   * @param options
   * @throws TransportException
   */
  protected SocketCoLocatedMessageContext(ServiceTransportSender sender,
                                          TransportOptions options)
    throws TransportException {
    super(sender, options);
  }

  /**
   * Returns a new {@link SocketResponseMetaData} instance, which is empty.
   *
   * @return returns a new empty SocketResponseMetaData object.
   */
  public ResponseMetaData createResponseMetaData() throws TransportException {
    String responseEncoding =
      ((SocketTransportEndPoint) getEndPoint()).getResponseEncoding();
    SocketResponseMetaData responseMetaData =
      new SocketResponseMetaData(responseEncoding);
    return responseMetaData;
  }

  /**
   * Returns {@link SocketResponseMetaData} initialized with headers and metadata
   * retreived from the passed XMLObject.
   *
   * @param rmdXML
   * @return
   * @throws TransportException
   */
  public ResponseMetaData createResponseMetaData(XmlObject rmdXML)
    throws TransportException {
    SocketResponseMetaDataXML xmlObject =
      SocketResponseMetaData.getSocketResponseMetaData(rmdXML);
    if (xmlObject != null) {
      return new SocketResponseMetaData(xmlObject);
    }
    return null;
  }

}
