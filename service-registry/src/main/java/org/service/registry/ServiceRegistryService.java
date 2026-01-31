package org.service.registry;

public interface ServiceRegistryService {
    ServiceRegistered register(RegisterCommand registerCommand);
    ServiceDeregistered deregister(DeregisterCommand deregisterCommand);
    HeartBeatResponse heartBeat(HeartBeatCommand heartBeatCommand);
}
