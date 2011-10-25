/*
 * Copyright 2011 Peter Kuterna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.peterkuterna.android.apps.devoxxsched.ui.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.peterkuterna.android.apps.devoxxsched.util.MapUtils;
import net.peterkuterna.android.apps.devoxxsched.util.Maps;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Point;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class RoomOverlay extends Overlay {

	protected static final String TAG = "RoomOverlay";

	private static final HashMap<Integer, Float> FONT_SIZE_MAP;

	private static Paint paint;
	private static Paint fillPaint;
	private static Paint highlightPaint;
	private static Paint selectedPaint;

	static {
		FONT_SIZE_MAP = Maps.newHashMap();
		FONT_SIZE_MAP.put(18, 6.0f);
		FONT_SIZE_MAP.put(19, 10.0f);
		FONT_SIZE_MAP.put(20, 14.0f);
		FONT_SIZE_MAP.put(21, 18.0f);
	}

	{
		paint = new Paint();
		paint.setColor(0xff014dab);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1.0f);
		paint.setAntiAlias(true);
	}

	{
		fillPaint = new Paint();
		fillPaint.setColor(0x99a6c3ff);
		fillPaint.setStyle(Paint.Style.FILL);
		fillPaint.setStrokeWidth(0.0f);
		fillPaint.setAntiAlias(true);
	}

	{
		highlightPaint = new Paint();
		highlightPaint.setColor(0xff014dab);
		highlightPaint.setStyle(Paint.Style.FILL);
		highlightPaint.setStrokeWidth(0.0f);
		highlightPaint.setAntiAlias(true);
	}

	{
		selectedPaint = new Paint();
		selectedPaint.setColor(0x99a6c3ff);
		selectedPaint.setStyle(Paint.Style.FILL);
		selectedPaint.setStrokeWidth(0.0f);
		selectedPaint.setAntiAlias(true);
	}

	private final String mRoomName;
	private final ArrayList<GeoPoint> mPoints;
	private final boolean mSelectable;
	private int mLatMin;
	private int mLatMax;
	private int mLonMin;
	private int mLonMax;
	private GeoPoint mTextLeft;
	private GeoPoint mTextRight;
	private final boolean mHighlight;
	private boolean mSelected = false;

	private final Path path;
	private final Path textPath;
	private final Point p;

	private OnTapListener mOnTapListener;

	public RoomOverlay(String roomName, boolean selectable, String encodedPoly,
			boolean highlight) {
		super();

		mRoomName = roomName;
		mSelectable = selectable;

		path = new Path();
		textPath = new Path();
		p = new Point();
		mPoints = MapUtils.decodePoly(encodedPoly, false);
		mHighlight = highlight;

		mLatMin = Integer.MAX_VALUE;
		mLatMax = Integer.MIN_VALUE;
		mLonMin = Integer.MAX_VALUE;
		mLonMax = Integer.MIN_VALUE;
		for (GeoPoint p : mPoints) {
			if (p.getLatitudeE6() < mLatMin) {
				mLatMin = p.getLatitudeE6();
			}
			if (p.getLatitudeE6() > mLatMax) {
				mLatMax = p.getLatitudeE6();
			}
			if (p.getLongitudeE6() < mLonMin) {
				mLonMin = p.getLongitudeE6();
			}
			if (p.getLongitudeE6() > mLonMax) {
				mLonMax = p.getLongitudeE6();
			}
		}

		mTextLeft = new GeoPoint(mLatMin, (mLonMin + mLonMax) / 2);
		mTextRight = new GeoPoint(mLatMax, (mLonMin + mLonMax) / 2);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);

		if (mPoints != null && !mPoints.isEmpty()) {
			redrawPath(mapView);
			synchronized (fillPaint) {
				canvas.drawPath(path, fillPaint);
			}
			synchronized (mHighlight ? highlightPaint : paint) {
				canvas.drawPath(path, mHighlight ? highlightPaint : paint);
			}
			if (mSelected) {
				synchronized (selectedPaint) {
					canvas.drawPath(path, selectedPaint);
				}
			}

			final Float fontSize = FONT_SIZE_MAP.get(mapView.getZoomLevel());
			if (fontSize != null) {
				final Paint textPaint = new Paint(paint);
				textPaint.setStyle(Paint.Style.FILL);
				textPaint.setStrokeWidth(0.0f);
				textPaint.setTextSize(fontSize.floatValue());
				textPaint.setTextAlign(Align.CENTER);
				textPaint.setColor(Color.WHITE);

				redrawTextPath(mapView);
				canvas.drawTextOnPath(mRoomName, textPath, 0.0f,
						fontSize / 4.0f, textPaint);
			}

		}
	}

	private void redrawPath(final MapView mv) {
		final Projection prj = mv.getProjection();
		path.rewind();
		final Iterator<GeoPoint> it = mPoints.iterator();
		prj.toPixels(it.next(), p);
		path.moveTo(p.x, p.y);
		while (it.hasNext()) {
			prj.toPixels(it.next(), p);
			path.lineTo(p.x, p.y);
		}
		path.setLastPoint(p.x, p.y);
	}

	private void redrawTextPath(final MapView mv) {
		final Projection prj = mv.getProjection();
		textPath.rewind();
		prj.toPixels(mTextLeft, p);
		textPath.moveTo(p.x, p.y);
		prj.toPixels(mTextRight, p);
		textPath.lineTo(p.x, p.y);
		textPath.setLastPoint(p.x, p.y);
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		if (mOnTapListener != null && hitTest(p)) {
			mOnTapListener.onTap();
		}
		return super.onTap(p, mapView);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		final GeoPoint p = mapView.getProjection().fromPixels((int) e.getX(),
				(int) e.getY());
		if (hitTest(p) && MotionEvent.ACTION_DOWN == e.getAction()) {
			mSelected = true;
		} else if (mSelected && MotionEvent.ACTION_UP == e.getAction()) {
			mSelected = false;
		}

		return super.onTouchEvent(e, mapView);
	}

	public void setOnTapListener(OnTapListener listener) {
		mOnTapListener = listener;
	}

	private boolean hitTest(GeoPoint p) {
		if (mSelectable) {
			final int lat = p.getLatitudeE6();
			final int lon = p.getLongitudeE6();
			if (lat < mLatMin || lat > mLatMax || lon < mLonMin
					|| lon > mLonMax) {
				return false;
			}
			return true;
		}

		return false;
	}

	public interface OnTapListener {

		void onTap();

	}

}
