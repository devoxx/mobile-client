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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class MetropolisOverlay extends Overlay {

	private static Paint paint;

	{
		paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setAlpha(70);
		paint.setStyle(Paint.Style.FILL);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(0.0f);
		paint.setAntiAlias(true);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);

		final Projection prj = mapView.getProjection();

		GeoPoint gp1 = null;
		GeoPoint gp2 = null;
		GeoPoint gp3 = null;
		final Point p1 = new Point();
		final Point p2 = new Point();
		final Point p3 = new Point();
		final Path path = new Path();
		gp1 = new GeoPoint((int) (51.246451 * 1e6), (int) (4.418067 * 1e6));
		prj.toPixels(gp1, p1);
		path.moveTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246451 * 1e6), (int) (4.417768 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246504 * 1e6), (int) (4.417768 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246504 * 1e6), (int) (4.416918 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246459 * 1e6), (int) (4.416918 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246459 * 1e6), (int) (4.415888 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246377 * 1e6), (int) (4.415888 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246377 * 1e6), (int) (4.415526 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246165 * 1e6), (int) (4.415343 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246187 * 1e6), (int) (4.415263 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245916 * 1e6), (int) (4.415043 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245905 * 1e6), (int) (4.41508 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245744 * 1e6), (int) (4.414957 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245764 * 1e6), (int) (4.414879 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245377 * 1e6), (int) (4.41456 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245053 * 1e6), (int) (4.415579 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245097 * 1e6), (int) (4.415925 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245262 * 1e6), (int) (4.415955 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245419 * 1e6), (int) (4.41595 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245431 * 1e6), (int) (4.415848 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp2 = new GeoPoint((int) (51.245945 * 1e6), (int) (4.415954 * 1e6));
		gp3 = new GeoPoint((int) (51.245760 * 1e6), (int) (4.416719 * 1e6));
		prj.toPixels(gp2, p2);
		prj.toPixels(gp3, p3);
		path.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
		gp1 = new GeoPoint((int) (51.245693 * 1e6), (int) (4.416676 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245638 * 1e6), (int) (4.416974 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245628 * 1e6), (int) (4.417164 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245688 * 1e6), (int) (4.417164 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245691 * 1e6), (int) (4.417256 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245571 * 1e6), (int) (4.417248 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245606 * 1e6), (int) (4.417572 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245665 * 1e6), (int) (4.417846 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.2457 * 1e6), (int) (4.417816 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.2457 * 1e6), (int) (4.417768 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245752 * 1e6), (int) (4.417768 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.245752 * 1e6), (int) (4.418067 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);
		gp1 = new GeoPoint((int) (51.246451 * 1e6), (int) (4.418067 * 1e6));
		prj.toPixels(gp1, p1);
		path.lineTo(p1.x, p1.y);

		canvas.drawPath(path, paint);
	}

}
