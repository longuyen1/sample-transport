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

import com.bea.alsb.presentation.CustomHelpProvider;
import com.bea.alsb.transports.socket.SocketTransportMessagesLogger;

import com.bea.wli.sb.services.BindingTypeInfo;
import com.bea.wli.sb.transports.EndPointConfiguration;
import com.bea.wli.sb.transports.TransportException;
import com.bea.wli.sb.transports.TransportManagerHelper;
import com.bea.wli.sb.transports.TransportValidationContext;
import com.bea.wli.sb.transports.ui.TransportEditField;
import com.bea.wli.sb.transports.ui.TransportUIBinding;
import com.bea.wli.sb.transports.ui.TransportUIContext;
import com.bea.wli.sb.transports.ui.TransportUIError;
import com.bea.wli.sb.transports.ui.TransportUIFactory;
import static com.bea.wli.sb.transports.ui.TransportUIFactory.getStringValues;
import com.bea.wli.sb.transports.ui.TransportUIGenericInfo;
import com.bea.wli.sb.transports.ui.TransportViewField;
import org.apache.xmlbeans.XmlObject;
import javax.management.remote.JMXConnector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;

import weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean;

/**
 * This class represents the binding between UI and the metdata of the
 * transport. It provides UI validation and rendering of the transport provider
 * specific objects.
 */
