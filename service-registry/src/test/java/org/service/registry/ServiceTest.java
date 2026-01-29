package org.service.registry;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.UUIDClock;
import com.fasterxml.uuid.UUIDGenerator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ServiceTest {
    @Test
    void createServiceObject() {
        var service = Service.withoutHeartbeatMessage(
                Generators.timeBasedEpochGenerator().generate(),
                Generators.timeBasedEpochGenerator().generate(),
                Name.of("service-1"),
                IpAddress.of("123.123.123.123"),
                Port.of(3434),
                Protocol.HTTP,
                Instant.now());
        assertNotNull(service);
    }
}
