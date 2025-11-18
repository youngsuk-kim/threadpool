package threadpool;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadPoolTest {

    @Test
    void threadPoolCreated() {
        ThreadPool threadPool = new ThreadPool(10);

        assertEquals(10, threadPool.size());
    }

    @Test
    void threadPoolTaskExecuted() {
        ThreadPool threadPool = new ThreadPool(1);
        AtomicInteger executionCount = new AtomicInteger(0);

        threadPool.run(executionCount::incrementAndGet);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(1, executionCount.get(), "Task should be executed once");
    }

}

