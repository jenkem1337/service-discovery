package org.service.registry;

import io.etcd.jetcd.*;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import io.etcd.jetcd.options.PutOption;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
public class EtcdRegistryDataSource implements RegistryDataSource{
    private final Client etcdClient;
    private final Lease leaseClient;
    private final KV keyValueClient;
    private final Watch watchClient;
    public static int TTL = 10;
    public EtcdRegistryDataSource(Client etcdClient) {
        this.etcdClient = etcdClient;
        this.leaseClient = this.etcdClient.getLeaseClient();
        this.keyValueClient = this.etcdClient.getKVClient();
        this.watchClient = this.etcdClient.getWatchClient();
    }

    @Override
    public PutResponse put(String key, String value) {
        try {
            var leaseId = leaseClient.grant(TTL).get().getID();
            ByteSequence $key = ByteSequence.from(key.getBytes());
            ByteSequence $value = ByteSequence.from(value.getBytes());

            var putResponse = keyValueClient.put($key, $value, PutOption.builder().withLeaseId(leaseId).build()).get();
            return new PutResponse(
                    key,
                    value,
                    putResponse.getPrevKv().getVersion(),
                    putResponse.getHeader().getRevision(),
                    leaseId);
        } catch (InterruptedException | ExecutionException e) {
            throw new DataSourceException(e.toString());
        }
    }

    @Override
    public PutResponse put(String key, byte[] value) {
        try {
            var leaseId = leaseClient.grant(TTL).get().getID();
            ByteSequence $key = ByteSequence.from(key.getBytes());
            ByteSequence $value = ByteSequence.from(value);

            var putResponse = keyValueClient.put($key, $value, PutOption.builder().withLeaseId(leaseId).build()).get();
            return new PutResponse(
                    key,
                    $value.toString(StandardCharsets.UTF_8),
                    putResponse.getPrevKv().getVersion(),
                    putResponse.getHeader().getRevision(),
                    leaseId);
        } catch (InterruptedException | ExecutionException e) {
            throw new DataSourceException(e.toString());
        }
    }

    @Override
    public DeleteResponse delete(String key) {
        try{
            var response = keyValueClient.delete(ByteSequence.from(key.getBytes())).get();
            var deleteList = response.getPrevKvs();
            return new DeleteResponse(
                    key,
                    response.getHeader().getRevision()
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new DataSourceException(e.toString());
        }
    }

    @Override
    public RenewResponse renew(long leaseId) {
        try {
            var response = this.leaseClient.keepAliveOnce(leaseId).get();
            return new RenewResponse(response.getID(), response.getTTL(), response.getHeader().getRevision());
        } catch (InterruptedException | ExecutionException e) {
            throw new DataSourceException(e.toString());
        }
    }

    @Override
    public org.service.registry.WatchResponse watch(String key, Consumer watchResponseConsumer){
        var watchOption = WatchOption.builder().withPrevKV(true).build();
        var watcher = watchClient.watch(ByteSequence.from(key.getBytes()), watchOption, (Consumer<WatchResponse>)watchResponseConsumer);
        return new org.service.registry.WatchResponse<>(watcher);
    }
}
