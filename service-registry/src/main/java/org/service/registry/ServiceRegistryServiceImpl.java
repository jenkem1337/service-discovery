package org.service.registry;

import com.fasterxml.uuid.Generators;

import java.time.Instant;

public class ServiceRegistryServiceImpl implements ServiceRegistryService{
    private final ServiceRegistryRepository serviceRegistryRepository;
    public ServiceRegistryServiceImpl(ServiceRegistryRepository serviceRegistryRepository) {
        this.serviceRegistryRepository = serviceRegistryRepository;
    }

    @Override
    public ServiceRegistered register(RegisterCommand registerCommand) {
        var service = Service.of(
                Generators.timeBasedEpochGenerator().generate(),
                registerCommand.serviceId(),
                Name.of(registerCommand.serviceName()),
                IpAddress.of(registerCommand.ipAddress()),
                Port.of(registerCommand.portNumber()),
                Protocol.from(registerCommand.protocol().toLowerCase()),
                Instant.now()
        );
        var response = serviceRegistryRepository.register(service);
        return new ServiceRegistered(response.leaseId());
    }

    @Override
    public ServiceDeregistered deregister(DeregisterCommand deregisterCommand) {
        serviceRegistryRepository.deregister(deregisterCommand.serviceId(), deregisterCommand.serviceName());
        return new ServiceDeregistered();
    }

    @Override
    public HeartBeatResponse heartBeat(HeartBeatCommand heartBeatCommand) {
        serviceRegistryRepository.heartBeat(heartBeatCommand.leaseId());
        return new HeartBeatResponse();
    }
}

