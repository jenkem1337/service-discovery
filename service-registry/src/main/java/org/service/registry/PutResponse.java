package org.service.registry;

public record PutResponse(String key, String value, long keyVersion,long revision, long leaseId) {
}
