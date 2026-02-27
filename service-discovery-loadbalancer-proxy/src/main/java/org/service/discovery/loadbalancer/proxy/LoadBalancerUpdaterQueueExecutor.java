package org.service.discovery.loadbalancer.proxy;

import java.util.Queue;

public class LoadBalancerUpdaterQueueExecutor {
    private final Queue<UpdateCommand> updateCommandQueue;
    private final LoadBalancerUpdater loadBalancerUpdater;
    private final Runnable eventLoop;
    private Thread executorThread;

    public LoadBalancerUpdaterQueueExecutor(Queue<UpdateCommand> updateCommandQueue, LoadBalancerUpdater loadBalancerUpdater, Runnable eventLoop ) {
        this.updateCommandQueue = updateCommandQueue;
        this.loadBalancerUpdater = loadBalancerUpdater;
        this.eventLoop = eventLoop;
    }


    public void assignEventLoopToPlatformThread(){
        executorThread = Thread.ofPlatform().unstarted(eventLoop);
    }

    public void assignEventLoopToVirtualThread() {
        executorThread = Thread.ofVirtual().unstarted(eventLoop);
    }

    public void start(){
        executorThread.start();
    }

    public void stop() {
        executorThread.interrupt();
    }
}
