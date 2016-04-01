package com.cottagecoders.dottie;

import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class DispStatus extends Activity {

	int myTextColor = 0;

	String TAG = "DottieStatus";
	Context ctx;
	String newday;
	String theWholeString = "";
	Boolean theAnd = false;
	int maxW = 150;

	int id;
	TableLayout t;
	public DottieDB mdb;
	Boolean firstTime = true;
	int happyMeter = 0;
	int healthMeter = 0;
	int smartMeter = 0;
	int maxHappyMeter = 0;
	int maxHealthMeter = 0;
	int maxSmartMeter = 0;
	int prevHappy = 0;
	int prevHealth = 0;
	int prevSmart = 0;
	int prevPet = 0;
	int prevLoot = 0;
	int catMeter[] = { 0, 0, 1, 0, 1, 4, 4, 4, 4, 2, 4 };
	int myNarBack;
	int myDefBack;
	int myInvBack;
	int myMeterBack;

	DottiePrefs prefs;

	final static int NUM_CAT = 11;
	final static int catMin[] = { 2, 3, 1, 1, 1, 1, 9, 8, 8, 2, 9 };
	final static String catItems[] = { "Fruit", "Food", "Leisure", "Exercise",
			"Sport", "Dessert", "Zoo", "Pet", "Loot", "Smart", "Party" };

	int catHow[] = new int[NUM_CAT];
	int days;
	int smileCounter;
	LinearLayout lin;
	Typeface type;

	//
	// pass to the program the number of days since last play
	// 0 for the very first time
	// *************************************************************************/
	// ***** to do add consumption code catmin is the number of items to deplete
	// with days tells us how many days should be subtracted */
	//
	//

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = getApplicationContext();

		smileCounter = 0;
		days = getIntent().getIntExtra("dottiedays", 0);
		if (days > 10) {
			days = 10;
		} else if (days < 1) {
			days = 1;
		}

		mdb = new DottieDB(ctx);

		if (mdb.getCount() == 0) {
			mdb.createItems();
		}

		// figure out the screen size...
		DisplayMetrics displaymetrics = new DisplayMetrics();
		((Activity) ctx).getWindowManager().getDefaultDisplay()
				.getMetrics(displaymetrics);
		// int height = displaymetrics.heightPixels;
		maxW = displaymetrics.widthPixels;

		// maxW = ((int) (getWindowManager().getDefaultDisplay().getWidth()));

		setContentView(R.layout.thetable);
		lin = (LinearLayout) findViewById(R.id.lin);
		lin.setBackgroundColor(getResources().getColor(R.color.White));

		myTextColor = getResources().getColor(R.color.Black);
		myNarBack = getResources().getColor(R.color.LightBlue);
		myDefBack = getResources().getColor(R.color.White);
		myInvBack = getResources().getColor(R.color.DarkGrey);
		myMeterBack = getResources().getColor(R.color.Mint);

		type = Typeface.createFromAsset(getAssets(), "fonts/Symbola.ttf");
	}

	@Override
	protected void onResume() {
		super.onResume();
		doSetUp();
	}

	public void doSetUp() {
		prefs = DottiePrefs.getInstance(ctx);

		healthMeter = prefs.getHealthMeter();
		happyMeter = prefs.getHappyMeter();
		smartMeter = prefs.getSmartMeter();
		prevHealth = 0;
		prevHappy = 0;
		prevSmart = 0;
		// OLD_getSharedPrefs();

		ItemRecord item;
		t = (TableLayout) findViewById(R.id.theTable);
		t.removeAllViews();

		doCreateConsumption();

		doConsumption();

		if (theWholeString.trim().equals("")) {
			//Log.d(TAG, " the whole string is spaces");
		} else {
			dispIt(theWholeString, myNarBack);
			dispIt(" ", myMeterBack);
		}

		// add consumption code here!!!!!
		// consumption record how many days how to store it in the database
		// healthMeter = 0;
		// happyMeter = 0;
		// smartMeter = 0;
		maxHealthMeter = 0;
		maxHappyMeter = 0;
		maxSmartMeter = 0;
		for (int j = 0; j < catItems.length; j++) {
			// the check for 9 is special -- so is 8.
			// consumption is optional so it will
			// not be used as an unmet requirement.
			//
			if (catMin[j] >= catHow[j] && catMin[j] != 9 && catMin[j] != 8) {
				int amt = catMin[j] - catHow[j];
				if (amt > 0)
					dispIt("Dottie needs " + Integer.toString(amt) + " more "
							+ catItems[j], myMeterBack);
			}

			if (catMeter[j] == 0) {
				healthMeter = healthMeter + catHow[j];
				maxHealthMeter += (catMin[j]);
			}
			if (catMeter[j] == 1) {
				happyMeter = happyMeter + catHow[j];
				maxHappyMeter += (catMin[j]);
			}

			if (catMeter[j] == 2) {
				smartMeter = smartMeter + catHow[j];
				maxSmartMeter += (catMin[j]);
			}

		}

		int ind = 0;
		if (days > 0) {
			ind = ((healthMeter / (maxHealthMeter * days)) * 100);
		} else {
			ind = healthMeter / maxHealthMeter * 100;
		}

		ind = ind / 10;
		String Title = "";
		if (ind < 6)
			Title = "Dottie's health is poor";
		if (ind > 6 && ind < 9)
			Title = "Dottie's Health needs improvement";
		if (ind > 9)
			Title = "Dottie's Health is good";
		if (ind == 10)
			Title = "Dottie's Health is great";

		dispIt(" ", myMeterBack);
		makeString("D83D", "DE24", ind, Title);

		dispIt("(HealthMeter  " + ind * 10 + " %)", myMeterBack);

		if (days > 0) {
			ind = ((happyMeter / (maxHappyMeter * days)) * 100);
		} else {
			ind = happyMeter / maxHappyMeter * 100;

		}
		ind = ind / 10;
		Title = "";
		if (ind < 6)
			Title = "Dottie is not happy";
		if (ind > 6 && ind < 9)
			Title = "Dottie feels sad";
		if (ind > 9)
			Title = "Dottie is happy";
		if (ind == 10)
			Title = "Dottie is very happy";

		dispIt(" ", myMeterBack);
		makeString("D83D", "DE1E", ind, Title);
		dispIt("(Happy Meter " + ind * 10 + " %) ", myMeterBack);

		ind = 0;
		if (days > 0) {
			ind = ((smartMeter / (maxSmartMeter * days)) * 100);
		} else {
			ind = smartMeter / maxSmartMeter * 100;
		}
		ind = ind / 10;
		Title = "";
		if (ind < 6)
			Title = "Dottie needs education";
		if (ind > 6 && ind < 9)
			Title = "Dottie needs more education";
		if (ind > 9)
			Title = "Dottie feels smart";
		if (ind == 10)
			Title = "Dottie feels very smart";

		dispIt(" ", myMeterBack);
		makeString("D83D", "DE33", ind, Title);
		dispIt("(Smart Meter  " + ind * 10 + " %) ", myMeterBack);
		dispIt(" ", myDefBack);

		for (int theCat = 0; theCat < catItems.length; theCat++) {
			catHow[theCat] = 0;
		}
		dispIt("Dottie still has: ", myDefBack);
		for (int theCat = 0; theCat < catItems.length; theCat++) {
			ArrayList<ItemInventoryRecord> IL;

			IL = mdb.getInventoryByCategory(catItems[theCat]);
			if (IL == null)
				catHow[theCat] = 0;
			else

				for (ItemInventoryRecord IR : IL) {
					catHow[theCat]++;
					if (IR.getNoofitems() == 0)
						continue;

					item = mdb.getItem(IR.id);
					if (item == null)
						continue;

					if (item.getUnicode1().trim().length() < 5)
						drawItem(IR.getNoofitems(), "\\u" + item.getUnicode1(),
								item.getName(), myDefBack);
					// dispIt();
					else {
						String concat = "\\u" + item.getUnicode2() + "\\u"
								+ (item.getUnicode3()) + " ";

						drawItem(IR.noofitems, concat, item.getName(),
								myDefBack);
					}
				}

		}

		Button finished = new Button(ctx);
		finished.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		// TODO: debugging.
		// mdb.dumpInventory();
	}

	public void makeString(String code1, String code2, int no, String title) {

		int xno = 10 - no;
		String happyUni = "\\u" + "D83D" + "\\u" + "DE04";
		String newUni = "\\u" + code1 + "\\u" + code2;
		String completeUni = "";
		TextView theUni = new TextView(ctx);
		theUni.setTextColor(myTextColor);
		theUni.setTextSize(24);
		theUni.setMaxWidth(maxW);

		TableRow theRow = new TableRow(ctx);
		for (int times = 0; times < no; times++) {
			completeUni = completeUni + (happyUni);
		}

		if (xno > 0)
			for (int times = 0; times < xno; times++) {
				completeUni = completeUni + (newUni);
			}

		completeUni = completeUni + " " + title;

		theUni.setTypeface(type);
		theUni.setText(StringEscapeUtils.unescapeJava(completeUni));

		// theUni.setText(completeUni);
		theRow.addView(theUni);
		t.addView(theRow);

	}

	public void dispIt(String theString, int myBackGroundColor) {
		// if (theString.trim().equals(""))
		// return;

		TextView theText = new TextView(ctx);
		theText.setTypeface(type);

		theText.setTextColor(myTextColor);
		theText.setBackgroundColor(myBackGroundColor);
		TableRow theRow = new TableRow(ctx);

		theText.setTextSize(22);
		theText.setText(StringEscapeUtils.unescapeJava(theString.trim()));
		// theText.setText(theString);
		theText.setMaxLines(20);

		theText.setMaxWidth(maxW);

		theText.setVerticalScrollBarEnabled(true);
		theRow.addView(theText);
		t.addView(theRow);
	}

	public void drawItem(int no, String uni, String title, int myBackGround) {

		String newUni = uni;
		String completeUni = "";
		TextView theUni = new TextView(ctx);
		theUni.setTextColor(myTextColor);
		theUni.setTextSize(20);
		theUni.setMaxWidth(maxW);
		theUni.setBackgroundColor(myBackGround);
		TableRow theRow = new TableRow(ctx);
		for (int times = 0; times < no; times++) {
			completeUni = completeUni + (newUni);
		}

		completeUni = completeUni + " " + title;
		theUni.setTypeface(type);
		theUni.setText(StringEscapeUtils.unescapeJava(completeUni));

		// theUni.setText(completeUni);
		theRow.addView(theUni);
		t.addView(theRow);

	}

	public void drawSmiley(int no, TableRow dottie) {

		String completeString = "";
		TextView dottieText = new TextView(ctx);
		dottieText.setTypeface(type);
		dottieText.setTextColor(myTextColor);
		dottieText.setTextSize(20);
		dottieText.setMaxWidth(maxW);

		// happy face
		String theString = "\uD83D\uDE03";

		// sad face
		if (no < 0) {
			theString = "\uD83D\uDE1E";
		}
		if (no > 0) {
			for (int i = 0; i < no; i++)
				completeString = completeString.trim() + (theString);
		} else {
			for (int i = no; i == 0; i++)
				completeString = completeString.trim() + (theString);

		}

		dottieText.setText(StringEscapeUtils.unescapeJava(completeString));
		dottie.addView(dottieText);
		t.addView(dottie);

	}

	public void doConsumption() {
		// this indicates this is a fresh string and will be created and the
		// first string should
		// not include and and
		theAnd = false;
		theWholeString = "";
		// end of initializing for narrative

		int x = 0;
		int used = 0;
		Boolean done = false;
		int consToday = 0;

		ArrayList<ItemInventoryRecord> theList;
		ConsumptionRecord nowToday = mdb.getConsumption(0);
		if (nowToday != null)

			for (int i = 0; i < catItems.length; i++) {
				x = 0;
				if (nowToday != null)
					consToday = getConsumptionNumber(i, nowToday);
				else
					consToday = 0;
				theList = mdb.getInventoryByCategory(catItems[i]);

				if (theList == null) {
					continue;
				}

				used = 0;
				done = false;
				while (!done) {

					int itemCount = theList.get(x).getNoofitems();

					if (catMin[i] == 9 || catMin[i] == 8) {
						used = itemCount;
						consToday = 0;
						catHow[i] = catHow[i] + 1;
						if (catMin[i] == 9)
							mdb.updateItemInventory(theList.get(x).getId(), 0);

						itemCount = 0;

					}
					if (catMin[i] != 9 && catMin[i] != 8) {
						if (itemCount > 0) {
							if (consToday >= itemCount) {
								consToday = consToday - itemCount;
								used = used + itemCount;
								catHow[i] = catHow[i] + itemCount;
								mdb.updateItemInventory(theList.get(x).getId(),
										0);
								itemCount = 0;

							} else {
								mdb.updateItemInventory(theList.get(x).getId(),
										(itemCount - consToday));
								used = used + consToday;
								catHow[i] = catHow[i] + consToday;
								itemCount = itemCount - consToday;
								consToday = 0;
							}
						}
					}

					// ****** debug only
					// ItemRecord tempRec = mdb.getItem(theList.get(x).getId());
					// end debug
					if (catMin[i] != 9)
						mdb.updateConsumptionItem(0, i, consToday);
					else
						mdb.updateConsumptionItem(0, i, 0);

					if (used > 0)
						createStringNarrative(used, theList.get(x).getId());
					used = 0;
					if (itemCount == 0) {
						x = x + 1;
						if (x > theList.size() - 1) {
							done = true;
						}
					}

					if (consToday == 0)
						done = true;
				}
			}

	}

	public void createStringNarrative(int used, int id)

	{
		String theUnicode;
		ItemRecord IR;
		if (theWholeString.length() == 0) {
			theWholeString = ("Dottie ");
			theAnd = false;
		}

		IR = mdb.getItem(id);

		if (IR != null) {
			if (IR.unicode1.trim().length() > 4)
				theUnicode = "\\u" + IR.getUnicode2() + "\\u"
						+ IR.getUnicode3();
			else
				theUnicode = "\\u" + IR.getUnicode1();

			if (theAnd)
				theWholeString = theWholeString + (" and also ");

			if (used == 1) {
				theWholeString = theWholeString + " " + IR.getVerb() + " "
						+ IR.getArticle() + " " + IR.getName().trim()
						+ theUnicode + " ";
			} else {
				theWholeString = theWholeString + " " + IR.getVerb() + " "
						+ Integer.toString(used) + " " + IR.getPlural().trim()
						+ " " + theUnicode + " ";
			}
		}
		theAnd = true;
	}

	public int getConsumptionNumber(int it, ConsumptionRecord the) {
		int cons = 0;
		switch (it) {
		case 0:
			cons = the.item1;
			break;
		case 1:
			cons = the.item2;
			break;
		case 2:
			cons = the.item3;

			break;
		case 3:
			cons = the.item4;

			break;
		case 4:
			cons = the.item5;

			break;
		case 5:
			cons = the.item6;

			break;
		case 6:
			cons = the.item7;

			break;
		case 7:
			cons = the.item8;

			break;
		case 8:
			cons = the.item9;

			break;
		case 9:
			cons = the.item10;

			break;
		case 10:
			cons = the.item11;

			break;
		case 11:
			cons = the.item12;

			break;
		default:
			break;
		}
		return (cons);
	}

	public void doCreateConsumption() {
		int j;
		int i;

		// catMin[6] and catMin[10] are special case with 0 required consumption
		// but should be all used up anyway hence to 9 in the minimum
		// consumption

		for (j = 0; j < catItems.length; j++)
			catHow[j] = 0;
		for (i = 0; i < days; i++) {
			mdb.insertIntoConsumption(0, catMin[0], catMin[1], catMin[2],
					catMin[3], catMin[4], catMin[5], 1, 1, 1, catMin[9], 1, 1);
		}
	}

	public void OLD_initSharedPrefs() {
		SharedPreferences shared_preferences;
		SharedPreferences.Editor shared_preferences_editor;

		shared_preferences = getSharedPreferences(
				DottiePrefs.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		shared_preferences_editor = shared_preferences.edit();
		shared_preferences_editor.putInt("healthMeter", 0);
		shared_preferences_editor.putInt("happyMeter", 0);
		shared_preferences_editor.putInt("smartMeter", 0);
		shared_preferences_editor.putInt("prevPet", 0);
		shared_preferences_editor.putInt("prevLoot", 0);
		shared_preferences_editor.commit();

	}

	public void OLD_getSharedPrefs() {

		prevHealth = prefs.getHealthMeter();
		prevHappy = prefs.getHappyMeter();
		prevSmart = prefs.getSmartMeter();
		prevPet = prefs.getPrevPet();
		prevLoot = prefs.getPrevLoot();

		if (days > 0) {
			prevHealth = prevHealth - (5 * days);
			prevSmart = prevSmart - (2 * days);
			prevHappy = prevHappy - (2 * days);
		}
	}

	public Boolean OLD_updateSharedPrefs() {
		if (healthMeter > 100)
			healthMeter = 100;
		if (happyMeter > 100)
			happyMeter = 100;
		if (smartMeter > 100)
			smartMeter = 100;

		prefs.setHappyMeter(happyMeter);
		prefs.setHealthMeter(healthMeter);
		prefs.setSmartMeter(smartMeter);

		return (true);
	}

	public void OLD_updateSharedPrefsPet(int no) {
		prevPet = prevPet + no;
		prefs.setPrevPet(prevPet);
	}

	public void OLD_updateSharedPrefsloot(int no) {
		prevLoot = no;
		prefs.setPrevLoot(no);
	}
}
