package org.service.registry;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.api.Event;
import io.etcd.jetcd.watch.WatchEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;

@Testcontainers
public class EtcdServiceRegistryDataSourceIntegrationTest {

    @Container
    GenericContainer etcdContainer = new GenericContainer<>(DockerImageName.parse("bitnami/etcd:latest"))
            .withExposedPorts(2379, 2380)
            .withEnv("ALLOW_NONE_AUTHENTICATION","yes")
            .withEnv("ETCD_ADVERTISE_CLIENT_URLS", "http://etcd-server:2379")
            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));

    EtcdRegistryDataSource etcdRegistry;

    @BeforeEach
    void setUp() {
        var ip = etcdContainer.getHost();
        var port = etcdContainer.getFirstMappedPort();
        var endpoint = "http://%s:%d".formatted(ip, port);
        etcdRegistry = new EtcdRegistryDataSource(
                Client.builder()
                        .endpoints(endpoint)
                        .build()
        );
    }
    @Test
    void environmentVariable() {
        var map = etcdContainer.getEnvMap();
        assertEquals("yes", map.get("ALLOW_NONE_AUTHENTICATION"));
        assertEquals("http://etcd-server:2379", map.get("ETCD_ADVERTISE_CLIENT_URLS"));

    }

    @Test
    void putItem(){
        var response = etcdRegistry.put("hello", "world");
        assertEquals("hello", response.key());
        assertEquals("world", response.value());
        assertInstanceOf(Long.class, response.revision());
        assertInstanceOf(Long.class, response.leaseId());
    }

    @Test
    void firstPutAnItemAfterRenew(){
        var putResponse = etcdRegistry.put("hello", "world");
        var renewResponse = etcdRegistry.renew(putResponse.leaseId());
        assertEquals(putResponse.leaseId(), renewResponse.leaseId());
        assertEquals(EtcdRegistryDataSource.TTL, renewResponse.ttl());
    }

    @Test
    void firstPutAnItemAfterDelete() {
        var putResponse = etcdRegistry.put("hello", "world");
        var deleteResponse = etcdRegistry.delete(putResponse.key());
        assertEquals(putResponse.key(), deleteResponse.key());
        assertEquals(3, deleteResponse.revision());
    }

    @Test
    void watch() throws InterruptedException {
        var cdl = new CountDownLatch(1);
        var atomicDataHolder = new AtomicReference<String>(null);
        Consumer<io.etcd.jetcd.watch.WatchResponse> callback = (eventResponse) -> {
            var events = eventResponse.getEvents();
            for(WatchEvent event : events) {
                System.out.println(event.getKeyValue().getValue().toString() + " " + event.getEventType().toString());
                if(event.getEventType() == WatchEvent.EventType.DELETE) {
                    var prevKv = event.getPrevKV();
                    if(prevKv != null) {
                        if(prevKv.getValue().toString().equals("world")) {
                            atomicDataHolder.set(prevKv.getValue().toString());
                            cdl.countDown();

                        }
                    }
                }
            }

        };
        WatchResponse<Watch.Watcher> watcherResponse = etcdRegistry.watch("hello", callback);


        etcdRegistry.put("hello", "world");
        etcdRegistry.delete("hello");
        cdl.await();
        watcherResponse.watchResponse().close();
        assertEquals("world", atomicDataHolder.get());
    }
}
