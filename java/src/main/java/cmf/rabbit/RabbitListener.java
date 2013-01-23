package cmf.rabbit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cmf.bus.Envelope;
import cmf.bus.IDisposable;
import cmf.bus.IEnvelopeFilterPredicate;
import cmf.bus.IRegistration;
import cmf.bus.berico.EnvelopeHelper;
import cmf.bus.berico.IEnvelopeReceivedCallback;
import cmf.rabbit.topology.Exchange;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.LongString;
import com.rabbitmq.client.QueueingConsumer;

public class RabbitListener extends Thread implements IDisposable {

    protected Channel channel;
    protected List<IListenerCloseCallback> closeCallbacks;
    /* fields */
    protected List<IEnvelopeReceivedCallback> envCallbacks;
    protected Exchange exchange;
    protected Logger log;
    protected IRegistration registration;
    protected boolean shouldContinue;

    public RabbitListener(IRegistration registration, Exchange exchange, Channel channel) {

        this.registration = registration;
        this.exchange = exchange;
        this.channel = channel;

        envCallbacks = new ArrayList<IEnvelopeReceivedCallback>();
        closeCallbacks = new ArrayList<IListenerCloseCallback>();

        log = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public void dispose() {
    	
    		log.info("Dispose called.");
    		
        shouldContinue = false;
    }

    @Override
    protected void finalize() {
        dispose();
    }

    public void onClose(IListenerCloseCallback callback) {
        closeCallbacks.add(callback);
    }

    /* events */
    public void onEnvelopeReceived(IEnvelopeReceivedCallback callback) {
        envCallbacks.add(callback);
    }

    protected void raise_onCloseEvent(IRegistration registration) {
        for (IListenerCloseCallback callback : closeCallbacks) {
            try {
                callback.onClose(registration);
            } catch (Exception ex) {
                log.error("Caught an unhandled exception raising the listener close event", ex);
            }
        }
    }

    protected void raise_onEnvelopeReceivedEvent(RabbitEnvelopeDispatcher dispatcher) {
        for (IEnvelopeReceivedCallback callback : envCallbacks) {
            try {
                callback.handleReceive(dispatcher);
            } catch (Exception ex) {
                log.error("Caught an unhandled exception raising the envelope received event", ex);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        log.debug("Enter Start");
        shouldContinue = true;

        try {
            // first, declare the exchange and queue
            channel.exchangeDeclare(exchange.getName(), exchange.getExchangeType(), exchange.getIsDurable(),
                            exchange.getIsAutoDelete(), exchange.getArguments());
            channel.queueDeclare(exchange.getQueueName(), exchange.getIsDurable(), false, exchange.getIsAutoDelete(),
                            exchange.getArguments());
            channel.queueBind(exchange.getQueueName(), exchange.getName(), exchange.getRoutingKey(),
                            exchange.getArguments());

            // next(), create a basic consumer
            QueueingConsumer consumer = new QueueingConsumer(channel);

            // and tell it to start consuming messages, storing the consumer tag
            String consumerTag = channel.basicConsume(exchange.getQueueName(), false, consumer);

            log.debug("Will now continuously listen for events using routing key: " + exchange.getRoutingKey());
            while (shouldContinue) {
                try {

                    QueueingConsumer.Delivery result = consumer.nextDelivery(100);
                    
                    if (null == result) {
                        continue;
                    }
                    
                    log.debug("Got something.");

                    EnvelopeHelper env = new EnvelopeHelper(new Envelope());
                    env.setReceiptTime(DateTime.now());
                    env.setPayload(result.getBody());

                    for (Entry<String, Object> prop : result.getProperties().getHeaders().entrySet()) {

                        try {
                            String key = prop.getKey();
                            byte[] valueBytes = ((LongString) prop.getValue()).getBytes();
                            String value = new String(valueBytes, "UTF-8");

                            env.setHeader(key, value);
                        } catch (Exception ex) {
                            log.debug("couldn't get property", ex);
                        }
                    }

                    log.debug("Incoming event headers: " + env.flatten());

                    if (shouldRaiseEvent(registration.getFilterPredicate(), env.getEnvelope())) {
                        RabbitEnvelopeDispatcher dispatcher =
                                        new RabbitEnvelopeDispatcher(registration, env.getEnvelope(), channel, result
                                                        .getEnvelope().getDeliveryTag());
                        raise_onEnvelopeReceivedEvent(dispatcher);
                    }
                } catch (InterruptedException interruptedException) {
                	
                		log.error("Listener interrupted...", interruptedException);
                    // The consumer was removed, either through
                    // channel or connection closure, or through the
                    // action of IModel.BasicCancel().
                    shouldContinue = false;
                } catch (Exception ex) {
                    log.warn("Caught an exception, but will not stop listening for messages", ex);
                }
            }
            log.debug("No longer listening for events");

            try {
                channel.basicCancel(consumerTag);
            } catch (IOException ex) {
            }
        } catch (Exception ex) {
            log.error("Caught an exception that will cause the listener to not listen for messages", ex);
        }

        raise_onCloseEvent(registration);
        log.debug("Leave Start");
    }

    protected boolean shouldRaiseEvent(IEnvelopeFilterPredicate filter, Envelope env) {
        // if there's no filter, the client wants it. Otherwise, see if they want it.
        boolean clientWantsIt = null == filter ? true : filter.filter(env);

        log.debug("Client's transport filter returns: " + clientWantsIt);
        return clientWantsIt;
    }

    public void stopListening() {
        log.debug("Enter Stop");
        shouldContinue = false;
        log.debug("Leave Stop");
    }
}