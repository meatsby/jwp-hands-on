package concurrency.stage0;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 스레드 풀은 무엇이고 어떻게 동작할까? 테스트를 통과시키고 왜 해당 결과가 나왔는지 생각해보자.
 * <p>
 * Thread Pools https://docs.oracle.com/javase/tutorial/essential/concurrency/pools.html
 * <p>
 * Introduction to Thread Pools in Java https://www.baeldung.com/thread-pool-java-and-guava
 */
class ThreadPoolsTest {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolsTest.class);

    @Test
    void testNewFixedThreadPool() {
        final var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        // ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        executor.submit(logWithSleep("hello fixed thread pools"));
        executor.submit(logWithSleep("hello fixed thread pools"));
        executor.submit(logWithSleep("hello fixed thread pools"));

        // 올바른 값으로 바꿔서 테스트를 통과시키자.
        final int expectedPoolSize = 2; // 스레드풀을 2개로 제한함
        final int expectedQueueSize = 1; // 3개의 명령을 submit 했으니 2개가 작업을 수행하고 하나는 큐에 쌓임

        assertThat(expectedPoolSize).isEqualTo(executor.getPoolSize());
        assertThat(expectedQueueSize).isEqualTo(executor.getQueue().size());
    }

    @Test
    void testNewCachedThreadPool() {
        final var executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        // ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
        executor.submit(logWithSleep("hello cached thread pools"));
        executor.submit(logWithSleep("hello cached thread pools"));
        executor.submit(logWithSleep("hello cached thread pools"));

        // 올바른 값으로 바꿔서 테스트를 통과시키자.
        final int expectedPoolSize = 3; // 스레드풀이 Integer.MAX_VALUE 만큼 있음
        final int expectedQueueSize = 0; // 스레드풀에서 필요한만큼 뽑아다 쓰기 때문에 큐에 쌓이지 않음

        assertThat(expectedPoolSize).isEqualTo(executor.getPoolSize());
        assertThat(expectedQueueSize).isEqualTo(executor.getQueue().size());
    }

    private Runnable logWithSleep(final String message) {
        return () -> {
            try {
                log.info("start");
                Thread.sleep(1000 * 10); // 10초 대기
                log.info("end"); // 그 전에 메인 스레드가 종료
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info(message);
        };
    }
}
