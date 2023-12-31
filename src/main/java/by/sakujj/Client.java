package by.sakujj;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class Client implements Callable<IntWrapper> {
    private final int threadCount;

    private final List<Integer> dataList;
    private final Server server;
    private AtomicLong accumulator = new AtomicLong();
    private final Lock lock = new ReentrantLock();

    private boolean isUsable = true;

    private final ExecutorService executorService;

    public Client(int n, int threadCount, Server server) {
        dataList = new ArrayList<>(n);
        IntStream
                .rangeClosed(1, n)
                .forEach(dataList::add);

        this.threadCount = threadCount;
        this.executorService = Executors.newFixedThreadPool(threadCount);
        this.server = server;
    }

    public List<Integer> getDataListCopy() {
        lock.lock();
        try {
            List<Integer> copy = new ArrayList<>(dataList.size());
            copy.addAll(dataList);
            return copy;
        } finally {
            lock.unlock();
        }
    }

    public int getSize() {
        lock.lock();
        try {
            return dataList.size();
        } finally {
            lock.unlock();
        }
    }

    public void sendDataToServer() {
        if (!isUsable) {
            throw new IllegalStateException();
        }
        isUsable = false;

        try {
            int n = dataList.size();
            if (n == 0) {
                return;
            }

            final List<Future<IntWrapper>> futures = new ArrayList<>(n);

            for (int i = 0; i < n; ++i) {
                Future<IntWrapper> serverSize = executorService.submit(this);
                futures.add(serverSize);
            }

            // Находим число потоков, которые будут участвовать в извлечении futures
            int threadGettingFutureCount = threadCount;
            if (threadGettingFutureCount > n) {
                threadGettingFutureCount = n;
            }
            int intervalLength = n / threadGettingFutureCount;

            final CountDownLatch allFuturesProcessedLatch = new CountDownLatch(threadGettingFutureCount);

            for (int i = 0; i < threadGettingFutureCount - 1; i++) {

                int startIndexIncl = i * intervalLength;
                int endIndexExcl = (i + 1) * intervalLength;

                executorService.submit(() ->
                        processFuturesInInterval(
                                futures,
                                startIndexIncl,
                                endIndexExcl,
                                allFuturesProcessedLatch,
                                accumulator)
                );
            }


            int startIndexIncl = (threadGettingFutureCount - 1) * intervalLength;
            int endIndexExcl = n;
            processFuturesInInterval(futures, startIndexIncl, endIndexExcl,
                    allFuturesProcessedLatch, accumulator);
            try {
                // Блокируем текущий поток, пока все futures не извлечены
                allFuturesProcessedLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } finally {
            executorService.shutdown();
        }
    }

    private static void processFuturesInInterval(List<Future<IntWrapper>> futures, int startIndexIncl, int endIndexExcl,
                                                 CountDownLatch allFuturesProcessedLatch, AtomicLong accumulator) {
        long accumulated = accumulateFromFutures(futures, startIndexIncl, endIndexExcl);
        accumulator.addAndGet(accumulated);
        allFuturesProcessedLatch.countDown();
    }

    /**
     * Извлекает и суммирует содержимое списка futures на заданном интервале,
     * а затем возвращает сумму.
     *
     * @param futures        список из которого берутся данные для суммирования
     * @param startIndexIncl индекс начала интервала (включая)
     * @param endIndexExcl   индекс конца интервала (исключая)
     * @return сумма содержимого списка на заданном интервале
     */
    private static long accumulateFromFutures(List<Future<IntWrapper>> futures, int startIndexIncl, int endIndexExcl) {
        long localAccumulator = 0;
        for (int j = startIndexIncl; j < endIndexExcl; j++) {
            try {
                localAccumulator += futures
                        .get(j)
                        .get()
                        .getValue();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return localAccumulator;
    }

    public long getAccumulator() {
        return accumulator.get();
    }


    @Override
    public IntWrapper call() throws Exception {
        Random random = new Random();
        long delay = random.nextLong(100, 501);
        TimeUnit.MILLISECONDS.sleep(delay);

        IntWrapper request = null;
        lock.lock();
        try {
            int index = random.nextInt(0, dataList.size());
            int requestValue = dataList.get(index);
            dataList.remove(index);

            request = new IntWrapper(requestValue);
        } finally {
            lock.unlock();
        }

        return server.processRequest(request);
    }
}
