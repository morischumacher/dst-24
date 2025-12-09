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

    // HTTP client to access the RabbitMQ management API
    private Client client;
    private Connection connection;
    private Channel channel;
    // Name of the queue to receive worker responses
    private String queue;
    // Map of region to list of processing times
    private Map<Region, List<Long>> processintTimes = new HashMap<>();
    // Jackson ObjectMapper to serialize/deserialize JSON
    private final ObjectMapper mapper = new ObjectMapper();
    // ConnectionFactory to create new connections to the RabbitMQ server
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

    // Initialize the RabbitMQ management API client wih the correct URL and credentials
    private void initializeClient() throws URISyntaxException, MalformedURLException {
        client = new Client(new URL(Constants.RMQ_API_URL), Constants.RMQ_USER, Constants.RMQ_PASSWORD);
    }

    // Set up the connectionFactory with the correct host, port, username and password
    private void initializeConnectionFactory() {
        connectionFactory.setHost(Constants.RMQ_HOST);
        connectionFactory.setPort(Integer.parseInt(Constants.RMQ_PORT));
        connectionFactory.setUsername(Constants.RMQ_USER);
        connectionFactory.setPassword(Constants.RMQ_PASSWORD);
    }

    // Create a new connection and channel to the RabbitMQ server
    private void createConnection() throws IOException, TimeoutException {
        connection = connectionFactory.newConnection();
        channel = connection.createChannel();
    }

    private void createQueue() throws IOException {
        // WorkloadMonitor queue is not durable (it won't survive a broker restart),
        // not exclusive (it can be accessed by other connections),
        // not auto-delete (it won't be deleted when the last consumer unsubscribes),
        // and no additional arguments.
        queue = channel.queueDeclare("", false, false, false, null).getQueue();
        for (var workQueue : Constants.WORK_QUEUES) {
            // Bind all topics (regions) topic exchange to this queue. So all publications of the worker ending up in
            // this queue when published to the topic exchange "Constants.TOPIC_EXCHANGE"
            var topic = "requests" + workQueue.substring(queue.indexOf("."));
            channel.queueBind(queue, Constants.TOPIC_EXCHANGE, topic);
        }
    }

    private void createDeliverCallback() throws IOException {
        // DeliverCallback for messages the topic exchange "Constants.TOPIC_EXCHANGE" in the "WorkloadMonitor queue".
        // Delivery contains the message in the "WorkloadMonitor queue".
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            // Get the message from the delivery
            String message = new String(delivery.getBody(), "UTF-8");
            // Get the region from the delivery
            var deliverYKey = delivery.getEnvelope().getRoutingKey();
            var region = Region.valueOf(deliverYKey.substring(deliverYKey.indexOf(".") + 1).toUpperCase());

            // Set the processing time of the worker response
            processMessage(message, region);
        };

        // Client (WorkloadManager) consume messages from the queue, with the deliverCallback, and no cancelCallback
        channel.basicConsume(queue, true, deliverCallback, consumerTag -> {});
    }

    synchronized private void processMessage(String message, Region region) {
        System.out.println(message);
        // Worker response of worker
        WorkerResponse response;
        try {
            // Deserialize the message to a WorkerResponse object
            response = mapper.readValue(message, WorkerResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Check in Map processingTimes if the region list is already in the map, if not add it
        processintTimes.computeIfAbsent(region, k -> new LinkedList<>());

        // Get the list of processing times for the region
        List<Long> list = processintTimes.get(region);
        // set the processing time of the worker response, if longer than 10, remove the first element
        list.add(response.getProcessingTime());
        if (list.size() > 10) {
            list.remove(0);
        }
    }

    @Override
    public Map<Region, Long> getRequestCount() {
        // Prepare a map to store the request counts
        Map<Region, Long> requestCounts = new HashMap<>();
        // List of all work queues defined in the QueueManager
        List<String> workQueues = Arrays.asList(Constants.WORK_QUEUES);

        // RabbitMQ client get all queues, filter the work queues and for each queue get the message count
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
        // Prepare a map to store the request counts
        Map<Region, Long> workerCounts = new HashMap<>();
        // List of all work queues defined in the QueueManager
        List<String> workQueues = Arrays.asList(Constants.WORK_QUEUES);

        // RabbitMQ client get all queues, filter the work queues and for each queue get the consumer count
        client.getQueues().stream()
                .filter(queue -> workQueues.contains(queue.getName()))
                .forEach(queue -> {
                    Region region = Region.valueOf(queue.getName().substring(4).toUpperCase());
                    long consumerCount = queue.getConsumerCount();
                    workerCounts.put(region, consumerCount);
                });

        return workerCounts;
    }


    // Get the average processing time of the worker responses, else return 0.0
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


    // Close the connection and the channel to the RabbitMQ server
    @Override
    public void close() throws IOException {
        // if connection is null, nothing to do, if not close the channel and the connection
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