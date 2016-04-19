package medic.gateway;

import android.telephony.*;

import java.util.*;

import static java.util.UUID.randomUUID;
import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.Utils.randomPhoneNumber;
import static medic.gateway.Utils.randomSmsContent;

public class WoRepo {
	public static final WoRepo $ = new WoRepo();

	private final SortedSet<WoMessage> messages;

	private WoRepo() {
		messages = new TreeSet<>();

		if(DEBUG) {
			// seed some data
			save(new WoMessage(randomUUID().toString(), "+254789123123", "hello kenya"));
			save(new WoMessage(randomUUID().toString(), "+34678123123", "hello spain"));
			save(new WoMessage(randomUUID().toString(), "+447890123123", "hello uk"));

			for(int i=0; i<20; ++i) {
				save(new WoMessage(randomUUID().toString(), randomPhoneNumber(), randomSmsContent()));
			}
		}
	}

	public synchronized void save(WoMessage m) {
		messages.add(m);
	}

	public synchronized List<WoMessage> getUnsent() {
		LinkedList<WoMessage> unsent = new LinkedList<>();
		for(WoMessage m : messages) {
			if(m.getStatus() == WoMessage.Status.UNSENT) unsent.add(m);
		}
		return unsent;
	}

	public synchronized List<WoMessage> getAll() {
		return new ArrayList(messages);
	}
}

class WoMessage implements Map<String, String>, Comparable<WoMessage> {
	public enum Status { UNSENT, PENDING, FAILED }

	public final String id;
	private long lastAction;
	private Status status;
	public final String to;
	public final String content;

	public WoMessage(String id, String to, String content) {
		this.id = id;
		setStatus(Status.UNSENT);
		this.to = to;
		this.content = content;
	}

//> ACCESSORS
	public Status getStatus() { return status; }
	public void setStatus(Status status) {
		this.lastAction = System.currentTimeMillis();
		this.status = status;
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
