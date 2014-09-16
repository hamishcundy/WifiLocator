package nz.co.cundyhami.wifilocator.ui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import util.geometry.Circle;
import util.geometry.CircleCircleIntersection;
import util.geometry.Vector2;

import nz.co.cundyhami.wifilocator.db.WifiDBHelper;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.provider.Settings;

/**
 * The primary activity for this applicaton, responsible for finding the users location
 * 
 * @author Hamish
 *
 */
public class LocalizeActivity extends Activity implements OnClickListener {

	private View startView, doneView, scanView, nextView;
	private WifiManager mainWifi;
	private WifiReceiver wifiRec;
	private boolean scanning;
	private ArrayList<HashMap<String, Double>> resultList;

	private int step;
	private Integer floor;
	private String resString;
	private int x;
	private int y;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_localize);
		findBaseViews();
		setButtonListeners();
		mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiRec = new WifiReceiver();

	}

	@Override
	public void onResume() {
		super.onResume();
		refreshPrereqs();
	}

	private void setButtonListeners() {
		findViewById(R.id.done_button).setOnClickListener(this);
		findViewById(R.id.gps_settings).setOnClickListener(this);
		findViewById(R.id.retry_button).setOnClickListener(this);
		findViewById(R.id.wifi_settings).setOnClickListener(this);
		findViewById(R.id.start_button).setOnClickListener(this);

	}

	private void findBaseViews() {
		startView = findViewById(R.id.pre_view);
		doneView = findViewById(R.id.done_view);
		scanView = findViewById(R.id.scan_view);
		nextView = findViewById(R.id.next_view);
	}

	private void prelocalizeOps() {
		registerReceiver(wifiRec, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		step = 0;
		resultList = new ArrayList<HashMap<String, Double>>();
	}

	private void postlocalizeOps() {
		this.unregisterReceiver(wifiRec);
	}

	public void refreshPrereqs() {
		boolean prereqsMet = true;

		if (mainWifi.isWifiEnabled()) {
			((TextView) findViewById(R.id.textView6))
					.setTextColor(getResources().getColor(R.color.green));
			((TextView) findViewById(R.id.textView6)).setText("Enabled");
			findViewById(R.id.wifi_settings).setVisibility(View.GONE);
		} else {
			prereqsMet = false;
			((TextView) findViewById(R.id.textView6))
					.setTextColor(getResources().getColor(R.color.red));
			((TextView) findViewById(R.id.textView6)).setText("Disabled");
			findViewById(R.id.wifi_settings).setVisibility(View.VISIBLE);
		}

		LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			((TextView) findViewById(R.id.textView7))
					.setTextColor(getResources().getColor(R.color.green));
			((TextView) findViewById(R.id.textView7)).setText("Disabled");
			findViewById(R.id.gps_settings).setVisibility(View.GONE);
		} else {
			prereqsMet = false;
			((TextView) findViewById(R.id.textView7))
					.setTextColor(getResources().getColor(R.color.red));
			((TextView) findViewById(R.id.textView7)).setText("Enabled");
			findViewById(R.id.gps_settings).setVisibility(View.VISIBLE);
		}

		if (prereqsMet) {
			findViewById(R.id.start_button).setEnabled(true);
		} else {
			findViewById(R.id.start_button).setEnabled(false);
		}

	}

	private void doScan() {
		this.scanView.setVisibility(View.VISIBLE);
		this.startView.setVisibility(View.GONE);
		this.doneView.setVisibility(View.GONE);
		this.nextView.setVisibility(View.GONE);
		step++;
		mainWifi.startScan();
		scanning = true;
		((TextView) findViewById(R.id.scantext)).setText("Performing scan "
				+ step + " of 12");
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_button:
			floor = Integer
					.valueOf((String) ((Spinner) findViewById(R.id.spinner1))
							.getSelectedItem());
			startLocalization();
			break;
		case R.id.done_button:
			doScan();
			break;
		case R.id.wifi_settings:
			startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			break;
		case R.id.gps_settings:
			startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			break;
		case R.id.retry_button:
			floor = Integer
					.valueOf((String) ((Spinner) findViewById(R.id.spinner2))
							.getSelectedItem());
			startLocalization();
			break;
		}

	}

	private void startLocalization() {
		// Toast.makeText(this, "Floor " + floor, Toast.LENGTH_SHORT).show();
		this.scanView.setVisibility(View.VISIBLE);
		this.startView.setVisibility(View.GONE);
		this.doneView.setVisibility(View.GONE);
		this.nextView.setVisibility(View.GONE);
		this.prelocalizeOps();
		doScan();

	}

	/**
	 * Broadcast receiver to listen for the Wifi scan intent. On receiving the
	 * broadcast, we check that is was our app that asked for the scan using a
	 * boolean flag. We the process the results, then update the ui as
	 * appropriate.
	 * 
	 * @author Hamish
	 * 
	 */
	private class WifiReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (scanning) {// flag is up, it was us that scanned
				scanning = false;
				processScanResults(mainWifi.getScanResults(), step);// process
																	// the
																	// results
																	// of this
																	// scan
				if (step == 12) {// last scan, perform final processing
					processData();
				} else if (step % 3 == 0) {// 3, 6 or 9th scan. prompt the user
											// to rotate

					showNextView();
				} else {// any other repetition. simply tell the wifi manager to
						// scan again
					doScan();
				}
			}
		}

	}

	/**
	 * Sets the provided x and y coordinates in the ui, and show the finished
	 * screen
	 * 
	 * @param x
	 * @param y
	 */
	public void setResult(int x, int y) {
		this.scanView.setVisibility(View.GONE);
		this.startView.setVisibility(View.GONE);
		this.doneView.setVisibility(View.VISIBLE);
		this.nextView.setVisibility(View.GONE);
		((TextView) this.findViewById(R.id.result_text)).setText(x + ", " + y);

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.localize, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {

		case R.id.action_scanner:
			Intent i = new Intent(this, MainActivity.class);// go to a class to
															// scan for and
															// record access
															// points
			startActivity(i);
			return true;
		}
		return false;
	}

	/**
	 * Process the results of a Wifi scan. In this step, we build a strength map
	 * based on ap, with each record holding the received signal strength and
	 * the frequency. We then calculate separate averages for the 2.4Ghz and
	 * 5Ghz SSID's received from this AP, and average the 2 readings. We then
	 * check that the AP is in our database, and if so save the average signal
	 * strength for that AP in a map. This map is then added to a global list
	 * for final processing
	 * 
	 * @param scanResults
	 * @param scanStep
	 */
	public void processScanResults(List<ScanResult> scanResults, int scanStep) {
		HashMap<String, HashMap<Integer, Double>> strengthMap = new HashMap<String, HashMap<Integer, Double>>();
		for (ScanResult sr : scanResults) {// for each recorded scan
			String prefix = sr.BSSID.substring(0, sr.BSSID.length() - 1);// get
																			// the
																			// bssid
																			// prefix.
																			// Any
																			// bssid's
																			// with
																			// all
																			// character
																			// but
																			// the
																			// last
																			// identical
																			// are
																			// from
																			// the
																			// same
																			// AP
			if (strengthMap.containsKey(prefix)) {// add to map if exists
				strengthMap.get(prefix).put(Integer.valueOf(sr.level),
						Double.valueOf(sr.frequency));
			} else {
				HashMap<Integer, Double> levels = new HashMap<Integer, Double>();// create
																					// and
																					// add
																					// if
																					// it
																					// doesnt
				levels.put(Integer.valueOf(sr.level),
						Double.valueOf(sr.frequency));
				strengthMap.put(prefix, levels);
			}
		}
		HashMap<String, Double> avStrengths = new HashMap<String, Double>();
		// we know want to calcualate the average
		// distance per ap for this scan
		// do 2.4 and 5ghz separate, then take average of those

		for (Entry<String, HashMap<Integer, Double>> ent : strengthMap
				.entrySet()) {
			ArrayList<Double> ghz5Dist = new ArrayList<Double>();//list of 5ghz bssid
			ArrayList<Double> ghz2Dist = new ArrayList<Double>();//list of 2.4ghz bssid
			for (Entry<Integer, Double> resent : ent.getValue().entrySet()) {
				double dist = Helper.calculateDistance(
						Math.abs(resent.getKey()), resent.getValue());
				if (resent.getValue() > 3500) {//add to 5ghz
					ghz5Dist.add(dist);
				} else {//add to 2.4ghz
					ghz2Dist.add(dist);
				}
			}
			double total5 = 0;
			if (ghz5Dist.size() >= 2) {//as each ap is broadcasting 6 ssids (or 12 if both frequencies) we only want to include if we receive at least 2 bssids.
				//if we receive less, it was probably an outlier we can safely ignore
				total5 = average(ghz5Dist);
			}
			double total2 = 0;
			if (ghz2Dist.size() >= 2) {
				total2 = average(ghz2Dist);
			}
			Log.d("Local", ent.getKey() + ": " + ghz5Dist.size() + " " + total5
					+ " " + ghz2Dist.size() + " " + total2);
			if (total5 != 0 && total2 != 0) {//both freqs at this ap, average
				avStrengths.put(ent.getKey(), (total5 + total2) / 2);
			} else if (total5 != 0) {//5ghz only
				avStrengths.put(ent.getKey(), total5);
			} else if (total2 != 0) {//2.4ghz only
				avStrengths.put(ent.getKey(), total2);
			}
		}
		// now check that ap is in our db
		HashMap<String, Double> checkedStrengths = new HashMap<String, Double>();

		WifiDBHelper dbh = new WifiDBHelper(this);
		SQLiteDatabase db = dbh.getReadableDatabase();
		Cursor c;
		for (Entry<String, Double> ent : avStrengths.entrySet()) {
			c = db.rawQuery(
					"SELECT * from APMap WHERE bssid_prefix = '" + ent.getKey()
							+ "'", null);//check if we have a bssid prefix matching this ap
			if (c.moveToFirst()) {
				checkedStrengths.put(ent.getKey(), ent.getValue());//if yes, add to last map
			}
			c.close();
		}
		db.close();
		dbh.close();

		// Log.d("Localize", "Results from scan " + step);
		// for(Entry<String, Double> ent: avStrengths.entrySet()){
		// Log.d("Localize", "AP " + ent.getKey() + ": " + ent.getValue() +
		// " metres away");
		// }
		resultList.add(checkedStrengths);//add the map of average signal strengths for this AP to global list

	}

	/**Helper method to take the average of a list of doubles
	 * 
	 * @param list
	 * @return
	 */
	private double average(ArrayList<Double> list) {
		if (list.size() == 0) {
			return 0;
		}
		double total = 0;
		for (double d : list) {
			total += d;
		}
		total = total / list.size();
		return total;

	}

	/**Show the 'next' ui, which prompts the user to turn 90 degrees then click to continue
	 * 
	 */
	public void showNextView() {
		this.scanView.setVisibility(View.GONE);
		this.startView.setVisibility(View.GONE);
		this.doneView.setVisibility(View.GONE);
		this.nextView.setVisibility(View.VISIBLE);

	}

	/**
	 * Called after all our scans are completed. It updates the ui and calls the calculateLocation method in another thread. At this point, we have a ArrayList of 12 Maps (1 per scan)
	 * Each map contains BSSID prefixes (AP's) and their estimated distance during this scane
	 */
	public void processData() {
		this.scanView.setVisibility(View.VISIBLE);
		this.startView.setVisibility(View.GONE);
		this.doneView.setVisibility(View.GONE);
		this.nextView.setVisibility(View.GONE);
		((TextView) findViewById(R.id.scantext))
				.setText("Calculating location..");
		new Thread(new Runnable() {
			public void run() {
				postlocalizeOps();
				calculcateLocation();
			}
		}).start();
	}

	/**
	 * This is the main method that calculates the user estimated location
	 */
	protected void calculcateLocation() {
		// now we have a list of 12 maps (1 per scan) which hold the average
		// calculated distance to each ap picked up in that scan
		HashMap<String, ArrayList<Double>> aps = new HashMap<String, ArrayList<Double>>();
		//first, we wish to sort the data by Access Point. This will allow us to take an average distance using the data from each scan
		for (HashMap<String, Double> scan : resultList) {
			Log.d("Local", "Scan: " + scan.size());
			for (Entry<String, Double> ent : scan.entrySet()) {
				if (aps.containsKey(ent.getKey())) {
					aps.get(ent.getKey()).add(ent.getValue());
				} else {
					ArrayList<Double> list = new ArrayList<Double>();
					list.add(ent.getValue());
					aps.put(ent.getKey(), list);
				}
			}
		}
		//we know take the average strength for each ap and store it in a map
		HashMap<String, Double> averageDistances = new HashMap<String, Double>();
		for (Entry<String, ArrayList<Double>> ent : aps.entrySet()) {
			Log.d("Local", "AvDist: " + ent.getValue().size());
			if (ent.getValue().size() > 5) {// to be included, has to have been
											// picked up on at least 5 scans
				double average = 0;
				for (double d : ent.getValue()) {
					average += d;
				}
				average = average / ent.getValue().size();
				averageDistances.put(ent.getKey(), average);
			}
		}
		resString = "";

		x = 0;
		y = 0;
		//we now create a list of point object, which contain the bssid prefix (ap identifier), recorded x, y and floor coordinates of the ap, and the estimated distance between the user and this ap
		ArrayList<Point> points = getPoints(averageDistances);
		resString = points.size() + "AP:\n";
		for (Point p : points) {
			resString += p.getString();
		}
		if (points.size() == 1) {// we only got one ap, so we have nothing else
									// to go on, set our location to this ap

			x = points.get(0).getX();
			y = points.get(0).getY();

		} else if (points.size() == 2) {// 2 points to go off, so calculate
										// point on line between them
			int x1 = points.get(0).getX(), x2 = points.get(1).getX(), y1 = points
					.get(0).getY(), y2 = points.get(1).getY();

			int difX = Math.abs(x1 - x2);//difference in x
			int difY = Math.abs(y1 - y2);//difference in y
			//Log.d("local", "difX: " + difX + "  difU: " + difY);
			double distBetweenPoints = Math.sqrt(Math.pow(difX, 2)
					+ Math.pow(difY, 2));// length of line between them
			//Log.d("local", "dist between " + distBetweenPoints);

			double distMeasured = points.get(0).getMeasuredDistance()
					+ points.get(1).getMeasuredDistance();
			//Log.d("local", "dist meas " + distMeasured);
			double ratio = distBetweenPoints / distMeasured;
			double percentFromX = (points.get(0).getMeasuredDistance() * ratio)
					/ distBetweenPoints;
			//Log.d("local", "perc " + percentFromX + "  ratio " + ratio);
			double addToX = (x1 - x2) * percentFromX;
			double addToY = (y1 - y2) * percentFromX;
			//Log.d("local", "addx " + addToX + "  addy " + addToY);
			x = (int) (x1 - addToX);
			y = (int) (y1 - addToY);

		} else if (points.size() > 2) {//more than 2 points found. Trilateration time!
			//first, create a list of Circle object (containing a radius and center coordinates). 
			//For each point, we create a circle with center on ap coordinates and radius the estimated distance to user
			ArrayList<Circle> circles = new ArrayList<Circle>();
			for (Point p : points) {
				int xt = p.getX();
				int yt = p.getY();
				
//				if (p.getFloor() != floor) {
//					 xt = (int) (xt * 1.3);
//					 yt = (int) (yt * 1.3);
//				}
				
				Vector2 v2 = new Vector2(xt, yt);
				Circle c2d = new Circle(v2, p.getMeasuredDistance());
				circles.add(c2d);
			}
			//We now calculate the intersects of each circle with every other circle. However, in most cases the distances are skewed by obstructions, and aps are closer than calculated.
			//This means it is easy to have 2 circles around access points inside each other, with no intersections.
			//To fix this , we scale the radius of the circle until we get at least one intersection. However, we cant start high and go low until we find an intersection - 
			//in some cases this will cause an intersection on the far side of the circles (past both centers), and the intersection we want (between the center) requires smaller circles.
			//Therefore, we start with very small circles, and gradually work our way up until we find an intersection
			ArrayList<Vector2> results = new ArrayList<Vector2>();
			for (int i = 0; i < circles.size(); i++) {
				for (int j = 0; j < circles.size(); j++) {
					if (j != i) {//dont calculate intersection with oneself

						double factor = 0.001;//starting factor
						Vector2[] intersects = null;
						Circle c1, c2;
						CircleCircleIntersection inters;
						do {//keep repeating until we have a intersection, or reach an upper limit
							c1 = new Circle(circles.get(i).c, circles.get(i).r
									* factor);
							c2 = new Circle(circles.get(j).c, circles.get(j).r
									* factor);//mutliply actual radius by factor
							Log.d("local",
									circles.get(i).r + " " + circles.get(j).r
											+ " " + factor + " "
											+ (circles.get(i).r * factor) + " "
											+ (circles.get(j).r * factor));
							inters = new CircleCircleIntersection(c1, c2);
							intersects = inters.getIntersectionPoints();
							factor = factor * 1.1;//multiply factor by 1.1 each repetition. Any bigger, an we risk jumping straight past the correc factor for intersection
						} while (intersects.length == 0 && factor < 1.5);
						if (intersects.length == 1) {//if we have 1 intersection (spot on), store

							resString += c1.c.toString() + " "
									+ c2.c.toString() + " "
									+ intersects[0].toString() + "\n";
							results.add(intersects[0]);
						} else if (intersects.length == 2) {//if we have 2, find the centroid between them and store that
							Vector2 med = centroid(intersects[0], intersects[1]);
							resString += c1.c.toString() + " "
									+ c2.c.toString() + " " + med.toString()
									+ " " + "\n";
							results.add(med);
						} else if (intersects.length == 0) {//if no intersections found (reached upper bound), use the point with the smaller radius - we are likely very close to this
							resString += c1.c.toString() + " "
									+ c2.c.toString() + " no intersect ("
									+ inters.type + "\n";
							if (c1.r > c2.r) {
								results.add(c2.c);
							} else {
								results.add(c1.c);
							}
						}

					}

				}
			}
			double totX = 0, totY = 0;
			for (Vector2 vect : results) {
				totX += vect.x;
				totY += vect.y;
			}
			//compute the centroid of all our recorded points
			x = (int) (totX / results.size());
			y = (int) (totY / results.size());

		}

		runOnUiThread(new Runnable() {
			public void run() {
				// AlertDialog.Builder builder = new AlertDialog.Builder(
				// LocalizeActivity.this);
				// builder.setMessage(resString);
				// builder.setPositiveButton("ok", null).create().show();
				setResult(x, y);
				((TextView) findViewById(R.id.textView4)).setText(resString);
			}
		});

	}

	/**Gets the midpoint of 2 points
	 * 
	 * @param c
	 * @param c2
	 * @return
	 */
	private Vector2 centroid(Vector2 c, Vector2 c2) {
		return new Vector2((c.x + c2.x) / 2, (c.y + c2.y) / 2);
	}

	/**Returns a list of Point objects from a map of recorded distances and the database
	 * 
	 * @param averageDistances
	 * @return
	 */
	private ArrayList<Point> getPoints(HashMap<String, Double> averageDistances) {
		String prefixes = "('";
		for (String str : averageDistances.keySet()) {
			prefixes = prefixes + str + "','";
		}
		prefixes += "')";
		WifiDBHelper dbh = new WifiDBHelper(this);
		SQLiteDatabase db = dbh.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM APMap WHERE bssid_prefix IN "
				+ prefixes, null);
		ArrayList<Point> points = new ArrayList<Point>();
		if (c.moveToFirst()) {
			do {
				Point p = new Point();
				p.setX(c.getInt(c.getColumnIndex("x")));
				p.setY(c.getInt(c.getColumnIndex("y")));
				p.setBssidPrefix(c.getString(c.getColumnIndex("bssid_prefix")));
				p.setFloor(c.getInt(c.getColumnIndex("z")));
				p.setMeasuredDistance(averageDistances.get(c.getString(c
						.getColumnIndex("bssid_prefix"))));
				points.add(p);
			} while (c.moveToNext());

		}
		c.close();
		db.close();
		dbh.close();
		return points;

	}

}
