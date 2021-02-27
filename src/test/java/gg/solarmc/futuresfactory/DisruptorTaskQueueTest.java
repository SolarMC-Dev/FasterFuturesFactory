package gg.solarmc.futuresfactory;

import com.lmax.disruptor.BusySpinWaitStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MainThreadSetup.class)
public class DisruptorTaskQueueTest {

	private DisruptorTaskQueue taskQueue;
	private ScheduledFuture<?> periodicTask;
	private ScheduledExecutorService mainThreadExecutor;

	private static final int QUEUE_CAPACITY = 8192;
	private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorTaskQueueTest.class);

	@BeforeEach
	public void setup(ScheduledExecutorService mainThreadExecutor) throws ExecutionException, InterruptedException {
		taskQueue = (DisruptorTaskQueue) DisruptorTaskQueue.create(new BusySpinWaitStrategy(), QUEUE_CAPACITY);
		periodicTask = mainThreadExecutor.scheduleWithFixedDelay(
				taskQueue::pollAndRunAll, 0L, 1L, TimeUnit.MILLISECONDS);
		this.mainThreadExecutor = mainThreadExecutor;
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
					action.run();
				}
				finishedLatch.countDown();
			});
		}
		// Go!
		startLatch.countDown();

		// Wait for everything to finish
		finishedLatch.await();

		threadPool.shutdown();
		assertTrue(threadPool.awaitTermination(10L, TimeUnit.SECONDS), "Failed termination of thread pool");
		LOGGER.info("Finished termination of thread pool");

		periodicTask.cancel(false);
		try {
			mainThreadExecutor.submit(taskQueue::pollAndRunAll).get(4L, TimeUnit.SECONDS);
		} catch (ExecutionException | TimeoutException ex) {
			fail(ex);
		}
		mainThreadExecutor.shutdown();
		try {
			assertTrue(mainThreadExecutor.awaitTermination(1L, TimeUnit.SECONDS), "Failed termination of main thread");
		} catch (AssertionFailedError ex) {
			LOGGER.error("Remaining capacity: {}. Thread dump: \n{}", remainingCapacity(), threadDump());
			throw ex;
		}
		assertEquals(QUEUE_CAPACITY, remainingCapacity(), "Failed to empty task queue");

		LOGGER.info("Finished termination of main thread");
	}

	private long remainingCapacity() {
		return taskQueue.elementQueue.remainingCapacity();
	}

	private static String threadDump() {
		StringBuilder dump = new StringBuilder();
		ThreadInfo[] threadInfos = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
		for (ThreadInfo threadInfo : threadInfos) {
			dump.append('"').append(threadInfo.getThreadName()).append("\" ");
			dump.append("\n   java.lang.Thread.State: ").append(threadInfo.getThreadState());
			for (StackTraceElement element : threadInfo.getStackTrace()) {
				dump.append("\n        at ").append(element);
			}
			dump.append("\n\n");
		}
		return dump.toString();
	}

	private void enqueue(Runnable task) {
		taskQueue.addTask(task);
	}

	@Test
	public void thrash() throws InterruptedException {
		int threadCount = 100; // An order of magnitude larger than 100 starts to lag
		int hitsPerThread = 1000;
		runThreadBazaar(threadCount, hitsPerThread, () -> enqueue(() -> { /* Super fast */}));
	}

	@Test
	public void increment() throws InterruptedException {
		AtomicInteger counter = new AtomicInteger();
		int threadCount = 50;
		int hitsPerThread = 1000;
		runThreadBazaar(threadCount, hitsPerThread, () -> enqueue(counter::getAndIncrement));
		assertEquals(threadCount * hitsPerThread, counter.get());
	}

	/*
	 * Self-enqueing test. This has the main thread submit a task back to the queue.
	 *
	 * This test passes if and only if the amount of tasks self-enqueued does not exceed
	 * the queue capacity.
	 */
	@Test
	public void selfEnqueue(ScheduledExecutorService mainThreadExecutor) throws InterruptedException {
		AtomicInteger counter = new AtomicInteger();
		int threadCount = 50;
		int hitsPerThread = 50;
		runThreadBazaar(threadCount, hitsPerThread, () -> {
			counter.getAndIncrement();
			enqueue(counter::getAndIncrement);
			mainThreadExecutor.execute(() -> enqueue(counter::getAndIncrement)); // Self-enqueue
		});
		assertEquals(threadCount * hitsPerThread * 3, counter.get());
	}

}
