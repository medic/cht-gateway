package medic.gateway.alert;

import static medic.gateway.alert.Utils.randomUuid;

/**
 * WtMessage - Webapp-Terminating Messages
 */
class WtMessage {
	public enum Status {
		WAITING, FORWARDED, FAILED;
		boolean canBeRetried() { return this == FAILED; }
	}

	public static class StatusUpdate {
		public final long id;
		public final String messageId;
		public final Status newStatus;
		public final long timestamp;
		public StatusUpdate(long id, String messageId, Status newStatus, long timestamp) {
			this.id = id;
			this.messageId = messageId;
			this.newStatus = newStatus;
			this.timestamp = timestamp;
		}
		public String toString() {
			return String.format("%s@%s-%s-%s", getClass().getSimpleName(), messageId, newStatus, timestamp);
		}
		public int hashCode() {
			int p = 92821;
			int h = 1;
			h = h * p + (int) id;
			return h;
		}
		public boolean equals(Object _that) {
			if(this == _that) return true;
			if(!(_that instanceof StatusUpdate)) return false;
			StatusUpdate that = (StatusUpdate) _that;
			return this.id == that.id &&
					this.messageId == null ? that.messageId == null : this.messageId.equals(that.messageId) &&
					this.newStatus == that.newStatus &&
					this.timestamp == that.timestamp;
		}
	}

	public final String id;
	private Status status;
	private long lastAction;
	public final String from;
	public final String content;
	public final long smsSent;
	public final long smsReceived;

	// TODO smsSent in practice actually appears to be the time that the SMS was received at the gateway
	public WtMessage(String from, String content, long smsSent) {
		this.id = randomUuid();
		setStatus(Status.WAITING);
		this.from = from;
		this.content = content;
		this.smsSent = smsSent;
		this.smsReceived = lastAction; // TODO this appears to be a no-op
	}

	public WtMessage(String id, Status status, long lastAction, String from, String content, long smsSent, long smsReceived) {
		this.id = id;
		this.status = status;
		this.lastAction = lastAction;
		this.from = from;
		this.content = content;
		this.smsSent = smsSent;
		this.smsReceived = smsReceived;
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
