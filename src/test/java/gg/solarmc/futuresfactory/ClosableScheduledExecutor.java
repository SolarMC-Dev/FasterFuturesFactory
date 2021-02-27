package gg.solarmc.futuresfactory;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.ScheduledExecutorService;

class ClosableScheduledExecutor implements ExtensionContext.Store.CloseableResource {

	private final ScheduledExecutorService scheduledExecutor;

	ClosableScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
		this.scheduledExecutor = scheduledExecutor;
	}

	ScheduledExecutorService scheduledExecutor() {
		return scheduledExecutor;
	}

	@Override
	public void close() throws Throwable {
		scheduledExecutor.shutdown();
	}
}
