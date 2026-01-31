package org.service.registry;

public enum Protocol {
    HTTP("http"),
    GRPC("grpc"),
    TCP("tcp"),
    UDP("udp");

    private final String value;

    Protocol(String value) {
        this.value = value;
    }

    public static Protocol from(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Protocol cannot be null");
        }

        for (Protocol protocol : values()) {
            if (protocol.value.equalsIgnoreCase(input)) {
                return protocol;
            }
        }

        throw new IllegalArgumentException("Unknown protocol: " + input);
    }

    }
