package com.n4y.client.core.event;

/** An event whose default behaviour a listener can stop. */
public abstract class Cancellable implements Event {
	private boolean cancelled;

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		cancelled = true;
	}
}
