﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;

using Common.Logging;

using cmf.bus;
using cmf.bus.berico;

namespace cmf.eventing.berico
{
    /// <summary>
    /// When you send a request of a type that you also subscribe to, it may be
    /// desirable to not receive your own requests.  This processor filters out
    /// requests that you have sent so that you do not receive them yourself.
    /// </summary>
    public class RpcFilter : IInboundEventProcessor, IOutboundEventProcessor
    {
        protected IList<Guid> _sentRequests;
        protected object _listLock = new object();
        protected ILog _log;


        public RpcFilter()
        {
            _sentRequests = new List<Guid>();

            _log = LogManager.GetLogger(this.GetType());
        }


        public bool ProcessInbound(ref object ev, ref Envelope env, ref IDictionary<string, object> context)
        {
            bool ourOwnRequest = false;

            try
            {
                if (env.IsRequest())
                {
                    Guid requestId = env.GetMessageId();

                    lock (_listLock)
                    {
                        if (_sentRequests.Contains(requestId))
                        {
                            _log.Info("Filtering out our own request: " + requestId.ToString());
                            ourOwnRequest = true;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _log.Error("Failed to inspect an incoming event for potential filtering", ex);
            }

            return !ourOwnRequest;
        }

        public void ProcessOutbound(ref object ev, ref Envelope env, IDictionary<string, object> context)
        {
            if (env.IsRequest())
            {
                Guid requestId = env.GetMessageId();
                TimeSpan timeout = env.GetRpcTimeout();

                lock (_listLock)
                {
                    _log.Debug(string.Format("Adding requestId {0} to the RPC Filter list", requestId.ToString()));
                    _sentRequests.Add(requestId);
                }

                if (timeout.IsNotEmpty())
                {
                    Timer gc = new Timer(this.RequestTimeout_GarbageCollect, requestId, timeout.Add(timeout), TimeSpan.Zero);
                }
                else
                {
                    _log.Warn(string.Format(
                        "Request {0} was sent without a timeout: it will never be removed from the RPC Filter list", 
                        requestId.ToString()));
                }
            }
        }

        public void RequestTimeout_GarbageCollect(object requestGuid)
        {
            Guid requestId = (Guid)requestGuid;

            lock (_listLock)
            {
                _log.Debug(string.Format("Removing requestId {0} from the RPC Filter list", requestId.ToString()));
                _sentRequests.Remove(requestId);
            }
        }
    }




    public static class TimeSpanExtensions
    {
        public static bool IsNotEmpty(this TimeSpan timeout)
        {
            return !TimeSpan.Zero.Equals(timeout);
        }
    }
}
