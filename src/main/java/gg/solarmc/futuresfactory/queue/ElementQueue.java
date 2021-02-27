package gg.solarmc.futuresfactory.queue;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.InsufficientCapacityException;
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

    /**
     * When the ring buffer is full, synchronize on enqueue using this object
     */
    private final Object mutex = new Object();

    ElementQueue(RingBuffer<ElementEvent<E>> ringBuffer, EventPoller<ElementEvent<E>> poller) {
        this.ringBuffer = ringBuffer;
        this.poller = poller;
    }

    /**
     * Adds an item to the queue. Blocks if the queue is at capacity. <br>
     * <br>
     * May be safely called across threads, with one exception. Care must be taken when enqueueing on the same
     * thread from which {@link #pollUsing(ElementHandler)} or {@link PollableElementQueue#pollUsingAttachedHandler()}
     * is called. Such self-enqueueing may cause the queue to fill up before its contents can be polled and emptied.
     *
     * @param element the element to add
     */
    public void add(E element) {
        long sequence;
        try {
            sequence = ringBuffer.tryNext();
        } catch (InsufficientCapacityException ex) {
            synchronized (mutex) {
                sequence = ringBuffer.next();
            }
        }
        try {
            ElementEvent<E> event = ringBuffer.get(sequence);
            event.setElement(element);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    /**
     * Provides an estimate as to the amount of space left in the queue. Often useful for monitoring purposes.
     *
     * @return an estimate of the remaining capacity
     */
    public long remainingCapacity() {
        return ringBuffer.remainingCapacity();
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
