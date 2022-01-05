package medic.gateway.alert;

import static medic.gateway.alert.GatewayLog.trace;

/**
 * WoMessage - Webapp-Originating Messages
 *
 * These are messages which originate at the webapp which cht-gateway is
 * acting as SMS gateway for... or occasionally they may actually originate from
 * cht-gateway itself, when cht-gateway is acting as the default messaging
 * app on the device.
 */
class WoMessage {
	public enum Status {
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
					this.failureReason == null ? that.failureReason == null : this.failureReason.equals(that.messageId) &&
					this.timestamp == that.timestamp;
		}
	}

	public final String id;
	public final long lastAction;
	public final Status status;
	private final String failureReason;
	public final String to;
	public final String content;
	// Retries is a counter. After a soft fail the WoMessage's status is set to UNSENT, then Gateway will retry to send it.
	// After limit is reached, WoMessage will hard fail. It can be retried manually later.
	public final int retries;
	private static final int MAX_RETRIES_SOFT_FAIL = 20; // Aprox 12H when WAIT_RETRY_SOFT_FAIL is 1min
	private static final int WAIT_RETRY_SOFT_FAIL = 60 * 1000; // Milliseconds

	public WoMessage(String id, String to, String content) {
		this.id = id;
		this.status = Status.UNSENT;
		this.failureReason = null;
		this.lastAction = System.currentTimeMillis();
		this.to = to;
		this.content = content;
		this.retries = 0;
	}

	public WoMessage(String id, Status status, String failureReason, long lastAction, String to, String content, int retries) {
		if ((status == Status.FAILED) == (failureReason == null)) {
			throw new IllegalArgumentException(String.format(
					"Provide a failureReason iff status is FAILED.  (status=%s, reason=%s)",
					status,
					failureReason));
		}

		this.id = id;
		this.status = status;
		this.failureReason = failureReason;
		this.lastAction = lastAction;
		this.to = to;
		this.content = content;
		this.retries = retries;
	}

//> ACCESSORS
	public String getFailureReason() {
		if(status == Status.FAILED) return failureReason;
		else throw new IllegalStateException("Cannot get failure reason unless status is FAILED");
	}

	public String toString() {
		return String.format("%s@%s-%s", getClass().getSimpleName(), id, status);
	}

	public boolean isMaxRetriesSoftFail() {
		return this.retries >= MAX_RETRIES_SOFT_FAIL;
	}

	/**
	 * The wait time is incremental according with the number of retries.
	 * @return time in milliseconds
	 */
	public int calcWaitTimeRetry(int retries) {
		return (int) (WAIT_RETRY_SOFT_FAIL * Math.pow(retries, 1.5));
	}

	public boolean canRetryAfterSoftFail() {
		long waitTime = this.lastAction + this.calcWaitTimeRetry(this.retries);
		long currentTime = System.currentTimeMillis();

		return currentTime >= waitTime;
	}
}
