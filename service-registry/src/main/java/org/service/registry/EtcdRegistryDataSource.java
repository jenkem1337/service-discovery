package org.service.registry;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.options.PutOption;

import java.util.concurrent.ExecutionException;

public class EtcdRegistryDataSource implements RegistryDataSource{
    private final Client etcdClient;
    private final Lease leaseClient;
    private final KV keyValueClient;
    public EtcdRegistryDataSource(Client etcdClient) {
        this.etcdClient = etcdClient;
        this.leaseClient = this.etcdClient.getLeaseClient();
        this.keyValueClient = this.etcdClient.getKVClient();
    }

    @Override
    public GetResponse get(String key) {
        return new GetResponse();
    }

    @Override
    public PutResponse put(String key, String value) {
        try {
            var leaseId = leaseClient.grant(10).get().getID();
            ByteSequence $key = ByteSequence.from(key.getBytes());
            ByteSequence $value = ByteSequence.from(value.getBytes());

            keyValueClient.put($key, $value, PutOption.builder().withLeaseId(leaseId).build()).get();
            return new PutResponse(leaseId);
        } catch (InterruptedException | ExecutionException e) {
            throw new DataSourceException(e.toString());
        }
    }

    @Override
    public PutResponse put(String key, byte[] value) {
        try {
            var leaseId = leaseClient.grant(10).get().getID();
            ByteSequence $key = ByteSequence.from(key.getBytes());
            ByteSequence $value = ByteSequence.from(value);

            var response = keyValueClient.put($key, $value, PutOption.builder().withLeaseId(leaseId).build()).get();
            return new PutResponse(response.getPrevKv().getLease());
        } catch (InterruptedException | ExecutionException e) {
            throw new DataSourceException(e.toString());
        }
    }

    @Override
    public void delete(String key) {
        try{
            keyValueClient.delete(ByteSequence.from(key.getBytes())).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new DataSourceException(e.toString());
        }
    }

    @Override
    public void renew(long leaseId) {
        try {
            this.leaseClient.keepAliveOnce(leaseId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DataSourceException(e.toString());
        }
    }
}
