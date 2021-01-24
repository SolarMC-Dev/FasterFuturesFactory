package gg.solarmc.futuresfactory;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import space.arim.managedwaits.TaskQueue;

class DisruptorTaskQueue implements TaskQueue {

	private final RingBuffer<TaskEvent> ringBuffer;
	private final EventPoller<TaskEvent> poller;
	private final EventPoller.Handler<TaskEvent> handler;

	DisruptorTaskQueue(RingBuffer<TaskEvent> ringBuffer, EventPoller<TaskEvent> poller,
					   EventPoller.Handler<TaskEvent> handler) {
		this.ringBuffer = ringBuffer;
		this.poller = poller;
		this.handler = handler;
	}

	@Override
	public boolean addTask(Runnable task) {
		long sequence = ringBuffer.next();
		try {
			TaskEvent event = ringBuffer.get(sequence);
			event.setTask(task);
		} finally {
			ringBuffer.publish(sequence);
		}
		return true;
	}

	@Override
	public void pollAndRunAll() {
		try {
			poller.poll(handler);
		} catch (Exception ex) {
			throw new RuntimeException("Unexpected exception while polling sync tasks", ex);
		}
	}

	static TaskQueue create(WaitStrategy waitStrategy) {
		RingBuffer<TaskEvent> ringBuffer = RingBuffer.createMultiProducer(TaskEvent::new, 512, waitStrategy);
		EventPoller<TaskEvent> poller = ringBuffer.newPoller();
		ringBuffer.addGatingSequences(poller.getSequence());
		return new DisruptorTaskQueue(ringBuffer, poller, new HandlerImpl());
	}

	private static class HandlerImpl implements EventPoller.Handler<TaskEvent> {

		@Override
		public boolean onEvent(TaskEvent event, long sequence, boolean endOfBatch) throws Exception {
			event.getTask().run();
			event.setTask(null);
			return true;
		}
	}
}
