package threadpool;

public class TaskThread extends Thread {

    private final TaskQueue taskQueue;

    public TaskThread(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    @Override
    public void run() {
        for (;;) {
            Runnable task = taskQueue.getTask();
            try {
                task.run();
            } finally {
                System.out.print(currentThread().getName() + "runs : ");
            }
        }
    }
}
