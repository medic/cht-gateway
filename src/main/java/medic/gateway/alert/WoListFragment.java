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

import java.util.LinkedList;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.relativeTimestamp;
import static medic.gateway.alert.Utils.setText;
import static medic.gateway.alert.WoMessage.Status.FAILED;
import static medic.gateway.alert.WoMessage.Status.UNSENT;

public class WoListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	public static final int LOADER_ID = 3;

	private static final DialogInterface.OnClickListener NO_CLICK_LISTENER = null;

	private Db db;

	@Override public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.db = Db.getInstance(getActivity());

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

		messageDetailDialog(m, position).show();
	}

	private AlertDialog messageDetailDialog(final WoMessage m, final int position) {
		LinkedList<String> content = new LinkedList<>();

		content.add(string(R.string.lblTo, m.to));
		if(m.status == FAILED) {
			content.add(string(R.string.lblStatusWithCause, m.status, m.getFailureReason()));
		} else {
			content.add(string(R.string.lblStatus, m.status));
		}
		content.add(string(R.string.lblLastAction, relativeTimestamp(m.lastAction)));
		content.add(string(R.string.lblContent, m.content));

		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		if(m.status.canBeRetried()) {
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

		WoMessage m = db.getWoMessage(id);

		if(m.status.canBeRetried()) {
			db.updateStatus(m, UNSENT);

			WoMessage updated = db.getWoMessage(id);

			View v = getListView().getChildAt(position);
			setText(v, R.id.txtWoStatus, updated.status.toString());
			setText(v, R.id.txtWoLastAction, relativeTimestamp(updated.lastAction));
		}
	}

	private final String string(int stringId, Object...args) {
		return String.format(getActivity().getString(stringId), args);
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

		setText(v, R.id.txtWoStatus, m.status.toString());
		setText(v, R.id.txtWoLastAction, relativeTimestamp(m.lastAction));
		setText(v, R.id.txtWoTo, m.to);
		setText(v, R.id.txtWoContent, m.content);

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

				activity.updateChecked(m.id, listIndex, isChecked);
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
