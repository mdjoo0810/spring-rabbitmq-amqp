# Spring Boot RabbitMQ 적용

Spring Boot 프로젝트에 RabbitMQ 를 손쉽게 사용하는 방법을 알아보겠습니다.
모든 소스는 [GitHub](https://github.com/mdjoo0810/spring-rabbitmq-amqp)에 존재합니다.

## 사용 스펙
- Spring boot 2.6.4
- Gradle 7

---
## Set RabbitMQ Broker
우선 RabbitMQ를 사용하기 전에 RabbitMQ Broker 를 세팅해야 합니다.

여기서 Broker란 Publish(송신)하는 서비스1과 Subscribe(수신)하는 서비스2 간의 메시지 전달을 담당합니다. 이때 이 메시지들이 적재되는 공간을 MQ (Message Queue)라고 하며 메시지의 그룹을 Topic이라고 합니다.

대표적으로 Apache Kafka, Redis, RabbitMQ .. 등이 있습니다.
자세한 내용은 [링크](https://ademcatamak.medium.com/what-is-message-broker-4f6698c73089)를 참고하시면 좋을 것 같습니다.


본론으로 돌아와서 우선 Docker Compose를 활용하여 RabbitMQ 브로커를 구축하겠습니다.

### Docker Compose
```yml
# Rabbit MQ Docker Compose
rabbitmq:
  image: rabbitmq:management
  ports:
    - "5672:5672"
    - "15672:15672"
```

`docker-compose up` 명령어를 활용해 브로커를 실행시켜보겠습니다.


---
## Start Spring Initializr
Spring Initializr를 활용해 스프링 프로젝트를 생성하겠습니다.
RabbitMQ를 검색 후 `Spring for RabbitMQ`의존성을 추가해주시면 됩니다.

```gradle
// ...
// 수동으로 추가도 가능합니다.
implementation 'org.springframework.boot:spring-boot-starter-amqp'
// ...
```

---
## Create RabbitMQ Message Receiver
우선 Produce 된 메시지를 수신하는 POJO객체를 만들어보겠습니다.

```java
@Slf4j
@Component
public class Receiver {

    private CountDownLatch latch = new CountDownLatch(1);

    public void receiveMessage(String message) {
        log.info("Received Message > {}", message);
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

}
```
@RabbitListener 를 활용하여 더욱 쉽게 생성이 가능합니다만 우선 POJO를 활용해서 개발해보겠습니다.

여기서 `CountDownLatch`는 별도의 Produce 서비스를 만들지 않고 해당 프로젝트 내에서 발행을 하기위해 쓰레드 작업을 기다리도록 하기 위해 사용하였습니다.

---
## Register Listener & Send Message
이제 Listener를 어플리케이션에 등록하고 메시지를 송신하는 과정을 진행해보겠습니다.

```java
@SpringBootApplication
public class Application {
    static final String topicExName = "spring-ex";
    static final String queueName = "spring";

    // org.springframework.amqp.core.Queue;
    @Bean
    Queue queue() {
        return new Queue(queueName, false); // name, durable
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(topicExName); // name
    }

    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("foo.bar.#"); //routingKey
    }

    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage"); // defaultListenerMethod
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```
### Methods 설명
#### listenerAdapter()
- 메시지 리스너를 컨테이너에 등록 (컨테이너 = container())
- Receiver 클래스는 POJO 객체이므로 MessageListenerAdapter로 Wrapping 하여 사용

#### queue()
- AMQP Queue를 생성

#### exchange()
- topic exchange를 생성

#### binding
- queue(), exchange() 두개를 연결함
- with("라우팅 부분")
  - 라우팅 할 수 있음
  - '#'은 와일드 카드

여기서 중요한 점은 Spring AMQP를 사용하기 위해서는 Queue, TopicExchange, Binding은 최상위 스프링 빈으로 선언해야합니다. (@Configuration에서 사용)

---
## Send a Test Message
한번 테스트 메세지를 전송해봅시다.
```java
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
        rabbitTemplate.convertAndSend(Application.topicExName, "foo.bar.baz", "Hello from RabbitMQ!");
        receiver.getLatch().await(10000, TimeUnit.MILLISECONDS);
    }
}
```

`receiver.getLatch().await(10000, TimeUnit.MILLISECONDS);` 해당 코드는 쓰레드가 대기상태로 receiver에 latch가 1 -> 0이 될때까지 기다린다하는 의미 입니다.

---
## 마무리

간단하게 구현하는 방법을 알아보았습니다.
다음번 포스트로

- Receiver에서 수신하는 메시지 타입이 String -> Custom Object (json)객체로 수신하기
- Receiver Service, Producer Service 분리 후 Worker를 통한 로직 설계하기

를 진행해보겠습니다.

자세한 내용은 해당
[링크](https://docs.spring.io/spring-amqp/docs/current/reference/html/)를 참고해주세요.