package threadpool;

public class TaskThread extends Thread {

    private final TaskQueue taskQueue;

    public TaskThread(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    // 메소드 실행 끝날때 스레드 종료
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Runnable task = taskQueue.getTask();
            if (task == null) {
                // interrupt로 인해 null이 반환됨 - 종료
                break;
            }
            task.run();
        }
    }
}
