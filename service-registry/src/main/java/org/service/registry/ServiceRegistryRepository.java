package org.service.registry;

import java.util.UUID;

public interface ServiceRegistryRepository {
    PutResponse register(Service service);
    DeleteResponse deregister(UUID serviceId, String serviceName);
    RenewResponse heartBeat(long leaseId);
}
