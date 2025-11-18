package threadpool;

import java.util.HashSet;
import java.util.Set;

public class ThreadPool {

    public Set<TaskThread> threads = new HashSet<>();
    public final TaskQueue taskQueue = new TaskQueue();

    public ThreadPool(int size) {
        for (int i = 0; i < size; i++) {
            threads.add(new TaskThread(taskQueue));
        }

        for (Thread thread : this.threads) {
            thread.start();
        }
    }

    public void run(Runnable runnable) {
        this.taskQueue.putTask(runnable);
    }

    public int size() {
        return this.threads.size();
    }

}


