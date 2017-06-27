package medic.gateway.alert;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import static medic.gateway.alert.Utils.randomUuid;
import static medic.gateway.alert.Utils.toast;

public class ComposeSmsActivity extends Activity {

//> EVENT HANDLERS
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.composer);

		String recipient = getSmsRecipient(getIntent());
		if(recipient != null) {
			text(R.id.txtComposer_recipients, recipient);
		}
	}

//> CUSTOM EVENT HANDLERS
	public void send(View view) {
		String[] recipients = getRecipients();

		if(recipients.length == 0) {
			showError(R.id.txtComposer_recipients, R.string.errComposer_noRecipients);
		} else {
			Db db = Db.getInstance(this);

			String content = text(R.id.txtComposer_content);

			for(String recipient : recipients) {
				WoMessage m = new WoMessage(randomUuid(), recipient, content);
				db.store(m);
			}

			toast(this, getResources().getQuantityString(R.plurals.txtComposer_sentConfirmation, recipients.length, recipients.length));

			finish();
		}
	}

//> PRIVATE HELPERS
	private String getSmsRecipient(Intent i) {
		Uri triggerUri = i.getData();
		if(triggerUri == null) return null;
		String recipient = triggerUri.getSchemeSpecificPart();
		if(recipient == null) return null;
		recipient = recipient.trim();
		if(recipient.length() == 0) return null;
		return recipient.replaceAll(" ", "");
	}

	private String[] getRecipients() {
		String userInput = text(R.id.txtComposer_recipients);
		return userInput.replaceAll("[-\\s]", "")
				.replaceAll("[,;:]+", ",")
				.split(",");
	}

	private String text(int componentId) {
		EditText field = (EditText) findViewById(componentId);
		return field.getText().toString();
	}

	private void text(int componentId, String value) {
		EditText field = (EditText) findViewById(componentId);
		field.setText(value);
	}

	private void showError(int componentId, int stringId) {
		TextView field = (TextView) findViewById(componentId);
		field.setError(getString(stringId));
	}
}
