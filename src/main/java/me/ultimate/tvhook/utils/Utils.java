package me.ultimate.tvhook.utils;

import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import me.ultimate.tvhook.Main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.*;
import java.util.Enumeration;
import java.util.Optional;

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

    public static String convertTradeAmount(double amount, double price, String currency) {
        BigDecimal originalDecimal = BigDecimal.valueOf(amount);
        int precision = Main.getAPI().getClient().getExchangeInfo().getSymbolInfo(currency).getBaseAssetPrecision(); // Round amount to base precision and LOT_SIZE
        String lotSize;
        Optional<String> minQtyOptional = Main.getAPI().getClient().getExchangeInfo().getSymbolInfo(currency)
                .getFilters().stream().filter(f -> FilterType.LOT_SIZE == f.getFilterType()).findFirst().map(SymbolFilter::getMinQty);
        Optional<String> minNotational = Main.getAPI().getClient().getExchangeInfo().getSymbolInfo(currency)
                .getFilters().stream().filter(f -> FilterType.MIN_NOTIONAL == f.getFilterType()).findFirst().map(SymbolFilter::getMinNotional);

        if (minQtyOptional.isPresent()) {
            lotSize = minQtyOptional.get();
        } else {
            Main.getLogger().error("Could not find lot size for {}, could not place trade.", currency);
            return null;
        }

        double minQtyDouble = Double.parseDouble(lotSize);
        if (amount < minQtyDouble) { // Check LOT_SIZE to make sure amount is not too small
            Main.getLogger().error("Amount smaller than min LOT_SIZE, could not open trade! (min LOT_SIZE={}, amount={})", lotSize, amount);
            return null;
        }

        // Convert amount to an integer multiple of LOT_SIZE and convert to asset precision
        String convertedAmount = new BigDecimal(lotSize).multiply(new BigDecimal((int) (amount / minQtyDouble))).setScale(precision, RoundingMode.HALF_DOWN).toString();

        if (minNotational.isPresent()) {
            double notational = Double.parseDouble(convertedAmount) * price;
            if (notational < Double.parseDouble(minNotational.get())) {
                Main.getLogger().error("Notational value {} is smaller than minimum {}", round(notational, 2), minNotational.get());
                return null;
            }
        }

        return convertedAmount;
    }

    public static String round(double value, int places) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.toString();
    }

    public static String decToStr(double dec) {
        return new BigDecimal(dec).toString();
    }
}
