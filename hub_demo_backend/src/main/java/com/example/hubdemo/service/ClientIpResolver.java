package com.example.hubdemo.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

@Service
public class ClientIpResolver {
    private final List<String> trustedProxies;

    public ClientIpResolver(@Value("${app.proxy.trusted-addresses:}") String trustedAddresses) {
        this.trustedProxies = Arrays.stream(trustedAddresses.split(","))
                .map(String::trim).filter(StringUtils::hasText).toList();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (!isTrusted(remoteAddress)) {
            return remoteAddress;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return normalize(forwardedFor.split(",", 2)[0]);
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? normalize(realIp) : remoteAddress;
    }

    private boolean isTrusted(String address) {
        return trustedProxies.stream().anyMatch(rule -> matches(address, rule));
    }

    private static boolean matches(String address, String rule) {
        try {
            if (!rule.contains("/")) {
                return InetAddress.getByName(address).equals(InetAddress.getByName(rule));
            }
            String[] parts = rule.split("/", 2);
            byte[] candidate = InetAddress.getByName(address).getAddress();
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            int prefix = Integer.parseInt(parts[1]);
            if (candidate.length != network.length || prefix < 0 || prefix > candidate.length * 8) return false;
            for (int bit = 0; bit < prefix; bit++) {
                int mask = 1 << (7 - bit % 8);
                if ((candidate[bit / 8] & mask) != (network[bit / 8] & mask)) return false;
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }
}
