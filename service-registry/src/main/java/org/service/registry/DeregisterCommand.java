package org.service.registry;

import java.util.UUID;

public record DeregisterCommand(UUID serviceId, String serviceName) {
}
