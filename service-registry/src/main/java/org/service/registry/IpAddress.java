package org.service.registry;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public record IpAddress(String ip) {
    public IpAddress {
    }

    public static IpAddress of(String ip) {
        if(Objects.isNull(ip)) {
            throw new IllegalArgumentException("Ip address is null");
        }

        try {
            InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }

        return new IpAddress(ip);
    }
}
