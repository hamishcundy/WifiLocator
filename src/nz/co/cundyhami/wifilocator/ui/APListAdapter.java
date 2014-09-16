package nz.co.cundyhami.wifilocator.ui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nz.co.cundyhami.wifilocator.db.WifiDBHelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * An adapter for displaying information about the scanned aps
 * @author Hamish
 *
 */
public class APListAdapter extends BaseAdapter{
	
	private Context con;

	public APListAdapter(Context con){
		this.con = con;
	}
	
	private List<ScanResult> results;

	public void setResults(List<ScanResult> resultList){
		this.results = resultList;
		Collections.sort(this.results, new Comparator<ScanResult>(){

			

			@Override
			public int compare(ScanResult lhs, ScanResult rhs) {
				if(lhs.level == rhs.level){
					return 0;
				}else if(lhs.level > rhs.level){
					return -1;
				}else{
					return 1;
				}
			}
			
		});
		this.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		if(results != null){
			return results.size();
		}
		return 0;
	}

	@Override
	public Object getItem(int position) {
		if(results != null){
			return results.get(position);
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.ap_list_item, null);
		
		ScanResult sr = (ScanResult) getItem(position);
		boolean isSaved = isAPSaved(sr.BSSID, con);
		((TextView) v.findViewById(R.id.textView1)).setText(sr.SSID);
		DecimalFormat df = new DecimalFormat("#.0");
		((TextView) v.findViewById(R.id.textView2)).setText(sr.BSSID + "	" + df.format(sr.frequency / (double)1000) + "Ghz");
		((TextView) v.findViewById(R.id.textView3)).setText(df.format(Helper.calculateDistance(sr.level, sr.frequency)) + "m");
		if(isSaved){
			((TextView) v.findViewById(R.id.textView4)).setVisibility(View.VISIBLE);
		}
		return v;
		
	}

	public static boolean isAPSaved(String bSSID, Context cont) {
		/*
		WifiDBHelper dbh = new WifiDBHelper(cont);
		SQLiteDatabase db = dbh.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM AccessPointLocations WHERE BSSID = '" + bSSID + "'", null);
		if(c.moveToFirst()){
			c.close();
			db.close();
			dbh.close();
			return true;
		}else{
			c.close();
			db.close();
			dbh.close();
			return false;
		}
		*/
		return false;
	}

}
