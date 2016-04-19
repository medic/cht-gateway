package medic.gateway;

import java.util.*;

import static java.util.UUID.randomUUID;

class WtMessage implements Map<String, String>, Comparable<WtMessage> {
	public enum Status { WAITING, FORWARDED, FAILED }

	public final String id;
	private long lastAction;
	private Status status;
	public final String from;
	public final String content;

	public WtMessage(String from, String content) {
		this.id = randomUUID().toString();
		setStatus(Status.WAITING);
		this.from = from;
		this.content = content;
	}

//> ACCESSORS
	public Status getStatus() { return status; }
	public void setStatus(Status status) {
		this.lastAction = System.currentTimeMillis();
		this.status = status;
	}

//> java.lang.Comparable methods
	public int compareTo(WtMessage that) {
		if(that == null) return -1;
		if(this.equals(that)) return 0;
		if(this.lastAction < that.lastAction) return -1;
		// N.B. if lastAction values are equal, x.compareTo(y) != -y.comapreTo(x);
		return 1;
	}

//> java.util.Map methods - implemented to provide easy display in a ListView
	public String get(Object key) {
		if("status".equals(key)) return status.toString();
		if("from".equals(key)) return from;
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
