package org.service.discovery.loadbalancer.proxy;

import java.util.Queue;

public class LoadBalancerUpdaterQueueExecutor {
    private final Queue<UpdateCommand> updateCommandQueue;
    private final LoadBalancerUpdater loadBalancerUpdater;
    private final UpdaterQueueCallback eventLoop;
    private Thread executorThread;
    private volatile boolean isRunning = true;

    public LoadBalancerUpdaterQueueExecutor(Queue<UpdateCommand> updateCommandQueue, LoadBalancerUpdater loadBalancerUpdater, UpdaterQueueCallback eventLoop ) {
        this.updateCommandQueue = updateCommandQueue;
        this.loadBalancerUpdater = loadBalancerUpdater;
        this.eventLoop = eventLoop;
    }


    public void assignEventLoopToPlatformThread(){
        executorThread = Thread.ofPlatform().unstarted(() -> {
            while(isRunning){
                try {
                    eventLoop.apply(updateCommandQueue, loadBalancerUpdater);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void assignEventLoopToVirtualThread() {
        executorThread = Thread.ofVirtual().unstarted(() -> {
            while(isRunning){
                try {
                    eventLoop.apply(updateCommandQueue, loadBalancerUpdater);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void start(){
        executorThread.start();
    }

    public void stop() {
        isRunning = false;
        executorThread.interrupt();
    }
}
