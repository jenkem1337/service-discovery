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
    private Instant latestHeartBeat;

    private Service(UUID id, UUID serviceId,  Name serviceName, IpAddress ipAddress, Port portNumber, Protocol protocol, Instant creationDate, Instant latestHeartBeat) {
        this.id = id;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.protocol = protocol;
        this.creationDate = creationDate;
        this.latestHeartBeat = latestHeartBeat;
    }

    public static Service of(UUID id, UUID serviceId,  Name serviceName, IpAddress ipAddress, Port portNumber, Protocol protocol, Instant creationDate, Instant latestHeartBeat) {
        return new Service(id,serviceId,serviceName,ipAddress,portNumber,protocol,creationDate,latestHeartBeat);
    }
    public static Service withoutHeartbeatMessage(UUID id, UUID serviceId,  Name serviceName, IpAddress ipAddress, Port portNumber, Protocol protocol, Instant creationDate){
        return new Service(id,serviceId,serviceName,ipAddress,portNumber,protocol,creationDate,null);
    }
}
