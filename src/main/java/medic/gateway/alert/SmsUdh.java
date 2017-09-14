package medic.gateway.alert;

import android.telephony.SmsMessage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.GatewayLog.logException;

@SuppressWarnings({ "PMD.StdCyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity" })
final class SmsUdh {
	private static final int TYPE_CONCAT_8_BIT = 0x00;
	private static final int TYPE_CONCAT_16_BIT = 0x08;

	final int multipartRef;
	final int partNumber;
	final int totalParts;
	final long sentTimestamp;

	private SmsUdh(int multipartRef, int totalParts, int partNumber, long sentTimestamp) throws EOFException {
		if(multipartRef == -1 || partNumber == -1 || totalParts == -1) throw new EOFException();
		this.multipartRef = multipartRef;
		this.totalParts = totalParts;
		this.partNumber = partNumber;
		this.sentTimestamp = sentTimestamp;
	}

	/**
	 * There's a good chance this method will not work correctly for CDMA
	 * messages.  If that turns out to be the case, it should switch on
	 * {@code TelephonyManager.getDefault().getCurrentPhoneType()},
	 * comparing values to
	 * {@code com.android.internal.telephony.SmsConstants.FORMAT_*}.
	 */
	public static SmsUdh from(SmsMessage m) {
		byte[] pdu = m.getPdu();
		if(pdu == null || pdu.length == 0) return null;

		trace(SmsUdh.class, "from() :: pdu=%s", Arrays.toString(pdu));

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
			int fromLength = in.read();
			int fromBytes = (fromLength >> 1) + (fromLength & 1) + 1;
			while(--fromBytes >= 0) in.read();

			// skip PID; skip DCS
			in.read(); in.read();

			// TODO process timestamp
			long sentTimestamp = 0;
			in.read(); in.read(); in.read(); in.read(); in.read(); in.read(); in.read();

			// skip UD length byte
			in.read();

			int bytesRemaining = in.read();
			while(bytesRemaining > 0) {
				bytesRemaining -= 2;
				final int elementTypeId = in.read();
				int elementContentLength = in.read();
				bytesRemaining -= elementContentLength;

				switch(elementTypeId) {
					case TYPE_CONCAT_8_BIT:
						return new SmsUdh(in.read(), in.read(), in.read(), sentTimestamp);
					case TYPE_CONCAT_16_BIT:
						return new SmsUdh(in.readUnsignedShort(), in.read(), in.read(), sentTimestamp);
					case -1:
						throw new EOFException();
					default:
						// Irrelevant UDH part - discard
						trace(SmsUdh.class, "from() :: Unrecognised UDH element.  Type ID: %s", elementTypeId);
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
