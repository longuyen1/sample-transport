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

import com.bea.wli.sb.transports.EndPointConfiguration;
import com.bea.wli.sb.transports.TransportException;
import org.xml.sax.SAXException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlTokenSource;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlSaxHandler;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.XmlException;
import weblogic.logging.NonCatalogLogger;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorFactory;
import javax.naming.Context;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Hashtable;

/**
 * This class provides utility methods that can be used in Socket Transport.
 */
public class SocketTransportUtil {
  public static final String D_CRLF = "\r\n";
  public static final NonCatalogLogger logger =
    new NonCatalogLogger("Sample.SocketTransport");

  /**
   * Returns the SocketEndpointConfiguration of the passed configuration. If
   * the instance is of same type it will be casted and returned else
   * instance's stream will be parsed and returns SocketEndpointConfiguration.
   *
   * @param configuration
   * @return
   * @throws TransportException
   */
  public static SocketEndpointConfiguration getConfig(
    EndPointConfiguration configuration) throws TransportException {
    XmlObject xbean = configuration.getProviderSpecific();

    if (xbean instanceof SocketEndpointConfiguration) {
      return (SocketEndpointConfiguration) xbean;
    } else {
      try {
        return SocketEndpointConfiguration.Factory.parse(xbean.newInputStream());
      }
      catch (Exception e) {
        throw new TransportException(e.getMessage(),e);
      }
    }
  }

  /**
   * Establish and return a JMX Connector using the internal 'wlx' protocol
   * where wlx is a collocated protocol.
   *
   * @param jndiName the JNDI name of the mbean server to connect to.
   */
  public static JMXConnector getServerSideConnection(String jndiName)
    throws IOException {
    return getConnection(COLOCATED_PROTOCOL, jndiName, null, 0, null, null);
  }

  private static final String COLOCATED_PROTOCOL = "wlx";
  private static final String JNDI_ROOT = "/jndi/";

  private static JMXConnector getConnection(String protocol, String URI,
                                            String hostName, int portNumber,
                                            String userName, String password)
    throws IOException, MalformedURLException {
    JMXServiceURL serviceURL = null;
    if (protocol.equals(COLOCATED_PROTOCOL)) {
      serviceURL = new JMXServiceURL(protocol, null, 0, JNDI_ROOT + URI);
    } else {
      serviceURL =
        new JMXServiceURL(protocol, hostName, portNumber, JNDI_ROOT + URI);
    }

    Hashtable<String, String> h = new Hashtable<String, String>();
    h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES,
      "weblogic.management.remote");
    if (userName != null && password != null) {
      h.put(Context.SECURITY_PRINCIPAL, userName);
      h.put(Context.SECURITY_CREDENTIALS, password);
    }

    JMXConnector connection = JMXConnectorFactory.connect(serviceURL, h);

    return connection;
  }
}
