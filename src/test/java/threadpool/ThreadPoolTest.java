package threadpool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadPoolTest {

    @Test
    @DisplayName("쓰레드 풀 생성 테스트")
    void threadPoolCreated() {
        ThreadPool threadPool = new ThreadPool(10);

        assertEquals(10, threadPool.size());
    }

    @Test
    @DisplayName("쓰레드 풀 작업 실행 테스트")
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

    @Test
    @DisplayName("쓰레드 풀 쓰레드 재사용 테스트")
    void threadPoolReuseThreads() throws InterruptedException {
        // 쓰레드 풀 크기가 2일 때, 5개의 작업을 실행하면 쓰레드가 재사용되어야 함
        ThreadPool threadPool = new ThreadPool(2);
        AtomicInteger completedTasks = new AtomicInteger(0);
        AtomicInteger maxConcurrentTasks = new AtomicInteger(0);
        AtomicInteger currentConcurrentTasks = new AtomicInteger(0);

        int totalTasks = 5;

        for (int i = 0; i < totalTasks; i++) {
            threadPool.run(() -> {
                int current = currentConcurrentTasks.incrementAndGet();

                // 최대 동시 실행 작업 수 추적
                maxConcurrentTasks.updateAndGet(max -> Math.max(max, current));

                try {
                    /*
                    Thread sleep을 통해 바로 전 코드가 동시에 실행되도록 함,
                    코드를 실행하는 쓰레드를 강제로 여러개 만듬 thread sleep 했을때 -> B 쓰레드 실행 -> C 쓰레드 실행
                     */
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                currentConcurrentTasks.decrementAndGet();
                completedTasks.incrementAndGet();
            });
        }

        // 모든 작업이 완료될 때까지 대기
        Thread.sleep(500);

        assertEquals(totalTasks, completedTasks.get(), "All tasks should be completed");
        // 재사용 하기 때문에, 쓰레드 수가 Task 수를 초과하면 안됨
        assertEquals(2, maxConcurrentTasks.get(), "Max concurrent tasks should not exceed pool size");
    }

    @Test
    @DisplayName("쓰레드 풀 크기 유지 테스트")
    void threadPoolSize() throws InterruptedException {
        // 쓰레드 풀의 크기가 유지되는지 확인
        ThreadPool threadPool = new ThreadPool(3);

        assertEquals(3, threadPool.size(), "Thread pool size should be 3");

        // 여러 작업 실행 후에도 풀 크기가 유지되는지 확인
        for (int i = 0; i < 10; i++) {
            threadPool.run(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        Thread.sleep(200);

        assertEquals(3, threadPool.size(), "Thread pool size should remain 3 after executing tasks");
    }

    @Test
    @DisplayName("shutdownNow 호출 시 대기 중인 쓰레드 종료 테스트")
    void shutdownNowInterruptsWaitingThreads() throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(3);
        Thread.sleep(50);

        threadPool.shutdownNow();

        for (TaskThread thread : threadPool.threads) {
            thread.join(1000);
            assertEquals(Thread.State.TERMINATED, thread.getState());
        }
    }

    @Test
    @DisplayName("shutdownNow 호출 시 실행 중인 작업 중단 테스트")
    void shutdownNowInterruptsRunningTasks() throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(2);
        AtomicInteger interruptedCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            threadPool.run(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    interruptedCount.incrementAndGet();
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(50);
        threadPool.shutdownNow();

        for (TaskThread thread : threadPool.threads) {
            thread.join(1000);
        }

        assertEquals(2, interruptedCount.get());
    }

    @Test
    @DisplayName("shutdownNow 호출 시 큐에 대기 중인 작업 실행 안함 테스트")
    void shutdownNowSkipsQueuedTasks() throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(1);
        AtomicInteger executedCount = new AtomicInteger(0);

        threadPool.run(() -> {
            executedCount.incrementAndGet();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        for (int i = 0; i < 5; i++) {
            threadPool.run(executedCount::incrementAndGet);
        }

        Thread.sleep(50);
        threadPool.shutdownNow();

        for (TaskThread thread : threadPool.threads) {
            thread.join(1000);
        }

        assertEquals(1, executedCount.get());
    }

}

