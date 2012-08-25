package cmf.bus.core;

public interface ITransportProvider {

    void register(IRegistration registration);

    void send(IEnvelope envelope);

}
