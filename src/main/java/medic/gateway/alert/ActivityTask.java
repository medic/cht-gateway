package medic.gateway.alert;

import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

// TODO rename this as ActivityBackgroundTask or something
abstract class ActivityTask<Parent extends Activity, Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
	private final WeakReference<Parent> parent;

	ActivityTask(Parent parent) {
		super();
		this.parent = new WeakReference<>(parent);
	}

	protected Parent getCtx() {
		Parent parent = this.parent.get();

		if(parent == null) return null;
		if(parent.isFinishing()) return null;
		if(SDK_INT >= JELLY_BEAN_MR1 && parent.isDestroyed()) return null;

		else return parent;
	}
}
