package medic.gateway.alert;

import android.telephony.*;

import java.io.*;
import java.util.*;

import static medic.gateway.alert.GatewayLog.*;

final class MultipartSms {
	private final int ref;
	private final SmsMessage[] parts;
	private final String originatingAddress;

	MultipartSms(SmsMessage seedMessage, MultipartData seedData) {
		ref = seedData.ref;
		parts = new SmsMessage[seedData.totalParts];
		originatingAddress = seedMessage.getOriginatingAddress();
		add(seedMessage, seedData);
	}

	void add(SmsMessage m, MultipartData d) {
		if(d.ref != ref) throw new IllegalArgumentException("Provided part is not for this message.");
		parts[d.partNumber-1] = m;
	}

	String getOriginatingAddress() {
		return originatingAddress;
	}

	String getMessageBody() {
		StringBuilder bob = new StringBuilder();
		for(SmsMessage m : parts) {
			if(m == null) bob.append('…');
			else bob.append(m.getMessageBody());
		}
		return bob.toString();
	}

	long getTimestampMillis() {
		return parts[0].getTimestampMillis();
	}

	List<SmsMessage> getParts() {
		return Arrays.asList(parts);
	}
}

@SuppressWarnings({ "PMD.StdCyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity" })
final class MultipartData {
	private static final int TYPE_CONCAT_8_BIT = 0x00;
	private static final int TYPE_CONCAT_16_BIT = 0x08;

	final int ref;
	final int partNumber;
	final int totalParts;

	private MultipartData(int ref, int totalParts, int partNumber) throws EOFException {
		if(ref == -1 || partNumber == -1 || totalParts == -1) throw new EOFException();
		this.ref = ref;
		this.totalParts = totalParts;
		this.partNumber = partNumber;
	}

	/**
	 * There's a good chance this method will not work correctly for CDMA
	 * messages.  If that turns out to be the case, it should switch on
	 * {@code TelephonyManager.getDefault().getCurrentPhoneType()},
	 * comparing values to
	 * {@code com.android.internal.telephony.SmsConstants.FORMAT_*}.
	 */
	public static MultipartData from(SmsMessage m) {
		byte[] pdu = m.getPdu();
		if(pdu == null || pdu.length == 0) return null;

		ByteArrayInputStream bais = null;
		DataInputStream in = null;
		try {
			bais = new ByteArrayInputStream(pdu);
			in = new DataInputStream(bais);

			// skip SMSC field
			int smscLen = in.read();
			while(--smscLen >= 0) in.read();

			int byte0 = in.read();

			// check if UDH is present
			if((byte0 & (1 << 6)) == 0) return null;

			// skip FROM phone number
			int fromLength = in.read() / 2 + 1;
			while(--fromLength >= 0) in.read();

			// skip PID; skip DCS
			in.read(); in.read();

			// skip timestamp
			in.read(); in.read(); in.read(); in.read(); in.read(); in.read(); in.read();

			// skip UD length byte
			in.read();

			int bytesRemaining = in.read();
			while(bytesRemaining > 0) {
				final int elementTypeId = in.read();
				int elementContentLength = in.read();
				bytesRemaining -= 2 - elementContentLength;

				switch(elementTypeId) {
					case TYPE_CONCAT_8_BIT:
						return new MultipartData(in.read(), in.read(), in.read());
					case TYPE_CONCAT_16_BIT:
						return new MultipartData(in.readUnsignedShort(), in.read(), in.read());
					case -1:
						throw new EOFException();
					default:
						// Irrelevant UDH part - discard
						while(--elementContentLength >= 0) in.read();
				}
			}

			// No multipart header found
			return null;
		} catch(IOException ex) {
			logException(ex, "Exception decoding UDH.  UDH will be ignored.");
			return null;
		} finally {
			if(in != null) try { in.close(); } catch(Exception ex) { logException(ex, "Error closing DataInputStream."); }
			if(bais != null) try { bais.close(); } catch(Exception ex) { logException(ex, "Error closing ByteArrayInputStream."); }
		}
	}
}
