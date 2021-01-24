package gg.solarmc.futuresfactory;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
		assertTrue(scheduledExecutor.awaitTermination(2L, TimeUnit.SECONDS));
	}
}
