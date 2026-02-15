package com.example.serviceregistryexample.application;

import com.fasterxml.uuid.Generators;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import org.service.registry.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
public class ServiceRegistryLifecycle {
    private final ServiceRegistryService registryService;
    private final AtomicReference<Service> serviceReference;
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;
    private final ApplicationContext context;

    private ScheduledFuture<?> heartbeatFuture;

    @Value("${service.name}")
    private String serviceName;

    public ServiceRegistryLifecycle(ServiceRegistryService registryService, AtomicReference<Service> serviceReference, ThreadPoolTaskScheduler threadPoolTaskScheduler, ApplicationContext context) {
        this.registryService = registryService;
        this.serviceReference = serviceReference;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.context = context;

        this.threadPoolTaskScheduler.setPoolSize(2);
        this.threadPoolTaskScheduler.setThreadNamePrefix("service-registry-");
        this.threadPoolTaskScheduler.initialize();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() throws UnknownHostException {
        var serviceId = Generators.timeBasedEpochGenerator().generate();
        var key = "/services/%s/%s".formatted(serviceName,serviceId.toString());

        Consumer<WatchResponse> watchCallback = (watchResponse) -> {
            var events = watchResponse.getEvents();
            for(WatchEvent event : events) {
                if(event.getEventType().equals(WatchEvent.EventType.DELETE)){
                    var prevKV = event.getPrevKV();
                    if(prevKV != null && prevKV.getKey().toString().equals(key)){
                        threadPoolTaskScheduler.execute(() -> {
                            var oldService = serviceReference.get();

                            var registerResponse = registryService.register(new RegisterCommand(oldService.serviceId(), oldService.serviceName().name(), oldService.ipAddress().ip(), oldService.portNumber().portNumber(), oldService.protocol().name()));

                            serviceReference.compareAndSet(oldService, registerResponse.service());

                        });
                    }
                }
            }
        };


        registryService.watch(new WatchRequest(key, watchCallback));
        int port = Objects.requireNonNull(((WebServerApplicationContext) context)
                        .getWebServer())
                .getPort();

        String ip = InetAddress.getLocalHost().getHostAddress();
        var response = registryService.register(
                new RegisterCommand(
                        serviceId,
                        serviceName,
                        ip,
                        port,
                        "http"
                    )
                );
        serviceReference.compareAndSet(null,response.service());
        heartbeatFuture = threadPoolTaskScheduler.scheduleAtFixedRate(() -> registryService.heartBeat(new HeartBeatCommand(response.leaseId())), Duration.ofSeconds(5));
    }
    @PreDestroy
    void onPreDestroy(){
        var service = serviceReference.get();
        heartbeatFuture.cancel(true);
        registryService.deregister(new DeregisterCommand(service.serviceId(), service.serviceName().name()));
        threadPoolTaskScheduler.shutdown();
    }
}
