package com.qintongxue.opensource.opensource_qintongxue.selfstartingtask;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProcessorThreadPoolManager {

    public static ExecutorService taskExecutor = new ThreadPoolExecutor(32, 128, 1L,
            TimeUnit.MINUTES, new ArrayBlockingQueue<>(128), runnable -> {
        Thread t = new Thread(runnable);
        t.setName("ProcessorThread");
        t.setDaemon(true);
        return t;
    }, new ThreadPoolExecutor.DiscardPolicy());


    public static void doRun(Runnable runnable, Object... param) {
        taskExecutor.execute(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.info("ProcessorThread, param={0}", param);
            }
        });
    }
}
