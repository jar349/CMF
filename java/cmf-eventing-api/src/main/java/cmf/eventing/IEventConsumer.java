package cmf.eventing;

import cmf.bus.IDisposable;
import cmf.bus.IEnvelopeFilterPredicate;

/**
 * An interface to define the methods by which an client may register to receive
 * events of a particular type.
 */
public interface IEventConsumer extends IDisposable {

	/**
	 * This method is used register an {@link IEventHandler} to handle all 
	 * received events of a particular type.
	 * 
	 * @param handler The event handler that will handle the received events.
	 * 
	 * @throws Exception
	 */
    <TEVENT> void subscribe(IEventHandler<TEVENT> handler) throws Exception;

    /**
	 * This method is used register an {@link IEventHandler} to handle only 
	 * select events of a particular type based on a predicate filter.
     * 
	 * @param handler The event handler that will handle the received events.
     * @param eventFilterPredicate The filter predicate to use in selecting 
     * the events which will be forwarded to the handler for processing.
	 *
     * @throws Exception
     */
    <TEVENT> void subscribe(IEventHandler<TEVENT> handler, IEnvelopeFilterPredicate eventFilterPredicate) throws Exception;
}
