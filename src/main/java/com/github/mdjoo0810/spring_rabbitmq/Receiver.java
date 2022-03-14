package com.github.mdjoo0810.spring_rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class Receiver {

    private final CountDownLatch latch = new CountDownLatch(1);

    public void receiveMessage(final CustomMessage message) {
        log.info("Received Message > {}", message);
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

}
