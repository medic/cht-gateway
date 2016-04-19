package medic.gateway;

import org.json.*;

public class JsonUtils {
	public static JSONObject json(Object... keyVals) throws JSONException {
		assert keyVals.length % 2 == 0;
		JSONObject o = new JSONObject();
		for(int i=keyVals.length-1; i>0; i-=2) {
			o.put(keyVals[i-1].toString(), keyVals[i].toString());
		}
		return o;
	}
}
