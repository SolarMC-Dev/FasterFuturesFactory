package gg.solarmc.futuresfactory.queue;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;

/**
 * Entry point for obtaining queues
 *
 */
public final class QueueCreator {

    private QueueCreator() {}

    private static int ceilingNextPowerOfTwo(final int x) {
        return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
    }

    /**
     * Creates with the given details
     *
     * @param waitStrategy the disruptor wait strategy
     * @param capacityHint a hint for the capacity, typically taken as a minimum value
     * @param <E> the element type
     * @return the element queue
     */
    public static <E> ElementQueue<E> create(WaitStrategy waitStrategy, int capacityHint) {
        int capacity = ceilingNextPowerOfTwo(capacityHint);
        RingBuffer<ElementEvent<E>> ringBuffer = RingBuffer.createMultiProducer(ElementEvent.factory(), capacity, waitStrategy);
        EventPoller<ElementEvent<E>> poller = ringBuffer.newPoller();
        ringBuffer.addGatingSequences(poller.getSequence());
        return new ElementQueue<>(ringBuffer, poller);
    }

}
