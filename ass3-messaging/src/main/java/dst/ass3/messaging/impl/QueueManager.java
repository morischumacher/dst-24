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

    // TCP connection to the RabbitMQ server (broker)
    // Shared between all threads
    private Connection conn;

    // ConnectionsFactory as RabbitMQ Class to make new connection to the RabbitMQ server (broker)
    @Override
    public void setUp() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(Constants.RMQ_HOST);
        connectionFactory.setPort(Integer.parseInt(Constants.RMQ_PORT));
        connectionFactory.setUsername(Constants.RMQ_USER);
        connectionFactory.setPassword(Constants.RMQ_PASSWORD);

        // Virtual connection within the TCP connection
        // All API calls are made on the channel
        Channel channel = null;
        try {
            // Make new connection with the given setup
            conn = connectionFactory.newConnection();

            // Create new channel
            channel = conn.createChannel();

            // Exchange is a message routing agent, which takes a message and routes it into one or more queues.
            // Types: direct, topic, headers, fanout.

            // Declare the exchanges with name and type, "topic" type means that the routingKey can contain a pattern.
            // Exchange to route TripRequest to the corresponding queues
            channel.exchangeDeclare("dst.custom.exchange", "topic");

            // Exchange to route WorkerResponse to the queue "WorkloadMonitor queue"
            channel.exchangeDeclare(Constants.TOPIC_EXCHANGE, "topic");

            // For every region a worker queue is created
            for (String queue : Constants.WORK_QUEUES) {

                // Queue is not durable (it won't survive a broker restart),
                // not exclusive (it can be accessed by other connections),
                // not auto-delete (it won't be deleted when the last consumer unsubscribes),
                // and no additional arguments.
                channel.queueDeclare(queue, false, false, false, null);

                // Create routing key for the queue (f.i. "topic.at_vienna")
                String routingKey = "topic" + queue.substring(queue.indexOf("."));

                // Bind the queue to the exchange with the routing key
                channel.queueBind(queue, "dst.custom.exchange", routingKey);
            }
        } catch (IOException | TimeoutException e) {
            LOGGER.log(Level.SEVERE, "Error setting up the queue manager", e);
            throw new RuntimeException(e);
        } finally {
            // If exception is thrown, close the channel
            closeChannel(channel);
        }
    }


    // Deletes the exchanges and queues that were set up (f.i when the program is closed)
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
            // If exception is thrown, close the channel
            closeChannel(channel);
        }
    }

    // Close the connection to the RabbitMQ server
    @Override
    public void close() throws IOException {
        if (conn != null) {
            conn.close();
        }
    }

    // Close the channel if it is open and exists
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