package com.cottagecoders.dottie;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DottieDB extends SQLiteOpenHelper {
	private static final String TAG = "DottieDB";

	private static final String DB_NAME = "DottieDB.sqlite";
	private static final int VERSION = 1;
	private static final String T_ITEM_INVENTORY = "iteminventory";
	// used for the ordered scores
	private static final String T_ITEMS = "item";
	private static final String T_CONSUMPTION = "consumption";
	// used for the alpha scores

	private static SQLiteDatabase db = null;

	public Context ctx;
	int mem_ctr = 0;

	public DottieDB(Context context) {
		super(context, DB_NAME, null, VERSION);
		ctx = context;
		return;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// create tables...
		if (db == null) {
			db = getWritableDatabase();
		}
		Log.i(TAG, "onCreate(): SQLiteDatabase -- got here.");
		String prof = "Create table "
				+ T_ITEM_INVENTORY
				+ " (id integer primary key, noofitems integer, category varChar(40) )";

		String prof_ix2 = "Create  index " + T_ITEM_INVENTORY + "_ix2 "
				+ " on " + T_ITEM_INVENTORY + " (category)";
		try {
			db.execSQL(prof);
		} catch (Exception e) {
			Log.e(TAG, "error creating table " + T_ITEM_INVENTORY + ": " + prof
					+ " " + e);
		}

		try {
			db.execSQL(prof_ix2);
		} catch (Exception e) {
			Log.e(TAG, "error creating index 2 on table " + T_ITEM_INVENTORY
					+ ": " + prof_ix2 + " " + e);
		}

		prof = "Create table "
				+ T_ITEMS
				+ " ( id integer,  type varchar(1), name varchar(40), category varchar(40), verb varchar(30), "
				+ " unicode1 varchar(5), unicode2 varchar(5), unicode3 varchar(5), used integer, plural varchar(40), article varchar(10))";
		try {
			db.execSQL(prof);
		} catch (Exception e) {
			Log.e(TAG, "error creating " + T_ITEMS + ": " + prof + " " + e);
		}

		String prof_ix1 = "Create unique index " + T_ITEMS + "_ix1 " + " on "
				+ T_ITEMS + " (id)";
		try {
			db.execSQL(prof_ix1);
		} catch (Exception e) {
			Log.e(TAG, "error creating index on " + T_ITEMS + ": " + prof_ix1
					+ " " + e);
		}
		prof_ix2 = "Create unique index " + T_ITEMS + "_ix2 " + " on "
				+ T_ITEMS + " (type, category, used, id)";
		try {
			db.execSQL(prof_ix2);
		} catch (Exception e) {
			Log.e(TAG, "error creating index " + T_ITEMS + ": " + prof_ix2
					+ " " + e);
		}
		String prof_ix3 = "";
		prof_ix3 = "Create unique index " + T_ITEMS + "_ix3 " + " on "
				+ T_ITEMS + " (unicode1)";

		try {
			db.execSQL(prof_ix3);
		} catch (Exception e) {
			Log.e(TAG, "error creating items index detail 3 " + prof_ix3 + " "
					+ e);
		}

		/*
		 * TODO: this is for debugging to catch duplicates. String prof_ix4 =
		 * "";
		 * 
		 * prof_ix4 = "Create unique index " + T_ITEMS + "_ix4 " + " on " +
		 * T_ITEMS + " (unicode2, unicode3)";
		 * 
		 * try { db.execSQL(prof_ix4); } catch (Exception e) { Log.e(TAG,
		 * "error creating items index detail 4 " + prof_ix4 + " " + e); }
		 */

		prof = "Create table "
				+ T_CONSUMPTION
				+ " (day integer, item1 iteger, item2 integer, item3 integer, item4 integer, item5 integer,"
				+ " item6 integer, item7 integer, item8 integer, item9 integer, item10 integer,"
				+ " item11 integer, item12 integer)";

		prof_ix1 = "Create unique index " + T_CONSUMPTION + "_ix1 " + " on "
				+ T_CONSUMPTION + " (day)";

		try {
			db.execSQL(prof);
		} catch (Exception e) {
			Log.e(TAG, "error creating database consumption" + prof + " " + e);
		}

		try {
			db.execSQL(prof_ix1);
		} catch (Exception e) {
			Log.e(TAG, "error creating index 1 database consumption "
					+ prof_ix1 + " " + e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// implement migration code here.
		Log.i(TAG, "onUpgrade() -- get here.");

	}

	public int getCount() {

		String stmt;
		if (db == null) {
			db = getWritableDatabase();
		}
		stmt = "select * from " + T_ITEMS;

		Cursor c;
		try {
			c = db.rawQuery(stmt, null);
		} catch (Exception e) {
			Log.e(TAG, "getCount() -- query failed." + e);
			return 0;
		}
		if (c == null)
			return 0;
		int x = c.getCount();
		c.close();
		return x;
	}

	public ArrayList<String> getCategories() {
		ArrayList<String> arr = new ArrayList<String>();
		for (int i = 0; i < DispStatus.catItems.length; i++) {
			String str = DispStatus.catItems[i];
			arr.add(str);
		}
		return arr;
	}

	public ArrayList<ItemRecord> getItemList(String cat) {

		String stmt;
		if (db == null) {
			db = getWritableDatabase();
		}
		stmt = "select id, type, name, category, verb, unicode1, unicode2, unicode3 , used, plural, article from "
				+ T_ITEMS
				+ " where category = "
				+ "\""
				+ cat
				+ "\""
				+ "order by category, used";

		Cursor c;
		try {
			c = db.rawQuery(stmt, null);
		} catch (Exception e) {
			Log.e(TAG, "getItemList() -- query failed." + e);
			return null;
		}

		ArrayList<ItemRecord> ans = new ArrayList<ItemRecord>();
		while (c.moveToNext()) {
			ItemRecord pr = new ItemRecord(c.getInt(0), c.getString(1),
					c.getString(2), c.getString(3), c.getString(4),
					c.getString(5), c.getString(6), c.getString(7),
					c.getInt(8), c.getString(9), c.getString(10));

			ans.add(pr);
		}
		c.close();
		return ans;
	}

	public ConsumptionRecord getConsumption(int day) {

		String stmt;
		if (db == null) {
			db = getWritableDatabase();
		}
		stmt = "select day, item1, item2, item3, item4, item5, item6, item7, item8, item9, item10, item11, item12 from "
				+ T_CONSUMPTION + " where day= " + day;

		Cursor c = null;
		try {
			c = db.rawQuery(stmt, null);
		} catch (Exception e) {
			Log.e(TAG, "getConsumption() -- query failed: " + stmt + " " + e);
			return null;
		}

		if (c == null)
			return null;

		if (c.getCount() == 0) {
			c.close();
			return null;
		}

		c.moveToFirst();
		ConsumptionRecord pr = new ConsumptionRecord(c.getInt(0), c.getInt(1),
				c.getInt(2), c.getInt(3), c.getInt(4), c.getInt(5),
				c.getInt(6), c.getInt(7), c.getInt(8), c.getInt(9),
				c.getInt(10), c.getInt(11), c.getInt(12));

		c.close();

		return pr;

	}

	public void addToConsumption(int day, int item1, int item2, int item3,
			int item4, int item5, int item6, int item7, int item8, int item9,
			int item10, int item11, int item12) {

		String stmt;
		if (db == null) {
			db = getWritableDatabase();
		}
		stmt = "update " + T_CONSUMPTION + " set item1 = item1 + " + item1
				+ ", item2 = item2 +  " + item2 + ", item3 = item3 + " + item3
				+ ", item4 = item4 + " + item4 + ", item5 = item5 + " + item5
				+ ", item6 = item6 +" + item6 + ", item7 = item7 + " + item7
				+ ", item8 = item8 + " + item8 + ", item9 = item9 + " + item9
				+ ", item10 = item10 + " + item10 + ", item11 = item11 + "
				+ item11 + ", item12 = item12 + " + item12 + " where day = "
				+ day;

		try {
			db.execSQL(stmt);
		} catch (Exception e) {
			Log.d(TAG, "addtoConsumption():aDDTO failed: " + stmt + " " + e);
			return;
		}
		Cursor c = null;

		stmt = "select changes() from " + T_CONSUMPTION;
		try {
			c = db.rawQuery(stmt, null);
		} catch (Exception e) {
			Log.e(TAG, "addToconsumption() -- query failed." + e + stmt);
			return;
		}

		if (c == null) {
			Log.d(TAG, " addtoconsumption no changes applied");

		} else if (c.getCount() == 0) {
			Log.d(TAG, " addtoconsumption no changes applied");

		} else {
			c.moveToFirst();
			//Log.d(TAG, "number of rows changed " + c.getInt(0));
		}

		c.close();
		return;
	}

	public void updateConsumptionItem(int day, int index, int theVal) {
		String stmt;
		if (db == null) {
			db = getWritableDatabase();
		}

		stmt = "update " + T_CONSUMPTION + " set ";

		switch (index) {
		case 0:
			stmt = stmt + " item1 = " + theVal;
			break;
		case 1:
			stmt = stmt + " item2 = " + theVal;
			break;
		case 2:
			stmt = stmt + " item3 = " + theVal;
			break;
		case 3:
			stmt = stmt + " item4 = " + theVal;
			break;
		case 4:
			stmt = stmt + " item5 = " + theVal;
			break;
		case 5:
			stmt = stmt + " item6 = " + theVal;
			break;
		case 6:
			stmt = stmt + " item7 = " + theVal;
			break;
		case 7:
			stmt = stmt + " item8 = " + theVal;
			break;
		case 8:
			stmt = stmt + " item9 = " + theVal;
			break;
		case 9:
			stmt = stmt + " item10 = " + theVal;
			break;
		case 10:
			stmt = stmt + " item11 = " + theVal;
			break;
		case 11:
			stmt = stmt + " item12 = " + theVal;
			break;
		default:
			return;
		}

		stmt = stmt + " where day = " + day;

		try {
			db.execSQL(stmt);
		} catch (Exception e) {
			Log.e(TAG, "updateConsumptionitem():aDDTO failed: " + stmt + " "
					+ e);
		}
	}

	public void insertIntoConsumption(int day, int item1, int item2, int item3,
			int item4, int item5, int item6, int item7, int item8, int item9,
			int item10, int item11, int item12) {

		if (db == null) {
			db = getWritableDatabase();
		}

		String stmt = "insert into "
				+ T_CONSUMPTION
				+ " (day, item1, item2, item3, item4, item5,"
				+ " item6, item7, item8, item9, item10, item11, item12) values ( "
				+ day + ", " + item1 + ", " + item2 + ", " + item3 + ", "
				+ item4 + ", " + item5 + ", " + item6 + ", " + item7 + ", "
				+ item8 + ", " + item9 + ", " + item10 + ", " + item11 + ", "
				+ item12 + " ) ";

		try {
			db.execSQL(stmt);
		} catch (Exception e) {
			Log.e(TAG, "insertintoConsumption(): insert failed: " + stmt + " "
					+ e);
			addToConsumption(day, item1, item2, item3, item4, item5, item6,
					item7, item8, item9, item10, item11, item12);

			return;

		}
	}

	public ItemRecord getItem(int id) {

		String stmt;
		if (db == null) {
			db = getWritableDatabase();
		}
		stmt = "select  id, type, name, category, verb, unicode1, unicode2, unicode3 , used, plural, article from "
				+ T_ITEMS + " where id= " + id;

		Cursor c;
		try {
			c = db.rawQuery(stmt, null);
		} catch (Exception e) {
			Log.e(TAG, "getItem() -- query failed." + e + stmt);
			return null;
		}

		if (c == null) {
			return null;

		} else if (c.getCount() == 0) {
			c.close();
			return null;

		} else {
			c.moveToFirst();
			ItemRecord pr = new ItemRecord(c.getInt(0), c.getString(1),
					c.getString(2), c.getString(3), c.getString(4),
					c.getString(5), c.getString(6), c.getString(7),
					c.getInt(8), c.getString(9), c.getString(10));
			c.close();
			return pr;
		}
	}

	public void insertItem(String type, int id, String name, String category,
			String verb, String unicode1, String unicode2, String unicode3,
			String plural, String article) {
		int used = 0;
		if (db == null) {
			db = getWritableDatabase();
		}

		// name = toTitleCase(name);
		// note = toTitleCase(note);

		String stmt = "";
		stmt = "INSERT INTO "
				+ T_ITEMS
				+ " ( id, type, name, category, verb, unicode1, unicode2, unicode3, used, plural, article) values  ( "
				+ id + ", " + "\"" + type + "\"" + ", " + "\"" + name + "\""
				+ ", " + "\"" + category + "\"" + ", " + "\"" + verb + "\""
				+ ", " + "\"" + unicode1 + "\"" + ", " + "\"" + unicode2 + "\""
				+ ", " + "\"" + unicode3 + "\"" + ", " + used + ", \"" + plural
				+ "\", \"" + article + "\" ) ";

		try {
			db.execSQL(stmt);
		} catch (Exception e) {
			Log.e(TAG, "insertitem(): insert failed: " + stmt + " " + e);
		}
	}

	public void updateItemUsed(int id) {

		if (db == null) {
			db = getWritableDatabase();
		}

		String stmt = "";
		try {

			stmt = "Update " + T_ITEMS + " set used =  used + 1 "
					+ " where id = " + id;
			db.execSQL(stmt);
		} catch (Exception e) {
			Log.e(TAG, "updateItemUsed(): change failed: " + stmt + " " + e);
		}
	}

	public void insertItemInventory(int id, String category) {

		if (db == null) {
			db = getWritableDatabase();
		}

		String stmt = "";
		try {
			stmt = "INSERT INTO " + T_ITEM_INVENTORY
					+ " (id, noofitems, category) values  ( " + id + " , " + 1
					+ ", " + "\"" + category + "\"" + " ) ";

			db.execSQL(stmt);
		} catch (Exception e) {
			updateItemByOneInventory(id);
		}
	}

	public void updateItemByOneInventory(int id) {

		if (db == null) {
			db = getWritableDatabase();
		}

		String stmt = "";
		stmt = "Update " + T_ITEM_INVENTORY
				+ " set noofitems = noofitems  + 1 " + " where id = " + id;
		try {
			db.execSQL(stmt);
		} catch (Exception e) {
			Log.e(TAG, "***updateitembyOneinventory(): change failed: " + stmt
					+ " " + e);
		}
	}

	public void updateItemInventory(int id, int newInv) {

		if (db == null) {
			db = getWritableDatabase();
		}

		String stmt = "";
		stmt = "Update " + T_ITEM_INVENTORY + " set noofitems =  " + newInv
				+ " where id = " + id;

		try {
			db.execSQL(stmt);
		} catch (Exception e) {
			Log.e(TAG, "***updateiteminventory(): change failed: " + stmt + " "
					+ e);
		}
	}

	public ArrayList<ItemInventoryRecord> getInventory() {

		db = getWritableDatabase();

		String stmt = "SELECT id, noofitems, category from " + T_ITEM_INVENTORY
				+ "  order by category";

		Cursor c = null;
		try {
			c = db.rawQuery(stmt, null);
		} catch (Exception e) {
			Log.e(TAG, "getInventory(): get failed: " + stmt + " " + e);
		}

		if (c == null)
			return null;

		if (c.getCount() == 0) {
			c.close();
			return null;
		}

		ArrayList<ItemInventoryRecord> ans = new ArrayList<ItemInventoryRecord>();

		while (c.moveToNext()) {
			ItemInventoryRecord pr = new ItemInventoryRecord(c.getInt(0),
					c.getInt(1), c.getString(2));
			ans.add(pr);
		}

		c.close();
		return ans;
	}

	public ArrayList<ItemInventoryRecord> getInventoryByCategory(String category) {

		db = getWritableDatabase();

		String stmt = "SELECT id, noofitems, category  " + " from "
				+ T_ITEM_INVENTORY + " where category = " + "\"" + category
				+ "\"";

		Cursor c = null;
		try {
			c = db.rawQuery(stmt, null);
		} catch (Exception e) {
			Log.e(TAG, "getInventoryByCategory(): get failed: " + stmt + " "
					+ e);
		}

		if (c == null)
			return null;

		if (c.getCount() == 0) {
			c.close();
			return null;
		}

		ArrayList<ItemInventoryRecord> ans = new ArrayList<ItemInventoryRecord>();

		while (c.moveToNext()) {
			ItemInventoryRecord pr = new ItemInventoryRecord(c.getInt(0),
					c.getInt(1), c.getString(2));
			ans.add(pr);
		}

		c.close();
		return ans;
	}

	/**
	 * initial data load into the tables from the flat file in the assets
	 * directory.
	 */

	public void createItems() {
		Boolean done;
		AssetManager am = ctx.getAssets();
		BufferedReader br = null;
		done = false;
		try {
			InputStream is = null;
			is = am.open("fonts/" + "unicodes.txt");
			br = new BufferedReader(new InputStreamReader(is));
		} catch (Exception e) {
			done = true;
			Log.d("TAG", "asset manager error in opening data" + e);
			return;
		}
		String stmt;

		while (!done) {
			try {
				stmt = br.readLine();
			} catch (Exception e) {
				Log.d(TAG, "error read buffered input asset file " + e);
				done = true;
				try {
					br.close();
					return;
				} catch (Exception e1) {
					return;

				}
			}
			if (stmt != null) {
				parseItem(stmt);
			} else { // something went wrong
				done = true;
			}

		}

	}

	public void parseItem(String stmt) {
		int i = 0;
		int start = 2;
		int number = 0;
		int id = 0;
		String name = "";
		String category = "";
		String unicode1 = "";
		String unicode2 = "";
		String unicode3 = "";
		String extractField;
		String type = "";
		String verb = "";
		String plural = "";
		String article = "";

		// skip for char already parsed
		type = stmt.substring(0, 1);

		for (int which = 0; which < 9; which++) {
			i = stmt.indexOf(":", start);

			extractField = stmt.substring(start, i);
			i++;
			start = i;
			switch (which) {
			case 0:
				try {
					number = Integer.parseInt(extractField);
				} catch (Exception e) {
					number = 0;
				}
				id = number;
				break;
			case 1:
				name = extractField;
				break;
			case 2:
				category = extractField;
				break;
			case 3:
				verb = extractField;
				break;
			case 4:
				unicode1 = extractField;
				break;
			case 5:
				unicode2 = extractField;
				break;
			case 6:
				unicode3 = extractField;
				break;
			case 7:
				plural = extractField;
				break;
			case 8:
				article = extractField;
				break;

			default:
				Log.d(TAG, "error parsing items ");
				break;
			}
		}

		insertItem(type, id, name, category, verb, unicode1, unicode2,
				unicode3, plural, article);

	}

	public void createFakeInventory() {
		ItemRecord ir = null;
		int maxCat = 11;
		int theOne = 99;
		for (int i = 1; i < 92; i++) {
			ir = getItem(i);

			if (ir == null)
				continue;

			insertItemInventory(ir.getId(), ir.getCategory());
			theOne = 99;
			for (int j = 0; j < maxCat; j++) {
				if (ir.getCategory().trim().contains(DispStatus.catItems[j])) {
					theOne = j;
				}

			}
			// fix all items at inventory 10
			if (theOne != 99)
				updateItemInventory(ir.getId(), 10);
			else
				updateItemInventory(ir.getId(), 10);
		}
	}

	public void dumpInventory() {
		ArrayList<ItemInventoryRecord> inventoryList = getInventory();
		if (inventoryList == null) {
			Log.d(TAG, "dumpInventory(): got here, inventoryList = null");
			return;
		}
		for (ItemInventoryRecord il : inventoryList) {
			ItemRecord i = getItem(il.getId());

			Log.d(TAG, "inventory name = " + i.getName() + " cat " + i.category
					+ " id " + i.id);
		}

	}

	public void dumpItems() {
		ArrayList<String> categories = getCategories();
		for (String cat : categories) {
			ArrayList<ItemRecord> items = this.getItemList(cat);
			if (!items.isEmpty()) {
				for (ItemRecord rec : items) {
					Log.d(TAG, "inventory name = " + rec.getName() + " cat "
							+ rec.category + " id " + rec.id);
				}
			} else {
				Log.d(TAG, "dumpItems(): got here, category " + cat
						+ " no items");
			}
		}
	}
}
