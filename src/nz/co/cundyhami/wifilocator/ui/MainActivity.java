package nz.co.cundyhami.wifilocator.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

import nz.co.cundyhami.wifilocator.db.WifiDBHelper;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {

	WifiManager mainWifi;
	WifiReceiver receiverWifi;
	List<ScanResult> wifiList;
	StringBuilder sb = new StringBuilder();
	private ListView lv;
	private ProgressBar pb;
	public APListAdapter adap;
	private boolean scanning;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

		mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		receiverWifi = new WifiReceiver();
		lv = (ListView) findViewById(R.id.ListView1);
		pb = new ProgressBar(this);
		pb.setIndeterminate(true);
		lv.setEmptyView(pb);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(APListAdapter.isAPSaved(((ScanResult)adap.getItem(position)).BSSID, MainActivity.this)){
					showAPDetails(position);
				}else{
					showSaveAPDialog(position);
				}
				

			}

		});
	}

	protected void showAPDetails(int position) {
		final ScanResult sr = (ScanResult) adap.getItem(position);
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		build.setTitle("AP " + sr.BSSID);
		WifiDBHelper dbh = new WifiDBHelper(this);
		SQLiteDatabase db = dbh.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM AccessPointLocations where BSSID = '" + sr.BSSID + "'", null);
		String x = null,y = null;
		int floor = 0;
		if(c.moveToFirst()){
			x = c.getString(c.getColumnIndex("x"));
			y = c.getString(c.getColumnIndex("y"));
			floor = c.getInt(c.getColumnIndex("floor"));
		}
		c.close();
		db.close();
		dbh.close();
		String data = "SSID: " + sr.SSID + "\nStrength: " + WifiManager.calculateSignalLevel(sr.level, 100) + "%\n\nX coord: " + x + "\nY coord: " + y + "\nFloor: " + floor;
		build.setMessage(data);
		build.setPositiveButton("Close", null);
		build.setNegativeButton("Delete record", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				deleteRecord(sr.BSSID);
				adap.notifyDataSetChanged();
				
			}
		});
		build.create().show();
		
	}

	protected void deleteRecord(String bSSID) {
		WifiDBHelper dbh = new WifiDBHelper(this);
		SQLiteDatabase db = dbh.getReadableDatabase();
		db.delete("AccessPointLocations", "BSSID = '" + bSSID + "'", null);
		db.close();
		dbh.close();
		
	}

	protected void showSaveAPDialog(int position) {
		final ScanResult sr = (ScanResult) adap.getItem(position);
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		build.setTitle("Save AP " + sr.BSSID);
		View customView = getLayoutInflater().inflate(R.layout.save_dialog,
				null);
		final EditText e1 = (EditText) customView.findViewById(R.id.editText1);
		final EditText e2 = (EditText) customView.findViewById(R.id.editText2);
		final Spinner s1 = (Spinner) customView.findViewById(R.id.spinner1);
		build.setView(customView);
		build.setPositiveButton("Save", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String x = e1.getText().toString();
				String y = e2.getText().toString();
				int floor = Integer.valueOf((String) s1.getSelectedItem());
				String ssid = sr.SSID;
				String bssid = sr.BSSID;
				saveAP(ssid, bssid, x, y, floor);
				adap.notifyDataSetChanged();
			}
		});
		build.setNegativeButton("Cancel", null);
		Dialog d = build.create();
		d.show();

	}

	protected void saveAP(String ssid, String bssid, String x, String y,
			int floor) {
		WifiDBHelper dbh = new WifiDBHelper(this);
		SQLiteDatabase db = dbh.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("SSID", ssid);
		cv.put("BSSID", bssid);
		cv.put("floor" , floor);
		cv.put("x", x);
		cv.put("y", y);
		db.insertOrThrow("AccessPointLocations", null, cv);
		db.close();
		dbh.close();
		
	}

	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.action_scan:
			doScan();
			return true;
		case R.id.action_export_db:
			copyDBtoSD();
			return true;
		
		}
		return false;
	}

	private void copyDBtoSD() {
		try {
	        File sd = Environment.getExternalStorageDirectory();
	        File data = Environment.getDataDirectory();

	        if (sd.canWrite()) {
	            String currentDBPath = "//data//nz.co.cundyhami.wifilocator.ui//databases//WIFIDB.sqlite";
	            String backupDBPath = "WIFIDB.sqlite";
	            File currentDB = new File(data, currentDBPath);
	            File backupDB = new File(sd, backupDBPath);

	            if (currentDB.exists()) {
	            	if(backupDB.exists()){
	            		backupDB.delete();
	            	}
	                FileChannel src = new FileInputStream(currentDB).getChannel();
	                FileChannel dst = new FileOutputStream(backupDB).getChannel();
	                dst.transferFrom(src, 0, src.size());
	                src.close();
	                dst.close();
	            }
	        }
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
		
	}

	protected void onResume() {
		registerReceiver(receiverWifi, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		super.onResume();
		doScan();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void doScan() {
		// lv.setAdapter(null);
		// adap = null;
		mainWifi.startScan();
		setProgressBarIndeterminateVisibility(true);
		scanning = true;
	}

	public void onPause() {
		super.onPause();
		this.unregisterReceiver(receiverWifi);
	}

	class WifiReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(scanning){
			List<ScanResult> resultList = mainWifi.getScanResults();
			scanning = false;
			if (adap == null) {
				adap = new APListAdapter(MainActivity.this);
			}
			adap.setResults(resultList);
			lv.setAdapter(adap);
			setProgressBarIndeterminateVisibility(false);
			}
		}
	}

}
