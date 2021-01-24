package gg.solarmc.futuresfactory;

import space.arim.managedwaits.TaskQueue;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Objects;

/**
 * A pairing of a {@link FactoryOfTheFuture} and a {@link TaskQueue}
 *
 */
public final class FactoryAndTaskQueue {

	private final FactoryOfTheFuture futuresFactory;
	private final TaskQueue taskQueue;

	/**
	 * Creates
	 *
	 * @param futuresFactory the futures factory
	 * @param taskQueue the task queue
	 * @throws NullPointerException if either parameter is null
	 */
	public FactoryAndTaskQueue(FactoryOfTheFuture futuresFactory, TaskQueue taskQueue) {
		this.futuresFactory = Objects.requireNonNull(futuresFactory);
		this.taskQueue = Objects.requireNonNull(taskQueue);
	}

	public FactoryOfTheFuture futuresFactory() {
		return futuresFactory;
	}

	public TaskQueue taskQueue() {
		return taskQueue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FactoryAndTaskQueue that = (FactoryAndTaskQueue) o;
		return futuresFactory == that.futuresFactory && taskQueue == that.taskQueue;
	}

	@Override
	public int hashCode() {
		int result = System.identityHashCode(futuresFactory);
		result = 31 * result + System.identityHashCode(taskQueue);
		return result;
	}
}
