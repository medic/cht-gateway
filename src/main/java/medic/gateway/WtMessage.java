package medic.gateway;

import java.util.*;

import static java.util.UUID.randomUUID;

class WtMessage {
	public enum Status { WAITING, FORWARDED, FAILED }

	public final String id;
	private Status status;
	private long lastAction;
	public final String from;
	public final String content;

	public WtMessage(String from, String content) {
		this.id = randomUUID().toString();
		setStatus(Status.WAITING);
		this.from = from;
		this.content = content;
	}

	public WtMessage(String id, Status status, long lastAction, String from, String content) {
		this.id = id;
		this.status = status;
		this.lastAction = lastAction;
		this.from = from;
		this.content = content;
	}

//> ACCESSORS
	public Status getStatus() { return status; }
	public void setStatus(Status status) {
		this.lastAction = System.currentTimeMillis();
		this.status = status;
	}

	public long getLastAction() { return lastAction; }

	public String toString() {
		return String.format("%s@%s-%s", getClass().getSimpleName(), id, status);
	}
}
