package dst.ass3.event.impl;

import dst.ass3.event.Constants;
import dst.ass3.event.EventPublisher;
import dst.ass3.event.EventSubscriber;
import dst.ass3.event.IEventSourceFunction;
import dst.ass3.event.model.domain.ITripEventInfo;
import org.apache.flink.api.common.functions.IterationRuntimeContext;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.guava30.com.google.common.util.concurrent.AtomicDouble;

import java.net.InetSocketAddress;

public class EventSourceFunction implements IEventSourceFunction {

    private boolean isRunning;
    private EventSubscriber eventSubscriber;
    private EventPublisher eventPublisher;
    private RuntimeContext runtimeContext;

    @Override
    public void open(Configuration parameters) {
        setupResources();
    }

    private void setupResources() {
        eventPublisher = new EventPublisher(Constants.EVENT_PUBLISHER_PORT);
        eventSubscriber = EventSubscriber.subscribe(new InetSocketAddress(Constants.EVENT_PUBLISHER_PORT));
    }

    @Override
    public void close() {
        releaseResources();
    }

    private void releaseResources() {
        eventPublisher.close();
        eventSubscriber.close();

    }

    @Override
    public IterationRuntimeContext getIterationRuntimeContext() {
        //todo
        return null;
    }

    @Override
    public RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    @Override
    public void run(SourceContext<ITripEventInfo> ctx) {
        executeEventProcessing(ctx);
    }

    private void executeEventProcessing(SourceContext<ITripEventInfo> ctx) {
        isRunning = true;
        while (isRunning) {
            ITripEventInfo eventInfo = fetchEventInfo();
            if (eventInfo == null) {
                stopRunning();
                break;
            }
            collectEventInfo(ctx, eventInfo);
        }
    }

    private ITripEventInfo fetchEventInfo() {
        return eventSubscriber.receive();
    }

    private void stopRunning() {
        isRunning = false;
    }

    private void collectEventInfo(SourceContext<ITripEventInfo> ctx, ITripEventInfo eventInfo) {
        ctx.collectWithTimestamp(eventInfo, eventInfo.getTimestamp());
    }
    @Override
    public void cancel() {
        isRunning = false;
    }
}