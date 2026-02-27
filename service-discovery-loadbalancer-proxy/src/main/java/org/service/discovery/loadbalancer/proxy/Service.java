package org.service.discovery.loadbalancer.proxy;

public record Service(String serviceId, String serviceName, String ip, int port, String protocol) {
}
