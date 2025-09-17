package io.github.racoondog.superuser;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

public final class TickScheduler implements AutoCloseable {
    public static final TickScheduler INSTANCE = new TickScheduler();

    private final List<Task> queue = new ObjectArrayList<>();

    public TickScheduler() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void add(Runnable runnable, long delay) {
        synchronized (queue) {
            this.queue.add(new Task(delay, runnable));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        synchronized (queue) {
            this.queue.removeIf(Task::tick);
        }
    }

    @Override
    public void close() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    private static class Task {
        private final Runnable runnable;
        private long delay;

        public Task(long delay, Runnable runnable) {
            this.delay = delay;
            this.runnable = runnable;
        }

        private boolean tick() {
            if (this.delay-- <= 0) {
                this.runnable.run();
                return true;
            }

            return false;
        }
    }
}
