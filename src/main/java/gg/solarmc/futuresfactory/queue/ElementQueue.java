package gg.solarmc.futuresfactory.queue;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.locks.LockSupport;

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
        long sequence = acquireSequence();
        try {
            ElementEvent<E> event = ringBuffer.get(sequence);
            event.setElement(element);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    private long acquireSequence() {
        try {
            return ringBuffer.tryNext();
        } catch (InsufficientCapacityException ignored) { }
        /*
         * This approach makes some improvements to RingBuffer#next which better prioritize
         * CPU cycles over latency. The disruptor is designed for extremely low latency,
         * so RingBuffer#next will run a quite busy spin loop using LockSupport.parkNanos(1)
         *
         * The synchronization on enqueue when the buffer is full is an idea taken from log4j
         * per an issue thread. The LockSupport.parkNanos(200L) spin loop is very similar to
         * the loop in SleepingWaitStrategy. Both solutions halt the operations of enqueing
         * threads, so the consuming thread may have time to process the buffer.
         */
        synchronized (mutex) {
            while (true) {
                try {
                    return ringBuffer.tryNext();
                } catch (InsufficientCapacityException ignored) { }
                LockSupport.parkNanos(200L);
            }
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
