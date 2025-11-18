package threadpool;

import java.util.HashSet;
import java.util.Set;

public class ThreadPool {

    public Set<Thread> threads = new HashSet<>();
    public final TaskQueue taskQueue;

    public ThreadPool(int size, TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
        for (int i = 0; i < size; i++) {
            threads.add(new TaskThread(taskQueue));
        }
    }

    public void run() {
        for (Thread thread : this.threads) {
            thread.start();
        }
    }

    public int size() {
        return this.threads.size();
    }
}


