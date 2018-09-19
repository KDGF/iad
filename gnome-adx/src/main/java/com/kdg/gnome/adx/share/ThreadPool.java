package com.kdg.gnome.adx.share;

import com.google.common.util.concurrent.*;
//import org.apache.log4j.Logger;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by hbwang on 2018/5/10
 */
public class ThreadPool implements HttpClientConstant {
    private final static Logger log = LogManager.getLogger("ES_OUT_INFO");
    private static ThreadPool threadPool;
    private static ExecutorService executorService;
    private static ListeningExecutorService service;
    private final static int THREAD_POOL_TIMES = 5;

    public static void initPool() {
        if (threadPool == null) {
            threadPool = new ThreadPool();
            if (threadPool.executorService == null) {
                threadPool.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * THREAD_POOL_TIMES);
            }
            if (threadPool.service == null) {
                threadPool.service = MoreExecutors.listeningDecorator(executorService);
            }
        }
    }

    public static void closePool() {
        if (threadPool == null) {
            if (threadPool.service != null) {
                threadPool.service.shutdown();
                threadPool.service.shutdownNow();
            }
            if (threadPool.executorService != null) {
                threadPool.executorService.shutdown();
                threadPool.executorService.shutdownNow();
            }
        }
    }

    public static ThreadPool getThreadPool() {
        return threadPool;
    }

    public Map<String, Object> runMap(Map<String, Callable> map) {
        Map<String, Object> rMap = new ConcurrentHashMap();
        final CountDownLatch latch = new CountDownLatch(map.size());
        try {
            for (Map.Entry<String, Callable> entry : map.entrySet()) {
                ListenableFuture future = service.submit(entry.getValue());
                Futures.addCallback(future, new FutureCallback() {
                    public void onSuccess(Object o) {
                        if (o != null) {
                            rMap.put(entry.getKey(), o);
                        }
                        latch.countDown();
                    }

                    public void onFailure(Throwable thrown) {
                        latch.countDown();
                    }
                });
            }
            latch.await(HttpClientConstant.THREAD_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
//            log.error(e);
        }
        return rMap;
    }

    public Map<String, Object> runMapWait(Map<String, Callable> map, long timeout, TimeUnit unit) {
        Map<String, Object> rMap = new ConcurrentHashMap();
        final CountDownLatch latch = new CountDownLatch(map.size());
        try {
            for (Map.Entry<String, Callable> entry : map.entrySet()) {
                ListenableFuture future = service.submit(entry.getValue());
                Futures.addCallback(future, new FutureCallback() {
                    public void onSuccess(Object o) {
                        if (o != null) {
                            rMap.put(entry.getKey(), o);
                        }
                        latch.countDown();
                    }

                    public void onFailure(Throwable thrown) {
                        latch.countDown();
                    }
                });
            }
            latch.await(timeout, unit);
        } catch (Exception e) {
//            log.error(e);
        }
        return rMap;
    }


    public void runRunnable(Runnable runnable) {
        try {
            service.execute(runnable);
        } catch (Exception e) {
//            log.error(e);
        }
    }
}
