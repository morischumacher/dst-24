package dst.ass3.messaging.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dst.ass3.messaging.Constants;
import dst.ass3.messaging.IRequestGateway;
import dst.ass3.messaging.TripRequest;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

// Gateway to submit trip requests to a RabbitMQ exchange
public class RequestGateway implements IRequestGateway {

    // TCP connection to the RabbitMQ server (broker)
    // Shared between all threads
    private Connection connection;

    // Virtual connection within the TCP connection
    // All API calls are made on the channel
    private Channel channel;
    private final ObjectMapper mapper;

    private final ConnectionFactory connectionFactory;

    public RequestGateway() {

        // New Jackson ObjectMapper
        this.mapper = new ObjectMapper();

        // Set up all attributes of the connectionFactory
        this.connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(Constants.RMQ_HOST);
        connectionFactory.setPort(Integer.parseInt(Constants.RMQ_PORT));
        connectionFactory.setUsername(Constants.RMQ_USER);
        connectionFactory.setPassword(Constants.RMQ_PASSWORD);
    }

    // Submit a trip request to the RabbitMQ exchange
    @Override
    public void submitRequest(TripRequest request) {
        try {
            // Serialize the trip request to a JSON string
            String serializedRequest = serializeRequest(request);

            // Connect to the RabbitMQ server
            connect();

            // Generate the topic (routingKey pattern) for the request
            String topic = generateTopic(request);

            // Publish the request to the exchange
            publishToExchange(serializedRequest, topic);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    // Serialize the trip request to a JSON string
    private String serializeRequest(TripRequest request) throws JsonProcessingException {
        return this.mapper.writeValueAsString(request);
    }

    // Connect to the RabbitMQ server
    private void connect() throws IOException, TimeoutException {
        if (connection != null) return;

        // Create both connection and channel in the connection
        connection = connectionFactory.newConnection();
        channel = connection.createChannel();
    }

    // Generate the topic for the request (name is a ENUM -->  lower case needed)
    private String generateTopic(TripRequest request) {
        return "topic." + request.getRegion().name().toLowerCase();
    }

    // Publish the request to the exchange
    private void publishToExchange(String message, String topic) throws IOException {
        // Publish the message to the exchange "dst.custom.exchange", with routingKey "topic" and the request in bytes
        channel.basicPublish("dst.custom.exchange", topic, null, message.getBytes());
    }

    // Close the connection and the channel to the RabbitMQ server
    @Override
    public void close() throws IOException {
        // if connection is null, nothing to do, if not close the channel and the connection
        if (connection == null) return;

        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (TimeoutException ignored) {
        } finally {
            channel = null;
        }

        if (connection.isOpen()) {
            connection.close();
        }
        connection = null;
    }
}