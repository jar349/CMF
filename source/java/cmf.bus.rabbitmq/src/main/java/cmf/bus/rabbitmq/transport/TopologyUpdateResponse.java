package cmf.bus.rabbitmq.transport;

public class TopologyUpdateResponse {

    private TopologyRegistry topologyRegistry;

    public TopologyRegistry getTopologyRegistry() {
        return topologyRegistry;
    }

    public void setTopologyRegistry(TopologyRegistry topologyRegistry) {
        this.topologyRegistry = topologyRegistry;
    }

}