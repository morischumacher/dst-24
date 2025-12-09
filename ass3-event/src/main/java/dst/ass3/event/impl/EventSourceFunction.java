package dst.ass3.event.impl;

import dst.ass3.event.Constants;
import dst.ass3.event.EventPublisher;
import dst.ass3.event.EventSubscriber;
import dst.ass3.event.IEventSourceFunction;
import dst.ass3.event.model.domain.ITripEventInfo;
import org.apache.flink.api.common.functions.IterationRuntimeContext;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;

import java.net.InetSocketAddress;

public class EventSourceFunction implements IEventSourceFunction {

    // is the event processing running
    private boolean isRunning;
    // subscriber (consumer) of the event
    private EventSubscriber eventSubscriber;
    // publisher (producer) of the event
    private EventPublisher eventPublisher;
    // runtime context function
    private RuntimeContext runtimeContext;

    // open run time context function
    @Override
    public void open(Configuration parameters) {
        setupResources();
    }

    // setup resources function
    private void setupResources() {
        eventPublisher = new EventPublisher(Constants.EVENT_PUBLISHER_PORT);
        eventSubscriber = EventSubscriber.subscribe(new InetSocketAddress(Constants.EVENT_PUBLISHER_PORT));
    }

    // close function
    @Override
    public void close() {
        releaseResources();
    }

    // release resources function
    private void releaseResources() {
        eventPublisher.close();
        eventSubscriber.close();

    }

    @Override
    public IterationRuntimeContext getIterationRuntimeContext() {
        //todo
        return null;
    }

    // get runtime context function
    @Override
    public RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    // set runtime context function
    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }


    @Override
    public void run(SourceContext<ITripEventInfo> ctx) {
        executeEventProcessing(ctx);
    }

    // execute event processing function as long as the event processing is running
    private void executeEventProcessing(SourceContext<ITripEventInfo> ctx) {
        isRunning = true;
        while (isRunning) {
            // fetch event info from the event subscriber
            ITripEventInfo eventInfo = fetchEventInfo();
            if (eventInfo == null) {
                cancel();
                break;
            }
            // collect event info if it is not null
            collectEventInfo(ctx, eventInfo);
        }
    }

    // fetch event info function by receiving the event info from the event subscriber
    private ITripEventInfo fetchEventInfo() {
        return eventSubscriber.receive();
    }


    private void collectEventInfo(SourceContext<ITripEventInfo> ctx, ITripEventInfo eventInfo) {
        ctx.collectWithTimestamp(eventInfo, eventInfo.getTimestamp());
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}