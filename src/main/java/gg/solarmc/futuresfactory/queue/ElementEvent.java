package gg.solarmc.futuresfactory.queue;

import com.lmax.disruptor.EventFactory;

final class ElementEvent<E> {

    private E element;

    private ElementEvent() {}

    E getElement() {
        return element;
    }

    void setElement(E element) {
        this.element = element;
    }

    void clear() {
        element = null;
    }

    static <E> EventFactory<ElementEvent<E>> factory() {
        return ElementEvent::new;
    }

}
