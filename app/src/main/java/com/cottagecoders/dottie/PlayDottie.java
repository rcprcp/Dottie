package com.cottagecoders.dottie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.cottagecoders.dottie.util.IabHelper;
import com.cottagecoders.dottie.util.IabResult;
import com.cottagecoders.dottie.util.Purchase;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class PlayDottie extends Activity {
	private final static String TAG = "PlayDottie";
	private static SensorManager myManager = null;
	private static AdView theAd = null;
	public static TextView scoreMessage;
	public static TextView statusMessage;
	private static View theView;
	private boolean purchaseInProgress;
	private boolean isIabEnabled = false;
	static Context ctx;
	private static Activity act;

	private IabHelper iabHelper;
	private DottiePrefs dottiePrefs;

	private Vibrator vib;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		ctx = getApplicationContext();
		act = this;

		dottiePrefs = DottiePrefs.getInstance(ctx);

		// go into full screen mode - remove the status bar and the action
		// bar...
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		selectLayout();

		vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
		if (vib == null)
			Log.d(TAG, "vib is null");

		/* set up all the stuff we need for In-App Billing. */
		iabHelper = new IabHelper(ctx, StartDottie.b64EncodedKey);
		iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				if (!result.isSuccess()) {
					isIabEnabled = false;
					Log.d(TAG,
							"Problem setting up In-app Billing: "
									+ result.toString());
				} else {
					isIabEnabled = true;
				}
			}
		});

	}

	@Override
	public void onResume() {

		Log.d(TAG, "onResume(): GOT HERE.");
		super.onResume();

		if (theAd != null)
			theAd.resume();

		// start (restart) the sensors...
		if (myManager == null)
			myManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		myManager.registerListener(myAccelListener,
				myManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);

		Dottie.isGamePaused = false;
		theView.invalidate();
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause(): GOT HERE.");
		super.onPause();

		Dottie.isGamePaused = true;

		myManager.unregisterListener(myAccelListener);
		if (theAd != null)
			theAd.pause();
	}

	@Override
	public void onDestroy() {

		if (theAd != null)
			theAd.destroy();

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/**
		 * this is done only once - the first time the menu gets called.
		 */
		getMenuInflater().inflate(R.menu.settings_menu, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		/**
		 * this gets called each time the menu is displayed.
		 */

		// stop the game action
		Dottie.isGamePaused = true;

		/*
		 * initialize this, we will use this when we have continuing workflow
		 * from the upgrade process.
		 */
		purchaseInProgress = false;

		/* is in app purchasing available? */
		if (isIabEnabled) {
			/*
			 * if the game is already unlocked, replace the purchase option with
			 * the thank you message
			 */
			if (dottiePrefs.isUnlocked()) {
				menu.findItem(R.id.unlock).setEnabled(false);
				menu.findItem(R.id.unlock).setTitle(R.string.thanks);
			} else {
				menu.findItem(R.id.unlock).setEnabled(true);
				menu.findItem(R.id.unlock).setTitle(R.string.unlock);
			}

		} else {
			// not enabled...
			menu.findItem(R.id.unlock).setEnabled(false);
			menu.findItem(R.id.unlock).setTitle(R.string.googleFail);
		}

		/*
		 * set the individual check boxes here.
		 */
		if (isSilent()) {
			/* phone is muted */
			menu.findItem(R.id.sound).setEnabled(false);
			menu.findItem(R.id.sound).setVisible(false);
		} else {
			/* set up user's choice for game sound. */
			menu.findItem(R.id.sound).setEnabled(true);
			menu.findItem(R.id.sound).setVisible(true);
			menu.findItem(R.id.sound).setChecked(dottiePrefs.isSound());
		}

		if (!vib.hasVibrator()) {
			/* apparently not all devices support vibrate... */
			menu.findItem(R.id.vibrate).setEnabled(false);
			menu.findItem(R.id.vibrate).setVisible(false);
		} else {
			/* set up user's choice for vibrate. */
			menu.findItem(R.id.vibrate).setEnabled(true);
			menu.findItem(R.id.vibrate).setVisible(true);
			menu.findItem(R.id.vibrate).setChecked(dottiePrefs.isVibrate());
		}

		return true;
	}

	/**
	 * Check if the phone is in mute mode.
	 * 
	 * @return - true if phone is muted, false otherwise.
	 */
	static public boolean isSilent() {
		AudioManager am = (AudioManager) ctx
				.getSystemService(Context.AUDIO_SERVICE);

		boolean rval = false;
		switch (am.getRingerMode()) {
		case AudioManager.RINGER_MODE_SILENT:
			rval = true;
			break;
		case AudioManager.RINGER_MODE_VIBRATE:
			rval = true;
			break;
		case AudioManager.RINGER_MODE_NORMAL:
			break;
		default:
			Log.d(TAG, "isSilent(): Unknown Ringer Mode! " + am.getRingerMode());
		}
		return rval;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// this routine gets called when the player touches a menu selection...

		boolean mode;

		switch (item.getItemId()) {
		case R.id.unlock: {
			Log.d(TAG,
					"onOptionItemSelected(): purchase unlock code was selected.");

			purchaseInProgress = true;

			iabHelper.launchPurchaseFlow(act, StartDottie.SKU_UNLOCK,
					StartDottie.RC_REQUEST, purchaseFinishedListener);
			break;
		}
		case R.id.sound: {
			Log.d(TAG, "onOptionItemSelected(): sound was selected.");
			/*
			 * the checked status is not updated by the control. so if the item
			 * is not checked currently, that means it's transitioning to the
			 * checked state. we set it in the UI, but the player might not see
			 * any difference, since the menu will be destroyed after we run
			 * this function.
			 */
			mode = false;
			if (!item.isChecked()) {
				mode = true;
				// maybe you can see this happen.
				item.setChecked(mode);
			}

			dottiePrefs.setSound(mode);

			break;
		}
		case R.id.vibrate: {
			Log.d(TAG, "onOptionItemSelected(): vibrate was selected.");
			/*
			 * the checked status is not updated by the control. so if the item
			 * is not checked currently, that means it's transitioning to the
			 * checked state. we set it in the UI, but the player might not see
			 * any difference, since the menu will be destroyed after we run
			 * this function.
			 */
			mode = false;
			if (!item.isChecked()) {
				mode = true;
				// maybe you can see this happen.
				item.setChecked(mode);
			}

			dottiePrefs.setVibrate(mode);

			break;
		}

		case R.id.gameStatus: {
			Log.d(TAG, "onOptionItemSelected(): gameStatus was selected.");
			Intent intent = new Intent(ctx, DispStatus.class);
			long s = System.currentTimeMillis() / 1000;
			int days = (int) (s - dottiePrefs.getLastPlayTime()) / 86400;
			dottiePrefs.setLastPlayTime(s);
			if (days < 1)
				days = 1;
			intent.putExtra("dottiedays", days);
			Log.d(TAG, "start the DispStatus intent... (int)days = " + days);
			startActivity(intent);
			break;
		}

		case R.id.reset: {
			Log.d(TAG, "onOptionItemSelected(): reset was selected.");
			dottiePrefs.reset();
			break;
		}

		default: {
			Log.d(TAG,
					"onOptionItemSelected(): invalid item: " + item.toString());
		}
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * this routine is called when the options menu is dismissed, either by
	 * touching something on the menu or by pressing back or pressing the menu
	 * button again. for most of the menu selections it's ok to continue the
	 * game, but in the case of purchasing the upgrade there is some continuing
	 * workflow after the menu is dismissed, so we need to know if there is a
	 * "purchase in progress".
	 */
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		if (!purchaseInProgress) {
			Dottie.isGamePaused = false;
			theView.invalidate();
		}

		purchaseInProgress = false;
	}

	// Callback when a purchase is finished
	IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: "
					+ purchase + " Dottie.isGamePaused " + Dottie.isGamePaused);

			// if we were disposed of in the meantime, quit.
			if (iabHelper == null) {
				Log.d(TAG, "purchaseFinishedListener(): iabHelper is null");

			} else if (result.isFailure()) {
				Log.d(TAG, "purchaseFinishedListener(): result.isFailure() "
						+ result);

			} else if (purchase.getSku().equals(StartDottie.SKU_UNLOCK)) {
				// bought the premium upgrade!
				dottiePrefs.setUnlocked(true);
				/* this handles the change from ad to no ad or vice-versa. */
				selectLayout();
			}

			Dottie.isGamePaused = false;
			theView.invalidate();
		}
	};

	private final SensorEventListener myAccelListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {

			// need to flip the sign, since the x/y positive/negative are
			// reversed from what we expect to move left or right (up or down)
			// on the screen.
			Dottie.roll = (int) (event.values[0] * -dottiePrefs
					.getAcceleration());

			Dottie.pitch = (int) (event.values[1] * -dottiePrefs
					.getAcceleration());
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	private void setupAd() {
		Log.d(TAG, "setupAd(): got here. theAd " + theAd + " unlocked " + dottiePrefs.isUnlocked());
		/*
		 * theView will be null when we've loaded the layout that does not have
		 * an ad.
		 */
		theView = (View) findViewById(R.id.theView);

		theAd = (AdView) findViewById(R.id.theAd);
		if (theAd == null) {
			return;
		}
		
		theAd.setBackgroundColor(getResources().getColor(R.color.MediumGrey));

		if(dottiePrefs.isUnlocked() == true) {
			return;
		}

		String addThis = "XBox, playStation, PS3, PS4,  Nintendo, Kandy Krush, Games, Toys, Puzzles, ";
		addThis += "Blocks, Blok, Block, Lego, Duplo, Play, Play Store, Sega, Space Invaders, ";
		addThis += "Centipede, Pong, Donkey Kong, Call of Duty, Berzerk, Pinball, SilverBall Mania, ";
		addThis += "PokeMon, Animal Crossing, Farmville, MineCraft, World of Warcraft, Warcraft, WoW, ";

		// Create the ad request
		AdRequest request = new AdRequest.Builder()
				.addTestDevice("A6013A53A1FE2C6963442E61764D1945")
				.addTestDevice("B3B37744EA7552599DE790E2E5FA8757")
				.addTestDevice("7D4826863B8F7F80C3715162F9AF225F")
				.addKeyword(addThis).build();
		theAd.loadAd(request);
	}

	private void destroyAdComponent() {
		if (theAd != null) {
			theAd.pause();
			theAd.destroy();
			theAd = null;
		}
	}

	private void selectLayout() {
		destroyAdComponent();

		theView = null;
		if (dottiePrefs.isUnlocked()) {
			Log.d(TAG, "selectLayout(): game is Unlocked");
			setContentView(R.layout.dottie);
		} else {
			Log.d(TAG, "selectLayout(): game is Locked");
			setContentView(R.layout.dottie_with_ads);
		}

		scoreMessage = (TextView) findViewById(R.id.scoreMessage);
		statusMessage = (TextView) findViewById(R.id.statusMessage);

		setupAd();
	}
}
