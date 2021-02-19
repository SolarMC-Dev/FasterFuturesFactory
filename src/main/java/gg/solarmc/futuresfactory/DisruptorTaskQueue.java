package gg.solarmc.futuresfactory;

import com.lmax.disruptor.WaitStrategy;
import gg.solarmc.futuresfactory.queue.ElementQueue;
import gg.solarmc.futuresfactory.queue.PollableElementQueue;
import gg.solarmc.futuresfactory.queue.QueueCreator;
import space.arim.managedwaits.TaskQueue;

class DisruptorTaskQueue implements TaskQueue {

    private final PollableElementQueue<Runnable> elementQueue;

    DisruptorTaskQueue(PollableElementQueue<Runnable> elementQueue) {
        this.elementQueue = elementQueue;
    }

    static TaskQueue create(WaitStrategy waitStrategy) {
        ElementQueue<Runnable> queue = QueueCreator.create(waitStrategy, 512);
        return new DisruptorTaskQueue(queue.attachHandler(Runnable::run));
    }

    @Override
    public boolean addTask(Runnable task) {
        elementQueue.add(task);
        return true;
    }

    @Override
    public void pollAndRunAll() {
        elementQueue.pollUsingAttachedHandler();
    }
}
