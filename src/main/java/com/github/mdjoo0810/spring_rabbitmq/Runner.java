package com.github.mdjoo0810.spring_rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class Runner implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;
    private final Receiver receiver;

    public Runner(RabbitTemplate rabbitTemplate, Receiver receiver) {
        this.rabbitTemplate = rabbitTemplate;
        this.receiver = receiver;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Sending Message ...");
        CustomMessage message = CustomMessage.builder()
                .text("Hello custom message :)")
                .priority(1)
                .secret(true)
                .build();
        rabbitTemplate.convertAndSend(Application.topicExName, "foo.bar.baz", message);
        receiver.getLatch().await(10000, TimeUnit.MILLISECONDS);
    }
}
