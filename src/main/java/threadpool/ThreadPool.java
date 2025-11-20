package threadpool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public void shutdownNow() {
        this.threads.forEach(Thread::interrupt);
    }

    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(10);
        threadPool.shutdownNow();
    }
}


