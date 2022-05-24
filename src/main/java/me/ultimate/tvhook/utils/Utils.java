package me.ultimate.tvhook.utils;

import me.ultimate.tvhook.Main;

import java.net.*;
import java.util.Enumeration;

public class Utils {
    public static String getLocalIP() {
        String ip = "";

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr instanceof Inet6Address) continue;

                    ip = addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        return ip;
    }

    public static String getPublicIP() {
        String ip = "";

        try {
            byte[] bytes = new URL("https://api.ipify.org").openConnection().getInputStream().readAllBytes();
            ip = new String(bytes);
        } catch (Exception ignored) {}

        // ipv6 check
        if (ip.contains(":"))
            Main.getLogger().warn("Your public IP is an IPv6 address. TradingView webhooks only support IPv4 addresses.");

        return ip;
    }
}
