# ThreadPool

Java로 구현한 동적 쓰레드 풀

## 기능

- 동적 쓰레드 생성 (작업 제출 시점에 필요한 만큼만 생성)
- 작업 큐를 통한 비동기 작업 실행
- 쓰레드 재사용으로 리소스 효율성 향상
- 큐가 가득 차면 maxPoolSize까지 스레드 동적 확장
- Double-Checked Locking을 사용한 동시성 제어
- `shutdownNow()`를 통한 즉시 종료

## 사용 방법

```java
// 쓰레드 풀 생성 (corePoolSize: 5, maxPoolSize: 10, maxQueueSize: 100)
ThreadPool threadPool = new ThreadPool(5);

// 작업 실행
threadPool.submit(() -> {
    System.out.println("Task executed");
});

// 커스텀 설정
ThreadPool customPool = new ThreadPool(2, 5, 10);

// 종료
threadPool.shutdownNow();
```

## 빌드 & 테스트

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test
```
