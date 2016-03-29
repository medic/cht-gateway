package medic.gateway;

import android.telephony.*;

import java.util.*;

import static medic.gateway.BuildConfig.DEBUG;

public class WtRepo {
	public static final WtRepo $ = new WtRepo();

	private final List<SmsMessage> messages;

	private WtRepo() {
		messages = new LinkedList<>();
	}

	public void save(SmsMessage m) {
		messages.add(m);
	}
}
