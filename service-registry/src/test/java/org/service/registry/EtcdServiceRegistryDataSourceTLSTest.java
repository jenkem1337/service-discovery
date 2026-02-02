package org.service.registry;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Testcontainers
public class EtcdServiceRegistryDataSourceTLSTest {
    @Container
    GenericContainer etcdContainer = new GenericContainer<>(DockerImageName.parse("bitnami/etcd:latest"))
            .withExposedPorts(2379, 2380)
            .withFileSystemBind(
                    "src/test/resources/tls",
                    "/etc/etcd/tls",
                    BindMode.READ_ONLY
            )
            .withEnv("ALLOW_NONE_AUTHENTICATION","no")
            .withEnv("ETCD_ROOT_PASSWORD","example")
            .withEnv("ETCD_ADVERTISE_CLIENT_URLS", "https://0.0.0.0:2379")
            .withEnv("ETCD_LISTEN_CLIENT_URLS", "https://0.0.0.0:2379")
            .withEnv("ETCD_CERT_FILE", "/etc/etcd/tls/server.pem")
            .withEnv("ETCD_KEY_FILE", "/etc/etcd/tls/server-key.pem")
            .withEnv("ETCD_TRUSTED_CA_FILE", "/etc/etcd/tls/ca.pem")
            .withEnv("ETCD_CLIENT_CERT_AUTH", "true")

            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));

    EtcdRegistryDataSource etcdRegistry;

    @BeforeEach
    void setUp(){
        var ip = etcdContainer.getHost();
        var port = etcdContainer.getFirstMappedPort();
        var endpoint = "https://%s:%d".formatted(ip, port);
        InputStream ca =
                EtcdServiceRegistryDataSourceTLSTest.class.getClassLoader().getResourceAsStream("tls/ca.pem");

        InputStream clientCert =
                EtcdServiceRegistryDataSourceTLSTest.class.getClassLoader().getResourceAsStream("tls/client.pem");

        InputStream clientKey =
                EtcdServiceRegistryDataSourceTLSTest.class.getClassLoader().getResourceAsStream("tls/client-key.pem");

        if (ca == null || clientCert == null || clientKey == null) {
            throw new IllegalStateException("TLS files not found");
        }
        SslContext sslContext;
        try {
            sslContext = GrpcSslContexts
                    .forClient()
                    .trustManager(ca)
                    .keyManager(clientCert, clientKey)
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }

        var etcdClientBuilder = Client.builder()
                .endpoints(endpoint)
                .password(ByteSequence.from("example".getBytes()))
                .sslContext(sslContext);

        etcdRegistry = new EtcdRegistryDataSource(
                etcdClientBuilder.build()
        );

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
