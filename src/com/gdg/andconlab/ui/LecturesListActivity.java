package com.gdg.andconlab.ui;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.gdg.andconlab.DBUtils;
import com.gdg.andconlab.DatabaseHelper;
import com.gdg.andconlab.R;
import com.gdg.andconlab.ServerCommunicationManager;

/**
 * Activity that displays a list of all lectures given in GDG
 * If no lectures were found in local DB - it will auto fetch lectures from the server,
 * displaying a wait dialog to the user. 
 * @author Ran Nachmany
 *
 */
public class LecturesListActivity extends SherlockActivity{

	private ListView mList;
	private ProgressDialog mProgressDialog;
	private BroadcastReceiver mUpdateReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.single_list_activity);
		mList = (ListView) findViewById(R.id.list);

		mUpdateReceiver = new BroadcastReceiver() {
			//TODO: [Ran] handle network failure
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equalsIgnoreCase(ServerCommunicationManager.RESULTS_ARE_IN)) {
					new lecturesLoader().execute((Void) null);
				}

				if (null != mProgressDialog)
					mProgressDialog.dismiss();
			}
		};
		
		new lecturesLoader().execute((Void)null);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (null != mUpdateReceiver) {
			unregisterReceiver(mUpdateReceiver);
			mUpdateReceiver = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		final IntentFilter filter = new IntentFilter();
		filter.addAction(ServerCommunicationManager.RESULTS_ARE_IN);
		registerReceiver(mUpdateReceiver, filter);
	}

	private void refreshList(boolean firstLoad) {
		if (firstLoad) {
			mProgressDialog = ProgressDialog.show(this, getString(R.string.progress_dialog_starting_title), getString(R.string.progress_dialog_starting_message));
		}
		
		ServerCommunicationManager.getInstance(getApplicationContext()).startSearch("Android", 1);
	}

	////////////////////////////////
	// Async task that queries the DB in background
	////////////////////////////////
	private class lecturesLoader extends AsyncTask<Void, Void, Cursor> {
		@Override
		protected Cursor doInBackground(Void... params) {


			SQLiteDatabase db = new DatabaseHelper(LecturesListActivity.this.getApplicationContext(), DatabaseHelper.DB_NAME,null , DatabaseHelper.DB_VERSION).getReadableDatabase();
			return DBUtils.getEventsCurosr(db);
		}

		@Override
		protected void onPostExecute(Cursor result) {
			if (0 == result.getCount()) {
				//we don't have anythign in our DB, force network refresh
				refreshList(true);
			}
			else {
				LecturesAdapter adapter = (LecturesAdapter) mList.getAdapter();
				if (null == adapter) {
					adapter = new LecturesAdapter(LecturesListActivity.this.getApplicationContext(), result);
					mList.setAdapter(adapter);	
				}
				else {
					adapter.changeCursor(result);
				}
			}
		}
	}
}
