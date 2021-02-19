package gg.solarmc.futuresfactory.queue;

import com.lmax.disruptor.EventPoller;

final class ElementHandlerAsPollerHandler<E> implements EventPoller.Handler<ElementEvent<E>> {

    private final ElementHandler<E> elementHandler;

    ElementHandlerAsPollerHandler(ElementHandler<E> elementHandler) {
        this.elementHandler = elementHandler;
    }

    @Override
    public boolean onEvent(ElementEvent<E> event, long sequence, boolean endOfBatch) throws Exception {
        elementHandler.handle(event.getElement());
        event.clear();
        return true;
    }

}
