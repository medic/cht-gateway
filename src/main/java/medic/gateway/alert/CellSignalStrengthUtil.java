package medic.gateway.alert;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;

@TargetApi(17)
final class CellSignalStrengthUtil {
	private CellSignalStrengthUtil() {}

	@SuppressWarnings("PMD.InsufficientStringBufferDeclaration") // looks like a bug in PMD
	static String getSignalStrengthDescription(Context ctx) {
		if(Build.VERSION.SDK_INT < 17) return "unavailable (API < 17)";

		TelephonyManager tMan = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		StringBuilder bob = new StringBuilder();
		for(CellInfo info : tMan.getAllCellInfo()) {
			bob.append(',');

			if(info.isRegistered()) {
				try {
					CellSignalStrength strength = getSignalStrengthFrom(info);
					bob.append(toString(strength));
				} catch(UnmetApiRequirementException ex) {
					bob.append(String.format("unavailable (API < %s)", ex.requiredApiVersion));
				}
			} else {
				bob.append("unregistered");
			}
		}
		return bob.length() > 0 ? bob.substring(1) : "not available";
	}

	private static CellSignalStrength getSignalStrengthFrom(CellInfo info) throws UnmetApiRequirementException {
		if(info instanceof CellInfoCdma) {
			return ((CellInfoCdma) info).getCellSignalStrength();
		} else if(info instanceof CellInfoGsm) {
			return ((CellInfoGsm) info).getCellSignalStrength();
		} else if(info instanceof CellInfoLte) {
			return ((CellInfoLte) info).getCellSignalStrength();
		} else if(info instanceof CellInfoWcdma) {
			if(Build.VERSION.SDK_INT < 18) throw new UnmetApiRequirementException(18);
			else return getCellSignalStrengthWcdma(info);
		}
		return null;
	}

	@TargetApi(18)
	private static CellSignalStrength getCellSignalStrengthWcdma(CellInfo info) {
		return ((CellInfoWcdma) info).getCellSignalStrength();
	}

	private static String toString(CellSignalStrength strength) {
		if(strength == null) {
			return "null";
		} else switch(strength.getLevel()) {
			case 0:
				return "none/unknown";
			case 1:
				return "poor";
			case 2:
				return "moderate";
			case 3:
				return "good";
			case 4:
				return "great";
		}
		return "unknown-level:" + strength.getLevel();
	}
}

class UnmetApiRequirementException extends Exception {
	final int requiredApiVersion;
	UnmetApiRequirementException(int requiredApiVersion) {
		this.requiredApiVersion = requiredApiVersion;
	}
}
