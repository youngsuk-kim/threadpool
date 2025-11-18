package threadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
            throw new RuntimeException(e);
        }
    }

}
