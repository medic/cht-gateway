package medic.gateway;

import android.telephony.*;

import java.util.*;

import static medic.gateway.BuildConfig.DEBUG;

public class WoRepo {
	public static final WoRepo $ = new WoRepo();

	private final SortedSet<WoMessage> messages;

	private WoRepo() {
		messages = new TreeSet<>();
	}

	public synchronized void save(WoMessage m) {
		messages.add(m);
	}

	public synchronized List<WoMessage> getUnsent() {
		LinkedList<WoMessage> unsent = new LinkedList<>();
		for(WoMessage m : messages) {
			if(m.status == WoMessage.Status.UNSENT) unsent.add(m);
		}
		return unsent;
	}
}

class WoMessage {
	public enum Status { UNSENT, PENDING, FAILED }

	public final String to;
	public final String content;
	public Status status;

	public WoMessage(String to, String content) {
		this.to = to;
		this.content = content;
		this.status = Status.UNSENT;
	}
}
