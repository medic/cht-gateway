package medic.gateway.alert;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import medic.gateway.alert.WtMessage.Status;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.absoluteTimestamp;
import static medic.gateway.alert.Utils.relativeTimestamp;
import static medic.gateway.alert.Utils.setText;
import static medic.gateway.alert.WtMessage.Status.WAITING;

public class WtListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	public static final int LOADER_ID = 2;

	private static final DialogInterface.OnClickListener NO_CLICK_LISTENER = null;

	private Db db;

	@Override public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.db = Db.getInstance(getActivity());

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

		messageDetailDialog(m, position).show();
	}

	// TODO this probably should not be done on the UI thread
	private AlertDialog messageDetailDialog(final WtMessage m, final int position) {
		LinkedList<String> content = new LinkedList<>();

		content.add(string(R.string.lblFrom, m.from));
		content.add(string(R.string.lblContent, m.content));
		content.add(string(R.string.lblStatusUpdates));

		List<WtMessage.StatusUpdate> updates = db.getStatusUpdates(m);
		Collections.reverse(updates);
		for(WtMessage.StatusUpdate u : updates) {
			content.add(String.format(
				"%s: %s", absoluteTimestamp(u.timestamp), u.newStatus.toString()));
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

		if(m.getStatus().canBeRetried()) {
			dialog.setPositiveButton(R.string.btnRetry, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					retry(m.id, position);
				}
			});
		}

		dialog.setItems(content.toArray(new String[content.size()]), NO_CLICK_LISTENER);

		return dialog.create();
	}

	void retry(String id, int position) {
		trace(this, "Retrying message at %d with id %s...", position, id);

		WtMessage m = db.getWtMessage(id);

		if(m.getStatus().canBeRetried()) {
			Status oldStatus = m.getStatus();
			m.setStatus(WAITING);
			db.updateStatusFrom(oldStatus, m);

			WtMessage updated = db.getWtMessage(id);

			View v = getListView().getChildAt(position);
			setText(v, R.id.txtWtStatus, updated.getStatus().toString());
			setText(v, R.id.txtWtLastAction, relativeTimestamp(updated.getLastAction()));
		}
	}

	private final String string(int stringId, Object...args) {
		return String.format(getActivity().getString(stringId), args);
	}
}

class WtCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	public WtCursorAdapter(Context ctx) {
		super(ctx, R.layout.wt_list_item, null, NO_FLAGS);
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
		cbx.setChecked(false);
		cbx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
				View listItem = (View) btn.getParent();
				ViewGroup parent = (ViewGroup) listItem.getParent();

				if(parent == null) return;

				int listIndex = parent.indexOfChild(listItem);

				trace(this, "Changed checkbox at %d to %s", listIndex, isChecked);

				((WtListActivity) ctx).updateChecked(m.id, listIndex, isChecked);
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
