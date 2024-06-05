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
    private SinkFunction<LifecycleEvent> lifecycleEventSink;
    private SinkFunction<MatchingDuration> matchingDurationSink;
    private SinkFunction<AverageMatchingDuration> averageMatchingDurationSink;
    private SinkFunction<MatchingTimeoutWarning> matchingTimeoutWarningSink;
    private SinkFunction<TripFailedWarning> tripFailedWarningSink;
    private SinkFunction<Alert> alertSink;

    @Override
    public void initialize(StreamExecutionEnvironment env) {
        EventSourceFunction source = createEventSourceFunction();
        var stream = setupStream(env, source);
        addLifecycleEventSink(stream);
    }

    private EventSourceFunction createEventSourceFunction() {
        return new EventSourceFunction();
    }

    private SingleOutputStreamOperator<LifecycleEvent> setupStream(StreamExecutionEnvironment env, EventSourceFunction source) {
        var watermarkStrategy = createWatermarkStrategy();
        return env.addSource(source)
                .filter(info -> info.getRegion() != null)
                .map(LifecycleEvent::new)
                .assignTimestampsAndWatermarks(watermarkStrategy);
    }

    private WatermarkStrategy<LifecycleEvent> createWatermarkStrategy() {
        return WatermarkStrategy.<LifecycleEvent>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                .withTimestampAssigner((event, timestamp) -> event.getTimestamp());
    }

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