public class SocketTransportUIBinding
    implements TransportUIBinding, CustomHelpProvider {
  private TransportUIContext uiContext;
  private static final String ENABLE_NAGLE_ALGORITHM =
    "Enable Nagle Algorithm";
  private static final String BACKLOG = "Backlog";
  private static final String TIME_OUT = "Timeout (in milli seconds)";
  private static final String REQUEST_RESPONSE = "Is response required";
  private static final String REQUEST_ENCODING = "Request encoding";
  private static final String RESPONSE_ENCODING = "Response encoding";
  private static final String DISPATCH_POLICY = "Dispatch policy";
  /*CWH: Added for Eximbank */
  private static final String INIT_BYTE = "Initial Byte Size";
  private static final String KEEP_INIT_BYTE = "Keep Initial Byte";
  private static final String KEEP_CONNECTION = "Keep Socket Connection";
  private static final String SOCKET_TYPE="Socket Type";
	
  private static final String MESSAGE_LENGTH="Message Length";
  private static final String DELIMITER="Delimiter";
  private static final String WELCOME_MESSAGE="Welcome Message";
  private static final String WELCOME_MESSAGE_COUNT="Welcome Message Count";
  //---------------------------
  private Locale locale;

  public SocketTransportUIBinding(TransportUIContext uiContext) {
    this.uiContext = uiContext;
    locale = uiContext.getLocale();
  }

  /**
   * Returns true if the message type is either TEXT or XML. Socket transport
   * supports XML and TEXT message types only for both the request and the
   * response messages.
   *
   * @param bindingType
   * @return
   */
  public boolean isServiceTypeSupported(BindingTypeInfo bindingType) {
    try {
      BindingTypeInfo.BindingTypeEnum type = bindingType.getType();

      /**
       * If the binding is mixed, request type should exist and it should be
       * either TEXT or XML type and  if there is any response type,
       * it must be either TEXT or XML.
       */
      if (type.equals(BindingTypeInfo.BindingTypeEnum.MIXED)) {
        BindingTypeInfo.MessageTypeEnum responseMessageType =
          bindingType.getResponseMessageType();
        if (responseMessageType != null) {
          if (!(
            BindingTypeInfo.MessageTypeEnum.TEXT.equals(responseMessageType) ||
              BindingTypeInfo.MessageTypeEnum.XML
                .equals(responseMessageType)
		/* CWH: Added for Eximbank */	  
		  || BindingTypeInfo.MessageTypeEnum.BINARY.equals(responseMessageType)
		  )) {
            return false;
          }
        }
        BindingTypeInfo.MessageTypeEnum requestMessageType =
          bindingType.getRequestMessageType();
        if (requestMessageType != null) {
          return
            BindingTypeInfo.MessageTypeEnum.TEXT.equals(requestMessageType) ||
              BindingTypeInfo.MessageTypeEnum.XML.equals(requestMessageType)
		/*CWH: Added for Eximbank */
		  || BindingTypeInfo.MessageTypeEnum.BINARY.equals(responseMessageType)
		  ;
        } else {
          return false;
        }
      }
      /**
       * Binding type must be either ABSTRACT_XML or XML.
       */
      return type.equals(BindingTypeInfo.BindingTypeEnum.ABSTRACT_XML)
        || type.equals(BindingTypeInfo.BindingTypeEnum.XML);
    } catch (TransportException e) {
      SocketTransportUtil.logger.error(e.getLocalizedMessage(), e);
      return false;
    }
  }


  /**
   * Called at service definition time to provide information that is provider
   * specific for the main transport page. This includes information like URI
   * hints and autofill field data.
   *
   * @return
   */
  public TransportUIGenericInfo getGenericInfo() {
    TransportUIGenericInfo genInfo = new TransportUIGenericInfo();
    if (uiContext.isProxy()) {
      genInfo.setUriFormat(
        TextMessages.getMessage(TextMessages.INBOUND_URI_FORMAT, locale));
      genInfo.setUriAutofill(TextMessages.getMessage(
        TextMessages.INBOUND_URI_FORMAT_AUTOFILL, locale));
    } else {
      genInfo.setUriFormat(
        TextMessages.getMessage(TextMessages.OUTBOUND_URI_FORMAT, locale));
      genInfo.setUriAutofill(TextMessages.getMessage(
        TextMessages.OUTBOUND_URI_FORMAT_AUTOFILL, locale));
    }
    return genInfo;
  }


  /**
   * Called at service definition time to get provider-specific contents of the
   * edit pane for endpoint configuration. This method is called when the
   * transport configuration page is first rendered.
   */
  public TransportEditField[] getEditPage(EndPointConfiguration config,
                                          BindingTypeInfo binding)
    throws TransportException {
    List<TransportEditField> fields = new ArrayList<TransportEditField>();
    SocketEndpointConfiguration sockConfig = null;
    if (config != null && config.isSetProviderSpecific()) {
      sockConfig = SocketTransportUtil.getConfig(config);
    }
    String requestEncoding = null;
    String responseEncoding = null;
    if (sockConfig != null) {
      requestEncoding = sockConfig.getRequestEncoding();
      responseEncoding = sockConfig.getResponseEncoding();
    }
    boolean requestResponse =
      sockConfig == null || sockConfig.getRequestResponse();
    TransportUIFactory.CheckBoxObject checkbox =
      TransportUIFactory.createCheckbox(null, requestResponse, true);
    TransportEditField editField =
      TransportUIFactory.createEditField(REQUEST_RESPONSE,
        TextMessages.getMessage(TextMessages.REQUEST_RESPONSE, locale),
        TextMessages.getMessage(TextMessages.REQUEST_RESPONSE_INFO, locale),
        false, checkbox);
    fields.add(editField);
    long timeout = 5000;
    boolean enableNA = true;
	int initbyte=4;
	boolean keepconnection=false;
	boolean keepinitbyte=false;

	String sockettype="Embedded Length";
	int messagelength=0;
	String delimiter="";
	String welcomemessage="By Line";
	int welcomemessagecount=0;


    if (uiContext.isProxy()) {
      int backlog = 5;
      if (sockConfig != null) {
        SocketInboundPropertiesType inboundProperties =
          sockConfig.getInboundProperties();
        backlog = inboundProperties.getBacklog();
        timeout = inboundProperties.getTimeout();
        enableNA = inboundProperties.getEnableNagleAlgorithm();
		//CWH: Added for Eximbank
		sockettype=inboundProperties.getSockettype();
		initbyte=inboundProperties.getInitbyte();
		keepinitbyte=inboundProperties.getKeepinitbyte();
		keepconnection=inboundProperties.getKeepconnection();
		
		messagelength=inboundProperties.getMessagelength();
		delimiter=inboundProperties.getDelimiter();
		welcomemessage=inboundProperties.getWelcomemessage();
		welcomemessagecount=inboundProperties.getWelcomemessagecount();

		//-----------------
      }
      
	  
	  TransportUIFactory.TextBoxObject textBox =
        TransportUIFactory.createTextBox(backlog + "", 20);
      editField =
        TransportUIFactory
          .createEditField(BACKLOG,
            TextMessages.getMessage(TextMessages.BACKLOG, locale),
            TextMessages.getMessage(TextMessages.BACKLOG_INFO, locale), false,
            textBox);
      //fields.add(editField);



    } else {
      if (sockConfig != null) {

        SocketOutboundPropertiesType outboundProperties =
          sockConfig.getOutboundProperties();
        timeout = outboundProperties.getTimeout();
        enableNA = outboundProperties.getEnableNagleAlgorithm();
		sockettype=outboundProperties.getSockettype();
		initbyte=outboundProperties.getInitbyte();
		keepinitbyte=outboundProperties.getKeepinitbyte();
		keepconnection=outboundProperties.getKeepconnection();
		messagelength=outboundProperties.getMessagelength();
		delimiter=outboundProperties.getDelimiter();
		welcomemessage=outboundProperties.getWelcomemessage();
		welcomemessagecount=outboundProperties.getWelcomemessagecount();

      }
    }

    TransportUIFactory.TextBoxObject textBox =
      TransportUIFactory.createTextBox(timeout + "", 20);
    editField = TransportUIFactory
      .createEditField(TIME_OUT,
        TextMessages.getMessage(TextMessages.TIME_OUT, locale),
        TextMessages.getMessage(TextMessages.TIME_OUT_INFO, locale), false,
        textBox);
    fields.add(editField);
    
/* CWH: Added for Eximbank */

	/*String [] display={"Normal", "ATM", "T24"};
	String Socket_Type="Normal";


	TransportUIFactory.SelectObject select = TransportUIFactory.createSelectObject(display, display, Socket_Type,0, true);
	editField=TransportUIFactory.createEditField("Socket Type", "Socket Type","Info", false, select);
	fields.add(editField);
*/
	
	String [] display={"Embedded Length", "Fixed Length", "New Line", "Delimited"};
	TransportUIFactory.SelectObject select = TransportUIFactory.createSelectObject(display, display, sockettype,1, true);
	editField=TransportUIFactory.createEditField("Socket Type", "Socket Type","Socket Type", false, select);
	fields.add(editField);

/*
	checkbox = TransportUIFactory.createCheckbox(useinitbyte);
    editField =
      TransportUIFactory.createEditField(USE_INIT_BYTE,
        TextMessages.getMessage(TextMessages.USE_INIT_BYTE, locale),
        TextMessages.getMessage(TextMessages.USE_INIT_BYTE_INFO,
          locale)
        , false, checkbox);
    fields.add(editField);
*/

    TransportUIFactory.TextBoxObject textBox2 =
      TransportUIFactory.createTextBox(initbyte + "", 20);
    editField = TransportUIFactory
      .createEditField(INIT_BYTE,
        TextMessages.getMessage(TextMessages.INIT_BYTE, locale),
        TextMessages.getMessage(TextMessages.INIT_BYTE_INFO, locale), false,
        textBox2);
    fields.add(editField);


  checkbox = TransportUIFactory.createCheckbox(keepinitbyte);
    editField =
      TransportUIFactory.createEditField(KEEP_INIT_BYTE,
        TextMessages.getMessage(TextMessages.KEEP_INIT_BYTE, locale),
        TextMessages.getMessage(TextMessages.KEEP_INIT_BYTE_INFO,
          locale)
        , false, checkbox);
    fields.add(editField);
	
	
	 textBox2 =
      TransportUIFactory.createTextBox(messagelength + "", 20);
    editField = TransportUIFactory
      .createEditField(MESSAGE_LENGTH,
        TextMessages.getMessage(TextMessages.MESSAGE_LENGTH, locale),
        TextMessages.getMessage(TextMessages.MESSAGE_LENGTH, locale), false,
        textBox2);
    fields.add(editField);


	  textBox2 =
      TransportUIFactory.createTextBox(delimiter + "", 20);
    editField = TransportUIFactory
      .createEditField(DELIMITER,
        TextMessages.getMessage(TextMessages.DELIMITER, locale),
        TextMessages.getMessage(TextMessages.DELIMITER, locale), false,
        textBox2);
    fields.add(editField);
if (!uiContext.isProxy()){
	String [] display_welcome={"By Line", "By Character"};
	TransportUIFactory.SelectObject select_welcome = TransportUIFactory.createSelectObject(display_welcome, display_welcome, welcomemessage,1, true);
	editField=TransportUIFactory.createEditField("Welcome Message", "Welcome Message","Welcome Message", false, select_welcome);
	fields.add(editField);
	

		  textBox2 =
      TransportUIFactory.createTextBox(welcomemessagecount + "", 20);
    editField = TransportUIFactory
      .createEditField(WELCOME_MESSAGE_COUNT,
        TextMessages.getMessage(TextMessages.WELCOME_MESSAGE_COUNT, locale),
        TextMessages.getMessage(TextMessages.WELCOME_MESSAGE_COUNT, locale), false,
        textBox2);
    fields.add(editField);
}
	  checkbox = TransportUIFactory.createCheckbox(keepconnection);
    editField =
      TransportUIFactory.createEditField(KEEP_CONNECTION,
        TextMessages.getMessage(TextMessages.KEEP_CONNECTION, locale),
        TextMessages.getMessage(TextMessages.KEEP_CONNECTION_INFO,
          locale)
        , false, checkbox);
    fields.add(editField);
/*-------------*/
    checkbox = TransportUIFactory.createCheckbox(enableNA);
    editField =
      TransportUIFactory.createEditField(ENABLE_NAGLE_ALGORITHM,
        TextMessages.getMessage(TextMessages.ENABLE_NAGLE_ALGORITHM, locale),
        TextMessages.getMessage(TextMessages.ENABLE_NAGLE_ALGORITHM_INFO,
          locale)
        , false, checkbox);
    fields.add(editField);

    TransportUIFactory.TransportUIObject uiObject =
      TransportUIFactory.createTextBox(requestEncoding, 10);
    TransportEditField field =
      TransportUIFactory.createEditField(
        REQUEST_ENCODING,
        TextMessages.getMessage(TextMessages.REQUEST_ENCODING, locale),
        TextMessages.getMessage(TextMessages.REQUEST_ENCODING_INFO, locale),
        uiObject);
    fields.add(field);

    uiObject = TransportUIFactory.createTextBox(responseEncoding, 10);
    field = TransportUIFactory.createEditField(RESPONSE_ENCODING,
      TextMessages.getMessage(TextMessages.RESPONSE_ENCODING, locale),
      TextMessages.getMessage(TextMessages.RESPONSE_ENCODING_INFO, locale),
      uiObject);
    fields.add(field);

    String curDispatchPolicy = DEFAULT_WORK_MANAGER;
    if (sockConfig != null && sockConfig.getDispatchPolicy() != null) {
      curDispatchPolicy = sockConfig.getDispatchPolicy();
    }
    if (curDispatchPolicy == null) {
      curDispatchPolicy = DEFAULT_WORK_MANAGER;
    }
    field = getDispatchPolicyEditField(curDispatchPolicy);
    fields.add(field);

    return updateEditPage(fields.toArray(new TransportEditField[fields.size()]),"Socket Type");
  }


  /**
   * Called at service definition time to get contents of the edit pane for
   * endpoint configuration. This method is called each time the event for the
   * field of the given name is triggered. The set of field can be updated
   * accordingly.
   */
  public TransportEditField[] updateEditPage(TransportEditField[] fields,
                                             String name)
    throws TransportException {
	
	if(name.equals("Socket Type"))
	  {
		if(((TransportUIFactory.SelectObject)fields[2].getObject()).getSelectedValue().equals("Embedded Length")){
			
			fields[3].setDisabled(false);
			fields[4].setDisabled(false);
			fields[5].setDisabled(true);
			fields[6].setDisabled(true);
		}
		else if(((TransportUIFactory.SelectObject)fields[2].getObject()).getSelectedValue().equals("Fixed Length")){
			fields[3].setDisabled(true);
			fields[4].setDisabled(true);
			fields[5].setDisabled(false);
			fields[6].setDisabled(true);
		}
		else if(((TransportUIFactory.SelectObject)fields[2].getObject()).getSelectedValue().equals("Delimited")){
			fields[3].setDisabled(true);
			fields[4].setDisabled(true);
			fields[5].setDisabled(true);
			fields[6].setDisabled(false);
		}
		else{
			fields[3].setDisabled(true);
			fields[4].setDisabled(true);
			fields[5].setDisabled(true);
			fields[6].setDisabled(true);
		}

	} 

    /** update the values only for REQUEST_RESPONSE field. */
    if (!REQUEST_RESPONSE.equals(name)) {
      return fields;
    }
    /** RESPONSE_ENCODING field should be enabled only when  REQUEST_RESPONSE
     * is true.*/
    Map<String, TransportEditField> fieldMap =
      TransportEditField.getFieldMap(fields);
    TransportEditField editField = fieldMap.get(REQUEST_RESPONSE);
    TransportUIFactory.CheckBoxObject selectObject =
      (TransportUIFactory.CheckBoxObject) editField.getObject();
    boolean b = selectObject.ischecked();
    fieldMap.get(RESPONSE_ENCODING).setDisabled(!b);
    return fields;
  }


  /**
   * Called at the time the service details are viewed in read-only mode to get
   * the contents of the summary pane for endpoint configuration
   */
  public TransportViewField[] getViewPage(EndPointConfiguration config)
    throws TransportException {
    List<TransportViewField> fields = new ArrayList<TransportViewField>();
    SocketEndpointConfiguration socketEndpointConfiguration =
      SocketTransportUtil.getConfig(config);
    TransportViewField field =
      new TransportViewField(REQUEST_RESPONSE,
        TextMessages.getMessage(TextMessages.REQUEST_RESPONSE, locale),
        socketEndpointConfiguration.getRequestResponse());
    fields.add(field);

    if (uiContext.isProxy()) {
      SocketInboundPropertiesType inboundProperties =
        socketEndpointConfiguration.getInboundProperties();

      field = new TransportViewField(BACKLOG,
        TextMessages.getMessage(TextMessages.BACKLOG, locale),
        inboundProperties.getBacklog());
      fields.add(field);

      field = new TransportViewField(TIME_OUT,
        TextMessages.getMessage(TextMessages.TIME_OUT, locale),
        inboundProperties.getTimeout());
      fields.add(field);

		/* CWH: Added for Eximbank*/
		


		field = new TransportViewField(SOCKET_TYPE,
        TextMessages.getMessage(TextMessages.SOCKET_TYPE, locale),
        inboundProperties.getSockettype());
      fields.add(field);

if(inboundProperties.getSockettype().equals("Embedded Length")){
		field = new TransportViewField(INIT_BYTE,
        TextMessages.getMessage(TextMessages.INIT_BYTE, locale),
        inboundProperties.getInitbyte());
      fields.add(field);


	  field = new TransportViewField(KEEP_INIT_BYTE,
        TextMessages.getMessage(TextMessages.KEEP_INIT_BYTE, locale),
        inboundProperties.getKeepinitbyte());
      fields.add(field);
}

if(inboundProperties.getSockettype().equals("Fixed Length")){
	    field = new TransportViewField(MESSAGE_LENGTH,
        TextMessages.getMessage(TextMessages.MESSAGE_LENGTH, locale),
        inboundProperties.getMessagelength());
      fields.add(field);
}

if(inboundProperties.getSockettype().equals("Delimited")){
	    field = new TransportViewField(DELIMITER,
        TextMessages.getMessage(TextMessages.DELIMITER, locale),
        inboundProperties.getDelimiter());
      fields.add(field);
}

/*
	    field = new TransportViewField(WELCOME_MESSAGE,
        TextMessages.getMessage(TextMessages.WELCOME_MESSAGE, locale),
        inboundProperties.getWelcomemessage());
      fields.add(field);

	    field = new TransportViewField(WELCOME_MESSAGE_COUNT,
        TextMessages.getMessage(TextMessages.WELCOME_MESSAGE_COUNT, locale),
        inboundProperties.getWelcomemessagecount());
      fields.add(field);
*/
	  field = new TransportViewField(KEEP_CONNECTION,
        TextMessages.getMessage(TextMessages.KEEP_CONNECTION, locale),
        inboundProperties.getKeepconnection());
      fields.add(field);
		/*------*/

      field = new TransportViewField(ENABLE_NAGLE_ALGORITHM,
        TextMessages.getMessage(TextMessages.ENABLE_NAGLE_ALGORITHM, locale),
        inboundProperties.getEnableNagleAlgorithm());
      fields.add(field);




    } else {
      SocketOutboundPropertiesType outboundProperties =
        socketEndpointConfiguration.getOutboundProperties();
      field = new TransportViewField(TIME_OUT,
        TextMessages.getMessage(TextMessages.TIME_OUT, locale),
        outboundProperties.getTimeout());
      fields.add(field);

/* added for Sacombank*/


	field = new TransportViewField(SOCKET_TYPE,
        TextMessages.getMessage(TextMessages.SOCKET_TYPE, locale),
        outboundProperties.getSockettype());
      fields.add(field);
	
if(outboundProperties.getSockettype().equals("Embedded Length")){
			field = new TransportViewField(INIT_BYTE,
        TextMessages.getMessage(TextMessages.INIT_BYTE, locale),
        outboundProperties.getInitbyte());
      fields.add(field);

	  field = new TransportViewField(KEEP_INIT_BYTE,
        TextMessages.getMessage(TextMessages.KEEP_INIT_BYTE, locale),
        outboundProperties.getKeepinitbyte());
      fields.add(field);
	}
	  field = new TransportViewField(KEEP_CONNECTION,
        TextMessages.getMessage(TextMessages.KEEP_CONNECTION, locale),
        outboundProperties.getKeepconnection());
      fields.add(field);
/*------------*/
      field = new TransportViewField(ENABLE_NAGLE_ALGORITHM,
        TextMessages.getMessage(TextMessages.ENABLE_NAGLE_ALGORITHM, locale),
        outboundProperties.getEnableNagleAlgorithm());
      fields.add(field);

 if(outboundProperties.getSockettype().equals("Fixed Length"))
	  {
field = new TransportViewField(MESSAGE_LENGTH,
        TextMessages.getMessage(TextMessages.MESSAGE_LENGTH, locale),
        outboundProperties.getMessagelength());
      fields.add(field);

}

if(outboundProperties.getSockettype().equals("Delimited"))
	  {
field = new TransportViewField(DELIMITER,
        TextMessages.getMessage(TextMessages.DELIMITER, locale),
        outboundProperties.getDelimiter());
      fields.add(field);

}

field = new TransportViewField(WELCOME_MESSAGE,
        TextMessages.getMessage(TextMessages.WELCOME_MESSAGE, locale),
        outboundProperties.getWelcomemessage());
      fields.add(field);


field = new TransportViewField(WELCOME_MESSAGE_COUNT,
        TextMessages.getMessage(TextMessages.WELCOME_MESSAGE_COUNT, locale),
        outboundProperties.getWelcomemessagecount());
      fields.add(field);

    field = new TransportViewField(REQUEST_ENCODING,
      TextMessages.getMessage(TextMessages.REQUEST_ENCODING, locale),
      socketEndpointConfiguration.getRequestEncoding());
    fields.add(field);

    field = new TransportViewField(RESPONSE_ENCODING,
      TextMessages.getMessage(TextMessages.RESPONSE_ENCODING, locale),
      socketEndpointConfiguration.getResponseEncoding());
    fields.add(field);
	}
    String dispatchPolicy = socketEndpointConfiguration.getDispatchPolicy();
    if (dispatchPolicy == null) {
      dispatchPolicy = DEFAULT_WORK_MANAGER;
    }
    field = new TransportViewField(DISPATCH_POLICY,
      TextMessages.getMessage(TextMessages.DISPATCH_POLICY, locale),
      dispatchPolicy
    );
    fields.add(field);

    return fields.toArray(new TransportViewField[fields.size()]);
  }

  public static final String DEFAULT_WORK_MANAGER = "default";


  /**
   * Builds the disptahc policies in the ui object.
   *
   * @param curDispatchPolicy
   * @return TransportEditField containing existing dispatch policies.
   */
  public TransportEditField getDispatchPolicyEditField(
    String curDispatchPolicy) {

    TransportUIFactory.TransportUIObject uiObject = null;
    Set<String> wmSet = null;
    JMXConnector jmxConnector = null;

    if (SocketTransportProviderFactory.isOffline()) {
      jmxConnector =
        (JMXConnector) uiContext.get(TransportValidationContext.JMXCONNECTOR);
    } else {
      try {
        jmxConnector = SocketTransportUtil.getServerSideConnection(
          DomainRuntimeServiceMBean.MBEANSERVER_JNDI_NAME);
      } catch (Exception e) {
        SocketTransportUtil.logger
          .error(SocketTransportMessagesLogger.noJmxConnectorAvailble(), e);
      }
    }
    try {
      if (jmxConnector != null) {
        wmSet = TransportManagerHelper.getDispatchPolicies(jmxConnector);
      } else {
        wmSet = TransportManagerHelper.getDispatchPolicies();
      }
    } catch (Exception ex) {
      wmSet = null; //continue
      SocketTransportUtil.logger
        .error(SocketTransportMessagesLogger.noDispatchPolicies(), ex);
    }


    if (wmSet == null) {
      // if JMXConnector not available or impossible to connect provide a simple edit field
      uiObject = TransportUIFactory.createTextBox(curDispatchPolicy);
    } else {
      // create a drop down list
      // adding default work manager to the list.
      wmSet.add(DEFAULT_WORK_MANAGER);

      String[] values = wmSet.toArray(new String[wmSet.size()]);
      uiObject = TransportUIFactory.createSelectObject(
        values,
        values,
        curDispatchPolicy,
        TransportUIFactory.SelectObject.DISPLAY_LIST,
        false);
    }

    return TransportUIFactory.createEditField(DISPATCH_POLICY,
      TextMessages.getMessage(TextMessages.DISPATCH_POLICY, locale),
      TextMessages.getMessage(TextMessages.DISPATCH_POLICY_INFO, locale),
      uiObject);
  }

  /**
   * Validates the main form of the transport by checking whether the configured
   * URIs are valid or not.
   *
   * @param fields
   * @return Returns an array of TransportUIError of the invalid URIs.
   */
  public TransportUIError[] validateMainForm(TransportEditField[] fields) {
    Map<String, TransportUIFactory.TransportUIObject> map =
      TransportEditField.getObjectMap(fields);

    List<TransportUIError> errors = new ArrayList<TransportUIError>();
    if (!uiContext.isProxy()) {
      List<String[]> uris = getStringValues(map, TransportUIBinding.PARAM_URI);
      for (String[] uristr : uris) {
        try {
          URI uri = new URI(uristr[0]);
          if (!(uri.getScheme().equals("tcp") && uri.getHost() != null &&
            uri.getPort() != -1)) {
            errors.add(new TransportUIError(TransportUIBinding.PARAM_URI,
              "Invalid URI"));
          }
        } catch (URISyntaxException e) {
          errors.add(new TransportUIError(TransportUIBinding.PARAM_URI,
            e.getMessage()));
        }
      }
    } else {
      List<String[]> uris = getStringValues(map, TransportUIBinding.PARAM_URI);
      for (String[] uristr : uris) {
        String str = uristr[0];
        if (str.startsWith("tcp://")) {
          try {
            Integer.parseInt(str.substring(6));
          } catch (NumberFormatException e) {
            errors.add(new TransportUIError(TransportUIBinding.PARAM_URI,
              e.getMessage()));
          }
        } else {
          errors.add(new TransportUIError(TransportUIBinding.PARAM_URI,
            "URI does not starts with tcp://"));
        }
      }
    }

    return errors == null || errors.isEmpty() ? null :
      errors.toArray(new TransportUIError[errors.size()]);
  }


  /**
   * validate the provider-specific transport endpoint parameters in the
   * request.
   */
  public TransportUIError[] validateProviderSpecificForm(
    TransportEditField[] fields) {
    /** Socket transport configuration cn be validated here. */
    return new TransportUIError[0];
  }


  /**
   * creates the Transport Provider Specific configuration from the UI form.
   * This method will be called only upon a successfull call to {@link
   * #validateMainForm} and {@link #validateProviderSpecificForm}
   */
  public XmlObject getProviderSpecificConfiguration(TransportEditField[] fields)
    throws TransportException {

    SocketEndpointConfiguration socketEndpointConfig =
      SocketEndpointConfiguration.Factory.newInstance();
    Map<String, TransportUIFactory.TransportUIObject> map =
      TransportEditField.getObjectMap(fields);
    socketEndpointConfig.setRequestResponse(
      TransportUIFactory.getBooleanValue(map, REQUEST_RESPONSE));

    if (uiContext.isProxy()) {
      SocketInboundPropertiesType socketInboundPropertiesType =
        socketEndpointConfig.addNewInboundProperties();
      socketInboundPropertiesType
        .setBacklog(TransportUIFactory.getIntValue(map, BACKLOG));
      socketInboundPropertiesType.setEnableNagleAlgorithm(
        TransportUIFactory.getBooleanValue(map, ENABLE_NAGLE_ALGORITHM));
      socketInboundPropertiesType
        .setTimeout(TransportUIFactory.getIntValue(map, TIME_OUT));
	  
	   /*CWH: Added for Eximbank */

	socketInboundPropertiesType.setSockettype(TransportUIFactory.getStringValue(map, SOCKET_TYPE));

	   socketInboundPropertiesType
        .setInitbyte(TransportUIFactory.getIntValue(map, INIT_BYTE));

	      socketInboundPropertiesType
        .setKeepinitbyte(TransportUIFactory.getBooleanValue(map, KEEP_INIT_BYTE));

		     socketInboundPropertiesType
        .setKeepconnection(TransportUIFactory.getBooleanValue(map, KEEP_CONNECTION));

	socketInboundPropertiesType.setMessagelength(TransportUIFactory.getIntValue(map, MESSAGE_LENGTH));

	socketInboundPropertiesType.setDelimiter(TransportUIFactory.getStringValue(map, DELIMITER));

	socketInboundPropertiesType.setWelcomemessage(TransportUIFactory.getStringValue(map, WELCOME_MESSAGE));

	socketInboundPropertiesType.setWelcomemessagecount(TransportUIFactory.getIntValue(map, WELCOME_MESSAGE_COUNT));
	   /*-----------)*/
    } else {
      SocketOutboundPropertiesType socketOutboundPropertiesType =
        socketEndpointConfig.addNewOutboundProperties();
/*added for sacombank*/


	   socketOutboundPropertiesType
        .setSockettype(TransportUIFactory.getStringValue(map, SOCKET_TYPE));



	   socketOutboundPropertiesType
        .setInitbyte(TransportUIFactory.getIntValue(map, INIT_BYTE));

	      socketOutboundPropertiesType
        .setKeepinitbyte(TransportUIFactory.getBooleanValue(map, KEEP_INIT_BYTE));

		     socketOutboundPropertiesType
        .setKeepconnection(TransportUIFactory.getBooleanValue(map, KEEP_CONNECTION));


			 socketOutboundPropertiesType.setMessagelength(TransportUIFactory.getIntValue(map, MESSAGE_LENGTH));

	socketOutboundPropertiesType.setDelimiter(TransportUIFactory.getStringValue(map, DELIMITER));

	socketOutboundPropertiesType.setWelcomemessage(TransportUIFactory.getStringValue(map, WELCOME_MESSAGE));

	socketOutboundPropertiesType.setWelcomemessagecount(TransportUIFactory.getIntValue(map, WELCOME_MESSAGE_COUNT));
//----

      socketOutboundPropertiesType.setEnableNagleAlgorithm(
        TransportUIFactory.getBooleanValue(map, ENABLE_NAGLE_ALGORITHM));
      socketOutboundPropertiesType
        .setTimeout(TransportUIFactory.getIntValue(map, TIME_OUT));
    }

    String reqEnc = TransportUIFactory.getStringValue(map, REQUEST_ENCODING);
    if (reqEnc != null && reqEnc.trim().length() != 0) {
      socketEndpointConfig.setRequestEncoding(reqEnc);
    }
    String resEnc = TransportUIFactory.getStringValue(map, RESPONSE_ENCODING);
    if (resEnc != null && resEnc.trim().length() != 0) {
      socketEndpointConfig.setResponseEncoding(resEnc);
    }

    String dispatchPolicy =
      TransportUIFactory.getStringValue(map, DISPATCH_POLICY);
    socketEndpointConfig.setDispatchPolicy(dispatchPolicy);

    return socketEndpointConfig;
  }

  public Reader getHelpPage() {
    String helpFile = "help/en/contexts_socketTransport.html";
    ClassLoader clLoader = Thread.currentThread().getContextClassLoader();
    InputStream is = clLoader.getResourceAsStream(helpFile);
    InputStreamReader helpReader = null;
    if(is!=null)
      helpReader = new InputStreamReader(is);
    else
      SocketTransportUtil.logger
        .warning(SocketTransportMessagesLogger.noHelpPageAvailableLoggable().
                getMessage(uiContext.getLocale()));
    return helpReader;
  }

}
