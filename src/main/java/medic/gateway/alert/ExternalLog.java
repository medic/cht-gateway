package medic.gateway.alert;

import android.content.Context;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.json.JSONObject;

import static android.os.Environment.MEDIA_MOUNTED;
import static android.os.Environment.getExternalStorageState;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.GatewayLog.warnEvent;
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
			File f = new File(ctx.getFilesDir(), "external.json.log");
			_instance = new ExternalLog(f);

			String storageState = getExternalStorageState();
			if(!MEDIA_MOUNTED.equals(storageState)) {
				warnEvent(ctx, "Cannot use external log file.  Storage state is currently: %s", storageState);
			}
		}
		return _instance;
	}

	synchronized boolean shouldProcess() {
		return f.length() > 0;
	}

	@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
	synchronized void process(Context ctx) {
		FileReader fr = null;
		BufferedReader br = null;

		try {
			Db db = Db.getInstance(ctx);

			fr = new FileReader(f);
			br = new BufferedReader(fr);

			String line;
			while((line = br.readLine()) != null) {
				processLine(db, line);
			}

			br.close();
			f.delete();
		} catch(Exception ex) {
			logException(ex, "Problem processing external log file at %s", f.getAbsolutePath());
		} finally {
			if(br != null) try { br.close(); } catch(Exception ex) { logException(ex, "Could not close BufferedReader."); }
			if(fr != null) try { fr.close(); } catch(Exception ex) { logException(ex, "Could not close FileReader."); }
		}
	}

	synchronized void log(WtMessage m) {
		try {
			JSONObject json = json(
				"type", "wt_message",
				"doc", json(
					"id", m.id,
					"from", m.from,
					"content", m.content
				)
			);
			writeLine(json);
		} catch(Exception ex) {
			logException(ex, "Problem writing WtMessage to log: %s", m);
		}
	}

	private void processLine(Db db, String line) {
		try {
			JSONObject json = new JSONObject(line);
			String type = json.getString("type");
			JSONObject doc = json.getJSONObject("doc");

			switch(type) {
				case "wt_message":
					WtMessage m = new WtMessage(
							doc.getString("id"),
							WtMessage.Status.WAITING,
							System.currentTimeMillis(),
							doc.getString("from"),
							doc.getString("content"));
					db.store(m);
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
	private void writeLine(JSONObject json) throws Exception {
		f.createNewFile();
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(f, true);
			bw = new BufferedWriter(fw);

			bw.write(json.toString());
			bw.newLine();
		} finally {
			if(bw != null) try { bw.close(); } catch(Exception ex) { logException(ex, "Could not close the file writer."); }
			if(fw != null) try { fw.close(); } catch(Exception ex) { logException(ex, "Could not close the file writer."); }
		}
	}
}

class UnrecongisedExternalLogType extends Exception {
	public UnrecongisedExternalLogType(String type) {
		super(type);
	}
}
