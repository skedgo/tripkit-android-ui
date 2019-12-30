package com.skedgo.tripkit.ui;

import android.graphics.*;

/**
 * Created by Adrian Schoenig on 3. May 2018.
 * CopyrightW 2018 SkedGo Pty Ltd. All rights reserved.
 * <p>
 * Generated by PaintCode
 * http://www.paintcodeapp.com
 *
 * @author Adrian Schoenig
 */
public class TripGoStyleKit {

  // Resizing Behavior
  public enum ResizingBehavior {
    AspectFit, //!< The content is proportionally resized to fit into the target rectangle.
    AspectFill, //!< The content is proportionally resized to completely fill the target rectangle.
    Stretch, //!< The content is stretched to match the entire target rectangle.
    Center, //!< The content is centered in the target rectangle, but it is NOT resized.
  }

  // Canvas Drawings
  // Bike Share

  private static class CacheForIconbikeshare {
    private static Paint paint = new Paint();
    private static RectF originalFrame = new RectF(0f, 0f, 40f, 40f);
    private static RectF resizedFrame = new RectF();
    private static RectF bezierRect = new RectF();
    private static Path bezierPath = new Path();
    private static RectF bezier2Rect = new RectF();
    private static Path bezier2Path = new Path();
  }

  public synchronized static void drawIconbikeshare(Canvas canvas) {
    TripGoStyleKit.drawIconbikeshare(canvas, new RectF(0f, 0f, 40f, 40f), ResizingBehavior.AspectFit);
  }

