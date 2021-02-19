package gg.solarmc.futuresfactory.queue;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;

/**
 * A slimmed down queue of elements, which does not support the full queue interface
 * of {@code java.util.Queue}
 *
 * @param <E> the element
 */
public class ElementQueue<E> {

    private final RingBuffer<ElementEvent<E>> ringBuffer;
    final EventPoller<ElementEvent<E>> poller;

    ElementQueue(RingBuffer<ElementEvent<E>> ringBuffer, EventPoller<ElementEvent<E>> poller) {
        this.ringBuffer = ringBuffer;
        this.poller = poller;
    }

    /**
     * Adds an item to the queue. Blocks if the queue is at capacity. <br>
     * <br>
     * May be safely called across threads.
     *
     * @param element the element to add
     */
    public void add(E element) {
        long sequence = ringBuffer.next();
        try {
            ElementEvent<E> event = ringBuffer.get(sequence);
            event.setElement(element);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    /**
     * Empties all the elements in this queue, running the specified handler
     * for each element. <br>
     * <br>
     * <b>Should only be called by the owning thread</b>
     *
     * @param handler the element handler
     */
    public void pollUsing(ElementHandler<E> handler) {
        var pollHandler = new ElementHandlerAsPollerHandler<>(handler);
        try {
            poller.poll(pollHandler);
        } catch (Exception ex) {
            throw new RuntimeException("Exception while polling elements", ex);
        }
    }

    /**
     * Creates a pollable element queue from this one
     *
     * @param handler the handler to attach
     * @return the pollable element queue
     */
    public PollableElementQueue<E> attachHandler(ElementHandler<E> handler) {
        return new PollableElementQueue<>(ringBuffer, poller, new ElementHandlerAsPollerHandler<>(handler));
    }
}
