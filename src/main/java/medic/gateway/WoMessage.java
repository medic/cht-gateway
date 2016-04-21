package medic.gateway;

import java.util.*;

class WoMessage implements Map<String, String>, Comparable<WoMessage> {
	public enum Status { UNSENT, PENDING, SENT, FAILED, REJECTED, DELIVERED }

	public final String id;
	private long lastAction;
	private Status status;
	/** Indicates if this status has been forwarded to the webapp yet. */
	public boolean statusForwarded;
	public final String to;
	public final String content;

	public WoMessage(String id, String to, String content) {
		this.id = id;
		setStatus(Status.UNSENT);
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
	public Status getStatus() { return status; }
	public void setStatus(Status status) {
		this.lastAction = System.currentTimeMillis();
		this.status = status;
		this.statusForwarded = false;
	}

	public long getLastAction() { return lastAction; }

	public String toString() {
		return String.format("%s@%s-%s", getClass().getSimpleName(), id, status);
	}

//> java.lang.Comparable methods
	public int compareTo(WoMessage that) {
		if(that == null) return -1;
		if(this.equals(that)) return 0;
		if(this.lastAction < that.lastAction) return -1;
		// N.B. if lastAction are equal, x.compareTo(y) != -y.comapreTo(x);
		return 1;
	}

//> java.util.Map methods - implemented to provide easy display in a ListView
	public String get(Object key) {
		if("status".equals(key)) return status.toString();
		if("to".equals(key)) return to;
		if("content".equals(key)) return content;
		if("lastAction".equals(key)) return Utils.relativeTimestamp(lastAction);
		throw new IllegalStateException();
	}

	public int size() { throw new IllegalStateException(); }
	public boolean isEmpty() { throw new IllegalStateException(); }
	public boolean containsKey(Object k) { throw new IllegalStateException(); }
	public boolean containsValue(Object value) { throw new IllegalStateException(); }
	public String put(String k, String v) { throw new IllegalStateException(); }
	public String remove(Object k) { throw new IllegalStateException(); }
	public void putAll(Map<? extends String, ? extends String> m) { throw new IllegalStateException(); }
	public void clear() { throw new IllegalStateException(); }
	public Set<String> keySet() { throw new IllegalStateException(); }
	public Collection<String> values() { throw new IllegalStateException(); }
	public Set<Map.Entry<String, String>> entrySet() { throw new IllegalStateException(); }
}
