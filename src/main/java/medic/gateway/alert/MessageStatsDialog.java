package medic.gateway.alert;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;

import java.util.LinkedList;

import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.Utils.NO_CLICK_LISTENER;
import static medic.gateway.alert.Utils.showAlert;

final class MessageStatsDialog {
	private MessageStatsDialog() {}

	static Thinking show(final Activity a) {
		final Thinking thinking = Thinking.show(a);
		AsyncTask.execute(new Runnable() {
			private final String string(int stringId, Object...args) {
				return a.getString(stringId, args);
			}

			public void run() {
				try {
					Db db = Db.getInstance(a);
					LinkedList<String> content = new LinkedList<>();
					MessageReport r = db.generateMessageReport();

					content.add(string(R.string.lblMessageStats_title));

					content.add(string(R.string.lblMessageStats_wt_total, r.wtmCount));
					for(WtMessage.Status s : WtMessage.Status.values()) {
						content.add(string(R.string.lblMessageStats_forStatus, s, r.getCount(s)));
					}

					content.add(string(R.string.lblMessageStats_wo_total, r.womCount));
					for(WoMessage.Status s : WoMessage.Status.values()) {
						content.add(string(R.string.lblMessageStats_forStatus, s, r.getCount(s)));
					}

					final AlertDialog.Builder dialog = new AlertDialog.Builder(a);

					dialog.setItems(content.toArray(new String[content.size()]), NO_CLICK_LISTENER);

					showAlert(a, dialog);
				} catch(Exception ex) {
					logException(a, ex, "Failed to load message stats dialog.");
				} finally {
					thinking.dismiss();
				}
			}
		});
		return thinking;
	}
}
