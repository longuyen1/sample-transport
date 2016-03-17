# sample-transport
=======
PURPOSE
========
This document describes how to build, deploy and test the sample socket transport.
Sample socket transport demonstrates how a transport can be developed by using
the Transport SDK and leverage the features provided.

==========
CONTENTS:
==========
sample-transport directory has the following structure.
SAMPLE-TRANSPORT-DIR
	|
	|---- build                                 [ Directory which contains all the compiled files, like classes]
	|---- eclipse                               [ Contains eclipse plugin files ]
		|---- help/                                 [ Help files for eclipse plugin ]
		|---- plugin.xml                            [ Descriptor file for eclipse plugin ]
	|---- l10n                                  [ Contains Internationalization files ]
	    |---- SocketTransportMessages.xml           [ Configuration file for logging internationalized messages ]
	    |---- SocketTransportTextMessages.xml       [ Configuration file for text messages which are displayed on console ]
	|---- META-INF	                            [ Contains EAR descriptor files ]
		|----- application.xml                      [ JEE application descriptor file ]
		|----- weblogic-application.xml             [ Weblogic application descriptor file ]
	|---- offline                               [ Configuration for OSB offline-mode (e.g. harvester) ]
	    |---- transport-socket.xml                  [ Descriptor file for offline plugin ]
	|---- resources                             [ Contains configuration files used as java resources ]
		|---- help/                                 [ Help files ]
		|---- SocketConfig.xml                      [ Socket transport provider configuration which is used by Transport SDK ]
	|---- schemas                               [ Contains the schemas defining this transport  ]
		|---- SocketTransport.xsd                    [ Describes Socket Endpoint Request/Response Metadat/headers ]
	|---- src                                   [ Source tree of the sample transport ]
	|---- test                                  [ Test files directory ]
		|---- src                                   [ Source tree for test server and client ]
    |---- webapp                                [ Help webapp ]
        |---- help/                                 [ Help files ]
        |---- WEB-INF/                              [ webapp descriptors ]
	|---- build.properties                      [ Properties file for ant ]
	|---- build.xml                             [ ant file which has different targets for compile, build and deploy ]


========================
SETTING THE ENVIRONMENT
========================
Set the domain environment as given below.
Go to $DOMAIN_DIR/bin and run setDomainEnv.cmd or setDomainEnv.sh

Go to $SAMPLES-DIRECTORY before doing the below operations.

=======================
BUILDING THE TRANSPORT
=======================
To build the socket transport, run the following command:

	ant build

	This command compiles all the sources including the test client and creates
	the corresponding transport JAR and EAR libraries.

=========================
INSTALLING THE TRANSPORT
=========================
In order to install the sample socket transport, run the following command:

	ant stage

    This command will stage the following in the OSB installation:
     - the JAR and EAR libraries
     - the Eclipse plugin
     - the plugin for OSB offline mode

=======================================
DEPLOYING THE TRANSPORT ON ALSB SERVER
=======================================
To deploy on a server,
	set the following variables in build.properties
		wls.hostname=<host-name of the machine>
		wls.port=<port number>
		wls.username=<user-name>
		wls.password=<password>
		wls.server.name=<Server name>
	All the above properties are of a ALSB admin server.
	To deploy on the above specified server, run the below command,

	ant deploy

===============================
TESTING THE DEPLOYED TRANSPORT
===============================
To test the sample transport, create a socket proxy-service and a business-service and
    make the proxy-service's route throgh business-service by using service bus console.
	
	Run the following command to start the external service, which is a server socket
	listens on a specified port and receives/sends the messages.

	java -classpath .\test\build\test-client.jar -Dfile-encoding=utf-8 -Drequest-encoding=utf-8 com.bea.alsb.transports.sample.test.TestServer <port> <message-file-location>

	port is the port number at which ServerSocket is listening, which is the port number in the business service.
	message-file-location (optional argument) is the location of the message-file
	which will be sent as a response to the business service.
  file-encoding is a system property which is the encoding of  the file.
  request-encoding is the encoding of the request that is sent by
  the socket business service.
  Both the above have default value as utf-8

	Run the following command to start initiating service, which is a client to
	a configured socket proxy-service.
	It sends message and receives the response from ALSB.

	java -classpath .\test\build\test-client.jar -Dfile-encoding=utf-8 -Dresponse-encoding=utf-8 com.bea.alsb.transports.sample.test.TestClient <host-name> <port> <thread-ct> <message-file-location>

	host-name is host name of the ALSB server,
	port is the port number at which the proxy service is listening,
	thread-ct is number of clients to send a message to ALSB and
	message-file-location (optional argument)is the location of the message-file
	which will be sent as a response to the business service.
	file-encoding is an optional argument which is the encoding of the file.
    response-encoding is the encoding of the response received from the
    socket proxy service.
    Both the above have default value as utf-8.
	
