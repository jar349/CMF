package cmf.eventing.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.joda.time.Duration;

import cmf.bus.IEnvelopeFilterPredicate;
import cmf.eventing.IEventHandler;
import cmf.eventing.patterns.rpc.IRpcEventBus;

public class MemoryEventBus implements IRpcEventBus {

    final Map<Class<?>, IEventHandler<?>> handlers = new HashMap<>();

    final Map<Class<?>, ExecutorService> queues = new HashMap<>();

    // This is a simple LRU for handling response routing for RPC requests.
    final Map<Map<String, String>, Object> temporaryQueues = new LinkedHashMap<Map<String, String>, Object>() {

        /**
         * Generated serial version UID.
         */
        private static final long serialVersionUID = -5639837634940418430L;

        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 10;
        }

    };

    public MemoryEventBus() {
    }

    public MemoryEventBus(List<IEventHandler> register) throws Exception {
        for (IEventHandler handler : register) {
            subscribe(handler);
        }
    }

    @Override
    public void publish(final Object event) throws Exception {
        publish(event, new HashMap<String, String>());
    }

    @Override
    public void publish(final Object event, Map<String, String> arg1)
            throws Exception {
        queues.get(event.getClass()).execute(new Runnable() {

            @Override
            public void run() {
                IEventHandler handler = handlers.get(event.getClass());
                handler.handle(event, new HashMap<String, String>());
            }

        });
    }

    @Override
    public <TEVENT> void subscribe(IEventHandler<TEVENT> handler)
            throws Exception {
        queues.put(handler.getEventType(), Executors.newSingleThreadExecutor());
        handlers.put(handler.getEventType(), handler);
    }

    @Override
    public <TEVENT> void subscribe(IEventHandler<TEVENT> handler,
            IEnvelopeFilterPredicate arg1) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void respondTo(Map<String, String> context, Object response) {
        temporaryQueues.put(context, response);
    }

    @Override
    public <TResponse> Collection<TResponse> gatherResponsesTo(Object arg0,
            Duration arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection gatherResponsesTo(Object arg0, Duration arg1,
            String... arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <TResponse> TResponse getResponseTo(final Object request,
            Duration duration, Class<TResponse> arg2) {
        final Class<?> requestKey = request.getClass();
        if (!queues.containsKey(requestKey)) {
            throw new IllegalStateException("No Handler Registered for: "
                    + requestKey);
        }
        final Future<TResponse> response = queues.get(requestKey).submit(
                new Callable<TResponse>() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public TResponse call() {
                        @SuppressWarnings("rawtypes")
                        IEventHandler handler = handlers.get(request.getClass());
                        Map<String, String> context = new HashMap<String, String>();
                        // TODO Determine how we intend to handle switching
                        // identities in the future.
                        context.put("cmf.bus.message.sender.identity", "CN=me");
                        // System.err.println(":) " + handler + " " + context);

                        handler.handle(request, context);
                        return (TResponse) temporaryQueues.get(context);
                    }

                });

        try {
            return response.get(duration.getMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getResponseTo(final Object event, Duration duration,
            String arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

}
