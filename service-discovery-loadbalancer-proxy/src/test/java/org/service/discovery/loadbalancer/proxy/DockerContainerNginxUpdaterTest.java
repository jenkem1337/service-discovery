package org.service.discovery.loadbalancer.proxy;

import com.dslplatform.json.DslJson;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.options.WatchOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class DockerContainerNginxUpdaterTest {
    DockerContainerNginxUpdater nginxUpdater;
    ServiceEventListener serviceEventListener;
    Queue<UpdateCommand> updateCommandQueue;
    LoadBalancerUpdaterQueueExecutor queueExecutor;
    DslJson<ServiceJsonDataMapper> dslJson = new DslJson<>();

    static Client etcdClient;
    static Network network = Network.newNetwork();
    @Container
    GenericContainer etcdContainer = new GenericContainer<>(DockerImageName.parse("bitnami/etcd:latest"))
            .withNetwork(network)
            .withExposedPorts(2379, 2380)
            .withEnv("ALLOW_NONE_AUTHENTICATION","yes")
            .withEnv("ETCD_ADVERTISE_CLIENT_URLS", "http://etcd-server:2379")
            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));

    @Container
    GenericContainer<?> nginxContainer = new GenericContainer<>(DockerImageName.parse("nginx:latest"))
            .withNetwork(network)
            .withCreateContainerCmdModifier(cmd -> cmd.withName("test-nginx"))
            .withExposedPorts(80)
            .withFileSystemBind(
                    Paths.get("src/test/resources/nginx.conf")
                            .toAbsolutePath()
                            .toString(),
                    "/opt/nginx/nginx.conf",
                    BindMode.READ_WRITE
            )
            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
            .withCommand("nginx", "-c", "/opt/nginx/nginx.conf", "-g", "daemon off;");

    @BeforeEach
    void setUp(){
        var ip = etcdContainer.getHost();
        var port = etcdContainer.getFirstMappedPort();
        var endpoint = "http://%s:%d".formatted(ip, port);

        etcdClient = Client.builder().endpoints(endpoint).build();
        Properties conf = new Properties();
        conf.setProperty("nginx.configuration.template", "src/test/resources/nginx.conf.template");
        conf.setProperty("nginx.configuration.actual", "src/test/resources/nginx.conf");
        conf.setProperty("nginx.configuration.backup","src/test/resources/nginx.conf.backup");

        updateCommandQueue = new ConcurrentLinkedQueue<>();
        nginxUpdater = new DockerContainerNginxUpdater(conf, "test-nginx", DockerClientFactory.instance().client());
        serviceEventListener = new ServiceEventListener("/services/user-service", updateCommandQueue);
        queueExecutor = new LoadBalancerUpdaterQueueExecutor(updateCommandQueue, nginxUpdater, () -> {
           while(true) {
               UpdateCommand cmd = updateCommandQueue.poll();
               if(cmd != null) {
                   nginxUpdater.onUpdateCommand(cmd);
               } else {
                   Thread.yield();
               }
           }
        });
        queueExecutor.assignEventLoopToPlatformThread();
        queueExecutor.start();

    }

    @Test
    void shouldRegisterServiceToLoadBalancer_WhenServiceAddedToServiceRegistry() throws IOException, InterruptedException {
        var watcher = etcdClient.getWatchClient().watch(ByteSequence.from("/services/user-service".getBytes()), WatchOption.builder().isPrefix(true).build(), serviceEventListener);

        var service = new ServiceJsonDataMapper(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    "user-service",
                    "123.123.123.123",
                    9090,
                    "http",
                    Instant.now().toString()
                );
        var service2 = new ServiceJsonDataMapper(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user-service",
                "123.123.123.124",
                9090,
                "http",
                Instant.now().toString()
        );
        var service3 = new ServiceJsonDataMapper(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user-service",
                "123.123.123.125",
                9090,
                "http",
                Instant.now().toString()
        );

        var os = new ByteArrayOutputStream(1024);

        dslJson.serialize(service, os);
        var instanceJson = os.toByteArray();

        os.reset();

        dslJson.serialize(service2, os);
        var instanceJson2 = os.toByteArray();

        os.reset();

        dslJson.serialize(service3, os);
        var instanceJson3 = os.toByteArray();
        var kv = etcdClient.getKVClient();
        var key = ByteSequence.from("/services/user-service/%s".formatted(service.serviceId).getBytes());
        var value = ByteSequence.from(instanceJson);
        kv.put(key, value);

        var key2 = ByteSequence.from("/services/user-service/%s".formatted(service2.serviceId).getBytes());
        var value2 = ByteSequence.from(instanceJson2);
        kv.put(key2, value2);

        var key3 = ByteSequence.from("/services/user-service/%s".formatted(service3.serviceId).getBytes());
        var value3 = ByteSequence.from(instanceJson3);
        kv.put(key3, value3);

        Thread.sleep(5000);

        var result = nginxContainer.execInContainer(
                "cat",
                "/opt/nginx/nginx.conf"
        );

        String content = result.getStdout();

        assertTrue(
                content.contains("server 123.123.123.123:9090;"),
                "Container içindeki nginx.conf expected server entry içermiyor"
        );
        assertTrue(
                content.contains("server 123.123.123.124:9090;"),
                "Container içindeki nginx.conf expected server entry içermiyor"
        );
        assertTrue(
                content.contains("server 123.123.123.125:9090;"),
                "Container içindeki nginx.conf expected server entry içermiyor"
        );
        watcher.close();
    }

    @Test
    void shouldRemoveServiceToLoadBalancer_WhenServiceRemovedFromServiceRegistry() throws IOException, InterruptedException {
       var watcher =  etcdClient.getWatchClient().watch(ByteSequence.from("/services/user-service".getBytes()), WatchOption.builder().isPrefix(true).withPrevKV(true).build(), serviceEventListener);

        var service = new ServiceJsonDataMapper(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user-service",
                "123.123.123.123",
                9090,
                "http",
                Instant.now().toString()
        );
        var service2 = new ServiceJsonDataMapper(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user-service",
                "123.123.123.124",
                9090,
                "http",
                Instant.now().toString()
        );
        var service3 = new ServiceJsonDataMapper(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user-service",
                "123.123.123.125",
                9090,
                "http",
                Instant.now().toString()
        );

        var os = new ByteArrayOutputStream(1024);

        dslJson.serialize(service, os);
        var instanceJson = os.toByteArray();

        os.reset();

        dslJson.serialize(service2, os);
        var instanceJson2 = os.toByteArray();

        os.reset();

        dslJson.serialize(service3, os);
        var instanceJson3 = os.toByteArray();
        var kv = etcdClient.getKVClient();
        var key = ByteSequence.from("/services/user-service/%s".formatted(service.serviceId).getBytes());
        var value = ByteSequence.from(instanceJson);
        kv.put(key, value);

        var key2 = ByteSequence.from("/services/user-service/%s".formatted(service2.serviceId).getBytes());
        var value2 = ByteSequence.from(instanceJson2);
        kv.put(key2, value2);

        var key3 = ByteSequence.from("/services/user-service/%s".formatted(service3.serviceId).getBytes());
        var value3 = ByteSequence.from(instanceJson3);
        kv.put(key3, value3);

        var key_del2 = ByteSequence.from("/services/user-service/%s".formatted(service2.serviceId).getBytes());
        kv.delete(key_del2);

        Thread.sleep(8000);

        var result = nginxContainer.execInContainer(
                "cat",
                "/opt/nginx/nginx.conf"
        );

        String content = result.getStdout();

        assertTrue(
                content.contains("server 123.123.123.123:9090;"),
                "Container içindeki nginx.conf expected server entry içermiyor"
        );
        assertFalse(
                content.contains("server 123.123.123.124:9090;"),
                "Container içindeki nginx.conf expected server entry içeriyor"
        );
        assertTrue(
                content.contains("server 123.123.123.125:9090;"),
                "Container içindeki nginx.conf expected server entry içermiyor"
        );
        watcher.close();
    }

    @Test
    void shouldReloadSuccess_WhenAllInstanceRemoved() throws IOException, InterruptedException, ExecutionException {
        var watcher =  etcdClient.getWatchClient().watch(ByteSequence.from("/services/user-service".getBytes()), WatchOption.builder().isPrefix(true).withPrevKV(true).build(), serviceEventListener);

        var service = new ServiceJsonDataMapper(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user-service",
                "123.123.123.123",
                9090,
                "http",
                Instant.now().toString()
        );
        var service2 = new ServiceJsonDataMapper(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user-service",
                "123.123.123.124",
                9090,
                "http",
                Instant.now().toString()
        );
        var service3 = new ServiceJsonDataMapper(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user-service",
                "123.123.123.125",
                9090,
                "http",
                Instant.now().toString()
        );

        var os = new ByteArrayOutputStream(1024);

        dslJson.serialize(service, os);
        var instanceJson = os.toByteArray();

        os.reset();

        dslJson.serialize(service2, os);
        var instanceJson2 = os.toByteArray();

        os.reset();

        dslJson.serialize(service3, os);
        var instanceJson3 = os.toByteArray();
        var kv = etcdClient.getKVClient();
        var key = ByteSequence.from("/services/user-service/%s".formatted(service.serviceId).getBytes());
        var value = ByteSequence.from(instanceJson);
        kv.put(key, value).get();

        var key2 = ByteSequence.from("/services/user-service/%s".formatted(service2.serviceId).getBytes());
        var value2 = ByteSequence.from(instanceJson2);
        kv.put(key2, value2).get();

        var key3 = ByteSequence.from("/services/user-service/%s".formatted(service3.serviceId).getBytes());
        var value3 = ByteSequence.from(instanceJson3);
        kv.put(key3, value3).get();

        kv.delete(key3).get();
        kv.delete(key2).get();
        kv.delete(key).get();

        Thread.sleep(10_000);
        var result = nginxContainer.execInContainer(
                "cat",
                "/opt/nginx/nginx.conf"
        );

        String content = result.getStdout();

        assertFalse(
                content.contains("server 123.123.123.123:9090;"),
                "Container içindeki nginx.conf expected server entry içermiyor"
        );
        assertFalse(
                content.contains("server 123.123.123.124:9090;"),
                "Container içindeki nginx.conf expected server entry içeriyor"
        );
        assertFalse(
                content.contains("server 123.123.123.125:9090;"),
                "Container içindeki nginx.conf expected server entry içermiyor"
        );

        assertTrue(
                content.contains("server 127.0.0.1:65535;"),
                "Container içindeki nginx.conf expected server entry içermiyor"
        );
        watcher.close();
    }

}
