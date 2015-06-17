package pl.djvuhtml5.client;

import java.util.ArrayList;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class BackgroundProcessor implements RepeatingCommand {

	public static interface Operation {
		int MAX_PRIORITY = 10;

		boolean doOperation(int priority);
	}

	private ArrayList<Operation> operations = new ArrayList<>();

	private boolean isRunning;

	private boolean isPauseScheduled;

	public void addOperation(Operation operation) {
		operations.add(operation);
	}

	public void removeOperation(Operation operation) {
		operations.remove(operation);
	}

	public void start() {
		if (!isRunning)
			Scheduler.get().scheduleIncremental(this);
		isRunning = true;
	}

	@Override
	public boolean execute() {
		for (int p = 0; p < Operation.MAX_PRIORITY; p++) {
			for (Operation operation : operations) {
				boolean didSomething = operation.doOperation(p);
				if (didSomething) {
					if (isPauseScheduled) {
						isPauseScheduled = false;
						Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
							
							@Override
							public boolean execute() {
								Scheduler.get().scheduleIncremental(BackgroundProcessor.this);
								return false;
							}
						}, 100);
						return false;
					}
					if (p > 2) {
						Scheduler.get().scheduleDeferred(new ScheduledCommand() {
							
							@Override
							public void execute() {
								Scheduler.get().scheduleIncremental(BackgroundProcessor.this);
							}
						});
						return false;
					}
					return true;
				}
			}
		}
		return isRunning = false;
	}

	public void pause() {
		if (!isRunning)
			return;
		isPauseScheduled = true;
	}

}
