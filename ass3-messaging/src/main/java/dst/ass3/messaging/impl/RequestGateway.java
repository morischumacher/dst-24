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

public class RequestGateway implements IRequestGateway {

    private Connection connection;
    private Channel channel;
    private final ObjectMapper mapper;
    private final ConnectionFactory connectionFactory;

    public RequestGateway() {
        this.mapper = new ObjectMapper();
        this.connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(Constants.RMQ_HOST);
        connectionFactory.setPort(Integer.parseInt(Constants.RMQ_PORT));
        connectionFactory.setUsername(Constants.RMQ_USER);
        connectionFactory.setPassword(Constants.RMQ_PASSWORD);
    }

    private void connect() throws IOException, TimeoutException {
        if (connection != null) return;

        connection = connectionFactory.newConnection();
        channel = connection.createChannel();
    }

    @Override
    public void submitRequest(TripRequest request) {
        try {
            String serializedRequest = serializeRequest(request);
            establishConnection();
            String topic = generateTopic(request);
            publishToExchange(serializedRequest, topic);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private String serializeRequest(TripRequest request) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(request);
    }

    private void establishConnection() throws IOException, TimeoutException {
        if (connection == null) {
            connect();
        }
    }

    private String generateTopic(TripRequest request) {
        return "topic." + request.getRegion().name().toLowerCase();
    }

    private void publishToExchange(String message, String topic) throws IOException {
        channel.basicPublish("dst.custom.exchange", topic, null, message.getBytes());
    }

    @Override
    public void close() throws IOException {
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