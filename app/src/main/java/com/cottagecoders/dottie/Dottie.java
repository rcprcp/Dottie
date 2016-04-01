package com.cottagecoders.dottie;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

public class Dottie extends View {

	Level level = new Level();
	static Paint textPaint = new Paint();
	Paint dummyPaint = new Paint();

	int messageCounter;

	int MAX_DISPLAY = 35; // frames.
	// alphaUnits change per frame...
	float alphaUnits = (float) 255 / (float) MAX_DISPLAY;

	Paint horizontalBorderPaint = new Paint();
	Paint verticalBorderPaint = new Paint();

	GradientAction gradientAction;

	/*
	 * TODO: These are static because they are accessed from the code that's
	 * reading the sensors. The sensor code lives in PlayDottie.java. The sensor
	 * code could/should be broken out into it's own class, i suppose.
	 */
	public static int roll;
	public static int pitch;
	public static double acceleration;

	private int previousRoll;
	private int previousPitch;

	/*
	 * there is code that updates isGamePaused in PlayDottie.java. that code is
	 * used when the player pulls up the setup menu.
	 */
	public static boolean isGamePaused;

	// Shared Preferences...
	DottiePrefs dottiePrefs;

	static final int DOTTIE_RADIUS = 20;
	static final int BORDER_WIDTH = 8;
	final int MIN_DOTTIE_RADIUS = 5;
	final int TEXT_OFFSET = 80;
	final int MAX_PIXELS_PER_CYCLE = 25;
	final int POPUP_TIMEOUT = 5;

	float HEIGHT;
	float WIDTH;

	static int dottieRadius;
	static final int MAX_TARGETS = 7;
	static int numTargets;
	static final int BEEP_DURATION = 100;

	private static final long MAXIMUM_BOARD_TIME = 30;
	static long playTimer;
	static long previousTime;
	boolean firstTime;
	int popupTimeout;
	int lostPointsMessageTimeout;
	int tempPoints;

	// target list
	static ArrayList<Target> targets = new ArrayList<Target>();

	static final String TAG = "onDraw";
	final int HITS_PER_LEVEL = 20;

	int x;
	int y;
	int yy;
	int bounceFlipX;
	int bounceFlipYY;

	ToneGenerator tone;
	Vibrator vib;
	static Random rand;

	boolean dieDottie;

	DottieDB db;

	Rect popupBorder = new Rect();
	Paint popupPaint = new Paint();
	Paint popupTextPaint = new Paint();
	Paint dottiePaint = new Paint();

	Point a = new Point();
	Point b = new Point();
	Point c = new Point();
	Path path = new Path();
	Paint dottieBowPaint = new Paint();


	// point in the center of Dottie, as a Rect.
	Rect dottieCenter;
	Rect dottieBounds;
	int messageFontSize;

	static Context ctx;

	public Dottie(Context context) {
		this(context, null);
		Log.d(TAG, "Dottie(Context context)");
		ctx = context;
		firstTime = true;
	}

	public Dottie(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		Log.d(TAG, "Dottie(Context context, AttributeSet attrs)");
		ctx = context;
		firstTime = true;

		db = new DottieDB(ctx);

		// figure out the screen size...
		DisplayMetrics displaymetrics = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay()
				.getMetrics(displaymetrics);
		// int height = displaymetrics.heightPixels;
		int width = displaymetrics.widthPixels;

		if (width <= 400) {
			messageFontSize = 16;
		} else {
			messageFontSize = 22;
		}

		/*
		 * initialize all the paint objects we use... set up the basics here...
		 */
		textPaint.setColor(getResources().getColor(R.color.Black));
		textPaint.setAntiAlias(true);
		textPaint.setDither(true);
		textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		textPaint.setTypeface(Typeface.SERIF);
		textPaint.setStrokeWidth(1.0f);
		textPaint.setTextSize(messageFontSize);
		textPaint.setTextAlign(Align.CENTER);

		popupPaint.set(textPaint);
		popupPaint.setColor(getResources().getColor(R.color.LightGrey));

		popupTextPaint.set(textPaint);
		popupTextPaint.setColor(getResources().getColor(R.color.Black));

		dottieBowPaint.set(Dottie.textPaint);
		dottieBowPaint.setColor(ctx.getResources().getColor(R.color.Fuchsia));

		verticalBorderPaint.set(textPaint);
		horizontalBorderPaint.set(textPaint);

		// load shared preferences...
		dottiePrefs = DottiePrefs.getInstance(PlayDottie.ctx);

		isGamePaused = false;

		/*
		 * to reduce garbage collection issues, create a list of target
		 * objects... this way we won't create and delete objects every time we
		 * re-draw the board, we just reuse them and re-init() them while
		 * they're on the list.
		 */
		for (int i = 0; i < MAX_TARGETS; i++) {
			targets.add(new Target());
			targets.get(i).once_only(ctx);
		}

		// create all the gradient objects we need for the entire game.
		gradientAction = new GradientAction();

		vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);

