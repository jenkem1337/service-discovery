package org.service.registry;

import com.dslplatform.json.DslJson;
import com.fasterxml.uuid.Generators;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class RegistryServiceIntegrationTest {
    @Container
    GenericContainer etcdContainer = new GenericContainer<>(DockerImageName.parse("bitnami/etcd:latest"))
            .withExposedPorts(2379, 2380)
            .withEnv("ALLOW_NONE_AUTHENTICATION","yes")
            .withEnv("ETCD_ADVERTISE_CLIENT_URLS", "http://etcd-server:2379")
            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));

    RegistryDataSource etcdRegistry;
    ServiceRegistryService serviceRegistryService;
    Client etcdClient;
    @BeforeEach
    void setUp() {
        var ip = etcdContainer.getHost();
        var port = etcdContainer.getFirstMappedPort();
        var endpoint = "http://%s:%d".formatted(ip, port);
        etcdClient = Client.builder()
                .endpoints(endpoint)
                .build();
        etcdRegistry = new EtcdRegistryDataSource(etcdClient);
        ServiceRegistryRepository repository = new ServiceRegistryRepositoryImpl(etcdRegistry);
        serviceRegistryService = new ServiceRegistryServiceImpl(repository);
    }

    @Test
    void deregisterService(){
        var serviceId = Generators.timeBasedEpochGenerator().generate();
        String serviceName = "user-service";
        String ipAddress = "123.123.123.123";
        int port = 9090;
        String protocol = "http";

        var registerResponse = serviceRegistryService.register(new RegisterCommand(serviceId, serviceName, ipAddress, port, protocol));

        var key = "/services/%s/%s".formatted(serviceName, serviceId.toString());

        var deregisterResponse = serviceRegistryService.deregister(new DeregisterCommand(serviceId, serviceName));

        assertNotNull(deregisterResponse);
    }

    @Test
    void sendHeartBeatMessage(){
        var serviceId = Generators.timeBasedEpochGenerator().generate();
        String serviceName = "user-service";
        String ipAddress = "123.123.123.123";
        int port = 9090;
        String protocol = "http";
        var registerResponse = serviceRegistryService.register(new RegisterCommand(serviceId, serviceName, ipAddress, port, protocol));
        var heartbeatResponse = serviceRegistryService.heartBeat(new HeartBeatCommand(registerResponse.leaseId()));
        assertEquals(registerResponse.leaseId(), heartbeatResponse.leaseId());
    }

    @Test
    void registerAfterReadJsonData() {
        var serviceId = Generators.timeBasedEpochGenerator().generate();
        String serviceName = "user-service";
        String ipAddress = "123.123.123.123";
        int port = 9090;
        String protocol = "HTTP";
        var registerResponse = serviceRegistryService.register(new RegisterCommand(serviceId, serviceName, ipAddress, port, protocol));

        try {
            var getResponse = etcdClient.getKVClient().get(ByteSequence.from("/services/user-service/".getBytes()), GetOption.builder().isPrefix(true).build()).get();
            var json = getResponse.getKvs().getFirst().getValue().toString(StandardCharsets.UTF_8).getBytes();
            var dslJson = new DslJson<>();
            var serviceJsonPOJO =  dslJson.deserialize(ServiceJsonDataMapper.class, json, json.length);
            assertEquals(serviceId.toString(), serviceJsonPOJO.serviceId);
            assertEquals(ipAddress, serviceJsonPOJO.ip);
            assertEquals(port, serviceJsonPOJO.port);
            assertEquals(serviceName, serviceJsonPOJO.serviceName);
            assertEquals(protocol, serviceJsonPOJO.protocol);
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void whenServiceDeregistered_ShouldDetectDDeletedService() throws InterruptedException {
        var cdl = new CountDownLatch(1);
        var atomicDataHolder = new AtomicReference<String>(null);
        var serviceId = Generators.timeBasedEpochGenerator().generate();
        var key = "/services/user-service/%s".formatted(serviceId.toString());

        Consumer<WatchResponse> watchCallback = (watchResponse) -> {
            var events = watchResponse.getEvents();
            for(WatchEvent event : events) {
                if(event.getEventType().equals(WatchEvent.EventType.DELETE)){
                    var prevKV = event.getPrevKV();
                    if(prevKV != null && prevKV.getKey().toString().equals(key)){
                        atomicDataHolder.set(key);
                        cdl.countDown();
                    }
                }
            }
        };

        org.service.registry.WatchResponse<Watch.Watcher> watchResponse = serviceRegistryService.watch(new WatchRequest(key, watchCallback));

        String serviceName = "user-service";
        String ipAddress = "123.123.123.123";
        int port = 9090;
        String protocol = "HTTP";

        serviceRegistryService.register(new RegisterCommand(serviceId, serviceName, ipAddress, port, protocol));
        serviceRegistryService.deregister(new DeregisterCommand(serviceId, serviceName));

        cdl.await();
        watchResponse.watchResponse().close();
        assertEquals(key, atomicDataHolder.get());
    }

    @Test
    void whenServiceLeaseEnd_ShouldDetectDDeletedService() throws InterruptedException {
        var cdl = new CountDownLatch(1);
        var atomicDataHolder = new AtomicReference<String>(null);
        var serviceId = Generators.timeBasedEpochGenerator().generate();
        var key = "/services/user-service/%s".formatted(serviceId.toString());

        Consumer<WatchResponse> watchCallback = (watchResponse) -> {
            var events = watchResponse.getEvents();
            for(WatchEvent event : events) {
                if(event.getEventType().equals(WatchEvent.EventType.DELETE)){
                    var prevKV = event.getPrevKV();
                    if(prevKV != null && prevKV.getKey().toString().equals(key)){
                        atomicDataHolder.set(key);
                        cdl.countDown();
                    }
                }
            }
        };

        org.service.registry.WatchResponse<Watch.Watcher> watchResponse = serviceRegistryService.watch(new WatchRequest(key, watchCallback));

        String serviceName = "user-service";
        String ipAddress = "123.123.123.123";
        int port = 9090;
        String protocol = "HTTP";

        serviceRegistryService.register(new RegisterCommand(serviceId, serviceName, ipAddress, port, protocol));


        cdl.await();
        watchResponse.watchResponse().close();
        assertEquals(key, atomicDataHolder.get());
    }

    @Test
    void whenServiceLeaseEnd_ShouldDetectDDeletedServiceAndRegister() throws InterruptedException {
        var cdl = new CountDownLatch(1);
        var leaseHolder = new AtomicLong();
        var serviceAtomicHolder = new AtomicReference<Service>();
        var serviceId = Generators.timeBasedEpochGenerator().generate();
        var key = "/services/user-service/%s".formatted(serviceId.toString());
        var dispatchThread = Executors.newSingleThreadExecutor();
        Consumer<WatchResponse> watchCallback = (watchResponse) -> {
            var events = watchResponse.getEvents();
            for(WatchEvent event : events) {
                if(event.getEventType().equals(WatchEvent.EventType.DELETE)){
                    var prevKV = event.getPrevKV();
                    if(prevKV != null && prevKV.getKey().toString().equals(key)){
                        dispatchThread.execute(() -> {
                            var oldService = serviceAtomicHolder.get();

                            var registerResponse = serviceRegistryService.register(new RegisterCommand(oldService.serviceId(), oldService.serviceName().name(), oldService.ipAddress().ip(), oldService.portNumber().portNumber(), oldService.protocol().name()));

                            serviceAtomicHolder.compareAndSet(oldService, registerResponse.service());

                            leaseHolder.set(registerResponse.leaseId());

                            cdl.countDown();

                        });
                    }
                }
            }
        };

        org.service.registry.WatchResponse<Watch.Watcher> watchResponse = serviceRegistryService.watch(new WatchRequest(key, watchCallback));

        String serviceName = "user-service";
        String ipAddress = "123.123.123.123";
        int port = 9090;
        String protocol = "HTTP";

        var registerResponse = serviceRegistryService.register(new RegisterCommand(serviceId, serviceName, ipAddress, port, protocol));
        serviceAtomicHolder.set(registerResponse.service());
        cdl.await();
        watchResponse.watchResponse().close();
        assertInstanceOf(Long.class, leaseHolder.get());
        assertNotEquals(registerResponse.leaseId(), leaseHolder.get());
        assertEquals(serviceId.toString(), serviceAtomicHolder.get().serviceId().toString());
    }


}
