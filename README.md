# ThreadPool

Java로 구현한 간단한 쓰레드 풀

## 기능

- 고정 크기의 쓰레드 풀 생성
- 작업 큐를 통한 비동기 작업 실행
- 쓰레드 재사용으로 리소스 효율성 향상
- `shutdownNow()`를 통한 즉시 종료

## 사용 방법

```java
// 쓰레드 풀 생성 (크기: 10)
ThreadPool threadPool = new ThreadPool(10);

// 작업 실행
threadPool.run(() -> {
    System.out.println("Task executed");
});

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