  public synchronized static void drawIconbikeshare(Canvas canvas, RectF targetFrame, ResizingBehavior resizing) {
    // General Declarations
    Paint paint = CacheForIconbikeshare.paint;

    // Local Colors
    int bikeColor = Color.argb(255, 255, 255, 255);

    // Resize to Target Frame
    canvas.save();
    RectF resizedFrame = CacheForIconbikeshare.resizedFrame;
    TripGoStyleKit.resizingBehaviorApply(resizing, CacheForIconbikeshare.originalFrame, targetFrame, resizedFrame);
    canvas.translate(resizedFrame.left, resizedFrame.top);
    canvas.scale(resizedFrame.width() / 40f, resizedFrame.height() / 40f);

    // Group
    {
      // Bezier
      RectF bezierRect = CacheForIconbikeshare.bezierRect;
      bezierRect.set(1.83f, 2.67f, 38.17f, 35.33f);
      Path bezierPath = CacheForIconbikeshare.bezierPath;
      bezierPath.reset();
      bezierPath.moveTo(27.33f, 7.94f);
      bezierPath.cubicTo(26.66f, 7.94f, 26.12f, 7.41f, 26.12f, 6.75f);
      bezierPath.cubicTo(26.12f, 6.09f, 26.66f, 5.56f, 27.33f, 5.56f);
      bezierPath.cubicTo(27.99f, 5.56f, 28.53f, 6.09f, 28.53f, 6.75f);
      bezierPath.cubicTo(28.53f, 7.41f, 27.99f, 7.94f, 27.33f, 7.94f);
      bezierPath.close();
      bezierPath.moveTo(25.78f, 2.67f);
      bezierPath.cubicTo(24.25f, 2.67f, 22.92f, 3.49f, 22.21f, 4.71f);
      bezierPath.lineTo(10.64f, 4.71f);
      bezierPath.lineTo(9.26f, 6.07f);
      bezierPath.lineTo(9.26f, 6.75f);
      bezierPath.lineTo(11.33f, 8.79f);
      bezierPath.lineTo(12.7f, 8.79f);
      bezierPath.lineTo(13.83f, 7.43f);
      bezierPath.lineTo(14.98f, 7.43f);
      bezierPath.lineTo(16.15f, 8.79f);
      bezierPath.lineTo(17.29f, 8.79f);
      bezierPath.lineTo(18.44f, 7.43f);
      bezierPath.lineTo(19.59f, 7.43f);
      bezierPath.lineTo(20.73f, 8.79f);
      bezierPath.lineTo(22.21f, 8.79f);
      bezierPath.cubicTo(22.92f, 10.01f, 24.25f, 10.83f, 25.78f, 10.83f);
      bezierPath.cubicTo(28.06f, 10.83f, 29.91f, 9.01f, 29.91f, 6.75f);
      bezierPath.cubicTo(29.91f, 4.5f, 28.06f, 2.67f, 25.78f, 2.67f);
      bezierPath.close();
      bezierPath.moveTo(31.15f, 33.6f);
      bezierPath.cubicTo(28.24f, 33.6f, 25.88f, 31.27f, 25.88f, 28.39f);
      bezierPath.cubicTo(25.88f, 25.52f, 28.24f, 23.19f, 31.15f, 23.19f);
      bezierPath.cubicTo(34.05f, 23.19f, 36.41f, 25.52f, 36.41f, 28.39f);
      bezierPath.cubicTo(36.41f, 31.27f, 34.05f, 33.6f, 31.15f, 33.6f);
      bezierPath.close();
      bezierPath.moveTo(31.15f, 21.45f);
      bezierPath.cubicTo(27.27f, 21.45f, 24.13f, 24.56f, 24.13f, 28.39f);
      bezierPath.cubicTo(24.13f, 32.22f, 27.27f, 35.33f, 31.15f, 35.33f);
      bezierPath.cubicTo(35.02f, 35.33f, 38.17f, 32.22f, 38.17f, 28.39f);
      bezierPath.cubicTo(38.17f, 24.56f, 35.02f, 21.45f, 31.15f, 21.45f);
      bezierPath.close();
      bezierPath.moveTo(8.85f, 33.6f);
      bezierPath.cubicTo(5.94f, 33.6f, 3.59f, 31.27f, 3.59f, 28.39f);
      bezierPath.cubicTo(3.59f, 25.52f, 5.94f, 23.19f, 8.85f, 23.19f);
      bezierPath.cubicTo(11.76f, 23.19f, 14.12f, 25.52f, 14.12f, 28.39f);
      bezierPath.cubicTo(14.12f, 31.27f, 11.76f, 33.6f, 8.85f, 33.6f);
      bezierPath.close();
      bezierPath.moveTo(8.85f, 21.45f);
      bezierPath.cubicTo(4.98f, 21.45f, 1.83f, 24.56f, 1.83f, 28.39f);
      bezierPath.cubicTo(1.83f, 32.22f, 4.98f, 35.33f, 8.85f, 35.33f);
      bezierPath.cubicTo(12.73f, 35.33f, 15.87f, 32.22f, 15.87f, 28.39f);
      bezierPath.cubicTo(15.87f, 24.56f, 12.73f, 21.45f, 8.85f, 21.45f);
      bezierPath.close();

      paint.reset();
      paint.setFlags(Paint.ANTI_ALIAS_FLAG);
      bezierPath.setFillType(Path.FillType.EVEN_ODD);
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(bikeColor);
      canvas.drawPath(bezierPath, paint);

      // Bezier 2
      RectF bezier2Rect = CacheForIconbikeshare.bezier2Rect;
      bezier2Rect.set(8.12f, 13.28f, 31.64f, 29.62f);
      Path bezier2Path = CacheForIconbikeshare.bezier2Path;
      bezier2Path.reset();
      bezier2Path.moveTo(19.49f, 26.35f);
      bezier2Path.lineTo(16.7f, 18.18f);
      bezier2Path.lineTo(16.32f, 17.12f);
      bezier2Path.lineTo(17.65f, 17.12f);
      bezier2Path.cubicTo(18.04f, 17.12f, 18.35f, 16.81f, 18.35f, 16.43f);
      bezier2Path.cubicTo(18.35f, 16.04f, 18.04f, 15.73f, 17.65f, 15.73f);
      bezier2Path.lineTo(13.27f, 15.73f);
      bezier2Path.cubicTo(12.88f, 15.73f, 12.57f, 16.04f, 12.57f, 16.43f);
      bezier2Path.cubicTo(12.57f, 16.81f, 12.88f, 17.12f, 13.27f, 17.12f);
      bezier2Path.lineTo(14.66f, 17.12f);
      bezier2Path.lineTo(15.18f, 18.85f);
      bezier2Path.lineTo(8.55f, 27.15f);
      bezier2Path.cubicTo(8.32f, 27.45f, 8.12f, 27.85f, 8.12f, 28.04f);
      bezier2Path.lineTo(8.12f, 28.93f);
      bezier2Path.cubicTo(8.12f, 29.31f, 8.43f, 29.62f, 8.82f, 29.62f);
      bezier2Path.lineTo(18.6f, 29.62f);
      bezier2Path.cubicTo(18.99f, 29.62f, 19.49f, 29.37f, 19.73f, 29.07f);
      bezier2Path.lineTo(26.14f, 20.01f);
      bezier2Path.lineTo(29.78f, 28.42f);
      bezier2Path.lineTo(30.17f, 29.19f);
      bezier2Path.cubicTo(30.34f, 29.53f, 30.76f, 29.67f, 31.11f, 29.5f);
      bezier2Path.lineTo(31.25f, 29.42f);
      bezier2Path.cubicTo(31.6f, 29.25f, 31.74f, 28.84f, 31.57f, 28.5f);
      bezier2Path.lineTo(31.18f, 27.73f);
      bezier2Path.lineTo(25.44f, 14.55f);
      bezier2Path.cubicTo(25.14f, 13.85f, 24.26f, 13.28f, 23.49f, 13.28f);
      bezier2Path.lineTo(22.35f, 13.28f);
      bezier2Path.cubicTo(21.96f, 13.28f, 21.65f, 13.59f, 21.65f, 13.98f);
      bezier2Path.cubicTo(21.65f, 14.36f, 21.96f, 14.67f, 22.35f, 14.67f);
      bezier2Path.lineTo(23.09f, 14.67f);
      bezier2Path.cubicTo(23.48f, 14.67f, 23.92f, 14.95f, 24.07f, 15.3f);
      bezier2Path.lineTo(25.39f, 18.18f);
      bezier2Path.lineTo(19.49f, 26.35f);
      bezier2Path.close();
      bezier2Path.moveTo(10.07f, 27.98f);
      bezier2Path.lineTo(15.78f, 20.54f);
      bezier2Path.lineTo(18.34f, 27.98f);
      bezier2Path.lineTo(10.07f, 27.98f);
      bezier2Path.close();

      paint.reset();
      paint.setFlags(Paint.ANTI_ALIAS_FLAG);
      bezier2Path.setFillType(Path.FillType.EVEN_ODD);
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(bikeColor);
      canvas.drawPath(bezier2Path, paint);
    }

    canvas.restore();
  }

