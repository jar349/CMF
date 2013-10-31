﻿using System;
using System.Collections.Generic;
using cmf.bus;

namespace cmf.eventing
{
    /// <summary>
    /// An interface to define the methods by which an client may register to receive
    /// of a particular type.
    /// </summary>
    /// <seealso cref="IEventBus"/>
    public interface IEventConsumer : IDisposable
    {
        /// <summary>
        /// Registers an <see cref="IEventHandler" /> to handle 
        /// a specific type of event (TEVENT) from the bus and further filters 
        /// the events received based on a predicate filter.
        /// </summary>        
        /// <param name="handler">The IEventHandler implementation that will 
        /// handle the event.</param>
        /// <param name="envelopeFilter">The filter predicate to use in selecting 
        /// the events which will be forwarded to the handler for processing.</param>
        /// <exception cref="System.Exception">May throw an exception.</exception>
        void Subscribe(IEventHandler handler, Predicate<Envelope> envelopeFilter);

        /// <summary>
        /// Registers a handler via an <see cref="Action" /> to handle 
        /// a specific type of event (TEVENT) from the bus and further filters 
        /// the events received based on a predicate filter.
        /// </summary>
        /// <typeparam name="TEvent">The specified type of event to listen for.</typeparam>
        /// <param name="handler">Action that handles for the TEvent type. The 
        /// IDictionary received are the message headers sent with the event.</param>
        /// <param name="envelopeFilter">The filter predicate to use in selecting 
        /// the events which will be forwarded to the handler for processing.</param>
        /// <exception cref="System.Exception">May throw an exception.</exception>
        void Subscribe<TEvent>(Action<TEvent, IDictionary<string, string>> handler, Predicate<Envelope> envelopeFilter) where TEvent : class;
    }

    /// <summary>
    /// This class defines extension methods that effectively provide overload to the methods defined on the IEventConsumer interface.
    /// </summary>
    /// <remarks>Defining overloads to the IEventConsumer methods as extensions
    /// rather than as members of the interface provides two advantages.  First
    /// it simplified the job of implementing the interface.  Second it ensures 
    /// that all implementations behave in the same manner with respects to the
    /// the overloads.  </remarks>
    /// <seealso cref="IEventConsumer"/>
    public static class EventConsumerExtensions
    {
        /// <summary>
        /// Registers an <see cref="IEventHandler" /> to handle 
        /// all events of a specific type of event (TEVENT) received by the bus.
        /// </summary>
        /// <param name="consumer">The consumer that will manage the subscription.</param>
        /// <param name="handler">The IEventHandler implementation that will 
        /// handle the event.</param>
        /// <exception cref="System.Exception">May throw an exception.</exception>
        public static void Subscribe(this IEventConsumer consumer, IEventHandler handler)
        {
            consumer.Subscribe(handler, envelope => true);
        }

        /// <summary>
        /// Registers a handler via an <see cref="Action" /> to handle 
        /// all events of a specific type of event (TEVENT) received by the bus.
        /// </summary>
        /// <param name="consumer">The consumer that will manage the subscription.</param>
        /// <typeparam name="TEvent">The specified type of event to listen for.</typeparam>
        /// <param name="handler">Action that handles for the TEvent type. The 
        /// IDictionary received are the message headers sent with the event.</param>
        /// <exception cref="System.Exception">May throw an exception.</exception>
        public static void Subscribe<TEvent>(this IEventConsumer consumer, Action<TEvent, IDictionary<string, string>> handler)
             where TEvent : class
        {
            consumer.Subscribe(handler, envelope => true);
        }
    }
}
