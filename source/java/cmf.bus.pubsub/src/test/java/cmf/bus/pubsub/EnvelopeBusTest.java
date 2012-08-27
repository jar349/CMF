package cmf.bus.pubsub;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import cmf.bus.core.DeliveryOutcome;
import cmf.bus.core.IEnvelope;
import cmf.bus.core.IEnvelopeHandler;
import cmf.bus.core.IRegistration;
import cmf.bus.core.processor.IInboundProcessor;
import cmf.bus.core.processor.IOutboundProcessor;
import cmf.bus.core.transport.ITransportProvider;
import cmf.bus.pubsub.EnvelopeBus;

@SuppressWarnings("unchecked")
public class EnvelopeBusTest {

    @Mock
    private Map<String, Object> context;
    @Mock
    private IEnvelope envelope;
    private EnvelopeBus envelopeBus;
    @Mock
    private IInboundProcessor inboundProcessor;
    private Collection<IInboundProcessor> inboundProcessorCollection = new LinkedList<IInboundProcessor>();
    @Mock
    private IOutboundProcessor outboundProcessor;
    private Collection<IOutboundProcessor> outboundProcessorCollection = new LinkedList<IOutboundProcessor>();
    @Mock
    private IRegistration registration;
    @Mock
    private ITransportProvider transportProvider;
    private IEnvelopeHandler envelopeBusRegistrationEnvelopeHandler;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doAnswer(new Answer<Object>() {
            
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                envelopeBusRegistrationEnvelopeHandler = (IEnvelopeHandler) invocation.getArguments()[0];
                
                return null;
            }
            
        }).when(registration).setEnvelopeHandler(any(IEnvelopeHandler.class));
        registration.setEnvelopeHandler(new IEnvelopeHandler() {

            @Override
            public DeliveryOutcome receive(IEnvelope envelope) {
                return DeliveryOutcome.Acknowledge;
            }
            
        });
        inboundProcessorCollection.add(inboundProcessor);
        outboundProcessorCollection.add(outboundProcessor);
        envelopeBus = new EnvelopeBus();
        envelopeBus.setInboundProcessorCollection(inboundProcessorCollection);
        envelopeBus.setOutboundProcessorCollection(outboundProcessorCollection);
        envelopeBus.setTransportProvider(transportProvider);
    }

    @Test
    public void inboundExceptionDoesntThrow() {
        doThrow(RuntimeException.class).when(inboundProcessor).processInbound(any(IEnvelope.class), any(Map.class));
        envelopeBus.register(registration);
        envelopeBusRegistrationEnvelopeHandler.receive(envelope);
    }

    @Test(expected = RuntimeException.class)
    public void outboundExceptionThrows() {
        doThrow(RuntimeException.class).when(outboundProcessor).processOutbound(any(IEnvelope.class), any(Map.class));
        envelopeBus.send(envelope);
    }

    @Test
    public void receiveCallsProcessors() {
        envelopeBus.register(registration);
        envelopeBusRegistrationEnvelopeHandler.receive(envelope);
        verify(inboundProcessor).processInbound(any(IEnvelope.class), any(Map.class));
    }

    @Test
    public void registerCallsTransportRegister() {
        envelopeBus.register(registration);
        verify(transportProvider).register(any(IRegistration.class));
    }

    @Test
    public void sendCallsProcessors() {
        envelopeBus.send(envelope);
        verify(outboundProcessor).processOutbound(any(IEnvelope.class), any(Map.class));
    }

    @Test
    public void sendCallsTransportSend() {
        envelopeBus.send(envelope);
        verify(transportProvider).send(any(IEnvelope.class));
    }

    @Test(expected = RuntimeException.class)
    public void transportRegisterExceptionThrows() {
        doThrow(RuntimeException.class).when(transportProvider).register(any(IRegistration.class));
        envelopeBus.register(registration);
    }

    @Test(expected = RuntimeException.class)
    public void transportSendExceptionThrows() {
        doThrow(RuntimeException.class).when(transportProvider).send(any(IEnvelope.class));
        envelopeBus.send(envelope);
    }

}