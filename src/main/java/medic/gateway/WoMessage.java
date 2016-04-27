package medic.gateway;

import java.util.*;

class WoMessage {
	public enum Status { UNSENT, PENDING, SENT, FAILED, REJECTED, DELIVERED }

	public final String id;
	public final long lastAction;
	public final Status status;
	public final String to;
	public final String content;

	public WoMessage(String id, String to, String content) {
		this.id = id;
		this.status = Status.UNSENT;
		this.lastAction = System.currentTimeMillis();
		this.to = to;
		this.content = content;
	}

	public WoMessage(String id, Status status, long lastAction, String to, String content) {
		this.id = id;
		this.status = status;
		this.lastAction = lastAction;
		this.to = to;
		this.content = content;
	}

//> ACCESSORS
	public String toString() {
		return String.format("%s@%s-%s", getClass().getSimpleName(), id, status);
	}
}
