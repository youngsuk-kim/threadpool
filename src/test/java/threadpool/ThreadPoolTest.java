package threadpool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadPoolTest {

    @Test
    @DisplayName("쓰레드 풀 작업 실행 테스트")
    void threadPoolTaskExecuted() throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(1);
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(1);

        threadPool.submit(() -> {
            executionCount.incrementAndGet();
            completionLatch.countDown();
        });

        completionLatch.await();
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

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(2); // 2개 스레드 준비 확인
        CountDownLatch completionLatch = new CountDownLatch(5); // 5개 작업 완료 확인
        int totalTasks = 5;

        for (int i = 0; i < totalTasks; i++) {
            threadPool.submit(() -> {
                int current = currentConcurrentTasks.incrementAndGet();

                // 최대 동시 실행 작업 수 추적
                maxConcurrentTasks.updateAndGet(max -> Math.max(max, current));

                // 처음 2개 스레드가 준비되었음을 알림
                if (readyLatch.getCount() > 0) {
                    readyLatch.countDown();
                }

                try {
                    // 모든 스레드가 준비될 때까지 대기
                    startLatch.await();
                    Thread.sleep(50); // 동시 실행 확인을 위한 최소한의 sleep
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                currentConcurrentTasks.decrementAndGet();
                completedTasks.incrementAndGet();
                completionLatch.countDown();
            });
        }

        // 2개 스레드가 작업을 받고 블로킹될 때까지 대기
        readyLatch.await();

        // 스레드 수 확인 (corePoolSize만큼만 생성되어야 함)
        assertEquals(2, threadPool.size(), "Thread pool should have exactly 2 threads");

        // 모든 작업 시작
        startLatch.countDown();

        // 모든 작업 완료 대기
        completionLatch.await();

        assertEquals(totalTasks, completedTasks.get(), "All tasks should be completed");
        // 재사용 하기 때문에, 쓰레드 수가 Task 수를 초과하면 안됨
        assertEquals(2, maxConcurrentTasks.get(), "Max concurrent tasks should not exceed pool size");
    }

    @Test
    @DisplayName("shutdownNow 호출 시 대기 중인 쓰레드 종료 테스트")
    void shutdownNowInterruptsWaitingThreads() throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(3);
        CountDownLatch readyLatch = new CountDownLatch(3);
        CountDownLatch blockLatch = new CountDownLatch(1);

        // 3개의 작업을 제출하여 스레드 생성
        for (int i = 0; i < 3; i++) {
            threadPool.submit(() -> {
                readyLatch.countDown();
                try {
                    blockLatch.await(); // shutdown까지 블로킹
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 모든 스레드가 준비될 때까지 대기
        readyLatch.await();

        threadPool.shutdownNow();

        for (TaskThread thread : threadPool.getThreads()) {
            thread.join(1000);
            assertEquals(Thread.State.TERMINATED, thread.getState());
        }
    }

    @Test
    @DisplayName("shutdownNow 호출 시 실행 중인 작업 중단 테스트")
    void shutdownNowInterruptsRunningTasks() throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(2);
        AtomicInteger interruptedCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch blockLatch = new CountDownLatch(1);

        for (int i = 0; i < 2; i++) {
            threadPool.submit(() -> {
                readyLatch.countDown();
                try {
                    blockLatch.await();
                } catch (InterruptedException e) {
                    interruptedCount.incrementAndGet();
                    Thread.currentThread().interrupt();
                }
            });
        }

        readyLatch.await();
        threadPool.shutdownNow();

        for (TaskThread thread : threadPool.getThreads()) {
            thread.join(1000);
        }

        assertEquals(2, interruptedCount.get());
    }

    @Test
    @DisplayName("shutdownNow 호출 시 큐에 대기 중인 작업 실행 안함 테스트")
    void shutdownNowSkipsQueuedTasks() throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(1);
        AtomicInteger executedCount = new AtomicInteger(0);
        CountDownLatch taskStartedLatch = new CountDownLatch(1);
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        // 첫 번째 작업: 스레드를 블로킹하여 큐에 다른 작업들이 쌓이게 함
        threadPool.submit(() -> {
            executedCount.incrementAndGet();
            taskStartedLatch.countDown(); // 작업 시작 알림
            try {
                shutdownLatch.await(); // shutdown까지 블로킹
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 첫 번째 작업이 시작될 때까지 대기
        taskStartedLatch.await();

        // 나머지 5개 작업은 큐에 쌓임
        for (int i = 0; i < 5; i++) {
            threadPool.submit(executedCount::incrementAndGet);
        }

        // shutdown 호출
        threadPool.shutdownNow();
        shutdownLatch.countDown();

        // 스레드 종료 대기
        for (TaskThread thread : threadPool.getThreads()) {
            thread.join(1000);
        }

        // 큐에 쌓인 5개 작업은 실행되지 않아야 함 (첫 번째 작업만 실행됨)
        assertEquals(1, executedCount.get());
    }

    @Test
    @DisplayName("큐가 가득 차면 스레드 동적으로 증가 테스트")
    void threadPoolDynamicallyIncreasesThreads() throws InterruptedException {
        // corePoolSize=2, maxPoolSize=5, maxQueueSize=3
        ThreadPool threadPool = new ThreadPool(2, 5, 3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch finishLatch = new CountDownLatch(6);

        // 6개의 작업 제출 (큐 3개 채우고 스레드 1개 추가 생성)
        for (int i = 0; i < 6; i++) {
            threadPool.submit(() -> {
                if (readyLatch.getCount() > 0) {
                    readyLatch.countDown();
                }
                try {
                    startLatch.await(); // 모든 작업이 동시에 시작되도록 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // 최소 2개 스레드가 준비될 때까지 대기
        readyLatch.await();

        assertTrue(threadPool.size() >= 2, "Thread pool should have at least 2 threads");
        assertTrue(threadPool.size() <= 5, "Thread pool should not exceed max size");

        startLatch.countDown(); // 모든 작업 시작
        finishLatch.await(); // 모든 작업 완료 대기
    }

    @Test
    @DisplayName("큐 가득 차고 최대 스레드 수 도달 시 예외 발생 테스트")
    void threadPoolRejectsTasksWhenExhausted() throws InterruptedException {
        // corePoolSize=1, maxPoolSize=2, maxQueueSize=2
        ThreadPool threadPool = new ThreadPool(1, 2, 2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(1);

        // 첫 번째 작업: 스레드 1 블로킹
        threadPool.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 첫 번째 스레드가 준비될 때까지 대기
        readyLatch.await();

        // 2, 3번째 작업: 큐를 가득 채움 (maxQueueSize=2)
        threadPool.submit(() -> {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        threadPool.submit(() -> {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 4번째 작업: 큐가 가득 찼으므로 스레드 2 생성
        threadPool.submit(() -> {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // maxPoolSize 도달 확인
        assertEquals(2, threadPool.size());

        // 5번째 작업은 예외 발생해야 함 (스레드 2개, 큐 2개 = 총 4개 이미 찼음)
        assertThrows(RejectedExecutionException.class, () -> {
            threadPool.submit(() -> {});
        }, "Should throw RejectedExecutionException when pool is exhausted");

        startLatch.countDown(); // 블로킹 해제
    }

    @Test
    @DisplayName("동시 다발적인 작업 제출 시 스레드 수 제한 테스트 (동시성)")
    void concurrentSubmitDoesNotExceedMaxPoolSize() throws InterruptedException {
        // corePoolSize=2, maxPoolSize=10, maxQueueSize=5
        ThreadPool threadPool = new ThreadPool(2, 10, 5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch submitLatch = new CountDownLatch(20);
        CountDownLatch completeLatch = new CountDownLatch(20);
        AtomicInteger maxThreadCount = new AtomicInteger(0);

        // 20개의 스레드가 동시에 작업을 제출
        for (int i = 0; i < 20; i++) {
            new Thread(() -> {
                try {
                    submitLatch.countDown();
                    submitLatch.await(); // 모든 스레드가 준비될 때까지 대기

                    threadPool.submit(() -> {
                        int currentSize = threadPool.size();
                        maxThreadCount.updateAndGet(max -> Math.max(max, currentSize));
                        try {
                            startLatch.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } catch (Exception e) {
                    // RejectedExecutionException 무시
                } finally {
                    completeLatch.countDown();
                }
            }).start();
        }

        // 모든 제출 완료 대기
        completeLatch.await();

        int finalThreadCount = threadPool.size();
        assertTrue(finalThreadCount <= 10,
            "Thread count should not exceed maxPoolSize (10), but was: " + finalThreadCount);

        startLatch.countDown(); // 모든 작업 해제
    }
}

