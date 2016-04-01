package com.cottagecoders.dottie;

import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

public class Target {

	private final String TAG = "Target";

	// targetSize in px.
	private final int targetSize = Dottie.DOTTIE_RADIUS * 2;
	static final int MAX_TARGET_SHAPES = 5;

	// number of px a moving target moves per cycle through onDraw.
	private static final int MAX_SPEED = 3;

	private boolean isActive;
	private boolean isMoving;
	private int whichDirection;
	private int x;
	private int y;
	private int speed;

	Typeface myTypeface;

	Point a = new Point();
	Point b = new Point();
	Point c = new Point();
	Path path = new Path();

	private int shape;
	private String unicode;

	private int score;
	private Rect bounds = new Rect();
	private Paint targetPaint = new Paint();
	private ItemRecord item;

	int colorNum;
	private int colors[] = { R.color.Red, R.color.LightBlue,
			R.color.GreenYellow, R.color.Tan, R.color.Orange, R.color.Violet,
			R.color.Yellow, R.color.GoldenRod, R.color.Salmon, R.color.Plum };
	// which position in the colors array has the color we
	// want to give points in the higher levels.
	private static final int RED_COLOR_NUM = 0;

	// which position in the colors array is the color for the non-points
	// color in the higher levels.
	private static final int OTHER_COLOR_POS = 1;

	static Context ctx;

	private int scores[] = { 10, 15, 20, 25, 30, 35, 40, 45, 50 };
	Paint scoreTextPaint = new Paint();
	RectF roundedRect = new RectF();

	DottiePrefs dottiePrefs;
	DottieDB db;
	ArrayList<String> categories;

	boolean once = true;

	/**
	 * constructor - also, note there is a special initialization routine.
	 * 
	 * @param canvas
	 *            - canvas for drawing code.
	 * @param moving
	 *            - new Target must be a moving target.
	 * @param makeItRed
	 *            - new Target must be red.
	 */
	public Target() {
		return;
	}

	/**
	 * one time initialization for the Target object.
	 * 
	 */
	public void once_only(Context ctx) {
		if (once) {
			once = false;
			Log.d(TAG, "performing once_only initialization");
			Target.ctx = ctx;
			scoreTextPaint.set(Dottie.textPaint);
			scoreTextPaint.setColor(ctx.getResources().getColor(R.color.Black));

			/* get the Shared Preferences. */
			dottiePrefs = DottiePrefs.getInstance(PlayDottie.ctx);
			/*
			 * create a Typeface so we can display various unicode characters.
			 */
			myTypeface = Typeface.createFromAsset(PlayDottie.ctx.getAssets(),
					"fonts/Symbola.ttf");

			/*
			 * create a database object - if this is the first time we're
			 * running, we load the data from the assets directory.
			 */

			db = new DottieDB(ctx);
			if (0 == db.getCount()) {
				db.createItems();
			}

			categories = db.getCategories();
			
			//TODO: debugging.
			//Log.d(TAG, "categories " + categories + " category count = "
				//	+ categories.size());
			//db.dumpItems();
		}
		return;
	}

