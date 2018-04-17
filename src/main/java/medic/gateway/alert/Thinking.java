package medic.gateway.alert;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Handle the lifecycle of {@code ProgressDialog}s.  These need a little bit of
 * care to make sure that:
 *
 * 1. they are dismissed when the parent {@code Context} is destroyed, and
 * 2. they are not dismissed when they are not being displayed
 *
 * If you create an instance of this class, be careful to call `.dismiss()` when
 * the parent {@code Context}'s {@code onDestroy()} method is called.
 */
final class Thinking {
	private final ProgressDialog dialog;

	private Thinking(ProgressDialog dialog) {
		this.dialog = dialog;
	}

	public void dismiss() {
		if(dialog.isShowing()) dialog.dismiss();
	}

//> FACTORIES
	static Thinking show(Context ctx) {
		return show(ctx, null);
	}

	static Thinking show(Context ctx, int messageId) {
		return show(ctx, ctx.getString(messageId));
	}

	static Thinking show(Context ctx, String message) {
		ProgressDialog p = new ProgressDialog(ctx);
		p.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		if(message != null) p.setMessage(message);
		p.setIndeterminate(true);
		p.setCanceledOnTouchOutside(false);

		p.show();

		return new Thinking(p);
	}
}
