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
import static medic.gateway.alert.WoMessage.Status.FAILED;

public class WoListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	public static final int LOADER_ID = 3;

	@Override public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		WoCursorAdapter adapter = new WoCursorAdapter(getCastActivity());
		setListAdapter(adapter);
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	public WoListActivity getCastActivity() {
		return (WoListActivity) getActivity();
	}

//> LoaderCallbacks
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		setListShown(false);
		return new WoCursorLoader(getActivity());
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
		Cursor c = (Cursor) getListView().getItemAtPosition(position);

		// Get a fresh copy of the message, in case it's been updated
		// more recently than the list
		WoMessage m = Db.getInstance(getActivity()).getWoMessage(c.getString(0));

		getCastActivity().showMessageDetailDialog(m);
	}
}

class WoCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	private final WoListActivity activity;

	public WoCursorAdapter(WoListActivity activity) {
		super(activity, R.layout.wo_list_item, null, NO_FLAGS);
		this.activity = activity;
	}

	public void bindView(View v, final Context ctx, Cursor c) {
		final WoMessage m = Db.woMessageFrom(c);

		String status;
		if(m.status == FAILED) {
			status = String.format("%s (%s)", m.status, m.getFailureReason());
		} else {
			status = m.status.toString();
		}
		setText(v, R.id.txtWoStatus, status);
		setText(v, R.id.txtWoLastAction, relativeTimestamp(m.lastAction));
		setText(v, R.id.txtWoTo, m.to);
		setText(v, R.id.txtWoContent, m.content);

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

class WoCursorLoader extends CursorLoader {
	private static final int MAX_WO_MESSAGES = 100;

	private final Db db;

	public WoCursorLoader(Context ctx) {
		super(ctx);
		this.db = Db.getInstance(ctx);
	}

	public Cursor loadInBackground() {
		return db.getWoMessages(MAX_WO_MESSAGES);
	}
}