  private static class CacheForBikeShareMap {
    private static Paint paint = new Paint();
    private static RectF backgroundRect = new RectF();
    private static Path backgroundPath = new Path();
    private static RectF fillRect = new RectF();
    private static Path fillPath = new Path();
    private static RectF borderRect = new RectF();
    private static Path borderPath = new Path();
    private static RectF bikieRect = new RectF();
    private static RectF bikieTargetRect = new RectF();
  }

  public synchronized static void drawBikeShareMap(Canvas canvas, float fraction, float borderWidth, float length) {
    // General Declarations
    Paint paint = CacheForBikeShareMap.paint;

    // Local Colors
    int fullColor = Color.argb(255, 15, 173, 0);
    int emptyColor = Color.argb(255, 227, 0, 0);
    int backgroundColor = Color.argb(255, 74, 74, 74);
    int normalColor = Color.argb(255, 203, 219, 0);

    // Local Variables
    float fillStart = fraction == 1f ? 0f : (float) Math.asin(fraction * 2f - 1f) * 180f / (float) Math.PI;
    RectF frame = new RectF(borderWidth, borderWidth, borderWidth + length, borderWidth + length);
    int fillColor = fraction > 0.66f ? fullColor : (fraction > 0.33f ? normalColor : emptyColor);
    RectF bikeFrame = new RectF(borderWidth + length * 0.125f, borderWidth + length * 0.125f, borderWidth + length * 0.125f + length * 0.75f, borderWidth + length * 0.125f + length * 0.75f);
    float fillEnd = fraction == 1f ? 1f : 180f - (float) Math.asin(fraction * 2f - 1f) * 180f / (float) Math.PI;

    // Background
    RectF backgroundRect = CacheForBikeShareMap.backgroundRect;
    backgroundRect.set(frame.left, frame.top, frame.left + frame.width(), frame.top + frame.height());
    Path backgroundPath = CacheForBikeShareMap.backgroundPath;
    backgroundPath.reset();
    backgroundPath.addOval(backgroundRect, Path.Direction.CW);

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(backgroundColor);
    canvas.drawPath(backgroundPath, paint);

    // Fill
    RectF fillRect = CacheForBikeShareMap.fillRect;
    fillRect.set(frame.left, frame.top, frame.left + frame.width(), frame.top + frame.height());
    Path fillPath = CacheForBikeShareMap.fillPath;
    fillPath.reset();
    fillPath.addArc(fillRect, -fillStart, fillStart - fillEnd + (-fillEnd < -fillStart ? 360f * (float) Math.ceil((fillEnd - fillStart) / 360f) : 0f));

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(fillColor);
    canvas.drawPath(fillPath, paint);

    // Border
    RectF borderRect = CacheForBikeShareMap.borderRect;
    borderRect.set(frame.left, frame.top, frame.left + frame.width(), frame.top + frame.height());
    Path borderPath = CacheForBikeShareMap.borderPath;
    borderPath.reset();
    borderPath.addOval(borderRect, Path.Direction.CW);

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setStrokeWidth(borderWidth);
    paint.setStrokeMiter(10f);
    canvas.save();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(Color.BLACK);
    canvas.drawPath(borderPath, paint);
    canvas.restore();

    // Bikie
    RectF bikieRect = CacheForBikeShareMap.bikieRect;
    bikieRect.set(bikeFrame.left, bikeFrame.top, bikeFrame.left + bikeFrame.width(), bikeFrame.top + bikeFrame.height());
    canvas.save();
    canvas.clipRect(bikieRect);
    canvas.translate(bikieRect.left, bikieRect.top);
    RectF bikieTargetRect = CacheForBikeShareMap.bikieTargetRect;
    bikieTargetRect.set(0f, 0f, bikieRect.width(), bikieRect.height());
    TripGoStyleKit.drawIconbikeshare(canvas, bikieTargetRect, ResizingBehavior.Stretch);
    canvas.restore();
  }

