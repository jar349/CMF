package cmf.bus.berico.rabbit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.rabbitmq.client.Channel;

import cmf.bus.IRegistration;
import cmf.bus.berico.IEnvelopeDispatcher;
import cmf.bus.berico.rabbit.support.RabbitRegistrationHelper;

public class QueueProvider {

    public static final int DEFAULT_QUEUE_LIFETIME = 1000 * 60 * 30; // 30 minute lifetime

    private IEnvelopeDispatcher envelopeDispatcher;
    private int queueLifetime = DEFAULT_QUEUE_LIFETIME;

    public QueueProvider(IEnvelopeDispatcher envelopeDispatcher) {
        this.envelopeDispatcher = envelopeDispatcher;
    }

    public void setQueueLifetime(int queueLifetime) {
        this.queueLifetime = queueLifetime;
    }

    public Queue newQueue(Channel channel, IRegistration registration) {
        try {
            String queueName = RabbitRegistrationHelper.RegistrationInfo.getQueueName(registration);
            if (queueName == null) {
                queueName = UUID.randomUUID().toString();
                RabbitRegistrationHelper.RegistrationInfo.setQueueName(registration, queueName);
            }
            String consumerTag = UUID.randomUUID().toString();
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("x-expires", queueLifetime);
            channel.queueDeclare(queueName, false, false, false, params);

            return new Queue(channel, registration, envelopeDispatcher, consumerTag);
        } catch (Exception e) {
            String queueName = RabbitRegistrationHelper.RegistrationInfo.getQueueName(registration);
            throw new RuntimeException(String.format("Error creating queue \"%s\"", queueName));
        }
    }
}
