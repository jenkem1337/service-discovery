package org.service.registry;

public record RenewResponse(long leaseId, long ttl, long revision) {
}
