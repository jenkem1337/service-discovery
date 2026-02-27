package org.service.discovery.loadbalancer.proxy;

import io.etcd.jetcd.ByteSequence;

public sealed interface UpdateCommand permits
        UpdateCommand.PutCommand,
        UpdateCommand.DeleteCommand {
    record PutCommand(ByteSequence value) implements UpdateCommand{}
    record DeleteCommand(ByteSequence value) implements UpdateCommand{}
}