  private static class CacheForIconcarshare {
    private static Paint paint = new Paint();
    private static RectF originalFrame = new RectF(0f, 0f, 32f, 32f);
    private static RectF resizedFrame = new RectF();
    private static RectF ovalRect = new RectF();
    private static Path ovalPath = new Path();
    private static RectF bezierRect = new RectF();
    private static Path bezierPath = new Path();
    private static RectF oval2Rect = new RectF();
    private static Path oval2Path = new Path();
    private static RectF bezier2Rect = new RectF();
    private static Path bezier2Path = new Path();
  }

  public synchronized static void drawIconcarshare(Canvas canvas) {
    TripGoStyleKit.drawIconcarshare(canvas, new RectF(0f, 0f, 32f, 32f), ResizingBehavior.AspectFit);
  }

  public synchronized static void drawIconcarshare(Canvas canvas, RectF targetFrame, ResizingBehavior resizing) {
    // General Declarations
    Paint paint = CacheForIconcarshare.paint;

    // Local Colors
    int fillColor4 = Color.argb(255, 255, 255, 255);

    // Resize to Target Frame
    canvas.save();
    RectF resizedFrame = CacheForIconcarshare.resizedFrame;
    TripGoStyleKit.resizingBehaviorApply(resizing, CacheForIconcarshare.originalFrame, targetFrame, resizedFrame);
    canvas.translate(resizedFrame.left, resizedFrame.top);
    canvas.scale(resizedFrame.width() / 32f, resizedFrame.height() / 32f);

    // Oval
    RectF ovalRect = CacheForIconcarshare.ovalRect;
    ovalRect.set(24f, 19f, 29f, 24f);
    Path ovalPath = CacheForIconcarshare.ovalPath;
    ovalPath.reset();
    ovalPath.addOval(ovalRect, Path.Direction.CW);

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(fillColor4);
    canvas.drawPath(ovalPath, paint);

    // Bezier
    RectF bezierRect = CacheForIconcarshare.bezierRect;
    bezierRect.set(0f, 10.06f, 32f, 22.86f);
    Path bezierPath = CacheForIconcarshare.bezierPath;
    bezierPath.reset();
    bezierPath.moveTo(9.27f, 22.29f);
    bezierPath.cubicTo(9.26f, 22.6f, 9.52f, 22.86f, 9.83f, 22.86f);
    bezierPath.lineTo(22.29f, 22.86f);
    bezierPath.cubicTo(22.6f, 22.86f, 22.86f, 22.6f, 22.86f, 22.29f);
    bezierPath.lineTo(22.86f, 21.14f);
    bezierPath.cubicTo(23.02f, 19.22f, 24.62f, 17.71f, 26.57f, 17.71f);
    bezierPath.cubicTo(28.53f, 17.71f, 30.14f, 19.22f, 30.29f, 21.14f);
    bezierPath.cubicTo(30.29f, 21.14f, 30.29f, 21.47f, 30.29f, 21.79f);
    bezierPath.cubicTo(30.29f, 22.1f, 30.52f, 22.26f, 30.81f, 22.13f);
    bezierPath.lineTo(31.43f, 21.86f);
    bezierPath.cubicTo(31.74f, 21.71f, 32f, 21.29f, 32f, 20.94f);
    bezierPath.lineTo(32f, 18.9f);
    bezierPath.cubicTo(32f, 18.55f, 31.91f, 17.99f, 31.8f, 17.66f);
    bezierPath.lineTo(31.12f, 16.63f);
    bezierPath.cubicTo(30.93f, 16.33f, 30.49f, 16.05f, 30.14f, 16f);
    bezierPath.lineTo(22.29f, 14.86f);
    bezierPath.lineTo(18.3f, 10.87f);
    bezierPath.cubicTo(17.85f, 10.42f, 16.98f, 10.06f, 16.35f, 10.06f);
    bezierPath.lineTo(4.35f, 10.06f);
    bezierPath.cubicTo(3.64f, 10.06f, 2.81f, 10.57f, 2.49f, 11.2f);
    bezierPath.lineTo(0.29f, 16.16f);
    bezierPath.cubicTo(0.13f, 16.47f, 0f, 17.02f, 0f, 17.37f);
    bezierPath.lineTo(0f, 20f);
    bezierPath.cubicTo(0f, 20.32f, 0.14f, 20.78f, 0.32f, 21.05f);
    bezierPath.lineTo(0.83f, 21.81f);
    bezierPath.cubicTo(1f, 22.07f, 1.06f, 22.13f, 1.38f, 22.13f);
    bezierPath.lineTo(1.38f, 22.13f);
    bezierPath.cubicTo(1.69f, 22.13f, 1.83f, 22.03f, 1.83f, 21.71f);
    bezierPath.lineTo(1.83f, 21.14f);
    bezierPath.cubicTo(1.99f, 19.22f, 3.59f, 17.71f, 5.55f, 17.71f);
    bezierPath.cubicTo(7.5f, 17.71f, 9.12f, 19.22f, 9.26f, 21.14f);
    bezierPath.cubicTo(9.27f, 21.14f, 9.27f, 21.79f, 9.27f, 22.29f);
    bezierPath.close();
    bezierPath.moveTo(8f, 14.73f);
    bezierPath.cubicTo(8f, 14.89f, 7.87f, 15.02f, 7.72f, 15.02f);
    bezierPath.lineTo(3.14f, 15.02f);
    bezierPath.cubicTo(2.99f, 15.02f, 2.91f, 14.9f, 2.97f, 14.75f);
    bezierPath.lineTo(4.35f, 11.95f);
    bezierPath.cubicTo(4.47f, 11.66f, 4.83f, 11.43f, 5.14f, 11.43f);
    bezierPath.lineTo(7.72f, 11.43f);
    bezierPath.cubicTo(7.87f, 11.43f, 8f, 11.56f, 8f, 11.71f);
    bezierPath.lineTo(8f, 14.73f);
    bezierPath.close();
    bezierPath.moveTo(19.71f, 15.02f);
    bezierPath.lineTo(10.29f, 15.02f);
    bezierPath.cubicTo(10.13f, 15.02f, 10f, 14.89f, 10f, 14.73f);
    bezierPath.lineTo(10f, 11.71f);
    bezierPath.cubicTo(10f, 11.56f, 10.13f, 11.43f, 10.29f, 11.43f);
    bezierPath.lineTo(16f, 11.43f);
    bezierPath.cubicTo(16.32f, 11.43f, 16.74f, 11.62f, 16.94f, 11.86f);
    bezierPath.lineTo(19.81f, 14.8f);
    bezierPath.cubicTo(19.92f, 14.92f, 19.87f, 15.02f, 19.71f, 15.02f);
    bezierPath.close();

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    bezierPath.setFillType(Path.FillType.EVEN_ODD);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(fillColor4);
    canvas.drawPath(bezierPath, paint);

    // Oval 2
    RectF oval2Rect = CacheForIconcarshare.oval2Rect;
    oval2Rect.set(3f, 19f, 8f, 24f);
    Path oval2Path = CacheForIconcarshare.oval2Path;
    oval2Path.reset();
    oval2Path.addOval(oval2Rect, Path.Direction.CW);

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(fillColor4);
    canvas.drawPath(oval2Path, paint);

    // Bezier 2
    RectF bezier2Rect = CacheForIconcarshare.bezier2Rect;
    bezier2Rect.set(21.71f, 8f, 32f, 12.57f);
    Path bezier2Path = CacheForIconcarshare.bezier2Path;
    bezier2Path.reset();
    bezier2Path.moveTo(29.71f, 8f);
    bezier2Path.cubicTo(28.87f, 8f, 28.14f, 8.46f, 27.75f, 9.14f);
    bezier2Path.lineTo(22.86f, 9.14f);
    bezier2Path.lineTo(21.71f, 9.72f);
    bezier2Path.lineTo(21.71f, 10.57f);
    bezier2Path.lineTo(22.86f, 11.43f);
    bezier2Path.lineTo(23.42f, 10.86f);
    bezier2Path.lineTo(23.99f, 10.86f);
    bezier2Path.lineTo(24.57f, 11.43f);
    bezier2Path.lineTo(25.14f, 11.43f);
    bezier2Path.lineTo(25.71f, 10.86f);
    bezier2Path.lineTo(26.28f, 10.86f);
    bezier2Path.lineTo(26.86f, 11.43f);
    bezier2Path.lineTo(27.74f, 11.43f);
    bezier2Path.cubicTo(28.14f, 12.11f, 28.87f, 12.57f, 29.71f, 12.57f);
    bezier2Path.cubicTo(30.98f, 12.57f, 32f, 11.55f, 32f, 10.29f);
    bezier2Path.cubicTo(32f, 9.02f, 30.98f, 8f, 29.71f, 8f);
    bezier2Path.close();
    bezier2Path.moveTo(30.29f, 10.86f);
    bezier2Path.cubicTo(29.97f, 10.86f, 29.71f, 10.6f, 29.71f, 10.29f);
    bezier2Path.cubicTo(29.71f, 9.97f, 29.97f, 9.71f, 30.29f, 9.71f);
    bezier2Path.cubicTo(30.6f, 9.71f, 30.86f, 9.97f, 30.86f, 10.29f);
    bezier2Path.cubicTo(30.86f, 10.6f, 30.6f, 10.86f, 30.29f, 10.86f);
    bezier2Path.close();

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(fillColor4);
    canvas.drawPath(bezier2Path, paint);

    canvas.restore();
  }

