package pl.djvuhtml5.client;

import java.util.ArrayList;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class BackgroundProcessor implements ScheduledCommand {

	public static interface Operation {
		int MAX_PRIORITY = 10;

		boolean doOperation(int priority);
	}

	private ArrayList<Operation> operations = new ArrayList<>();

	private boolean isRunning;

	public void addOperation(Operation operation) {
		operations.add(operation);
	}

	public void removeOperation(Operation operation) {
		operations.remove(operation);
	}

	public void start() {
		if (!isRunning)
			Scheduler.get().scheduleDeferred(this);
		isRunning = true;
	}

	@Override
	public void execute() {
		for (int p = 0; p < Operation.MAX_PRIORITY; p++) {
			for (Operation operation : operations) {
				boolean didSomething = operation.doOperation(p);
				if (didSomething) {
					Scheduler.get().scheduleDeferred(this);
					return;
				}
			}
		}
		isRunning = false;
	}

}
