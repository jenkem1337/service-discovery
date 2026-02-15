package com.example.serviceregistryexample.application;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import org.service.registry.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class ServiceRegistryConfiguration {
    @Value("${etcd.endpoint}")
    String etcdEndpoint;
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    public Client etcdClient(){
        return Client.builder()
                .endpoints(etcdEndpoint)
                .build();
    }
    @Bean
    public RegistryDataSource etcdRegistryDataSource(Client etcdClient){
        return new EtcdRegistryDataSource(etcdClient);
    }

    @Bean
    public ServiceRegistryRepository serviceRegistryRepository(RegistryDataSource registryDataSource) {
        return new ServiceRegistryRepositoryImpl(registryDataSource);
    }

    @Bean
    public ServiceRegistryService serviceRegistryService(ServiceRegistryRepository serviceRegistryRepository) {
        return new ServiceRegistryServiceImpl(serviceRegistryRepository);
    }
    @Bean
    public AtomicReference<Service> serviceReference(){
        return new AtomicReference<>(null);
    }
}
