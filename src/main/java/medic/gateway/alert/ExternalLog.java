package medic.gateway.alert;

import android.content.Context;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONObject;

import static android.os.Environment.MEDIA_MOUNTED;
import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStorageState;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.GatewayLog.warn;
import static medic.gateway.alert.Utils.json;

/**
 * All non-private methods dealing with the filesystem should be {@code synchronized}.
 * All non-private methods <b>must not</b> throw {@code Exception}s.
 */
// TODO rename this as PersistentLog or EmergencyMessageLog?
@SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
class ExternalLog {
	private static ExternalLog _instance;

	private final File f;

	private ExternalLog(File f) { this.f = f; }

	static synchronized ExternalLog getInstance(Context ctx) {
		if(_instance == null) {
			String storageState = getExternalStorageState();
			if(!MEDIA_MOUNTED.equals(storageState)) {
				warn("Cannot use external log file.  Storage state is currently: %s", storageState);
			}

			File directory = new File(getExternalStorageDirectory(), "Documents");
			boolean dirCreated = directory.mkdirs();
			if(!dirCreated && !directory.exists())
				warn("Failed to create directory for saving external logfile at %s.  External logging probably won't work.", directory.getAbsolutePath());

			File f = new File(directory, ".cht-gateway.json.log");
			_instance = new ExternalLog(f);
		}
		return _instance;
	}

	synchronized boolean shouldProcess() {
		trace(this, "shouldProcess() :: f.len=%s", f.length());
		return f.length() > 0;
	}

	synchronized void process(Context ctx) {
		trace(this, "process()");
		FileReader fr = null;
		BufferedReader br = null;

		try {
			Db db = Db.getInstance(ctx);

			fr = new FileReader(f);
			br = new BufferedReader(fr);

			String line;
			while((line = br.readLine()) != null) {
				trace(this, "process() :: line=%s", line);
				processLine(db, line);
			}

			boolean deleteSuccess = f.delete();

			if(!deleteSuccess) trace(this, "process() :: failed to delete log file after processing.");
		} catch(Exception ex) {
			logException(ex, "Problem processing external log file at %s", f.getAbsolutePath());
		} finally {
			closeSafely(br);
			closeSafely(fr);
		}
	}

	synchronized void log(WtMessage m) {
		trace(this, "log() :: %s", m);
		try {
			JSONObject json = json(
				"type", "wt_message",
				"doc", json(
					"id", m.id,
					"from", m.from,
					"content", m.content,
					"sms_sent", m.smsSent,
					"sms_received", m.smsReceived
				)
			);
			writeLine(json);
		} catch(Exception ex) {
			logException(ex, "Problem writing WtMessage to log: %s", m);
		}
	}

	private void processLine(Db db, String line) {
		trace(this, "processLine() :: line=%s", line);
		try {
			JSONObject json = new JSONObject(line);
			String type = json.getString("type");
			JSONObject doc = json.getJSONObject("doc");

			switch(type) {
				case "wt_message":
					// N.B. if adding any new fields here in the future, be
					// careful to fetch them using `optString()`, `optLong()`
					// etc. and specify the same defaults as in Db.
					WtMessage m = new WtMessage(
							doc.getString("id"),
							WtMessage.Status.WAITING,
							System.currentTimeMillis(),
							doc.getString("from"),
							doc.getString("content"),
							doc.getLong("sms_sent"),
							doc.getLong("sms_received"));
					db.storeWithoutLoggingExternally(m);
					break;
				default: throw new UnrecongisedExternalLogType(type);
			}
		} catch(Exception ex) {
			logException(ex, "Problem processing line: %s", line);
		}
	}

	// TODO We probably don't want to re-open it every time we process anything.  Consider how we keep a file handle open for writing to the file.  How will it be safely closed when the app exits?  What if it wasn't safely closed last time?
	@SuppressWarnings("PMD.SignatureDeclareThrowsException")
	@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
	private void writeLine(JSONObject json) throws IOException {
		trace(this, "writeLine() :: json=%s", json);
		f.createNewFile();
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(f, true);
			bw = new BufferedWriter(fw);

			bw.write(json.toString());
			bw.newLine();
		} finally {
			closeSafely(bw);
			closeSafely(fw);
		}
	}

	private void closeSafely(Closeable c) {
		if(c != null) try { c.close(); } catch(Exception ex) { logException(ex, "Problem while closing; will be ignored."); }
	}
}

class UnrecongisedExternalLogType extends Exception {
	public UnrecongisedExternalLogType(String type) {
		super(type);
	}
}
