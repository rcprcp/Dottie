package com.cottagecoders.dottie;


public class Level {

	// level description
	// 0 = normal play. all colors, all targets give some points.
	// 1 = only red targets give points.
	// 2 = red targets give points, other targets lose points.
	// 3 = moving targets give points, static targets - no points.
	// 4 = moving targets give points, static targets lose points.

	String[] hints = { "ANY target GIVES points",
			"RED targets GIVE points", "BLUE targets LOSE points",
			"MOVING targets GIVE points", "STILL targets LOSE points" };

	boolean hasChanged;
	DottiePrefs dottiePrefs;

	public Level() {
		dottiePrefs = DottiePrefs.getInstance(PlayDottie.ctx); 
		this.hasChanged = false;
	}

	public void setHasChanged(boolean val) {
		this.hasChanged = val;
	}

	public boolean hasChanged() {
		return this.hasChanged;
	}

	public String levelHint() {
		return hints[dottiePrefs.getLevel()];
	}

	public  int getNumLevels() {
		return hints.length;
	}

	public boolean requiresRedTargets() {
		if (dottiePrefs.getLevel() == 1 || dottiePrefs.getLevel() == 2)
			return true;
		return false;
	}

	public boolean requiresMovingTargets() {
		if (dottiePrefs.getLevel() == 3 || dottiePrefs.getLevel() == 4)
			return true;
		return false;
	}
}
