package cmf.bus.eventing.berico;

import java.util.Map;

import cmf.bus.Envelope;
import cmf.bus.berico.rabbit.support.RabbitEnvelopeHelper;
import cmf.bus.eventing.IInboundEventProcessor;
import cmf.bus.eventing.IOutboundEventProcessor;
import cmf.bus.eventing.ISerializer;

public class SerializationProcessor implements IInboundEventProcessor, IOutboundEventProcessor {

    private ISerializer serializer;

    public SerializationProcessor(ISerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public Object processInbound(Object event, Envelope envelope, Map<String, Object> context) {
        try {
            String eventType = RabbitEnvelopeHelper.Headers.getType(envelope);
            Class<?> type = Class.forName(eventType);
            event = serializer.byteDeserialize(envelope.getPayload(), type);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing event");
        }

        return event;
    }

    @Override
    public Object processOutbound(Object event, Envelope envelope, Map<String, Object> context) {
        byte[] payload = serializer.byteSerialize(event);
        envelope.setPayload(payload);
        RabbitEnvelopeHelper.Headers.setType(envelope, event);

        return event;
    }
}
