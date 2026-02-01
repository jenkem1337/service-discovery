package org.service.registry;

import io.etcd.jetcd.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
}
