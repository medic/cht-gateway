package medic.gateway;

import android.telephony.*;

import java.util.*;

import static java.util.UUID.randomUUID;
import static medic.gateway.BuildConfig.DEBUG;

public class WtRepo {
	public static final WtRepo $ = new WtRepo();

	private final SortedSet<WtMessage> messages;

	private WtRepo() {
		messages = new TreeSet<>();
	}

	public synchronized void save(SmsMessage sms) {
		WtMessage m = new WtMessage(
				sms.getOriginatingAddress(),
				sms.getMessageBody());
		messages.add(m);
	}

	public synchronized List<WtMessage> getWaiting() {
		LinkedList<WtMessage> waiting = new LinkedList<>();

		for(WtMessage m : messages)
			if(m.status == WtMessage.Status.WAITING)
				waiting.add(m);

		return waiting;
	}

	public void updateAll(Collection<WtMessage> messages) {
		// in memory, these are already updated
	}
}

class WtMessage {
	public enum Status { WAITING, FORWARDED, FAILED }

	public final String id;
	public Status status;
	public final String from;
	public final String content;

	public WtMessage(String from, String content) {
		this.id = randomUUID().toString();
		this.status = Status.WAITING;
		this.from = from;
		this.content = content;
	}
}
