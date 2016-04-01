package com.cottagecoders.dottie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.cottagecoders.dottie.util.IabHelper;
import com.cottagecoders.dottie.util.IabResult;
import com.cottagecoders.dottie.util.Inventory;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class StartDottie extends Activity {
	public static boolean networkError = false;
	private static final String TAG = "StartDottie";
	private static AdView theAd = null;
	private DottiePrefs dottiePrefs;
	Context ctx;
	// public key
	static String b64EncodedKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmP15WRIo+MicRVs6D+Nux5SMCyAuxxhd5MBT69zmdfno0evv4dl38Y+seHhpV9w4ytSUSASTvqHDvHFhTmoqViz6iw9g6EGnXnVc9OATU9g1IBmvFQS+nylMIpxgJ/RY8d+T6abjbF6+YQTd5d1aizaDUklXkoHr9LKE9ax5dZx8cns3t0Ht4OHKM3fCzCiFkEfBBUhZLZDLty/pgKKHgB8+SOa9hL2gefPjih1HY+mPGFfYnvl4ic/SMOSNcXgfawfNpKl56NNtAvA5DTD4z1W766PZ5gV1B4jrxiKPli1H9z4OnGlf86iFei07u7co2Eu8kpAEfxWqOgJ4GiiHnwIDAQAB";

	private IabHelper iabHelper;
	static final String SKU_UNLOCK = "remove.ads.from.dottie";

	// (arbitrary) request code for the purchase flow
	static final int RC_REQUEST = 10001;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate(): got here.");
		
		ctx = getApplicationContext();

		dottiePrefs = DottiePrefs.getInstance(ctx);

		/* set up all the stuff we need for In-App Billing. */
		iabHelper = new IabHelper(ctx, b64EncodedKey);
		// iabHelper.enableDebugLogging(true);
		iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				if (result.isSuccess()) {
					iabHelper.queryInventoryAsync(queryFinishedListener);
				}
			}
		});

		startDottieLoadXML();
	}

	@Override
	public void onResume() {

		Log.d(TAG, "onResume(): GOT HERE.");
		super.onResume();

		if (dottiePrefs.isUnlocked()) {
			if (theAd != null)
				theAd.resume();
		}
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause(): GOT HERE.");
		super.onPause();

		if (theAd != null)
			theAd.pause();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy(): GOT HERE.");

		if (iabHelper != null)
			iabHelper.dispose();
		iabHelper = null;

		destroyAdComponent();
		super.onDestroy();
	}

	public void setupAd() {
		if (theAd == null)
			return;

		String addThis = "XBox, playStation, PS3, PS4,  Nintendo, Kandy Krush, Games, Toys, Puzzles, Blocks, Blok, Block, Lego, Duplo, Play, Play Store, Sega, Space Invaders, ";
		addThis += " Centipede, Pong, Donkey Kong, Call of Duty, Berzerk, Pinball, SilverBall Mania, PokeMon, Animal Crossing, Farmville, MineCraft, Trivia Crack, World of Warcraft, Warcraft, WoW, ";

		// Create the ad request
		AdRequest request = new AdRequest.Builder()
				.addTestDevice("A6013A53A1FE2C6963442E61764D1945")
				.addTestDevice("B3B37744EA7552599DE790E2E5FA8757")
				.addTestDevice("7D4826863B8F7F80C3715162F9AF225F")
				.addKeyword(addThis).build();

		theAd.setBackgroundColor(getResources().getColor(R.color.MediumGrey));
		theAd.loadAd(request);
	}

	IabHelper.QueryInventoryFinishedListener queryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result,
				Inventory inventory) {
			Log.d(TAG, "queryFinishedListener(): GOT HERE.");
			if (result.isFailure()) {
				Log.d(TAG,
						"Query Inventory error " + result + " "
								+ result.getMessage());
				networkError = true;
				return;
			}

			// Did we previously purchase the premium upgrade?
			boolean unlocked = inventory.hasPurchase(SKU_UNLOCK);
			Log.d(TAG, "queryFinishedListener(): hasPurchased: " + SKU_UNLOCK
					+ " unlocked " + unlocked);
			dottiePrefs.setUnlocked(unlocked);
			startDottieLoadXML();
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (iabHelper == null)
			return;

		// Pass on the activity result to the helper for handling
		if (!iabHelper.handleActivityResult(requestCode, resultCode, data)) {
			/**
			 * not handled, so handle it ourselves (here's where you'd perform
			 * any handling of activity results not related to in-app billing...
			 */
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void destroyAdComponent() {
		if (theAd != null) {
			theAd.pause();
			theAd.destroy();
			theAd = null;
		}
	}

	private void startDottieLoadXML() {
		destroyAdComponent();

		Log.d(TAG, "startDottieLoadXML(): dottiePrefs.isUnlocked() "
				+ dottiePrefs.isUnlocked());

		if (dottiePrefs.isUnlocked()) {
			setContentView(R.layout.splash);
		} else {
			setContentView(R.layout.splash_with_ads);
		}

		TextView instructions = (TextView) findViewById(R.id.instructions);

		final String p1 = "<head><body><p>Dottie is a fast and fun arcade-style game, in which you move your character (Dottie) around the board and hit targets by tipping the phone.  Start with your phone held flat and facing up (toward the ceiling), Dottie can roll in any direction by tipping the phone.  ";
		final String p2 = "At first, Dottie will move slowly, but as you play the game, Dottie\'s speed will increase.</p>";
		final String p3 = "<p>Rainbow-colored borders are Timeout Borders, when you hit the border, Dottie will stick there for several seconds.  </p>";
		final String p4 = "<p>Red and Pink borders are Wrap-Around Borders, which permit you to roll Dottie off the screen and come back in on the opposite side of the screen.</p>";
		final String p5 = "<p>Dark Blue and Light Blue Borders are Bouncey Borders, when you hit a Bouncey Border you may bounce off or you may stick to the border, tip your phone if you need to get unstuck. </p>";
		final String p6 = "<p>Black and Yellow borders are Crash Borders, when you hit a Crash Border your Dottie dies, and the points that remain on the screen are subtracted from your score.</p>";
		final String p7 = "<p>Please send your feedback and suggestions to <a href=mailto:cottagecoders@gmail.com>cottagecoders@gmail.com</a> Thanks!</p></body></html>";

		final String ins = p1 + p2 + p3 + p4 + p5 + p6 + p7;

		// convert HTML to whatever works in a TextView...
		instructions.setText(Html.fromHtml(ins));

		// make TextVIew links clickable...
		instructions.setMovementMethod(LinkMovementMethod.getInstance());

		// set font size...
		instructions.setTextSize((float) 18.0);

		// listener for the button...
		findViewById(R.id.button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(), PlayDottie.class);
				startActivity(intent);
			}
		});
		theAd = (AdView) findViewById(R.id.theAd);
		setupAd();
	}
}
