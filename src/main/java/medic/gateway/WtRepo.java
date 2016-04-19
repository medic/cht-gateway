package medic.gateway;

import android.telephony.*;

import java.util.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.Utils.randomPhoneNumber;
import static medic.gateway.Utils.randomSmsContent;

public class WtRepo {
	public static final WtRepo $ = new WtRepo();

	private final SortedSet<WtMessage> messages;

	private WtRepo() {
		messages = new TreeSet<>();

		if(DEBUG) {
			// seed some data
			messages.add(new WtMessage("+254789123123", "hello from kenya"));
			messages.add(new WtMessage("+34678123123", "hello from spain"));
			messages.add(new WtMessage("+447890123123", "hello from uk"));

			for(int i=0; i<20; ++i) {
				messages.add(new WtMessage(randomPhoneNumber(), randomSmsContent()));
			}
		}
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
			if(m.getStatus() == WtMessage.Status.WAITING)
				waiting.add(m);

		return waiting;
	}

	public synchronized List<WtMessage> getAll() {
		return new ArrayList<WtMessage>(messages);
	}

	public void updateAll(Collection<WtMessage> messages) {
		// in memory, these are already updated
	}
}
