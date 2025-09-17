package io.github.racoondog.superuser;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public final class OffthreadScheduler implements AutoCloseable {
    public static final OffthreadScheduler INSTANCE = new OffthreadScheduler();

    private final DelayQueue<Task> queue = new DelayQueue<>();
    private final Thread runner;
    private volatile boolean stopping = false;

    public OffthreadScheduler() {
        Thread runnerThread = new Thread(this::run);
        runnerThread.setName("Offthread Scheduler");
        runnerThread.setDaemon(true);
        runnerThread.start();

        this.runner = runnerThread;
    }

    public void add(Runnable runnable, long time) {
        this.queue.add(new Task(time, runnable));
    }

    private void run() {
        try {
            while (true) {
                if (this.stopping) return;
                Task next = this.queue.take();
                long scheduled = next.stamp();

                while (scheduled - 100 > System.currentTimeMillis()) {
                    //noinspection BusyWait
                    Thread.sleep(50);
                }

                while (scheduled > System.currentTimeMillis()) {
                    Thread.yield();
                }

                while (next != null && next.stamp() <= System.currentTimeMillis()) {
                    next.runnable().run();
                    next = this.queue.poll();
                }
            }
        } catch (InterruptedException ignored) {
            // scheduler closed
        }
    }

    @Override
    public void close() {
        this.stopping = true;
    }

    public void closeBlocking() throws InterruptedException {
        close();
        this.runner.join();
    }

    private record Task(long stamp, Runnable runnable) implements Delayed {
        @Override
        public int compareTo(@NotNull Delayed o) {
            if (o instanceof Task task) return Long.compare(stamp(), task.stamp());
            return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            long delayMillis = System.currentTimeMillis() - stamp();
            return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
        }
    }
}
