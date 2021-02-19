package gg.solarmc.futuresfactory.queue;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;

/**
 * An element queue to which an element handler is attached.
 *
 * @param <E> the element type
 */
public final class PollableElementQueue<E> extends ElementQueue<E> {

    private final EventPoller.Handler<ElementEvent<E>> pollHandler;

    PollableElementQueue(RingBuffer<ElementEvent<E>> ringBuffer, EventPoller<ElementEvent<E>> poller,
                         EventPoller.Handler<ElementEvent<E>> pollHandler) {
        super(ringBuffer, poller);
        this.pollHandler = pollHandler;
    }

    /**
     * Empties all the elements in this queue, running the attached
     * handler for each element. <br>
     * <br>
     * <b>Should only be called by the owning thread</b>
     *
     */
    public void pollUsingAttachedHandler() {
        try {
            poller.poll(pollHandler);
        } catch (Exception ex) {
            throw new RuntimeException("Exception while polling elements", ex);
        }
    }
}