  private static class CacheForCarShareMap {
    private static Paint paint = new Paint();
    private static RectF backgroundRect = new RectF();
    private static Path backgroundPath = new Path();
    private static RectF fillRect = new RectF();
    private static Path fillPath = new Path();
    private static RectF borderRect = new RectF();
    private static Path borderPath = new Path();
    private static RectF bikieRect = new RectF();
    private static RectF bikieTargetRect = new RectF();
  }

  public synchronized static void drawCarShareMap(Canvas canvas, float fraction, float borderWidth, float length) {
    // General Declarations
    Paint paint = CacheForCarShareMap.paint;

    // Local Colors
    int fullColor = Color.argb(255, 15, 173, 0);
    int emptyColor = Color.argb(255, 227, 0, 0);
    int backgroundColor = Color.argb(255, 74, 74, 74);
    int normalColor = Color.argb(255, 203, 219, 0);

    // Local Variables
    float fillStart = fraction == 1f ? 0f : (float) Math.asin(fraction * 2f - 1f) * 180f / (float) Math.PI;
    RectF frame = new RectF(borderWidth, borderWidth, borderWidth + length, borderWidth + length);
    int fillColor = fraction > 0.66f ? fullColor : (fraction > 0.33f ? normalColor : emptyColor);
    RectF bikeFrame = new RectF(borderWidth + length * 0.125f, borderWidth + length * 0.125f, borderWidth + length * 0.125f + length * 0.75f, borderWidth + length * 0.125f + length * 0.75f);
    float fillEnd = fraction == 1f ? 1f : 180f - (float) Math.asin(fraction * 2f - 1f) * 180f / (float) Math.PI;

    // Background
    RectF backgroundRect = CacheForCarShareMap.backgroundRect;
    backgroundRect.set(frame.left, frame.top, frame.left + frame.width(), frame.top + frame.height());
    Path backgroundPath = CacheForCarShareMap.backgroundPath;
    backgroundPath.reset();
    backgroundPath.addOval(backgroundRect, Path.Direction.CW);

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(backgroundColor);
    canvas.drawPath(backgroundPath, paint);

    // Fill
    RectF fillRect = CacheForCarShareMap.fillRect;
    fillRect.set(frame.left, frame.top, frame.left + frame.width(), frame.top + frame.height());
    Path fillPath = CacheForCarShareMap.fillPath;
    fillPath.reset();
    fillPath.addArc(fillRect, -fillStart, fillStart - fillEnd + (-fillEnd < -fillStart ? 360f * (float) Math.ceil((fillEnd - fillStart) / 360f) : 0f));

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(fillColor);
    canvas.drawPath(fillPath, paint);

    // Border
    RectF borderRect = CacheForCarShareMap.borderRect;
    borderRect.set(frame.left, frame.top, frame.left + frame.width(), frame.top + frame.height());
    Path borderPath = CacheForCarShareMap.borderPath;
    borderPath.reset();
    borderPath.addOval(borderRect, Path.Direction.CW);

    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setStrokeWidth(borderWidth);
    paint.setStrokeMiter(10f);
    canvas.save();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(Color.BLACK);
    canvas.drawPath(borderPath, paint);
    canvas.restore();

    // Bikie
    RectF bikieRect = CacheForCarShareMap.bikieRect;
    bikieRect.set(bikeFrame.left, bikeFrame.top, bikeFrame.left + bikeFrame.width(), bikeFrame.top + bikeFrame.height());
    canvas.save();
    canvas.clipRect(bikieRect);
    canvas.translate(bikieRect.left, bikieRect.top);
    RectF bikieTargetRect = CacheForCarShareMap.bikieTargetRect;
    bikieTargetRect.set(0f, 0f, bikieRect.width(), bikieRect.height());
    TripGoStyleKit.drawIconcarshare(canvas, bikieTargetRect, ResizingBehavior.Stretch);
    canvas.restore();
  }

  // Resizing Behavior
  public static void resizingBehaviorApply(ResizingBehavior behavior, RectF rect, RectF target, RectF result) {
    if (rect.equals(target) || target == null) {
      result.set(rect);
      return;
    }

    if (behavior == ResizingBehavior.Stretch) {
      result.set(target);
      return;
    }

    float xRatio = Math.abs(target.width() / rect.width());
    float yRatio = Math.abs(target.height() / rect.height());
    float scale = 0f;

    switch (behavior) {
      case AspectFit: {
        scale = Math.min(xRatio, yRatio);
        break;
      }
      case AspectFill: {
        scale = Math.max(xRatio, yRatio);
        break;
      }
      case Center: {
        scale = 1f;
        break;
      }
    }

    float newWidth = Math.abs(rect.width() * scale);
    float newHeight = Math.abs(rect.height() * scale);
    result.set(target.centerX() - newWidth / 2,
               target.centerY() - newHeight / 2,
               target.centerX() + newWidth / 2,
               target.centerY() + newHeight / 2);
  }

}