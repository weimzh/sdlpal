/*
 2012/7 Created by AKIZUKI Katane
 */

package com.codeplex.sdlpal;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.SurfaceView;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

class MouseCursor extends View {
/*
	public final static int WIDTH  = 12;
	public final static int HEIGHT = 20;
*/
	public final static int WIDTH  = 24;
	public final static int HEIGHT = 40;	

	private Path mPath;
	private Paint mPaint;
	
	private int mFillColorR = 0;
	private int mFillColorG = 0;
	private int mFillColorB = 0;
	
	private int mStrokeColorR = 255;
	private int mStrokeColorG = 255;
	private int mStrokeColorB = 255;
	
	public MouseCursor(Context c){
		super(c);
			
		mPath = new Path();
/*			
		mPath.moveTo(0, 0);
		mPath.lineTo(0, 17);
		mPath.lineTo(4, 13);
		mPath.lineTo(7, 19);
		mPath.lineTo(10, 17);
		mPath.lineTo(7, 11);
		mPath.lineTo(11, 11);
		mPath.close();
*/
		mPath.moveTo(0, 0);
		mPath.lineTo(0, 34);
		mPath.lineTo(8, 26);
		mPath.lineTo(13, 38);
		mPath.lineTo(19, 34);
		mPath.lineTo(13, 23);
		mPath.lineTo(23, 23);
		mPath.close();

		mPaint = new Paint();
		
		setMouseCursorRGB(0, 0, 0, 255, 255, 255);
		
		setBackgroundColor(Color.TRANSPARENT);
	}
	
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setARGB(255, mFillColorR, mFillColorG, mFillColorB);
		canvas.drawPath(mPath, mPaint);
		
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setARGB(255, mStrokeColorR, mStrokeColorG, mStrokeColorB);
		canvas.drawPath(mPath, mPaint);
	}
	
	public void setMouseCursorRGB(int fillColorR, int fillColorG, int fillColorB, int strokeColorR, int strokeColorG, int strokeColorB)
	{
		mFillColorR = fillColorR;
		mFillColorG = fillColorG;
		mFillColorB = fillColorB;
		
		mStrokeColorR = strokeColorR;
		mStrokeColorG = strokeColorG;
		mStrokeColorB = strokeColorB;
		
		invalidate();
	}
}
