package org.service.registry;

public record Port(int portNumber) {
    public static Port of(int portNumber) {
        if(portNumber < 0) {
            throw new IllegalArgumentException("Port number must not be negative !");
        }
        if (portNumber > ( Math.pow(2,16) - 1)){
            throw new IllegalArgumentException("Port number must not be bigger than 65,535 !");
        }
        return new Port(portNumber);
    }
}
