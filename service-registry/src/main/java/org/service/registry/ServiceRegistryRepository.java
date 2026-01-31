package org.service.registry;

import java.util.UUID;

public interface ServiceRegistryRepository {
    PutResponse register(Service service);
    void deregister(UUID serviceId, String serviceName);
    void heartBeat(long leaseId);
}