	/**
	 * initialization routine - this way we can re-initialize a Target object
	 * instead of re-creating it.
	 * 
	 * @param canvas
	 *            -
	 * @param moving
	 *            - new Target must be a moving target.
	 * @param makeItRed
	 *            - new Target must be red.
	 * @return - initialized Target object.
	 */
	public Target init(Canvas canvas, boolean moving, boolean makeItRed) {
		// important - "moving" variable set to true means
		// the constructor MUST return a moving target,
		// if moving is false, it can be static OR moving.

		// makeItRed indicates we need a red color target.

		// check if this will be a moving target.
		// 0 = up, 1 = down, 2 = left , 3 = right
		// 50% chance of moving target.
		if (moving || Dottie.rand.nextInt(2) == 1) {
			this.isMoving = true;
			this.speed = Dottie.rand.nextInt(27) % MAX_SPEED + 1;

			whichDirection = Dottie.rand.nextInt(237) % 4;

			switch (whichDirection) {
			case 0:
				// start at bottom...
				x = Dottie.rand.nextInt(canvas.getWidth() - targetSize)
						+ Dottie.BORDER_WIDTH;
				y = canvas.getHeight() - targetSize;
				break;

			case 1:
				// start at the top...
				x = Dottie.rand.nextInt(canvas.getWidth() - targetSize)
						+ Dottie.BORDER_WIDTH;
				y = targetSize;
				break;

			case 2:
				// start on the left...
				x = Dottie.BORDER_WIDTH;
				y = Dottie.rand.nextInt(canvas.getHeight() - targetSize)
						+ Dottie.BORDER_WIDTH;
				break;

			case 3:
				// start on the right...
				x = canvas.getWidth() - targetSize;
				y = Dottie.rand.nextInt(canvas.getHeight() - targetSize)
						+ Dottie.BORDER_WIDTH;
				break;
			}

		} else {
			isMoving = false;

			// get random numbers for the x and y.
			x = Dottie.rand.nextInt(canvas.getWidth() - targetSize);
			if (x < targetSize)
				x = targetSize;

			y = Dottie.rand.nextInt(canvas.getHeight() - targetSize);
			if (y < targetSize)
				y = targetSize;
		}

		// load rectangle for collision detection...

		// target size is a rectangle around the entire target.
		bounds.set(x, y, x + targetSize, y + targetSize);

		targetPaint.setTypeface(myTypeface);
		targetPaint.setAntiAlias(true);
		targetPaint.setStyle(Paint.Style.FILL);
		targetPaint.setStrokeWidth(3.0f);
		targetPaint.setTextSize(40.0f);
		targetPaint.setTextAlign(Align.CENTER);

		// logic to handle the various levels.
		// the targets can be set up differently based on level.

		switch (dottiePrefs.getLevel()) {

		case 0: {
			// level 0, choose a random color and random score.
			targetPaint.setColor(ctx.getResources().getColor(
					colors[Dottie.rand.nextInt(colors.length)]));
			score = scores[Dottie.rand.nextInt(scores.length)];
			shape = Dottie.rand.nextInt(MAX_TARGET_SHAPES);
			if (shape == MAX_TARGET_SHAPES - 1) {
				this.select_character();
				targetPaint
						.setColor(ctx.getResources().getColor(R.color.Black));
			}
			break;
		}
		case 1: {
			// 1 = only red targets give points.
			if (!makeItRed) {
				colorNum = Dottie.rand.nextInt(colors.length);
			} else {
				colorNum = RED_COLOR_NUM;
			}

			targetPaint.setColor(ctx.getResources().getColor(colors[colorNum]));

			if (colorNum == RED_COLOR_NUM) {
				score = scores[Dottie.rand.nextInt(scores.length)];
			} else {
				score = 0;
			}

			shape = Dottie.rand.nextInt(MAX_TARGET_SHAPES);
			if (shape == MAX_TARGET_SHAPES - 1) {
				this.select_character();
				if (colorNum != RED_COLOR_NUM) {
					targetPaint.setColor(ctx.getResources().getColor(
							R.color.Black));
				}
			}
			break;
		}
		case 2: {
			// 2 = only two colors...
			// RED_POS targets give points,
			// OTHER_COLOR_POS targets lose points.
			if (Dottie.rand.nextInt(2) == 1) {
				colorNum = RED_COLOR_NUM;
				targetPaint.setColor(ctx.getResources().getColor(
						colors[RED_COLOR_NUM]));
				score = scores[Dottie.rand.nextInt(scores.length)];
			} else {
				colorNum = OTHER_COLOR_POS;
				targetPaint.setColor(ctx.getResources().getColor(
						colors[OTHER_COLOR_POS]));
				score = -scores[Dottie.rand.nextInt(scores.length)];
			}
			shape = Dottie.rand.nextInt(MAX_TARGET_SHAPES);
			if (shape == MAX_TARGET_SHAPES - 1) {
				this.select_character();
			}
			break;
		}
		case 3: {
			// 3 = moving targets give points, static targets - no points.
			colorNum = Dottie.rand.nextInt(colors.length);
			targetPaint.setColor(ctx.getResources().getColor(colors[colorNum]));
			if (isMoving) {
				score = scores[Dottie.rand.nextInt(scores.length)];
			} else {
				score = 0;
			}
			shape = Dottie.rand.nextInt(MAX_TARGET_SHAPES);
			if (shape == MAX_TARGET_SHAPES - 1) {
				this.select_character();
				targetPaint
						.setColor(ctx.getResources().getColor(R.color.Black));
			}
			break;
		}
		case 4: {
			// 4 = moving targets give points, static targets lose points.
			colorNum = Dottie.rand.nextInt(colors.length);
			targetPaint.setColor(ctx.getResources().getColor(colors[colorNum]));
			if (isMoving) {
				score = scores[Dottie.rand.nextInt(scores.length)];
			} else {
				score = -scores[Dottie.rand.nextInt(scores.length)];
			}
			shape = Dottie.rand.nextInt(MAX_TARGET_SHAPES);
			if (shape == MAX_TARGET_SHAPES - 1) {
				this.select_character();
				targetPaint
						.setColor(ctx.getResources().getColor(R.color.Black));
			}
			break;
		}
		} // switch

		isActive = true;
		return this;
	}

