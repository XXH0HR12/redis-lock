package com.xxh.lock;

import com.xxh.lock.service.impl.LockServiceRedisImpl;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import redis.clients.jedis.JedisPool;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class ApplicationTests {

    private static final String KEY = "20200507";
    /**
     * 并发数
     */
    private static final int THREAD_NUM = 10;
    /**
     * 并发栅栏
     */
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Autowired
    private JedisPool jedisPool;

    @Test
    public void contextLoads() {
        for (int i = 0; i < THREAD_NUM; i++) {
            new Thread(() -> {
                try {
                    countDownLatch.await();
                    LockServiceRedisImpl lockService = new LockServiceRedisImpl(jedisPool);
                    log.info("value={}", lockService.lock(KEY));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            countDownLatch.countDown();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("主线程：{}，开始运行。。。。", Thread.currentThread().getName());
    }

}
