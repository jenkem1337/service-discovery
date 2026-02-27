package org.service.discovery.loadbalancer.proxy;


import com.dslplatform.json.CompiledJson;

@CompiledJson
public class ServiceJsonDataMapper {
    public final String id;
    public final String serviceId;
    public final String serviceName;
    public final String ip;
    public final int port;
    public final String protocol;
    public final String creationDate;

    public ServiceJsonDataMapper(String id, String serviceId, String serviceName, String ip, int port, String protocol, String creationDate) {
        this.id = id;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.ip = ip;
        this.port = port;
        this.protocol = protocol;
        this.creationDate = creationDate;
    }
}
