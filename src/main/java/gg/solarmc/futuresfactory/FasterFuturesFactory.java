package gg.solarmc.futuresfactory;

import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import space.arim.managedwaits.LightSleepManagedWaitStrategy;
import space.arim.managedwaits.ManagedWaitStrategy;
import space.arim.managedwaits.TaskQueue;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Objects;

/**
 * Builder of effective {@link FactoryOfTheFuture} implementations
 *
 */
public final class FasterFuturesFactory {

	private ManagedWaitStrategy futuresWaitStrategy;
	private WaitStrategy disruptorWaitStrategy;

	/**
	 * Creates the builder
	 *
	 */
	public FasterFuturesFactory() {}

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

		TaskQueue taskQueue = DisruptorTaskQueue.create(disruptorWaitStrategy);
		return new FactoryAndTaskQueue(
				new KnownMainThreadFuturesFactory(taskQueue, futuresWaitStrategy, mainThread),
				taskQueue);
	}

}
