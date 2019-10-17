package medic.gateway.alert;

import android.app.Application;
import android.content.Context;


public class MedicGetwayApplication extends Application {

	public static Context mContext;

	public void onCreate() {
		super.onCreate();
		MedicGetwayApplication.mContext = getApplicationContext();
	}

	public static Context getMedicApplicationContext() {
		return MedicGetwayApplication.mContext;
	}
}
