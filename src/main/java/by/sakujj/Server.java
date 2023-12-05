package by.sakujj;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final List<Integer> dataList;

    @Getter
    private int size = 0;

    private final Lock lock = new ReentrantLock();

    public Server(int initialCapacity) {
        dataList = new ArrayList<>(initialCapacity);
    }

    public List<Integer> getDataListCopy() {
        lock.lock();
        try {
            List<Integer> copy = new ArrayList<>(size);
            copy.addAll(dataList);
            return copy;
        } finally {
            lock.unlock();
        }
    }

    public IntWrapper processRequest(IntWrapper request) throws InterruptedException {
        Random random = new Random();
        long delay = random.nextLong(100, 1001);
        TimeUnit.MILLISECONDS.sleep(delay);

        IntWrapper response = null;
        lock.lock();
        try {
            dataList.add(request.getValue());
            size++;
            response = new IntWrapper(size);
        } finally {
            lock.unlock();
        }

        return response;
    }

}
