package threadpool;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool {

    private static final int DEFAULT_MAX_POOL_SIZE_MULTIPLIER = 2;
    private static final int DEFAULT_MAX_QUEUE_SIZE = 100;

    private final int corePoolSize;
    private final int maxPoolSize;
    private final int maxQueueSize;

    private final Set<TaskThread> threads = ConcurrentHashMap.newKeySet();
    private final TaskQueue taskQueue = new TaskQueue();
    private final ReentrantLock lock = new ReentrantLock();

    public ThreadPool(int corePoolSize, int maxPoolSize, int maxQueueSize) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.maxQueueSize = maxQueueSize;
    }

    public ThreadPool(int corePoolSize) {
        this(corePoolSize, corePoolSize * DEFAULT_MAX_POOL_SIZE_MULTIPLIER, DEFAULT_MAX_QUEUE_SIZE);
    }

    /**
     * 작업을 스레드 풀에 제출합니다.
     * <p>
     * 다음 순서로 처리됩니다:
     * <ol>
     *   <li>현재 스레드 수가 corePoolSize보다 작으면 새 스레드를 생성하여 작업을 할당합니다.</li>
     *   <li>corePoolSize에 도달했으면 작업 큐에 작업을 추가합니다.</li>
     *   <li>큐가 가득 차고 스레드 수가 maxPoolSize보다 작으면 추가 스레드를 생성합니다.</li>
     *   <li>스레드 풀과 큐가 모두 가득 차면 RejectedExecutionException을 발생시킵니다.</li>
     * </ol>
     *
     * @param runnable 실행할 작업
     * @throws RejectedExecutionException 스레드 풀과 큐가 모두 가득 찬 경우
     */
    public void submit(Runnable runnable) {
        if (tryAddThreadIfBelowLimit(corePoolSize, runnable)) {
            return;
        }

        if (tryAddToQueue(runnable)) {
            return;
        }

        if (tryAddThreadIfBelowLimit(maxPoolSize, runnable)) {
            return;
        }

        throw new RejectedExecutionException("ThreadPool exhausted: queue full and max threads reached");
    }

    private boolean tryAddThreadIfBelowLimit(int limit, Runnable runnable) {
        lock.lock();
        try {
            if (threads.size() < limit) {
                createAndStartThread(runnable);
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    private void createAndStartThread(Runnable runnable) {
        TaskThread taskThread = new TaskThread(taskQueue);
        threads.add(taskThread);
        taskThread.start();
        taskQueue.putTask(runnable);
    }

    private boolean tryAddToQueue(Runnable runnable) {
        if (taskQueue.size() < maxQueueSize) {
            taskQueue.putTask(runnable);
            return true;
        }
        return false;
    }

    public int size() {
        return this.threads.size();
    }

    public void shutdownNow() {
        threads.forEach(Thread::interrupt);
    }

    public Set<TaskThread> getThreads() {
        return this.threads;
    }

}


