package medic.gateway;

import java.text.*;
import java.util.*;

public class DebugLog {
	private static final LinkedList<DebugLogEntry> ENTRIES = new LinkedList<>();

	public synchronized static List<DebugLogEntry> getEntries(int maxCount) {
		int count = Math.min(maxCount, ENTRIES.size());
		ArrayList<DebugLogEntry> entries = new ArrayList<>(count);

		for(int i=0; i<count; ++i) entries.add(ENTRIES.get(i));

		return entries;
	}

	public synchronized static void logEvent(String message) {
		ENTRIES.addFirst(new DebugLogEntry(System.currentTimeMillis(), message));
	}
}

class DebugLogEntry implements Map<String, String> {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");

	private final long timestamp;
	private final String message;

	DebugLogEntry(long timestamp, String message) {
		this.timestamp = timestamp;
		this.message = message;
	}

	public String get(Object key) {
		if("message".equals(key)) return message;
		if("date".equals(key)) return DATE_FORMAT.format(new Date(timestamp));
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
