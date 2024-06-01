package dst.ass3.messaging.impl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dst.ass3.messaging.Constants;
import dst.ass3.messaging.IQueueManager;
import java.util.concurrent.TimeoutException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueueManager implements IQueueManager {

    private static final Logger LOGGER = Logger.getLogger(QueueManager.class.getName());
    private Connection conn;

    @Override
    public void setUp() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(Constants.RMQ_HOST);
        connectionFactory.setPort(Integer.parseInt(Constants.RMQ_PORT));
        connectionFactory.setUsername(Constants.RMQ_USER);
        connectionFactory.setPassword(Constants.RMQ_PASSWORD);

        Channel channel = null;
        try {
            conn = connectionFactory.newConnection();
            channel = conn.createChannel();

            // Declare the exchange
            channel.exchangeDeclare(Constants.TOPIC_EXCHANGE, "topic");
            channel.exchangeDeclare("dst.custom.exchange", "topic");

            // Declare the queues and bind them to the exchange
            for (String queue : Constants.WORK_QUEUES) {
                channel.queueDeclare(queue, false, false, false, null);
                String routingKey = "topic" + queue.substring(queue.indexOf("."));
                channel.queueBind(queue, "dst.custom.exchange", routingKey);
            }
        } catch (IOException | TimeoutException e) {
            LOGGER.log(Level.SEVERE, "Error setting up the queue manager", e);
            throw new RuntimeException(e);
        } finally {
            closeChannel(channel);
        }
    }

    @Override
    public void tearDown() {
        Channel channel = null;
        try {
            channel = conn.createChannel();

            // Delete the exchanges
            channel.exchangeDelete("dst.custom.exchange");
            channel.exchangeDelete(Constants.TOPIC_EXCHANGE);

            // Delete the queues
            for (String queue : Constants.WORK_QUEUES) {
                channel.queueDelete(queue);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error tearing down the queue manager", e);
            throw new RuntimeException(e);
        } finally {
            closeChannel(channel);
        }
    }

    @Override
    public void close() throws IOException {
        if (conn != null) {
            conn.close();
        }
    }

    private void closeChannel(Channel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                LOGGER.log(Level.WARNING, "Error closing channel", e);
            }
        }
    }
}