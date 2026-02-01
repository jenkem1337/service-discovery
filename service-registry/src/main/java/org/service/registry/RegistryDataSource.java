package org.service.registry;

public interface RegistryDataSource {
    PutResponse put(String key, String value);
    PutResponse put(String key, byte[] value);
    DeleteResponse delete(String key);
    RenewResponse renew(long leaseId);
}
