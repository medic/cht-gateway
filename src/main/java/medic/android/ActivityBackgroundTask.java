package medic.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

public abstract class ActivityBackgroundTask<Parent extends Activity, Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
	private final WeakReference<Parent> parent;

	public ActivityBackgroundTask(Parent parent) {
		super();
		this.parent = new WeakReference<>(parent);
	}

	/**
	 * @param caller the name of the calling class and method for use in logging and <code>Throwable</code>s.
	 * @return the parent context of this task
	 * @throws IllegalStateException if no parent context was found
	 */
	protected Parent getRequiredCtx(String caller) {
		Parent ctx = getCtx();

		if(ctx == null) throw new IllegalStateException(String.format("%s :: couldn't get parent activity.", caller));

		return ctx;
	}

	/**
	 * @return the parent context of this task, or <code>null</code> if the task is finishing, is destroyed, or has been dereferenced.
	 */
	@SuppressLint("ObsoleteSdkInt")
	protected Parent getCtx() {
		Parent parent = this.parent.get();

		if(parent == null) return null;
		if(parent.isFinishing()) return null;
		if(SDK_INT >= JELLY_BEAN_MR1 && parent.isDestroyed()) return null;

		else return parent;
	}
}
