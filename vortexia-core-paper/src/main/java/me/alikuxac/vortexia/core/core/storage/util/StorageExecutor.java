// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.storage.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class StorageExecutor {

    private final ExecutorService pool;

    public StorageExecutor(int threads) {
        int size = Math.max(1, threads);
        this.pool = Executors.newFixedThreadPool(size, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Vortexia-Storage-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }

    public CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(task, pool);
    }

    public <T> CompletableFuture<T> supply(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, pool);
    }

    public void shutdown() {
        pool.shutdown();
    }
}