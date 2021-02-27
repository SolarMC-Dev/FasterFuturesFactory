package gg.solarmc.futuresfactory;

import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import space.arim.managedwaits.LightSleepManagedWaitStrategy;
import space.arim.managedwaits.ManagedWaitStrategy;
import space.arim.managedwaits.TaskQueue;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Objects;

/**
 * Builder of effective {@link FactoryOfTheFuture} implementations. <br>
 * <br>
 * The {@code build} method returns both the futures factory and its task queue. The task queue's
 * {@code pollAndRunAll()} should be run periodically to fulfill the system. <br>
 * <br>
 * In most cases, tasks should be added to the futures factory, and not with the task queue's {@code add} method.
 * This is because of a difference in behavior when adding a task while on the main thread. If the calling code
 * attempts to run a sync task using the futures factory, the futures factory will run that task immediately,
 * whereas if the task queue was used, the task queue will add it to the back of the queue. Therefore, if the task queue
 * is used, and more tasks are added than the capacity of the queue within the interval of polling the queue,
 * the queue will fill up.
 *
 */
public final class FasterFuturesFactory {

	private int capacityHint = 512;
	private ManagedWaitStrategy futuresWaitStrategy;
	private WaitStrategy disruptorWaitStrategy;

	/**
	 * Creates the builder
	 *
	 */
	public FasterFuturesFactory() {}

	/**
	 * Provides a hint as to the capacity of the underlying queue. The actual capacity used
	 * may be larger.
	 *
	 * @param capacityHint the capacity hint
	 * @return this builder
	 */
	public FasterFuturesFactory capacityHint(int capacityHint) {
		this.capacityHint = capacityHint;
		return this;
	}

	/**
	 * Sets the {@link ManagedWaitStrategy} used when awaiting the result of a future
	 *
	 * @param futuresWaitStrategy the managed wait strategy for created futures
	 * @return this builder
	 */
	public FasterFuturesFactory futuresWaitStrategy(ManagedWaitStrategy futuresWaitStrategy) {
		this.futuresWaitStrategy = Objects.requireNonNull(futuresWaitStrategy);
		return this;
	}

	/**
	 * Sets the disruptor {@link WaitStrategy} used when adding a task to run
	 *
	 * @param disruptorWaitStrategy the disruptor wait strategy for adding tasks
	 * @return this builder
	 */
	public FasterFuturesFactory disruptorWaitStrategy(WaitStrategy disruptorWaitStrategy) {
		this.disruptorWaitStrategy = Objects.requireNonNull(disruptorWaitStrategy);
		return this;
	}

	/**
	 * Creates a futures factory. Yields the task queue used by the factory and the factory itself. <br>
	 * <br>
	 * The returned task queue's {@code pollAndRunAll} method should be called periodically while on
	 * the main thread. <br>
	 * <br>
	 * May be used repeatedly without side effects.
	 *
	 * @param mainThread the main thread
	 * @return the factory and its task queue
	 */
	public FactoryAndTaskQueue build(Thread mainThread) {
		Objects.requireNonNull(mainThread, "main thread");
		var futuresWaitStrategy = Objects.requireNonNullElseGet(this.futuresWaitStrategy, LightSleepManagedWaitStrategy::new);
		var disruptorWaitStrategy = Objects.requireNonNullElseGet(this.disruptorWaitStrategy, SleepingWaitStrategy::new);

		TaskQueue taskQueue = DisruptorTaskQueue.create(disruptorWaitStrategy, capacityHint);
		return new FactoryAndTaskQueue(
				new KnownMainThreadFuturesFactory(taskQueue, futuresWaitStrategy, mainThread),
				taskQueue);
	}

}
