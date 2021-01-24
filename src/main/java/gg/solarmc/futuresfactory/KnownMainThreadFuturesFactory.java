package gg.solarmc.futuresfactory;

import space.arim.managedwaits.DeadlockFreeFutureFactory;
import space.arim.managedwaits.ManagedWaitStrategy;
import space.arim.managedwaits.TaskQueue;

class KnownMainThreadFuturesFactory extends DeadlockFreeFutureFactory {

	private final Thread mainThread;

	KnownMainThreadFuturesFactory(TaskQueue taskQueue, ManagedWaitStrategy waitStrategy, Thread mainThread) {
		super(taskQueue, waitStrategy);
		this.mainThread = mainThread;
	}

	@Override
	public boolean isPrimaryThread() {
		return Thread.currentThread() == mainThread;
	}

	@Override
	public Thread getPrimaryThread() {
		return mainThread;
	}

}
