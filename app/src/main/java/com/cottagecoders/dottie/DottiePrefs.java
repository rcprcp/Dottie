package com.cottagecoders.dottie;

import android.content.Context;
import android.content.SharedPreferences;

public final class DottiePrefs {
	public static final String SHARED_PREFERENCES_NAME = "DottiePrefs";
	//private static final String TAG = "DottiePrefs";
	private static DottiePrefs INSTANCE = null;
	private boolean sound;
	private boolean vibrate;
	private boolean unlocked;
	private int level;
	private int totalHits;
	private float acceleration;
	private int score;
	private long lastPlayTime;
	
	private int happy;
	private int health ;
	private int smart;
	private int prevPet;
	private int prevLoot;

	private SharedPreferences sp;
	private SharedPreferences.Editor spe;

	private static final float START = (float) 1.0;
	private static final float END = (float) 8.0;
	private static final float STEP = (float) 1.0;

	private DottiePrefs(Context ctx) {
		sp = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME,
				Context.MODE_PRIVATE);
		spe = sp.edit();

		// initialize as so:
		this.sound = sp.getBoolean("sound", false);
		this.vibrate = sp.getBoolean("vibrate", false);
		this.unlocked = sp.getBoolean("unlocked", false);
		this.level = sp.getInt("level", 0);
		this.acceleration = sp.getFloat("acceleration", START);
		this.score = sp.getInt("score", 0);
		this.totalHits = sp.getInt("totalHits", 0);
		this.lastPlayTime = sp.getLong("lastPlayTime",
				System.currentTimeMillis() / 1000);
		this.health = sp.getInt("healthMeter", 0);
		this.happy = sp.getInt("happyMeter", 0);
		this.smart = sp.getInt("smartMeter", 0);
		this.prevPet = sp.getInt("prevPet", 0);
		this.prevLoot = sp.getInt("prevLoot", 0);
		
	}

	public static DottiePrefs getInstance(Context ctx) {
		if (INSTANCE == null)
			INSTANCE = new DottiePrefs(ctx);
		return INSTANCE;
	}

	public void reset() {
		this.sound = true;
		spe.putBoolean("sound", this.sound);

		this.vibrate = true;
		spe.putBoolean("vibrate", this.vibrate);

		this.acceleration = START;
		spe.putFloat("acceleration", this.acceleration);

		this.score = 0;
		spe.putInt("score", this.score);

		this.level = 0;
		spe.putInt("level", this.level);

		this.totalHits = 0;
		spe.putInt("totalHits", this.totalHits);

		this.lastPlayTime = System.currentTimeMillis() / 1000;
		spe.putLong("lastPlayTime", this.lastPlayTime);
		
		this.health = 0;
		spe.putInt("healthMeter", this.health);
		
		this.happy = 0;
		spe.putInt("happyMeter", this.happy);
		
		this.smart = 0;
		spe.putInt("smartMeter", this.smart);
		
		this.prevPet = 0;
		spe.putInt("prevPet", this.prevPet);
		
		this.prevLoot = 0;
		spe.putInt("prevLoot", this.prevLoot);

		spe.commit();

	}

	public boolean isSound() {
		return sound;
	}

	public void setSound(boolean sound) {
		this.sound = sound;
		spe.putBoolean("sound", sound);
		spe.apply();
	}

	public int getTotalHits() {
		return totalHits;
	}

	public void setTotalHits(int totalHits) {
		this.totalHits = totalHits;
		spe.putInt("totalHits", totalHits);
		spe.apply();
	}

	public int getLevel() {
		return level;
	}
	
	public void setHealthMeter(int health) {
		spe.putInt("healthMeter", health);
		spe.apply();
	}
	public int getHealthMeter(){
		return this.health;
	}
	public void setHappyMeter(int happy) {
		this.happy = happy;
		spe.putInt("happyMeter", happy);
			spe.apply();
	}
	public int getHappyMeter(){
		return this.happy;
	}
	public void setSmartMeter(int smart) {
		this.smart = smart;
		spe.putInt("smartMeter", smart);
		spe.apply();
		
	}
	public int getSmartMeter(){
		return this.smart;
	}
	public void setPrevPet(int prevPet) {
		this.prevPet = prevPet;
		spe.putInt("prevPet", prevPet);
		spe.apply();
		
	}
	public int getPrevPet(){
		return this.prevPet;
	}
	public void setPrevLoot(int prevLoot) {
		this.prevLoot = prevLoot;
		spe.putInt("prevLoot", prevLoot);
		spe.apply();		
	}
	public int getPrevLoot(){
		return this.prevLoot;
	}
	
	public void setLevel(int level) {
		this.level = level;
		spe.putInt("level", level);
		spe.apply();
	}

	public boolean isVibrate() {
		return vibrate;
	}

	public void setVibrate(boolean vibrate) {
		this.vibrate = vibrate;
		spe.putBoolean("vibrate", vibrate);
		spe.apply();
	}

	public boolean isUnlocked() {
		return unlocked;
	}

	public void setUnlocked(boolean unlocked) {
		this.unlocked = unlocked;
		spe.putBoolean("unlocked", unlocked);
		spe.apply();
	}

	public float getAcceleration() {
		if (acceleration < START) {
			acceleration = START;
			setAcceleration(acceleration);
		}
		return acceleration;
	}

	public void setAcceleration(float acceleration) {
		this.acceleration = acceleration;
		spe.putFloat("acceleration", acceleration);
		spe.apply();
	}

	public int getScore() {
		return score;
	}

	public long getLastPlayTime() {
		return lastPlayTime;
	}

	public void setLastPlayTime(long lastPlayTime) {
		this.lastPlayTime = lastPlayTime;
		spe.putLong("lastPlayTime", lastPlayTime);
		spe.apply();
	}

	public void setScore(int score) {
		this.score = score;
		spe.putInt("score", score);
		spe.apply();
	}

	public void incrementAcceleration() {
		float x = acceleration + STEP;
		if (x <= END) {
			setAcceleration(x);
		}
	}
}