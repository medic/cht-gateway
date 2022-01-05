package medic.gateway.alert;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.ResourceCursorAdapter;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.relativeTimestamp;
import static medic.gateway.alert.Utils.setText;

public class WtListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	public static final int LOADER_ID = 2;

	@Override public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		WtCursorAdapter adapter = new WtCursorAdapter(getCastActivity());
		setListAdapter(adapter);
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	public WtListActivity getCastActivity() {
		return (WtListActivity) getActivity();
	}

//> LoaderCallbacks
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		setListShown(false);
		return new WtCursorLoader(getActivity());
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		((CursorAdapter) this.getListAdapter()).swapCursor(cursor);
		setListShown(true);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		((CursorAdapter) this.getListAdapter()).swapCursor(null);
	}

//> EVENT HANDLERS
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		Cursor c = (Cursor) list.getItemAtPosition(position);

		// Get a fresh copy of the message, in case it's been updated
		// more recently than the list
		WtMessage m = Db.getInstance(getActivity()).getWtMessage(c.getString(0));

		getCastActivity().showMessageDetailDialog(m, position);
	}
}

class WtCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	private final WtListActivity activity;

	public WtCursorAdapter(WtListActivity activity) {
		super(activity, R.layout.wt_list_item, null, NO_FLAGS);
		this.activity = activity;
	}

	public void bindView(View v, final Context ctx, Cursor c) {
		final WtMessage m = Db.wtMessageFrom(c);

		setText(v, R.id.txtWtStatus, m.getStatus().toString());
		setText(v, R.id.txtWtLastAction, relativeTimestamp(m.getLastAction()));
		setText(v, R.id.txtWtFrom, m.from);
		setText(v, R.id.txtWtContent, m.content);

		CheckBox cbx = (CheckBox) v.findViewById(R.id.cbxMessage);
		// Old list items get re-used, so we need to make sure that the
		// checkbox is de-checked.
		cbx.setChecked(activity.isChecked(m));
		cbx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
				trace(this, "Changed checkbox to %s", isChecked);
				activity.updateChecked(m, isChecked);
			}
		});
	}
}

class WtCursorLoader extends CursorLoader {
	private static final int MAX_WT_MESSAGES = 100;

	private final Db db;

	public WtCursorLoader(Context ctx) {
		super(ctx);
		this.db = Db.getInstance(ctx);
	}

	public Cursor loadInBackground() {
		return db.getWtMessages(MAX_WT_MESSAGES);
	}
}
