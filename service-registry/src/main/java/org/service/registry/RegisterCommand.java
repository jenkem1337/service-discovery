package org.service.registry;

import java.time.Instant;
import java.util.UUID;

public record RegisterCommand(
        UUID serviceId,
        String serviceName,
        String ipAddress,
        int portNumber,
        String protocol
) {
}
