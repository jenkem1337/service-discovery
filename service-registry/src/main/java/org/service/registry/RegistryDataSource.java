package org.service.registry;

public interface RegistryDataSource {
    GetResponse get(String key);
    PutResponse put(String key, String value);
    PutResponse put(String key, byte[] value);
    void delete(String key);
    void renew(long leaseId);
}
