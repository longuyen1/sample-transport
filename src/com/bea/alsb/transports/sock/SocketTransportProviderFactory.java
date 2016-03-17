package com.bea.alsb.transports.sock;

import com.bea.wli.sb.transports.TransportManager;
import com.bea.wli.sb.transports.TransportException;
import com.bea.wli.sb.transports.TransportProviderFactory;

public class SocketTransportProviderFactory implements TransportProviderFactory {

    public static boolean isOffline() {
        return isOffline;
    }

    private static boolean isOffline = false;

    public void registerProvider(TransportManager tm) throws TransportException {
        isOffline = true;
        SocketTransportProvider instance = SocketTransportProvider.getInstance();
        tm.registerProvider(instance, null);
    }

    public String getId() {
        return SocketTransportProvider.ID;
    }
}