		// "alarm" stream (for beeps) -- 50% volume
		tone = new ToneGenerator(AudioManager.STREAM_DTMF, 100);

		rand = new Random();
		rand.setSeed(System.currentTimeMillis());

		dottieCenter = new Rect();
		dottieBounds = new Rect();
		lostPointsMessageTimeout = 0;

		// initialize the time and start the timer thread last

		// idiom to start a new board.
		playTimer = 0;

	}

	public Dottie(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		Log.d(TAG, "Dottie(Context context, AttributeSet attrs, int defStyle)");
		ctx = context;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		WIDTH = canvas.getWidth();
		HEIGHT = canvas.getHeight();

		if (firstTime) {
			firstTime = false;
			x = canvas.getWidth() / 2;
			yy = canvas.getHeight() / 2;
		}

		/*
		 * check for specific delays for the gradient borders - at present, the
		 * rainbow border uses timers when Dottie "sticks" to the border and to
		 * prevent instant re-sticking to the border after the timeout is over.
		 */
		if (previousTime != System.currentTimeMillis() / 1000) {
			// rolled over a second...
			previousTime = System.currentTimeMillis() / 1000;

			gradientAction.tick();

			if (lostPointsMessageTimeout > 0)
				lostPointsMessageTimeout--;

			/* this is a hierarchy of messages... */
			if (lostPointsMessageTimeout > 0) {
				displayLostPoints(tempPoints);
			} else if (gradientAction.getTimeout() > 0) {
				displayTimeoutMessage();
			} else {
				displayStatus();
			}

			if (popupTimeout > 0)
				popupTimeout--;

			displayScore();
		}

		if (popupTimeout > 0) {
			displayNextBoardMessage(canvas);
		} else {
			resetPopup();
		}

		/* check if we should start a new board... */
		if (playTimer < (System.currentTimeMillis() / 1000)) {

			/*
			 * run the garbage collector. hopefully, this will prevent the
			 * various pauses and stutters during game play.
			 */
			System.gc();

			playTimer = (System.currentTimeMillis() / 1000)
					+ MAXIMUM_BOARD_TIME;

			/*
			 * Special case code for the rainbow border... when starting a new
			 * board, we need to reset the rainbow timer, and the waitTime,
			 * which controls whether bumping into the rainbow border will lock
			 * up the Dottie. ALSO, if Dottie was stuck against the rainbow
			 * border when the board ended, we need to remove the gradient.
			 */
			gradientAction.setTimeout(0);
			gradientAction.setWaitTime(0);

			if (dottiePaint.getShader() != null)
				dottiePaint.setShader(null);

			// randomly choose a gradient - or no gradient.
			gradientAction.setGradient();

			dieDottie = false;
			dottieRadius = DOTTIE_RADIUS;

			if (dottiePrefs.getLevel() != (dottiePrefs.getTotalHits() / HITS_PER_LEVEL)
					% level.getNumLevels()) {
				level.setHasChanged(true);
				dottiePrefs
						.setLevel((dottiePrefs.getTotalHits() / HITS_PER_LEVEL)
								% level.getNumLevels());
			}

			/*
			 * check if we should make Dottie faster (more sensitive to
			 * acceleration).
			 */
			if (dottiePrefs.getTotalHits() > 0) {
				if (dottiePrefs.getTotalHits() % HITS_PER_LEVEL == 0) {
					dottiePrefs.incrementAcceleration();
				}
			}

			/*
			 * set Dottie's initial location when the new board starts. it's the
			 * center of the screen for now.
			 */
			x = canvas.getWidth() / 2;
			yy = canvas.getHeight() / 2;
			roll = 0;
			pitch = 0;

			// reset all the targets.
			for (int i = 0; i < MAX_TARGETS; i++) {
				targets.get(i).setIsActive(false);
			}

			// number of targets for this board.
			numTargets = rand.nextInt(MAX_TARGETS) + 1;
			// initialize and ensure they do not overlap one another.
			for (int i = 0; i < numTargets; i++) {

				while (true) {
					boolean collision;
					collision = false;

					/*
					 * loop here initializing the target (and reinitializing it
					 * if necessary) until the target configuration has all the
					 * desired criteria.
					 */

					/*
					 * if this is the first target we're adding to the list,
					 * make sure it's created with the specific criteria.
					 */
					if (i == 0) {
						if (level.requiresMovingTargets()) {
							targets.get(i).init(canvas, true, true);
						} else if (dottiePrefs.getLevel() == 1) {
							targets.get(i).init(canvas, true, true);
						} else {
							targets.get(i).init(canvas, true, false);
						}
					} else {
						// otherwise, just make a random target.
						targets.get(i).init(canvas, false, false);
					}

					// check if it touches any other targets.
					for (int j = 0; j < i; j++) {
						if (targets.get(i).collidesWith(targets.get(j))) {
							collision = true;
							break; // this for loop
						}
					}

					// make sure this target does not touch Dottie:
					if (targets.get(i).collidesWith(dottieBounds)) {
						collision = true;
					}

					/*
					 * check if this target touches a gradient along the side of
					 * the screen.
					 */
					if (gradientAction.touchesSide(canvas, targets.get(i)
							.getBounds())) {
						collision = true;
					}

					// is this target is OK...
					if (collision == false) {
						break; // break the infinite, inner while loop
					}
				}
			}
			if (popupTimeout == 0
					&& (level.hasChanged() || gradientAction.hasChanged())) {
				popupTimeout = POPUP_TIMEOUT;
			}
		}

		/*
		 * check if we're currently stuck on the wall -- occurs if we hit a
		 * rainbow gradient.
		 */
		if (gradientAction.getTimeout() == 0) {
			/* limit roll and pitch to stop extreme movement. */
			if (roll < -MAX_PIXELS_PER_CYCLE) {
				roll = -MAX_PIXELS_PER_CYCLE;
				bounceFlipX = 0;
				bounceFlipYY = 0;
			}
			if (roll > MAX_PIXELS_PER_CYCLE) {
				roll = MAX_PIXELS_PER_CYCLE;
				bounceFlipX = 0;
				bounceFlipYY = 0;
			}
			if (pitch < -MAX_PIXELS_PER_CYCLE) {
				pitch = -MAX_PIXELS_PER_CYCLE;
				bounceFlipX = 0;
				bounceFlipYY = 0;
			}
			if (pitch > MAX_PIXELS_PER_CYCLE) {
				pitch = MAX_PIXELS_PER_CYCLE;
				bounceFlipX = 0;
				bounceFlipYY = 0;
			}

			/* check if the direction has changed, if so, stop the bounce code. */
			if ((previousRoll < 0 && roll > 0)
					|| (previousRoll > 0 && roll < 0)) {
				bounceFlipX = 0;
				bounceFlipYY = 0;
			}

			if ((previousPitch < 0 && pitch > 0)
					|| (previousPitch > 0 && pitch < 0)) {
				bounceFlipX = 0;
				bounceFlipYY = 0;
			}

			// save for next time...
			previousRoll = roll;
			previousPitch = pitch;

			// figure out where Dottie is...
			if (bounceFlipX > 0) {
				bounceFlipX--;
				if (marij(bounceFlipX))
					x += (-roll);
				if (x < gradientAction.borderOffset()) {
					x += gradientAction.borderOffset();
				} else if (x > canvas.getWidth()
						- gradientAction.borderOffset()) {
					x -= gradientAction.borderOffset();
				}
			} else {
				x += roll;
			}

			if (x < dottieRadius)
				x = dottieRadius;

			if (x > canvas.getWidth() - dottieRadius)
				x = canvas.getWidth() - dottieRadius;

			if (bounceFlipYY > 0) {
				bounceFlipYY--;
				if (marij(bounceFlipYY))
					yy += (-pitch);
				if (yy < gradientAction.borderOffset()) {
					yy += gradientAction.borderOffset();
				} else if (yy > canvas.getHeight()
						- gradientAction.borderOffset()) {
					yy -= gradientAction.borderOffset();
				}
			} else {
				yy += pitch;
			}
			if (yy < dottieRadius)
				yy = dottieRadius;

			if (yy > canvas.getHeight() - dottieRadius)
				yy = canvas.getHeight() - dottieRadius;

			// coordinates are backwards in the y-axis
			y = canvas.getHeight() - yy;

		}

		if (dieDottie) {
			dottieRadius--;
			if (dottieRadius == MIN_DOTTIE_RADIUS) {
				// idiom to get a new board.
				playTimer = 0;
			}
		}

		/*
		 * dottieCenter is the point in the center of Dottie, it's a tiny
		 * rectangle that has to intersect any point of the target's bounding
		 * rectangle to make a "hit".
		 */
		dottieCenter.set(x, y, x, y);

		/*
		 * dottieBounds is used to figure out collisions with the border.
		 */
		dottieBounds.set(x - dottieRadius, y - dottieRadius, x + dottieRadius,
				y + dottieRadius);

		drawDottie(canvas, x, y);

		deleteTargetsGoingOffScreen(canvas);

		handleTargetCollisions(canvas);

		/*
		 * if we have deleted the last target on the screen, we reset the time
		 * to cause a new board to be created during the next iteration of the
		 * onDraw() loop.
		 */
		if (!activeTargets(targets)) {
			// idiom to get a new board.
			playTimer = 0;
		}

		/*
		 * check if there are moving targets on levels that require moving
		 * targets.
		 */
		if (level.requiresMovingTargets() && nothingIsMoving(targets)) {
			// idiom to get a new board.
			playTimer = 0;
		}

		/*
		 * check if there is at least one target with positive points when the
		 * level has targets with negative points, or targets with zero points.
		 * make a new board if there are no more targets with positive points.
		 */
		if (!anyTargetsWithPositivePoints(targets)) {
			// idiom to get a new board.
			playTimer = 0;
		}

		/*
		 * check if this level requires red targets - if so, check if all of the
		 * red targets are gone...
		 */
		if (level.requiresRedTargets() && isThereARedTarget(targets) == false) {
			// idiom to get a new board.
			playTimer = 0;
		}

		gradientAction.applyGradients(canvas);

		if (gradientAction.getWaitTime() == 0)
			gradientAction.doGradientAction(canvas, dottieBounds);

		if (!isGamePaused) {
			postInvalidate();
		}
	}

	private void drawDottie(Canvas canvas, int x, int y) {

		final int BOW_WIDTH = 12;
		
		// lh side of the hair bow.
		int ly = y - Dottie.DOTTIE_RADIUS;
		
		a.set(x, ly);
		b.set(x - BOW_WIDTH, ly - BOW_WIDTH);
		c.set(x - BOW_WIDTH, ly + BOW_WIDTH);

		// path.setFillType(FillType.EVEN_ODD);
		path.reset();
		path.moveTo(a.x, a.y);
		path.lineTo(b.x, b.y);
		path.lineTo(c.x, c.y);
		path.close();
		canvas.drawPath(path, dottieBowPaint);
			
		// rh side of the hair bow.
		a.set(x, ly);
		b.set(x + BOW_WIDTH, ly - BOW_WIDTH);
		c.set(x + BOW_WIDTH, ly + BOW_WIDTH);

		// path.setFillType(FillType.EVEN_ODD);
		path.reset();
		path.moveTo(a.x, a.y);
		path.lineTo(b.x, b.y);
		path.lineTo(c.x, c.y);
		path.close();
		canvas.drawPath(path, dottieBowPaint);

		dottiePaint.set(Dottie.textPaint); // copy and customize...
		dottiePaint.setColor(getResources().getColor(R.color.Green));

		canvas.drawCircle(x, y, Dottie.dottieRadius, dottiePaint);
	}
	
	/** 
	 * displays something that sort of resembles a dialog box.
	 * @param canvas
	 */
	private void displayNextBoardMessage(Canvas canvas) {
		int left = canvas.getWidth() / 4;
		int right = canvas.getWidth() / 4 * 3;
		int top = canvas.getHeight() / 6 * 2;
		int bottom = canvas.getHeight() / 6 * 3;

		int boxheight = bottom - top;
		int border = boxheight / 5;
		int line1 = boxheight / 4;
		int line2 = line1 * 2;
		int line3 = line1 * 3;
		line1 += top + border;
		line2 += top + border;
		line3 += top + border;
		int boxWidth = right - left - 50;

		popupBorder.set(left, top, right, bottom);
		canvas.drawRect(popupBorder, popupPaint);

		// figure out the font size we need....
		int lenStr1 = 0;
		int lenStr2 = 0;
		int fontSize = 0;
		if (level.hasChanged())
			lenStr1 = determineMaxTextSize(level.levelHint(), boxWidth);
		if (gradientAction.hasChanged())
			lenStr2 = determineMaxTextSize(gradientAction.gradientHint(),
					boxWidth);

		if (lenStr1 > 0 && lenStr2 > 0) {
			fontSize = Math.min(lenStr1, lenStr2);
		} else if (lenStr1 > 0) {
			fontSize = lenStr1;
		} else {
			fontSize = lenStr2;
		}
		popupTextPaint.setTextSize(fontSize);
		popupTextPaint.setTextAlign(Align.CENTER);

		int yPos = line1;
		if (level.hasChanged()) {
			canvas.drawText(level.levelHint(), canvas.getWidth() / 2, yPos,
					popupTextPaint);
			yPos = line2;
		}

		if (gradientAction.hasChanged())
			canvas.drawText(gradientAction.gradientHint(),
					canvas.getWidth() / 2, yPos, popupTextPaint);

		yPos = line3;

		popupTextPaint.setTextAlign(Align.RIGHT);
		canvas.drawText("" + popupTimeout, right - 10, yPos, popupTextPaint);

	}

	/**
	 * Retrieve the maximum text size to fit in a given width.
	 * 
	 * @param str
	 *            (String): Text to check for size.
	 * @param maxWidth
	 *            (float): Maximum allowed width.
	 * @return (int): The desired text size.
	 */
	private int determineMaxTextSize(String str, float maxWidth) {
		int size = 10; // minimum size.

		if (str == null || str.trim().length() == 0)
			return 0;

		do {
			dummyPaint.setTextSize(++size);
		} while (dummyPaint.measureText(str) < maxWidth);

		if (size > 22)
			size = 22; // check if we exceeded maximum size.
		return size;
	}

	private void resetPopup() {
		level.setHasChanged(false);
		gradientAction.setHasChanged(false);
	}

	private boolean marij(int num) {
		int[] marij = { 1, 2, 3, 5, 8, 11, 15, 19, 23 };
		for (int i = 0; i < marij.length; i++) {
			if (num == marij[i])
				return true;
		}
		return false;
	}

	/**
	 * display the player's score.
	 * 
	 */
	private void displayScore() {
		String myScore = getResources().getString(R.string.score) + " "
				+ dottiePrefs.getScore();
		PlayDottie.scoreMessage.setText(myScore);
		PlayDottie.scoreMessage.setTextColor(getResources().getColor(
				R.color.Black));
		PlayDottie.scoreMessage.setBackgroundColor(getResources().getColor(
				R.color.Orange));

	}

	private void displayStatus() {
		PlayDottie.statusMessage.setText(level.levelHint());
		PlayDottie.statusMessage.setTextColor(getResources().getColor(
				R.color.Black));
		PlayDottie.statusMessage.setBackgroundColor(getResources().getColor(
				R.color.Orange));

	}

	/**
	 * delete any moving targets which are going off the screen.
	 * 
	 * @param canvas
	 */
	private void deleteTargetsGoingOffScreen(Canvas canvas) {
		for (int i = 0; i < numTargets; i++) {

			// only draw active targets.
			if (targets.get(i).isActive()) {
				// drawMe returns true when it's drawn on-screen,
				// false if it's a moving target which is going off the screen.
				if (!targets.get(i).drawMe(canvas)) {
					targets.get(i).setIsActive(false);
				}
			}
		}
	}

	/**
	 * Check for collisions between the Dottie and any target. If Dottie
	 * collides, we play a sound, update the score and set the target to
	 * inactive. Also, if the target was a special character, we update the
	 * database for that category.
	 * 
	 * @param canvas
	 */
	private void handleTargetCollisions(Canvas canvas) {

		// check for collisions...
		for (int i = 0; i < numTargets; i++) {
			if (targets.get(i).isActive()) {
				if (targets.get(i).collidesWith(dottieCenter)) {
					/* we hit a target, increment the total hit counter... */
					dottiePrefs.setTotalHits(dottiePrefs.getTotalHits() + 1);

					if (targets.get(i).getShape() == Target.MAX_TARGET_SHAPES - 1) {
						/*
						 * We collided with a special target - update the
						 * database.
						 */
						//Log.d(TAG, "COLLISION!  hit Symbola character " + targets.get(i).getItemId() + 
							//		" category " + targets.get(i).getCategory());
						db.insertItemInventory(targets.get(i).getItemId(),
								targets.get(i).getCategory());
					}

					// add this target's score to the total score, even if it's
					// negative.
					dottiePrefs.setScore(targets.get(i).getScore()
							+ dottiePrefs.getScore());
					displayScore();

					if (targets.get(i).getScore() > 0) {
						// positive points...
						goodBeep(BEEP_DURATION / 2);
					} else if (targets.get(i).getScore() < 0) {
						// negative points...
						badBeep(BEEP_DURATION);
						tempPoints = targets.get(i).getScore();
						lostPointsMessageTimeout = 2;
						displayLostPoints(tempPoints);
					} else {
						// zero points...
					}

					targets.get(i).setIsActive(false);
					break;

				}
			}
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldW, int oldH) {
		// debugging and currently not used.
		//Log.d(TAG, "onSizeChanged(): w: " + w + " h: " + h + " oldW " + oldW
		//		+ " oldH " + oldH);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//Log.d(TAG, "onMeasure(): GOT HERE " + widthMeasureSpec + " "
		//		+ heightMeasureSpec);

		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
				MeasureSpec.getSize(heightMeasureSpec));
	}

	/**
	 * display a message to tell the player he/she just lost some points.
	 * 
	 * @param points
	 *            - how many points.
	 */
	private void displayLostPoints(int points) {

		PlayDottie.statusMessage.setText("You Lose " + Math.abs(points)
				+ " points!");
		PlayDottie.statusMessage.setTextColor(getResources().getColor(
				R.color.White));
		PlayDottie.statusMessage.setBackgroundColor(getResources().getColor(
				R.color.Indigo));
	}

	private void displayTimeoutMessage() {
		PlayDottie.statusMessage.setText("Timeout "
				+ gradientAction.getTimeout());
		PlayDottie.statusMessage.setBackgroundColor(getResources().getColor(
				R.color.Red));
		PlayDottie.statusMessage.setTextColor(getResources().getColor(
				R.color.White));
	}

	/**
	 * the name pretty much says it all. if sound is enabled in the shared
	 * prefs, beep for beepDuration. if vibrate is enabled in the shared prefs,
	 * vibrate for beepDuration. they are independent settings of one another,
	 * but share the BEEP_DURATION.
	 * 
	 * @param - beepDuration (in millis).
	 */
	private void badBeep(int beepDuration) {
		if (dottiePrefs.isSound() && !PlayDottie.isSilent())
			tone.startTone(ToneGenerator.TONE_DTMF_P, beepDuration);

		if (vib.hasVibrator() && dottiePrefs.isVibrate())
			vib.vibrate(beepDuration);
	}

	/**
	 * the name pretty much says it all. if sound is enabled in the shared
	 * prefs, beep for beepDuration.
	 * 
	 * @param - beepDuration (in millis).
	 */
	private void goodBeep(int beepDuration) {
		if (dottiePrefs.isSound() && !PlayDottie.isSilent())
			tone.startTone(ToneGenerator.TONE_DTMF_S, beepDuration);
	}

	/**
	 * method to determine if there are any moving targets.
	 * 
	 * @param targets
	 * 
	 * @return - false if there is a moving target, true if none of the targets
	 *         on the screen are moving.
	 */
	private boolean nothingIsMoving(ArrayList<Target> targets) {
		for (int i = 0; i < numTargets; i++) {
			if (targets.get(i).isActive() && targets.get(i).isMoving()) {
				return false; // something is moving...
			}
		}
		return true; // nothing is moving.
	}

	/**
	 * method to check if there are any active targets in the set of targets.
	 * 
	 * @param targets
	 * @return - true if there are active targets, false otherwise.
	 */
	private boolean activeTargets(ArrayList<Target> targets) {
		for (int i = 0; i < numTargets; i++) {
			if (targets.get(i).isActive()) {
				return true; // active targets.
			}
		}
		return false; // no active targets.
	}

	/**
	 * check is there is an active, red target. if there are multiple matching
	 * targets, this method returns after detecting the first one.
	 * 
	 * @param targets
	 * @return -- true if there is an active, red target. otherwise, false.
	 */
	private boolean isThereARedTarget(ArrayList<Target> targets) {
		for (int i = 0; i < numTargets; i++) {
			if (targets.get(i).isRed() && targets.get(i).isActive()) {
				return true; // there is an active, red target.
			}
		}
		return false; // no active, red targets.
	}

	/**
	 * return absolute value of the score of the remaining active targets. This
	 * will be subtracted from the player's score.
	 * 
	 * @param targets
	 * @return - sum of the absolute values of the remaining target's points.
	 */
	private int sumOfPoints(ArrayList<Target> targets) {
		int points = 0;
		for (int i = 0; i < numTargets; i++) {
			if (targets.get(i).isActive())
				points += Math.abs(targets.get(i).getScore());
		}
		return points;
	}

	/**
	 * check if any targets have a positive score, this will permit us to create
	 * a new board when all the positive targets have been hit.
	 * 
	 * @param targets
	 * @return - true if there are still targets with positive value, false
	 *         otherwise
	 */
	private boolean anyTargetsWithPositivePoints(ArrayList<Target> targets) {
		for (int i = 0; i < numTargets; i++) {
			if (targets.get(i).getScore() > 0) {
				return true;
			}
		}
		return false;
	}



	private class GradientAction {
		int gradient;
		final int GRAD_RAINBOW = 0;
		final int GRAD_BOUNCE = 1;
		final int GRAD_WARP = 2;
		final int GRAD_CRASH = 3;
		final int NUM_GRADIENTS = 4;

		int timeout = 0;
		int waitTime = 0;
		boolean hasChanged;
		int setGradientCount;

		final String[] gradientHint = { "Sticky Borders!", "Bouncey Borders!",
				"Wrap-Around Borders!", "Crash Borders!" };

		LinearGradient[] verticalGradients = new LinearGradient[NUM_GRADIENTS];
		LinearGradient[] horizontalGradients = new LinearGradient[NUM_GRADIENTS];
		RadialGradient dottieRadial;

		/**
		 * method to create all the gradients we'll need. vertical and
		 * horizontal are dealt with separately, since the gradients display
		 * differently left to right or top to bottom.
		 */
		private GradientAction() {
			// 0 is rainbow. Dottie should stick to the wall for a few seconds
			// as a timeout.
			dottieRadial = new RadialGradient(WIDTH / (float) 2, HEIGHT
					/ (float) 2, (float) DOTTIE_RADIUS, new int[] {
					getResources().getColor(R.color.Red),
					getResources().getColor(R.color.Orange),
					getResources().getColor(R.color.Yellow),
					getResources().getColor(R.color.Green),
					getResources().getColor(R.color.Blue),
					getResources().getColor(R.color.Indigo),
					getResources().getColor(R.color.Violet), },
			// null can replace the interval array.
					null,
					// new float[] { (float) 0.14, (float) 0.28, (float) 0.42,
					// (float) 0.56, (float) 0.70, (float) 0.84, 1 },
					Shader.TileMode.MIRROR);

			verticalGradients[GRAD_RAINBOW] = new LinearGradient(0, 0, 1, 400,
					new int[] { getResources().getColor(R.color.Red),
							getResources().getColor(R.color.Orange),
							getResources().getColor(R.color.Yellow),
							getResources().getColor(R.color.Green),
							getResources().getColor(R.color.Blue),
							getResources().getColor(R.color.Indigo),
							getResources().getColor(R.color.Violet), },
					// replace interval array.
							null,
					//new float[] { (float) 0.14, (float) 0.28, (float) 0.42,
					//		(float) 0.56, (float) 0.70, (float) 0.84, 1 },
					Shader.TileMode.MIRROR);

			horizontalGradients[GRAD_RAINBOW] = new LinearGradient(0, 0, 400,
					1, new int[] { getResources().getColor(R.color.Red),
							getResources().getColor(R.color.Orange),
							getResources().getColor(R.color.Yellow),
							getResources().getColor(R.color.Green),
							getResources().getColor(R.color.Blue),
							getResources().getColor(R.color.Indigo),
							getResources().getColor(R.color.Violet), },
					new float[] { (float) 0.14, (float) 0.28, (float) 0.42,
							(float) 0.56, (float) 0.70, (float) 0.84, 1 },
					Shader.TileMode.MIRROR);

			// bounce off the borders.
			verticalGradients[GRAD_BOUNCE] = new LinearGradient(0, 0, 1, 100,
					new int[] { getResources().getColor(R.color.Cyan),
							getResources().getColor(R.color.Blue), },
					new float[] { (float) 0.5, 1, }, Shader.TileMode.MIRROR);

			horizontalGradients[GRAD_BOUNCE] = new LinearGradient(0, 0, 100, 1,
					new int[] { getResources().getColor(R.color.Cyan),
							getResources().getColor(R.color.Blue), },
					new float[] { (float) 0.5, 1 }, Shader.TileMode.MIRROR);

			// is warp to the other side of the screen.
			verticalGradients[GRAD_WARP] = new LinearGradient(0, 0, 1, 100,
					new int[] { getResources().getColor(R.color.Pink),
							getResources().getColor(R.color.Red), },
					new float[] { (float) 0.5, 1, }, Shader.TileMode.MIRROR);

			horizontalGradients[GRAD_WARP] = new LinearGradient(0, 0, 100, 1,
					new int[] { getResources().getColor(R.color.Pink),
							getResources().getColor(R.color.Red), },
					new float[] { (float) 0.5, 1 }, Shader.TileMode.MIRROR);

			// "hit the border, lose some points".
			verticalGradients[GRAD_CRASH] = new LinearGradient(0, 0, 1, 100,
					new int[] { getResources().getColor(R.color.Black),
							getResources().getColor(R.color.Yellow),
							getResources().getColor(R.color.Black),
							getResources().getColor(R.color.Yellow), },
					new float[] { (float) .25, (float) 0.5, (float) .75, 1 },
					Shader.TileMode.MIRROR);

			horizontalGradients[GRAD_CRASH] = new LinearGradient(0, 0, 100, 1,
					new int[] { getResources().getColor(R.color.Black),
							getResources().getColor(R.color.Yellow),
							getResources().getColor(R.color.Black),
							getResources().getColor(R.color.Yellow), },
					new float[] { (float) .25, (float) 0.5, (float) .75, 1 },
					Shader.TileMode.MIRROR);

			timeout = 0;
			waitTime = 0;
			hasChanged = false;
			setGradientCount = 0;
		}

		private void setGradient() {
			final int BORDER_CHANCES = 5;
			if (setGradientCount % 5 == 0) {
				int temp;
				if (rand.nextInt(BORDER_CHANCES) == 0) {
					temp = (rand.nextInt(verticalGradients.length));
				} else {
					temp = -1;
				}

				if (temp != -1 && gradient != temp) {
					this.hasChanged = true;
				}
				gradient = temp;
			}
			setGradientCount++;
		}

		private String gradientHint() {
			if (gradient == -1) {
				return "No Gradient";
			} else {
				return this.gradientHint[gradient];
			}
		}

		private boolean hasChanged() {
			return this.hasChanged;
		}

		private void setHasChanged(boolean val) {
			this.hasChanged = val;
		}

		/**
		 * method to actually do the needful when Dottie hits the border
		 * gradient.
		 * 
		 * @param canvas
		 * @param theRect
		 *            - bounding rectangle of the object to check.
		 */
		private void doGradientAction(Canvas canvas, Rect theRect) {
			if (gradient == GRAD_RAINBOW) {
				/*
				 * here we make Dottie stick to the rainbow gradient wall and
				 * change to a rainbow gradient from the normal green. We also
				 * impose a time penalty and make Dottie stick to the wall for
				 * the "timeout" time.
				 */
				if (timeout == 0 && waitTime == 0) {
					if (hitAnyBorder(canvas, theRect)) {
						timeout = 5;
						displayTimeoutMessage();
						dottiePaint.setShader(dottieRadial);
					}
				}

			} else if (gradient == GRAD_BOUNCE) {
				// figure out which border we hit.
				// TODO: use hitTheBorder method?

				if (theRect.left <= BORDER_WIDTH) {
					if (bounceFlipX > 0 || bounceFlipYY > 0)
						return;
					bounceFlipX = 25;
				} else if (theRect.right >= canvas.getWidth() - BORDER_WIDTH) {
					if (bounceFlipX > 0 || bounceFlipYY > 0)
						return;
					bounceFlipX = 25;
				} else if (theRect.top <= BORDER_WIDTH) {
					if (bounceFlipX > 0 || bounceFlipYY > 0)
						return;
					bounceFlipYY = 25;
				} else if (theRect.bottom >= canvas.getHeight() - BORDER_WIDTH) {
					if (bounceFlipX > 0 || bounceFlipYY > 0)
						return;
					bounceFlipYY = 25;
				}

			} else if (gradient == GRAD_WARP) {
				// warp - when we go off one side, come back on the opposite
				// side.

				// TODO: use hitTheBorder method.
				if (theRect.left <= BORDER_WIDTH) {
					x = canvas.getWidth() - (BORDER_WIDTH * 2) - DOTTIE_RADIUS
							- 2;
				} else if (theRect.right >= canvas.getWidth() - BORDER_WIDTH) {
					x = BORDER_WIDTH + DOTTIE_RADIUS + 2;
				} else if (theRect.top <= BORDER_WIDTH) {
					yy = BORDER_WIDTH + DOTTIE_RADIUS + 2;
				} else if (theRect.bottom >= canvas.getHeight() - BORDER_WIDTH) {
					yy = canvas.getHeight() - (BORDER_WIDTH * 2)
							- DOTTIE_RADIUS - 2;
				}

			} else if (gradient == GRAD_CRASH) {
				// check if this is the first time Dottie touched the edge of
				// the screen:
				if (dieDottie == false
						&& gradientAction.touchesSide(canvas, dottieBounds)) {
					dieDottie = true;
					badBeep(BEEP_DURATION);

					// update the score and save in shared prefs.
					dottiePrefs.setScore(dottiePrefs.getScore()
							- sumOfPoints(targets));
					displayScore();

					/*
					 * there is a timing issue where you can hit the wall but
					 * there are no targets left, thereby losing 0 points for
					 * hitting the wall. so it looks pretty stupid to put up a
					 * message that says "you lost 0 points". check the value of
					 * the remaining points, only display the message if
					 * positive.
					 */
					if (sumOfPoints(targets) > 0) {
						lostPointsMessageTimeout = 2;
						tempPoints = sumOfPoints(targets);
						displayLostPoints(tempPoints);
					}
				}
			}
		}

		/**
		 * check to see if the passed-in rectangle touches any border.
		 * 
		 * @param theRect
		 * @return true if it touches, false otherwise.
		 */
		private boolean hitAnyBorder(Canvas canvas, Rect theRect) {

			if (theRect.left <= BORDER_WIDTH) {
				return true;

			} else if (theRect.right >= canvas.getWidth() - BORDER_WIDTH) {
				return true;

			} else if (theRect.top <= BORDER_WIDTH) {
				return true;

			} else if (theRect.bottom >= canvas.getHeight() - BORDER_WIDTH) {
				return true;
			}
			return false;

		}

		private int getTimeout() {
			return timeout;
		}

		private void setTimeout(int t) {
			timeout = t;
		}

		private void setWaitTime(int t) {
			waitTime = t;
		}

		private void tick() {
			if (timeout > 0) {
				timeout--;
				if (timeout == 0 && dottiePaint.getShader() != null) {
					dottiePaint.setShader(null);
					waitTime = 4;
				}
			}
			if (waitTime > 0)
				waitTime--;
		}

		private int getWaitTime() {
			return waitTime;
		}

		/**
		 * check if the rectangle r touches any of the sides of the board.
		 * 
		 * @param c
		 *            - Canvas
		 * @param r
		 *            - Rect
		 * @return - true if they touch, false otherwise.
		 */
		private boolean touchesSide(Canvas c, Rect r) {
			if (gradient > -1) {
				if (r.left < BORDER_WIDTH) {
					return true;
				} else if (r.top < BORDER_WIDTH) {
					return true;
				} else if (r.right > c.getWidth() - BORDER_WIDTH) {
					return true;
				} else if (r.bottom > c.getHeight() - BORDER_WIDTH) {
					return true;
				}
			}
			return false;
		}

		/**
		 * this routine puts the appropriate gradients on the screen based on
		 * the game level.
		 * 
		 * @param canvas
		 */
		private void applyGradients(Canvas canvas) {

			if (gradient == -1) {
				return; // no border.
			} else {
				verticalBorderPaint.setShader(verticalGradients[gradient]);
				horizontalBorderPaint.setShader(horizontalGradients[gradient]);
			}

			// left and right.
			canvas.drawRect(0, 0, BORDER_WIDTH, canvas.getHeight(),
					verticalBorderPaint);
			canvas.drawRect(canvas.getWidth() - BORDER_WIDTH, 0,
					canvas.getWidth(), canvas.getHeight(), verticalBorderPaint);

			// top and bottom.
			canvas.drawRect(0, 0, canvas.getWidth(), BORDER_WIDTH,
					horizontalBorderPaint);
			canvas.drawRect(0, canvas.getHeight() - BORDER_WIDTH,
					canvas.getWidth(), canvas.getHeight(),
					horizontalBorderPaint);
		}

		/**
		 * return the border width in number of pixels.
		 * 
		 * @return - if there is no border, return 0.
		 */
		private int borderOffset() {
			if (gradient == -1) {
				return 0;
			} else {
				return BORDER_WIDTH;
			}
		}
	}
}
