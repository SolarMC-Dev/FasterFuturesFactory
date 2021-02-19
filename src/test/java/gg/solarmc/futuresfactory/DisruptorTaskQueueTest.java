package gg.solarmc.futuresfactory;

import com.lmax.disruptor.BusySpinWaitStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.managedwaits.TaskQueue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MainThreadSetup.class)
public class DisruptorTaskQueueTest {

	private TaskQueue taskQueue;
	private ScheduledFuture<?> periodicTask;

	@BeforeEach
	public void setup(ScheduledExecutorService scheduledExecutor) throws ExecutionException, InterruptedException {
		taskQueue = DisruptorTaskQueue.create(new BusySpinWaitStrategy());
		periodicTask = scheduledExecutor.scheduleWithFixedDelay(
				taskQueue::pollAndRunAll, 0L, 1L, TimeUnit.MILLISECONDS);
	}

	private void runThreadBazaar(int threadCount, int hitsPerThread, Runnable action) throws InterruptedException {
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch finishedLatch = new CountDownLatch(threadCount);
		// On your marks
		ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
		for (int n = 0; n < threadCount; n++) {
			// Get set
			threadPool.execute(() -> {
				try {
					startLatch.await();
				} catch (InterruptedException ex) {
					fail(ex);
				}
				for (int m = 0; m < hitsPerThread; m++) {
					taskQueue.addTask(action);
				}
				finishedLatch.countDown();
			});
		}
		// Go!
		startLatch.countDown();

		// Wait for everything to finish
		finishedLatch.await();

		threadPool.shutdown();
		assertTrue(threadPool.awaitTermination(10L, TimeUnit.SECONDS));
	}

	@Test
	public void thrash() throws InterruptedException {
		int threadCount = 100; // An order of magnitude larger than 100 starts to lag
		int hitsPerThread = 1000;
		runThreadBazaar(threadCount, hitsPerThread, () -> { /* Super fast */});
	}

	@Test
	public void increment() throws InterruptedException {
		AtomicInteger counter = new AtomicInteger();
		int threadCount = 50;
		int hitsPerThread = 1000;
		runThreadBazaar(threadCount, hitsPerThread, counter::getAndIncrement);
		assertEquals(threadCount * hitsPerThread, counter.get());
	}

	@AfterEach
	public void tearDown() throws InterruptedException {
		periodicTask.cancel(false);
	}

}
