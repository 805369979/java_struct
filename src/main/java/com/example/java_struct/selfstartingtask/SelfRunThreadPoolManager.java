package com.qintongxue.opensource.opensource_qintongxue.selfstartingtask;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
/**
 * 自启动任务扫描
 **/
@Component
@Slf4j
public class SelfRunThreadPoolManager implements InitializingBean {
    // 没有需要处理的任务时，避免线程空转，默认30次为最大的设置线程空转睡眠时间
    private static final int MAX_SLEEP_TIME_CNT = 30;
    // 当前空转次数
    private static final AtomicInteger CURRENT_CNT = new AtomicInteger(0);
    // 线程是否在处理任务
    private static boolean PROCESS_FLAG = false;
    // 任务操作统一处理类
    @Autowired
    private Operator operator;
    // static 可以全局操作任务执行
    private static Operator taskOperator;
    // 控制当前线程的引用，可中断线程
    private volatile static Thread singleThread;

    //全局操作任务执行赋值
    @PostConstruct
    public void init() {
        taskOperator = this.operator;
    }
    // 自定义线程池
    public static ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MINUTES, new ArrayBlockingQueue<>(2), runnable -> {
        Thread t = new Thread(runnable);
        t.setName("ScannerThread");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void afterPropertiesSet() {
        doRun(SelfRunThreadPoolManager::extracted);
    }

    // 自启动任务执行入口
    public static void doRun(Runnable runnable, Object... param) {
        if(taskExecutor.getActiveCount()<1) {
            taskExecutor.execute(() -> {
                try {
                    singleThread = Thread.currentThread();
                    runnable.run();
                } catch (Exception e) {
                    log.info("ScannerThread, param={0}执行发生异常", param);
                    e.printStackTrace();
                }
            });
        }
    }

    public static void reStart() {
        log.info("有任务调度失败，激活（重置）扫描线程池");
        if (!PROCESS_FLAG && singleThread != null) {
            // 恢复线程的运行
            LockSupport.unpark(singleThread);
        }
        CURRENT_CNT.set(0);
    }

    // 停止自启动线程
    public static void stop() {
        if (singleThread != null) {
            LockSupport.park(singleThread);
        }
        PROCESS_FLAG = false;
        CURRENT_CNT.set(0);
    }

    private static void extracted() {
        while (true) {
            // 如果当前线程被中断
            if (singleThread != null && singleThread.isInterrupted()) {
                return;
            }
            // 获取需要执行的任务编号
            List<Long> needExecuteTaskIdList = getNeedExecuteTask();
            // 不存在的时候需要让线程睡眠，避免一直空转，浪费资源
            if (CollectionUtils.isEmpty(needExecuteTaskIdList)) {
                int curCnt = CURRENT_CNT.incrementAndGet();
                int minVal = Math.min(curCnt, MAX_SLEEP_TIME_CNT);
                // 设置当前的空转次数
                CURRENT_CNT.set(minVal);
                long sleepSeconds = minVal * 5L;
                log.info("扫描线程池发生空转，为避免资源消耗开始执行休眠，时长为：{0} 秒", sleepSeconds);
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(sleepSeconds));
                continue;
            }
            PROCESS_FLAG = true;
            for (Long taskId : needExecuteTaskIdList) {
                // 在线程池中依次处理任务
                ProcessorThreadPoolManager.doRun(() -> taskOperator.execute(taskId));
            }
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
            PROCESS_FLAG = false;
        }
    }
    // 数据库查询需要处理的任务类，这里模拟一下
    private static List<Long> getNeedExecuteTask() {
        ArrayList<Long> longs = new ArrayList<>();
        longs.add(1L);
        return longs;
    }
}
