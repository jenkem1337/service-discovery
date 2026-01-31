package org.service.registry;

import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryRepositoryImplTest {
    @Mock
    RegistryDataSource dataSource;

    ServiceRegistryRepository serviceRegistryRepository;

    @BeforeEach
    void setUp(){
        serviceRegistryRepository = new ServiceRegistryRepositoryImpl(dataSource);
    }

    @Test
    void registerService() {
        final long leaseId = 123456789;
        when(dataSource.put(any(String.class), any(byte[].class))).thenReturn(new PutResponse(leaseId));
        var response = serviceRegistryRepository.register(Service.of(
                Generators.timeBasedEpochGenerator().generate(),
                Generators.timeBasedEpochGenerator().generate(),
                Name.of("service-1"),
                IpAddress.of("123.123.123.123"),
                Port.of(3434),
                Protocol.from("http"),
                Instant.now()));
        assertEquals(leaseId, response.leaseId());
    }

    @Test
    void deregisterService() {
        doNothing().when(dataSource).delete(any(String.class));
        UUID serviceId = Generators.timeBasedEpochGenerator().generate();
        serviceRegistryRepository.deregister(serviceId,"user-service");
        verify(dataSource, times(1)).delete("/services/user-service/%s".formatted(serviceId.toString()));
    }

    @Test
    void whenSomethingGoesWrongCallingDeregister_ShouldThrowDataSourceException(){
        doThrow(DataSourceException.class).when(dataSource).delete(isA(String.class));

        assertThrows(DataSourceException.class, () -> {
            serviceRegistryRepository.deregister(UUID.randomUUID(), "lalalal");
        });
    }

    @Test
    void sendHeartBeatMessage(){
        doNothing().when(dataSource).renew(any(long.class));
        final long leaseId = 123456789;
        serviceRegistryRepository.heartBeat(leaseId);
        verify(dataSource, times(1)).renew(leaseId);
    }

    @Test
    void whenSomethingGoesWrongCallingHeartBeat_ShouldThrowDataSourceException(){
        doThrow(DataSourceException.class).when(dataSource).renew(isA(long.class));
        assertThrows(DataSourceException.class, () -> {
            final long leaseId = 123456789;
            serviceRegistryRepository.heartBeat(leaseId);
        });

    }
}