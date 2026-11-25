package com.ejemplo.tienda.bus;

import com.ejemplo.tienda.event.Event;
import com.ejemplo.tienda.event.Events;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Adapter: the event bus is a RabbitMQ broker.
 *
 * <p>Layout inside the broker (the {@code namespace} is the prefix of every name):
 * <pre>
 *   publish ──► exchange "&lt;ns&gt;.events" (topic) ──► queue "&lt;ns&gt;.billing"       ──► BillingService
 *                  routing key = event name       ──► queue "&lt;ns&gt;.notifications" ──► NotificationService
 *                                                 ──► ...one queue per consumer
 *   handler fails ──► exchange "&lt;ns&gt;.dead" (fanout) ──► queue "&lt;ns&gt;.dead-letters"
 * </pre>
 *
 * <p>Delivery is "at least once": a message is acknowledged only after the handler
 * finished. If the connection drops in between, the broker sends it again, so
 * handlers must be idempotent. A handler that throws sends the message to the
 * dead-letter queue instead of blocking the consumer's queue.
 */
public class RabbitMqEventBus implements EventBus, AutoCloseable {

    /** Where the broker is. Read from the environment, with the compose.yaml values by default. */
    public record Settings(String host, int port, String user, String password) {

        public static Settings fromEnv() {
            return new Settings(
                env("RABBIT_HOST", "localhost"),
                Integer.parseInt(env("RABBIT_PORT", "5672")),
                env("RABBIT_USER", "tienda"),
                env("RABBIT_PASSWORD", "tienda"));
        }

        public String address() {
            return host + ":" + port;
        }

        private static String env(String name, String fallback) {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    private record Registration(Class<? extends Event> type, Consumer<Event> handler) {
    }

    private record QueueConsumer(String queue, Channel channel, List<Registration> registrations) {
    }

    private final ObjectMapper json = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Map<String, QueueConsumer> consumers = new ConcurrentHashMap<>();
    private final Object publishLock = new Object();
    private final String namespace;
    private final String eventsExchange;
    private final String deadExchange;
    private final String deadQueue;
    private final Connection connection;
    private final Channel publishChannel;

    public RabbitMqEventBus(Settings settings, String namespace) {
        this.namespace = namespace;
        this.eventsExchange = namespace + ".events";
        this.deadExchange = namespace + ".dead";
        this.deadQueue = namespace + ".dead-letters";
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(settings.host());
            factory.setPort(settings.port());
            factory.setUsername(settings.user());
            factory.setPassword(settings.password());
            factory.setConnectionTimeout(3_000);
            this.connection = factory.newConnection("tienda-" + namespace);
            this.publishChannel = connection.createChannel();
            publishChannel.confirmSelect(); // the broker confirms each message it stored
            publishChannel.exchangeDeclare(eventsExchange, "topic", true);
            publishChannel.exchangeDeclare(deadExchange, "fanout", true);
            publishChannel.queueDeclare(deadQueue, true, false, false, null);
            publishChannel.queueBind(deadQueue, deadExchange, "");
        } catch (Exception error) {
            throw new EventBusException("Cannot connect to RabbitMQ at " + settings.address(), error);
        }
    }

    @Override
    public void publish(Event event) {
        try {
            byte[] body = json.writeValueAsBytes(event);
            String name = Events.nameOf(event);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2) // persistent: survives a broker restart
                .type(name)
                .messageId(event.eventId().toString())
                .timestamp(Date.from(event.occurredAt()))
                .build();
            synchronized (publishLock) { // a channel is not thread-safe
                publishChannel.basicPublish(eventsExchange, name, properties, body);
                publishChannel.waitForConfirmsOrDie(5_000);
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new EventBusException("Interrupted while publishing " + event, error);
        } catch (Exception error) {
            throw new EventBusException("Cannot publish " + event, error);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T extends Event> void subscribe(
            String consumerName, Class<T> type, Consumer<T> handler) {
        boolean everything = type == Event.class;
        if (!everything && !Events.isConcrete(type)) {
            throw new IllegalArgumentException("Not an event: " + type);
        }
        try {
            QueueConsumer consumer = consumers.get(consumerName);
            boolean first = consumer == null;
            if (first) {
                consumer = openQueue(consumerName);
                consumers.put(consumerName, consumer);
            }
            consumer.registrations().add(new Registration(type, event -> handler.accept((T) event)));
            String routingKey = everything ? "#" : type.getSimpleName();
            consumer.channel().queueBind(consumer.queue(), eventsExchange, routingKey);
            if (first) {
                startConsuming(consumer);
            }
        } catch (IOException error) {
            throw new EventBusException("Cannot subscribe " + consumerName, error);
        }
    }

    private QueueConsumer openQueue(String consumerName) throws IOException {
        String queue = namespace + "." + consumerName;
        Channel channel = connection.createChannel();
        // x-dead-letter-exchange: where the broker sends a message that was rejected
        channel.queueDeclare(queue, true, false, false, Map.of("x-dead-letter-exchange", deadExchange));
        return new QueueConsumer(queue, channel, new CopyOnWriteArrayList<>());
    }

    private void startConsuming(QueueConsumer consumer) throws IOException {
        // autoAck=false: we acknowledge by hand, after the handlers finished
        consumer.channel().basicConsume(consumer.queue(), false,
            (tag, delivery) -> onDelivery(consumer, delivery),
            tag -> { });
    }

    private void onDelivery(QueueConsumer consumer, Delivery delivery) throws IOException {
        long deliveryTag = delivery.getEnvelope().getDeliveryTag();
        try {
            Event event = json.readValue(delivery.getBody(),
                Events.classNamed(delivery.getProperties().getType()));
            for (Registration registration : consumer.registrations()) {
                if (registration.type().isInstance(event)) {
                    registration.handler().accept(event);
                }
            }
            consumer.channel().basicAck(deliveryTag, false);
        } catch (Exception error) {
            System.err.println("[bus] " + consumer.queue() + " failed, message goes to "
                + deadQueue + ": " + error);
            consumer.channel().basicNack(deliveryTag, false, false); // requeue=false -> dead letter
        }
    }

    /** How many messages are waiting in the dead-letter queue. */
    public long deadLetterCount() {
        try {
            synchronized (publishLock) {
                return publishChannel.messageCount(deadQueue);
            }
        } catch (IOException error) {
            throw new EventBusException("Cannot read " + deadQueue, error);
        }
    }

    /** Deletes every queue and exchange of this namespace (the demo and the tests clean up). */
    public void deleteAll() {
        try {
            synchronized (publishLock) {
                for (QueueConsumer consumer : consumers.values()) {
                    publishChannel.queueDelete(consumer.queue());
                }
                publishChannel.queueDelete(deadQueue);
                publishChannel.exchangeDelete(eventsExchange);
                publishChannel.exchangeDelete(deadExchange);
            }
        } catch (IOException error) {
            throw new EventBusException("Cannot delete namespace " + namespace, error);
        }
    }

    @Override
    public void close() {
        try {
            if (connection.isOpen()) {
                connection.close();
            }
        } catch (IOException error) {
            throw new EventBusException("Cannot close the connection", error);
        }
    }
}
