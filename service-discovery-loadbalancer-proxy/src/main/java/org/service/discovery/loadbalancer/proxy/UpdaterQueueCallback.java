package org.service.discovery.loadbalancer.proxy;

import java.util.Queue;

@FunctionalInterface
public interface UpdaterQueueCallback {
    void apply(Queue<UpdateCommand> queue, LoadBalancerUpdater loadBalancerUpdater) throws InterruptedException;
}
