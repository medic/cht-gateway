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
