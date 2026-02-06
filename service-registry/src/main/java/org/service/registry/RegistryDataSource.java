package org.service.registry;

import java.util.function.Consumer;

public interface RegistryDataSource {
    PutResponse put(String key, String value);
    PutResponse put(String key, byte[] value);
    DeleteResponse delete(String key);
    RenewResponse renew(long leaseId);

    WatchResponse watch(String key, Consumer watchResponseConsumer);
}
