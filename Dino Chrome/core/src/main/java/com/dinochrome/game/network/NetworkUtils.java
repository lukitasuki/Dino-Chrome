package com.dinochrome.game.network;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class NetworkUtils {

    private NetworkUtils() {}

    public static List<InetAddress> getBroadcastAddresses() {
        List<InetAddress> result = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress bcast = ia.getBroadcast();
                    if (bcast != null && bcast instanceof Inet4Address) {
                        result.add(bcast);
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback (último recurso)
        if (result.isEmpty()) {
            try { result.add(InetAddress.getByName("255.255.255.255")); } catch (Exception ignored) {}
        }

        return result;
    }
}
