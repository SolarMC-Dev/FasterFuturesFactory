package gg.solarmc.futuresfactory;

final class TaskEvent {

	private Runnable task;

	void setTask(Runnable task) {
		this.task = task;
	}

	Runnable getTask() {
		return task;
	}
}
