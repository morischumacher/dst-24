package dst.ass3.event.impl;

import dst.ass3.event.IEventProcessingEnvironment;
import dst.ass3.event.model.events.*;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;

public class EventProcessingEnvironment implements IEventProcessingEnvironment {

    private Time timeout;
    // SinkFunction to generate Flink sink for the lifecycle event (indicating a change in the lifecycle state of an ITripEventInfo)
    private SinkFunction<LifecycleEvent> lifecycleEventSink;
    private SinkFunction<MatchingDuration> matchingDurationSink;
    private SinkFunction<AverageMatchingDuration> averageMatchingDurationSink;
    private SinkFunction<MatchingTimeoutWarning> matchingTimeoutWarningSink;
    private SinkFunction<TripFailedWarning> tripFailedWarningSink;
    private SinkFunction<Alert> alertSink;

    @Override
    public void initialize(StreamExecutionEnvironment env) {
        // Creates the event source which receives the TripEventInfo
        EventSourceFunction source = createEventSourceFunction();
        // Sets up the stream
        var stream = setupStream(env, source);
        // Adds the lifecycle event sink
        addLifecycleEventSink(stream);
    }

    private EventSourceFunction createEventSourceFunction() {
        return new EventSourceFunction();
    }

    private SingleOutputStreamOperator<LifecycleEvent> setupStream(StreamExecutionEnvironment env, EventSourceFunction source) {
        // Sets up the watermark strategy for the event lifecycle stream
        var watermarkStrategy = createWatermarkStrategy();

        // Adds the source to the environment, filters out events with no region, maps the event to a lifecycle event, assigns timestamps and watermarks
        return env.addSource(source)
                .filter(info -> info.getRegion() != null)
                .map(LifecycleEvent::new)
                .assignTimestampsAndWatermarks(watermarkStrategy);
    }

    private WatermarkStrategy<LifecycleEvent> createWatermarkStrategy() {
        // when we get a new event, we will wait for 20 seconds before we consider the event as out of order
        // in oder words if we get an event, we assume all events with a timestamp older than 20 seconds are already received
        // It subtracts 20 seconds from the latest event timestamp and uses this as the watermark
        return WatermarkStrategy.<LifecycleEvent>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                // we use the timestamp of the event as the event time
                .withTimestampAssigner((event, timestamp) -> event.getTimestamp());
    }

    // Adds the lifecycle event sink (endpoint of the stream where the data will be written to) to the lifecycle stream
    private void addLifecycleEventSink(SingleOutputStreamOperator<LifecycleEvent> stream) {
        if (lifecycleEventSink != null) {
            stream.addSink(lifecycleEventSink);
        }
    }

    @Override
    public void setMatchingDurationTimeout(Time time) {
        this.timeout = time;
    }

    @Override
    public void setLifecycleEventStreamSink(SinkFunction<LifecycleEvent> sink) {
        this.lifecycleEventSink = sink;
    }

    @Override
    public void setMatchingDurationStreamSink(SinkFunction<MatchingDuration> sink) {
        this.matchingDurationSink = sink;
    }

    @Override
    public void setAverageMatchingDurationStreamSink(SinkFunction<AverageMatchingDuration> sink) {
        this.averageMatchingDurationSink = sink;
    }

    @Override
    public void setMatchingTimeoutWarningStreamSink(SinkFunction<MatchingTimeoutWarning> sink) {
        this.matchingTimeoutWarningSink = sink;
    }

    @Override
    public void setTripFailedWarningStreamSink(SinkFunction<TripFailedWarning> sink) {
        this.tripFailedWarningSink = sink;
    }

    @Override
    public void setAlertStreamSink(SinkFunction<Alert> sink) {
        this.alertSink = sink;
    }
}