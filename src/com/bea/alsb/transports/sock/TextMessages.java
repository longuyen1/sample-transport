package com.bea.alsb.transports.sock;

import weblogic.i18n.Localizer;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.HashMap;

/**
 * It contains messages used by {@link SocketTransportUIBinding}. It has an API
 * to get localized messages.
 *
 * These are mapped in /l10n/SocketTransportTextMessages.xml
 *
 * Messages can be added here and map it in the above mapping xml.
 * More information can be found at http://edocs.bea.com/wls/docs91/i18n/MssgCats.html
 *  
 */
public class TextMessages {
  public static final String BACKLOG = "BACKLOG";
  public static final String BACKLOG_INFO = "BACKLOG_INFO";
  public static final String TIME_OUT = "TIME_OUT";
  public static final String TIME_OUT_INFO = "TIME_OUT_INFO";
  public static final String REQUEST_RESPONSE = "REQUEST_RESPONSE";
  public static final String REQUEST_RESPONSE_INFO = "REQUEST_RESPONSE_INFO";
  public static final String REQUEST_ENCODING = "REQUEST_ENCODING";
  public static final String REQUEST_ENCODING_INFO = "REQUEST_ENCODING_INFO";
  public static final String RESPONSE_ENCODING = "RESPONSE_ENCODING";
  public static final String RESPONSE_ENCODING_INFO = "RESPONSE_ENCODING_INFO";
  public static final String DISPATCH_POLICY = "DISPATCH_POLICY";
  public static final String DISPATCH_POLICY_INFO = "DISPATCH_POLICY_INFO";
  public static final String ENABLE_NAGLE_ALGORITHM = "ENABLE_NAGLE_ALGORITHM";
  public static final String ENABLE_NAGLE_ALGORITHM_INFO =
    "ENABLE_NAGLE_ALGORITHM_INFO";

/* Added by Cipto*/

  public static final String SOCKET_TYPE =
    "SOCKET_TYPE";

  public static final String SOCKET_TYPE_INFO =
    "SOCKET_TYPE_INFO";


  public static final String INIT_BYTE =
    "INIT_BYTE";

  public static final String INIT_BYTE_INFO =
    "INIT_BYTE_INFO";

    public static final String KEEP_INIT_BYTE =
    "KEEP_INIT_BYTE";

  public static final String KEEP_INIT_BYTE_INFO =
    "KEEP_INIT_BYTE_INFO";

  public static final String MESSAGE_LENGTH =
    "MESSAGE_LENGTH";

  public static final String MESSAGE_LENGTH_INFO =
    "MESSAGE_LENGTH_INFO";

  public static final String DELIMITER =
    "DELIMITER";

    public static final String DELIMITER_INFO =
    "DELIMITER_INFO";


	public static final String WELCOME_MESSAGE =
    "WELCOME_MESSAGE";

    public static final String WELCOME_MESSAGE_INFO =
    "WELCOME_MESSAGE_INFO";

	public static final String WELCOME_MESSAGE_COUNT =
    "WELCOME_MESSAGE_COUNT";

    public static final String WELCOME_MESSAGE_COUNT_INFO =
    "WELCOME_MESSAGE_COUNT_INFO";



  public static final String KEEP_CONNECTION =
    "KEEP_CONNECTION";

  public static final String KEEP_CONNECTION_INFO =
    "KEEP_CONNECTION_INFO";
/*-----------------*/


  public static final String INBOUND_URI_FORMAT = "INBOUND_URI_FORMAT";
  public static final String INBOUND_URI_FORMAT_AUTOFILL =
    "INBOUND_URI_FORMAT_AUTOFILL";
  public static final String OUTBOUND_URI_FORMAT = "OUTBOUND_URI_FORMAT";
  public static final String OUTBOUND_URI_FORMAT_AUTOFILL =
    "OUTBOUND_URI_FORMAT_AUTOFILL";

  private static final String propsClazz =
    "com.bea.alsb.transports.socket.SocketTransportTextMessagesTextLocalizer";
  static HashMap<Locale, Localizer> resourceBundleMap =
    new HashMap<Locale, Localizer>();

  public static String getMessage(String id, Locale locale) {
    Localizer localizer = resourceBundleMap.get(locale);
    if (localizer == null) {
      ResourceBundle resourceBundle =
        ResourceBundle.getBundle(propsClazz, locale);
      localizer = new Localizer(resourceBundle);
      resourceBundleMap.put(locale, localizer);
    }
    return localizer.get(id);
  }

  public static String getMessage(String id) {
    return getMessage(id, Locale.getDefault());
  }
}
