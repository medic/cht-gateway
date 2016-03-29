package medic.gateway;

import android.telephony.*;

import java.util.*;

import static medic.gateway.BuildConfig.DEBUG;

public class WtRepo {
	public static final WtRepo $ = new WtRepo();

	private final SortedSet<SmsMessage> messages;

	private WtRepo() {
		messages = new TreeSet<>();
	}

	public void save(SmsMessage m) {
		messages.add(m);
	}
}
