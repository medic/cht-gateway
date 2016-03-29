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
			if("unsent".equals(m.status)) unsent.add(m);
		}
		return unsent;
	}
}

class WoMessage {
	public final String to;
	public final String message;
	public String status;

	public WoMessage(String to, String message) {
		this(to, message, "unsent");
	}

	public WoMessage(String to, String message, String status) {
		this.to = to;
		this.message = message;
		this.status = status;
	}
}
