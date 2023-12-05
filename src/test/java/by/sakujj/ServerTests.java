package by.sakujj;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
public class ServerTests {

    static Stream<Arguments> defaultSource() {
        return Stream.of(
                arguments(101, 50),
                arguments(102, 111),
                arguments(103, 350),
                arguments(100, 100),
                arguments(150, 100),
                arguments(17, 100),
                arguments(0, 100),
                arguments(99, 8)
        );
    }

    @ParameterizedTest
    @MethodSource("defaultSource")
    void shouldContainValuesFromOneToNInDataListAfterProcessing(int n, int threadCount) throws ExecutionException, InterruptedException {
        // given
        Server server = new Server(n);
        AtomicInteger integer = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<IntWrapper>> futures = new ArrayList<>(n);
        Callable<IntWrapper> task = () -> server.processRequest(new IntWrapper(integer.incrementAndGet()));

        // when
        IntStream
                .range(0, n)
                .forEach(i -> {
                    Future<IntWrapper> future = executorService.submit(task);
                    futures.add(future);
                });

        futures
                .forEach(f -> {
                    try {
                        f.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        // then
        List<Integer> expected = new ArrayList<>(n);
        IntStream.rangeClosed(1, n)
                .forEachOrdered(expected::add);
        int expectedSize = n;

        List<Integer> actual = server.getDataListCopy();

        assertThat(actual.size()).isEqualTo(expectedSize);

        Collections.sort(actual);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("defaultSource")
    void shouldReturnNonRepeatableResponsesFromOneToN(int n, int threadCount) throws ExecutionException, InterruptedException {
        // given
        Server server = new Server(n);
        AtomicInteger integer = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<IntWrapper>> futures = new ArrayList<>(n);

        Set<Integer> responsesSet = new TreeSet<>();
        IntStream.rangeClosed(1, n)
                .forEach(responsesSet::add);

        Callable<IntWrapper> task = () -> server.processRequest(new IntWrapper(integer.incrementAndGet()));

        // when
        IntStream
                .range(0, n)
                .forEach(i -> {
                    Future<IntWrapper> future = executorService.submit(task);
                    futures.add(future);
                });

        // then
        futures.forEach(f -> {
            try {
                int actualValue = f.get().getValue();

                assertThat(actualValue).isIn(responsesSet);
                responsesSet.remove(actualValue);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertThat(responsesSet).isEmpty();
    }
}
