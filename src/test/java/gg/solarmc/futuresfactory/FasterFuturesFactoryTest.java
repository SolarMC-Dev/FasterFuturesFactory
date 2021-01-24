package gg.solarmc.futuresfactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MainThreadSetup.class)
public class FasterFuturesFactoryTest {

	private ScheduledFuture<?> periodicTask;
	private FactoryOfTheFuture futuresFactory;

	@BeforeEach
	public void setup(ScheduledExecutorService scheduledExecutor) throws ExecutionException, InterruptedException {
		Thread mainThread = scheduledExecutor.submit(Thread::currentThread).get();
		FactoryAndTaskQueue factoryAndTaskQueue = new FasterFuturesFactory().build(mainThread);
		periodicTask = scheduledExecutor.scheduleWithFixedDelay(
				factoryAndTaskQueue.taskQueue()::pollAndRunAll, 0L, 50L, TimeUnit.MILLISECONDS);
		futuresFactory = factoryAndTaskQueue.futuresFactory();
	}

	@Test
	public void submitSyncTaskUsingCompletableFuture() {
		CompletableFuture<?> future = new CompletableFuture<>();
		futuresFactory.executeSync(() -> future.complete(null));
		future.orTimeout(1L, TimeUnit.SECONDS).join();
	}

	@Test
	public void submitSyncTaskUsingDeadlockFreeFuture() {
		futuresFactory.runSync(() -> {}).join();
	}

	@Test
	public void oneExceptionDoesNotBlowUpEntireFactory() {
		var throwingFuture = futuresFactory.runSync(() -> { throw new IllegalStateException(); });
		assertThrows(CompletionException.class, throwingFuture::join);
		futuresFactory.runSync(() -> {}).join();
	}

	@AfterEach
	public void tearDown() throws InterruptedException {
		periodicTask.cancel(false);
	}
}