	public void select_character() {
		/*
		 * special code to select a font character when the random number is
		 * MAX_TARGET_SHAPES - 1.
		 */
		item = null;
		/* first select a category... */
		int i = Dottie.rand.nextInt(categories.size());
		Log.d(TAG, "DB: random category " + i);
		/* fetch all the items in this category... */
		ArrayList<ItemRecord> items = db.getItemList(categories.get(i));
		Log.d(TAG, "DB: item list is " + items.size());
		/* choose an item */
		
		// TODO: saw a crash here. is items.size == zero?
		// yes, it was zero - it was caused by the "dummy" extra 
		// category - which has been removed.
		i = Dottie.rand.nextInt(items.size());
		item = items.get(i);

		/*
		 * NOTE: check the unicode values to assemble the correct value for the
		 * character we want.
		 */
		if (item.getUnicode1().trim().length() < 5)
			unicode = "\\u" + item.getUnicode1().trim();
		else {
			unicode = "\\u" + item.getUnicode2() + "\\u" + (item.getUnicode3())
					+ " ";
		}
	}

	public boolean collidesWith(Rect r) {
		return Rect.intersects(bounds, r);
	}

	// helper routine so we can call it with a Target object,
	// instead of a Rect.
	public boolean collidesWith(Target t) {
		return collidesWith(t.bounds);
	}

	public Rect getBounds() {
		return bounds;
	}

	public boolean isMoving() {
		return isMoving;
	}

	public boolean isActive() {
		return isActive;
	}

	public boolean isRed() {
		if (colorNum == RED_COLOR_NUM)
			return true;
		return false;
	}

	public void setIsActive(boolean active) {
		isActive = active;
	}

	public int getItemId() {
		return this.item.id;
	}

	public String getCategory() {
		return this.item.category;
	}

	public int getScore() {
		return this.score;
	}

	public int getShape() {
		return this.shape;
	}

	public boolean drawMe(Canvas canvas) {

		// return true if target was drawn,
		// otherwise false.
		// this is the case when a moving target runs off the screen.

		if (isMoving) {
			switch (whichDirection) {
			case 0: {
				// up
				y -= this.speed;
				if (y <= 0)
					return false;
				break;
			}
			case 1: {
				// down
				y += this.speed;
				if (y > canvas.getHeight() - targetSize)
					return false;
				break;
			}
			case 2: {
				// right
				x += this.speed;
				if (x > canvas.getWidth() - targetSize)
					return false;
				break;
			}
			case 3: {
				// left
				x -= this.speed;
				if (x <= 0)
					return false;
				break;
			}
			}
			// when we have a moving target, besides the new x, y we also
			// need to update the bounds rectangle that we're going
			// to use for collision detection.
			bounds.set(x, y, x + targetSize, y + targetSize);

		}

		// draw the target.
		switch (shape) {
		case 0: // circle
			canvas.drawCircle(x + (targetSize / 2), y + (targetSize / 2),
					targetSize / 2, targetPaint);
			break;

		case 1: // rounded rectangle
			roundedRect.set(bounds);
			canvas.drawRoundRect(roundedRect, 9, 9, targetPaint);
			break;

		case 2: // rectangle
			canvas.drawRect(bounds, targetPaint);
			break;

		case 3: // triangle
			a.set(x + (targetSize / 2), y);
			b.set(x, y + targetSize);
			c.set(x + targetSize, y + targetSize);

			path.reset();
			path.setFillType(FillType.EVEN_ODD);
			path.moveTo(a.x, a.y);
			path.lineTo(b.x, b.y);
			path.lineTo(c.x, c.y);
			path.close();

			canvas.drawPath(path, targetPaint);
			break;

		case 4: // display special character!
			canvas.drawText(StringEscapeUtils.unescapeJava(unicode), x
					+ targetSize / 2, y + targetSize / 2, targetPaint);

		} // switch

		/*
		 * display the target's point value...
		 */
		final int DISP_SCORE_ON_CORNER = 18;
		final int DISP_SCORE_IN_MIDDLE = 8;
		if (shape == MAX_TARGET_SHAPES - 1) {
			/*
			 * try to put the score just on the lower right corner of the
			 * target.
			 */
			canvas.drawText("" + score, bounds.left + (targetSize / 2)
					+ DISP_SCORE_ON_CORNER, bounds.top + (targetSize / 2)
					+ DISP_SCORE_ON_CORNER, scoreTextPaint);
		} else {
			/* try to plop the score dead-center */
			canvas.drawText("" + score, bounds.left + (targetSize / 2),
					bounds.top + (targetSize / 2) + DISP_SCORE_IN_MIDDLE,
					scoreTextPaint);
		}
		return true;
	}

}
