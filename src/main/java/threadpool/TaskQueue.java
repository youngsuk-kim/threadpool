package threadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.currentThread;

public class TaskQueue {
    BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    public void putTask(Runnable runnable) {
        try {
            this.tasks.put(runnable);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Runnable getTask() {
        try {
            return this.tasks.take();
        } catch (InterruptedException e) {
            // 명시 목적 호출
            currentThread().interrupt();
            return null;
        }
    }

    public int size() {
        return this.tasks.size();
    }
}
