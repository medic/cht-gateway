package medic.gateway.alert;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.ResourceCursorAdapter;

import static medic.gateway.alert.Utils.absoluteTimestamp;
import static medic.gateway.alert.Utils.setText;

public class GatewayEventLogFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	public static final int LOADER_ID = 1;

	private Db db;

	@Override public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.db = Db.getInstance(getActivity());

		GatewayEventLogEntryCursorAdapter adapter = new GatewayEventLogEntryCursorAdapter(getActivity());
		setListAdapter(adapter);
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

//> LoaderCallbacks
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		setListShown(false);
		return new GatewayEventLogEntryCursorLoader(getActivity(), db);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		((CursorAdapter) this.getListAdapter()).swapCursor(cursor);
		setListShown(true);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		((CursorAdapter) this.getListAdapter()).swapCursor(null);
	}
}

class GatewayEventLogEntryCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	public GatewayEventLogEntryCursorAdapter(Context ctx) {
		super(ctx, R.layout.event_log_item, null, NO_FLAGS);
	}

	public void bindView(View v, Context ctx, Cursor c) {
		setText(v, R.id.txtGatewayEventLogDate, absoluteTimestamp(c.getLong(1)));
		setText(v, R.id.txtGatewayEventLogMessage, c.getString(2));
	}
}

class GatewayEventLogEntryCursorLoader extends CursorLoader {
	private final Db db;

	public GatewayEventLogEntryCursorLoader(Context ctx, Db db) {
		super(ctx);
		this.db = db;
	}

	public Cursor loadInBackground() {
		return db.getLogEntries();
	}
}
