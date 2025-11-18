package threadpool;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadPoolTest {

    @Test
    void threadPoolCreated() {
        ThreadPool threadPool = new ThreadPool(10, new TaskQueue());

        assertEquals(10, threadPool.size());
    }

    @Test
    void threadPoolTaskExecuted() {
        TaskQueue taskQueue = new TaskQueue();
        ThreadPool threadPool = new ThreadPool(1, taskQueue);

        AtomicInteger executionCount = new AtomicInteger(0);

        taskQueue.putTask(executionCount::incrementAndGet);

        threadPool.run();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(1, executionCount.get(), "Task should be executed once");
    }
}