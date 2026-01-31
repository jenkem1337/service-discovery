package org.service.registry;

import com.dslplatform.json.DslJson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ServiceRegistryRepositoryImpl implements ServiceRegistryRepository{
    private static final DslJson<Object> JSON = new DslJson<>();
    private final RegistryDataSource dataSource;

    public ServiceRegistryRepositoryImpl(final RegistryDataSource dataSource) {
        this.dataSource = dataSource;
    }
    private static byte[] serialize(ServiceJsonDataMapper serviceJsonDataMapper) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(1024)) {
            JSON.serialize(serviceJsonDataMapper, os);
            return os.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize ServiceJsonDataMapper", e);
        }

    }
    @Override
    public PutResponse register(Service service) {
        var serviceJsonDataMapper = new ServiceJsonDataMapper(
                service.id().toString(),
                service.serviceId().toString(),
                service.serviceName().name(),
                service.ipAddress().ip(),
                service.portNumber().portNumber(),
                service.protocol().name(),
                service.creationDate().toString());
        var serializedData = serialize(serviceJsonDataMapper);
        var key = "/services/%s/%s".formatted(service.serviceName().name(), service.serviceId().toString());
        return dataSource.put(key, serializedData);
    }

    @Override
    public void deregister(UUID serviceId, String serviceName) {
        var key = "/services/%s/%s".formatted(serviceName, serviceId.toString());
        dataSource.delete(key);
    }

    @Override
    public void heartBeat(long leaseId) {
        dataSource.renew(leaseId);
    }
}
