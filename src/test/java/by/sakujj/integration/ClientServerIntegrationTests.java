package by.sakujj.integration;

import by.sakujj.Client;
import by.sakujj.Server;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ClientServerIntegrationTests {

    static Stream<Arguments> defaultSource() {
        return Stream.of(
                arguments(101, 50),
                arguments(150, 100),
                arguments(100, 100),
                arguments(102, 111),
                arguments(103, 350),
                arguments(17, 100),
                arguments(0, 100),
                arguments(99, 8)
        );
    }

    @ParameterizedTest
    @MethodSource("defaultSource")
    void clientShouldCorrectlySendAllDataListToServer(int n, int threadCount) {
        // given
        Server server = new Server(n);
        Client client = new Client(n, threadCount, server);

        int expectedClientSize = 0;
        int expectedServerSize = n;

        // when
        client.sendDataToServer();

        // then
        int actualClientSize = client.getSize();
        assertThat(actualClientSize).isEqualTo(expectedClientSize);

        int actualServerSize = server.getSize();
        assertThat(actualServerSize).isEqualTo(expectedServerSize);

        List<Integer> expected = new ArrayList<>(n);
        IntStream.rangeClosed(1, n)
                .forEachOrdered(expected::add);
        List<Integer> actual = server.getDataListCopy();

        Collections.sort(actual);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("defaultSource")
    void clientShouldCorrectlyAccumulateCorrectServerResponses(int n, int threadCount) {
        // given
        Server server = new Server(n);
        Client client = new Client(n, threadCount, server);
        long expected = (1L + n) * n / 2L;

        // when
        client.sendDataToServer();
        long actual = client.getAccumulator();

        // then
        assertThat(actual).isEqualTo(expected);
    }
}
