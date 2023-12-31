package by.sakujj;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientTests {
    @Mock
    private Server server;

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
    void shouldHaveDataListFilledCorrectly(int n, int threadCount) {
        // given
        Client client = new Client(n, threadCount, server);

        List<Integer> expected = new ArrayList<>(n);
        IntStream.rangeClosed(1, n)
                .forEachOrdered(expected::add);

        // when
        List<Integer> actual = client.getDataListCopy();

        // then
        assertThat(actual).isEqualTo(expected);

    }

    @ParameterizedTest
    @MethodSource("defaultSource")
    void shouldEmptyTheDataList(int n, int threadCount) throws InterruptedException {
        // given
        if (n > 0) {
            when(server.processRequest(any()))
                    .thenReturn(new IntWrapper(1));
        }
        Client client = new Client(n, threadCount, server);
        int expected = 0;

        // when
        client.sendDataToServer();

        // then
        assertThat(client.getSize()).isEqualTo(expected);
        verify(
                server,
                times(n)
        ).processRequest(any());

    }

    @ParameterizedTest
    @MethodSource("defaultSource")
    void shouldAccumulateCorrectly(int n, int threadCount) throws InterruptedException {
        // given
        if (n > 0) {
            when(server.processRequest(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }
        Client client = new Client(n, threadCount, server);
        long expected = (1L + n) * n / 2L;

        // when
        client.sendDataToServer();
        long actual = client.getAccumulator();

        // then
        assertThat(actual).isEqualTo(expected);
        verify(server, times(n))
                .processRequest(any());
    }


}
