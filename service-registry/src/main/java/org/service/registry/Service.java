package org.service.registry;

import java.time.Instant;
import java.util.UUID;

public class Service {
    private final UUID id;
    private final UUID serviceId;
    private Name serviceName;
    private IpAddress ipAddress;
    private Port portNumber;
    private Protocol protocol;
    private final Instant creationDate;

    private Service(UUID id, UUID serviceId,  Name serviceName, IpAddress ipAddress, Port portNumber, Protocol protocol, Instant creationDate) {
        this.id = id;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.protocol = protocol;
        this.creationDate = creationDate;
    }

    public static Service of(UUID id, UUID serviceId,  Name serviceName, IpAddress ipAddress, Port portNumber, Protocol protocol, Instant creationDate) {
        return new Service(id,serviceId,serviceName,ipAddress,portNumber,protocol,creationDate);
    }
    public UUID id() {
        return id;
    }

    public UUID serviceId() {
        return serviceId;
    }

    public Name serviceName() {
        return serviceName;
    }

    public IpAddress ipAddress() {
        return ipAddress;
    }

    public Port portNumber() {
        return portNumber;
    }

    public Protocol protocol() {
        return protocol;
    }

    public Instant creationDate() {
        return creationDate;
    }

}
