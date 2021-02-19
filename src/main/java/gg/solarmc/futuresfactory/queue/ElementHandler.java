package gg.solarmc.futuresfactory.queue;

/**
 * Action run for polled elements
 *
 * @param <E> the element type
 */
public interface ElementHandler<E> {

    /**
     * Handles an element which was polled from the queue
     *
     * @param element the element
     * @throws RuntimeException any unchecked exceptions are propagated to
     * {@link ElementQueue#pollUsing(ElementHandler)} and {@link PollableElementQueue#pollUsingAttachedHandler()}
     */
    void handle(E element);

}
