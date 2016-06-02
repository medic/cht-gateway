package medic.gateway.alert;

import java.util.*;

class WoMessage {
	public enum Status {
		UNSENT, PENDING, SENT, FAILED, DELIVERED;
		boolean canBeRetried() { return this != UNSENT && this != SENT && this != DELIVERED; }
	}

	public final String id;
	public final long lastAction;
	public final Status status;
	private final String failureReason;
	public final String to;
	public final String content;

	public WoMessage(String id, String to, String content) {
		this.id = id;
		this.status = Status.UNSENT;
		this.failureReason = null;
		this.lastAction = System.currentTimeMillis();
		this.to = to;
		this.content = content;
	}

	public WoMessage(String id, Status status, String failureReason, long lastAction, String to, String content) {
		if((status == Status.FAILED) == (failureReason == null))
			throw new IllegalArgumentException(String.format(
					"Provide a failureReason iff status is FAILED.  (status=%s, reason=%s)",
					status, failureReason));
		this.id = id;
		this.status = status;
		this.failureReason = failureReason;
		this.lastAction = lastAction;
		this.to = to;
		this.content = content;
	}

//> ACCESSORS
	public String getFailureReason() {
		if(status == Status.FAILED) return failureReason;
		else throw new IllegalStateException("Cannot get failure reason unless status is FAILED");
	}

	public String toString() {
		return String.format("%s@%s-%s", getClass().getSimpleName(), id, status);
	}
}
