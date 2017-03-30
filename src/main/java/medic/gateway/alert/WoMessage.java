package medic.gateway.alert;

import static medic.gateway.alert.GatewayLog.trace;

/**
 * WoMessage - Webapp-Originating Messages
 *
 * These are messages which originate at the webapp which medic-gateway is
 * acting as SMS gateway for... or occasionally they may actually originate from
 * medic-gateway itself, when medic-gateway is acting as the default messaging
 * app on the device.
 */
class WoMessage {
	public static enum Status {
		UNSENT, PENDING, SENT, FAILED, DELIVERED;
		boolean canBeRetried() { return this != UNSENT && this != SENT && this != DELIVERED; }
	}

	public static class StatusUpdate {
		public final long id;
		public final String messageId;
		public final Status newStatus;
		public final String failureReason;
		public final long timestamp;
		public StatusUpdate(long id, String messageId, Status newStatus, String failureReason, long timestamp) {
			this.id = id;
			this.messageId = messageId;
			this.newStatus = newStatus;
			this.failureReason = failureReason;
			this.timestamp = timestamp;

			if((newStatus == Status.FAILED) == (failureReason == null)) {
				trace(this, "Attempting to set failure reason on a non-failed message: %s", this);
			}
		}
		public String toString() {
			if(newStatus == Status.FAILED) {
				return String.format("%s@%s-%s[%s]-%s", getClass().getSimpleName(), messageId, newStatus, failureReason, timestamp);
			} else {
				return String.format("%s@%s-%s-%s", getClass().getSimpleName(), messageId, newStatus, timestamp);
			}
		}
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
