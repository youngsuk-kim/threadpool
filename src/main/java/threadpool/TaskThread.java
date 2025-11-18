package threadpool;

public class TaskThread extends Thread {

    private final TaskQueue taskQueue;

    public TaskThread(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    @Override
    public void run() {
        for (; ; ) {
            Runnable task = taskQueue.getTask();
            task.run();
        }
    }
}
