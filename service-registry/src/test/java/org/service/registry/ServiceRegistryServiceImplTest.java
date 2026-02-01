package org.service.registry;

import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceRegistryServiceImplTest {

    @Mock
    ServiceRegistryRepository serviceRegistryRepository;

    ServiceRegistryService serviceRegistryService;

    @BeforeEach
    void setUp() {
        serviceRegistryService = new ServiceRegistryServiceImpl(serviceRegistryRepository);
    }

    @Test
    void saveServiceObject(){
        var command = new RegisterCommand(Generators.timeBasedEpochGenerator().generate(),"service-1","123.123.123.123",3434,"http");

        when(serviceRegistryRepository.register(any(Service.class))).thenReturn(new PutResponse("hello", "world", 1,1, 12341213));

        var response = serviceRegistryService.register(command);

        assertEquals(12341213, response.leaseId());
    }

    @Test
    void deregisterService(){
        var serviceId = Generators.timeBasedEpochGenerator().generate();
        var serviceName = "user-service";
        var key = "/services/%s/%s".formatted(serviceName, serviceId.toString());
        when(serviceRegistryRepository.deregister(any(UUID.class), any(String.class))).thenReturn(new DeleteResponse(key,1));
        serviceRegistryService.deregister(new DeregisterCommand(serviceId, serviceName));
        verify(serviceRegistryRepository, times(1)).deregister(serviceId, serviceName);
    }

    @Test
    void sendHeartBeatMessage(){
        final long leaseId = 123456789;
        when(serviceRegistryRepository.heartBeat(any(long.class))).thenReturn(new RenewResponse(leaseId, 10, 2));
        serviceRegistryService.heartBeat(new HeartBeatCommand(leaseId));
        verify(serviceRegistryRepository, times(1)).heartBeat(leaseId);
    }

    @Test
    void firstRegisterServiceAfterSendHeartBeatMessage() {
        var registerCommand = new RegisterCommand(Generators.timeBasedEpochGenerator().generate(),"service-1","123.123.123.123",3434,"http");
        final long leaseId = 123456789;
        when(serviceRegistryRepository.register(any(Service.class))).thenReturn(new PutResponse("hello", "world", 1,1, leaseId));
        var serviceRegisteredResponse = serviceRegistryService.register(registerCommand);
        assertEquals(leaseId, serviceRegisteredResponse.leaseId());

        when(serviceRegistryRepository.heartBeat(any(long.class))).thenReturn(new RenewResponse(leaseId, 10, 2));
        serviceRegistryService.heartBeat(new HeartBeatCommand(serviceRegisteredResponse.leaseId()));
        verify(serviceRegistryRepository, times(1)).heartBeat(serviceRegisteredResponse.leaseId());

    }
}
