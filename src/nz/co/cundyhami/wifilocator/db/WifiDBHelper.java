package nz.co.cundyhami.wifilocator.db;

import java.io.IOException;
import java.util.Scanner;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WifiDBHelper extends SQLiteOpenHelper{
	
	private final static String DATABASE_NAME = "WIFIDB.sqlite";
	private final static int DATABASE_VERSION = 1;
	private Context con;
	
	/**
	 * Database helper to assist with creating and managing the database
	 * @param con
	 */
	public WifiDBHelper(Context con){
		super(con, DATABASE_NAME, null, DATABASE_VERSION);
		this.con = con;
	}

	/**Called when database first created. Parse in a schema and data for our app from its assets
	 * 
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		if(con != null){
		try {
			Scanner sc = new Scanner(con.getAssets().open("APMap.sql"));
			while(sc.hasNextLine()){
				db.execSQL(sc.nextLine());
			}
			sc.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
}
