package org.service.discovery.loadbalancer.proxy;

public interface LoadBalancerUpdater {
    void onUpdateCommand(UpdateCommand updateCommand);
}
