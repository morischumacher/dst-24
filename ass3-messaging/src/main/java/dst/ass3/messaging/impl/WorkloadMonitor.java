package dst.ass3.messaging.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.http.client.Client;
import dst.ass3.messaging.Constants;
import dst.ass3.messaging.IWorkloadMonitor;
import dst.ass3.messaging.Region;
import dst.ass3.messaging.WorkerResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class WorkloadMonitor implements IWorkloadMonitor {

    private Client client;
    private Connection connection;
    private Channel channel;
    private String queue;
    private Map<Region, List<Long>> processintTimes = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConnectionFactory connectionFactory = new ConnectionFactory();

    public WorkloadMonitor() {
        try {
            initializeClient();
            initializeConnectionFactory();
            createConnection();
            createQueue();
            createDeliverCallback();
        } catch (URISyntaxException | IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeClient() throws URISyntaxException, MalformedURLException {
        client = new Client(new URL(Constants.RMQ_API_URL), Constants.RMQ_USER, Constants.RMQ_PASSWORD);
    }

    private void initializeConnectionFactory() {
        connectionFactory.setHost(Constants.RMQ_HOST);
        connectionFactory.setPort(Integer.parseInt(Constants.RMQ_PORT));
        connectionFactory.setUsername(Constants.RMQ_USER);
        connectionFactory.setPassword(Constants.RMQ_PASSWORD);
    }

    private void createConnection() throws IOException, TimeoutException {
        connection = connectionFactory.newConnection();
        channel = connection.createChannel();
    }

    private void createQueue() throws IOException {
        queue = channel.queueDeclare("", false, false, false, null).getQueue();
        for (var workQueue : Constants.WORK_QUEUES) {
            var topic = "requests" + workQueue.substring(queue.indexOf("."));
            channel.queueBind(queue, Constants.TOPIC_EXCHANGE, topic);
        }
    }

    private void createDeliverCallback() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            var deliverYKey = delivery.getEnvelope().getRoutingKey();
            var region = Region.valueOf(deliverYKey.substring(deliverYKey.indexOf(".") + 1).toUpperCase());
            processMessage(message, region);
        };
        channel.basicConsume(queue, true, deliverCallback, consumerTag -> {});
    }

    synchronized private void processMessage(String message, Region region) {
        System.out.println(message);
        WorkerResponse response;
        try {
            response = mapper.readValue(message, WorkerResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        processintTimes.computeIfAbsent(region, k -> new LinkedList<>());
        List<Long> list = processintTimes.get(region);
        list.add(response.getProcessingTime());
        if (list.size() > 10) {
            list.remove(0);
        }
    }

    @Override
    public Map<Region, Long> getRequestCount() {
        Map<Region, Long> requestCounts = new HashMap<>();
        List<String> workQueues = Arrays.asList(Constants.WORK_QUEUES);

        client.getQueues().stream()
                .filter(queue -> workQueues.contains(queue.getName()))
                .forEach(queue -> {
                    Region region = Region.valueOf(queue.getName().substring(4).toUpperCase());
                    long messageCount = queue.getMessagesReady();
                    requestCounts.put(region, messageCount);
                });

        return requestCounts;
    }

    @Override
    public Map<Region, Long> getWorkerCount() {
        Map<Region, Long> workerCounts = new HashMap<>();
        List<String> workQueues = Arrays.asList(Constants.WORK_QUEUES);

        client.getQueues().stream()
                .filter(queue -> workQueues.contains(queue.getName()))
                .forEach(queue -> {
                    Region region = Region.valueOf(queue.getName().substring(4).toUpperCase());
                    long consumerCount = queue.getConsumerCount();
                    workerCounts.put(region, consumerCount);
                });

        return workerCounts;
    }

    @Override
    synchronized public Map<Region, Double> getAverageProcessingTime() {
        Map<Region, Double> averageProcessingTimes = new HashMap<>();

        processintTimes.entrySet().stream()
                .forEach(entry -> {
                    double average = entry.getValue().stream().mapToDouble(t -> t).average().orElse(0.0);
                    averageProcessingTimes.put(entry.getKey(), average);
                });

        return averageProcessingTimes;
    }
    @Override
    public void close() throws IOException {
        if (client == null) return;

        channel.queueDelete(queue);
        queue = null;

        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        channel = null;

        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        connection = null;
    }
}