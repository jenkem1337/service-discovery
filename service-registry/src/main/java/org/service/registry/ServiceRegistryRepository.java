package org.service.registry;

import java.util.UUID;
import java.util.function.Consumer;

public interface ServiceRegistryRepository {
    PutResponse register(Service service);
    DeleteResponse deregister(UUID serviceId, String serviceName);
    RenewResponse heartBeat(long leaseId);
    WatchResponse watch(String key, Consumer watchCallback);
}
