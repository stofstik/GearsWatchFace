/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.stofstik.sprocketwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;

/*TODO
Companion app
Color chooser
Different gears
Wheels
Pulleys

 */

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class SprocketWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    // private static final long INTERACTIVE_UPDATE_RATE_MS = 66;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        Paint mBackgroundPaint;
        Paint mHourHandPaint, mAmbientHrHandPaint;
        Paint mMinHandPaint, mAmbientMinHandPaint;
        Paint mHourDotPaint, mAmbientHourDotPaint;
        Paint mSecHandPaint;
        Paint mBitmapPaint;
        Bitmap mBitmapMediumGear, mBitmapBigGear;

        boolean mAmbient;
        Calendar mCalendar;

        private static final float TWO_PI = (float) Math.PI * 2f;

        // final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SprocketWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = SprocketWatchFace.this.getResources();

            // PAINT
            // Background
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.analog_background));

            // Hour hand
            mHourHandPaint = new Paint();
            mHourHandPaint.setColor(resources.getColor(R.color.analog_hour_hand));
            mHourHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hour_hand_stroke));
            mHourHandPaint.setAntiAlias(true);
            mHourHandPaint.setStrokeCap(Paint.Cap.ROUND);
            mAmbientHrHandPaint = new Paint();
            mAmbientHrHandPaint.setColor(resources.getColor(R.color.analog_ambient_hour_hand));
            mAmbientHrHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hour_ambient_hand_stroke));
            mAmbientHrHandPaint.setAntiAlias(false);
            mAmbientHrHandPaint.setStrokeCap(Paint.Cap.ROUND);

            // Minute hand
            mMinHandPaint = new Paint();
            mMinHandPaint.setColor(resources.getColor(R.color.analog_minute_hand));
            mMinHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_minute_hand_stroke));
            mMinHandPaint.setAntiAlias(true);
            mMinHandPaint.setStrokeCap(Paint.Cap.ROUND);
            mAmbientMinHandPaint = new Paint();
            mAmbientMinHandPaint.setColor(resources.getColor(R.color.analog_ambient_minute_hand));
            mAmbientMinHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_minute_ambient_hand_stroke));
            mAmbientMinHandPaint.setAntiAlias(false);
            mAmbientMinHandPaint.setStrokeCap(Paint.Cap.ROUND);

            // Second hand
            mSecHandPaint = new Paint();
            mSecHandPaint.setColor(resources.getColor(R.color.analog_second_hand));
            mSecHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_second_hand_stroke));
            mSecHandPaint.setAntiAlias(true);
            mSecHandPaint.setStrokeCap(Paint.Cap.ROUND);

            // Hour dot markers
            mHourDotPaint = new Paint();
            mHourDotPaint.setColor(resources.getColor(R.color.analog_hour_markers));
            mHourDotPaint.setAntiAlias(true);
            mAmbientHourDotPaint = new Paint();
            mAmbientHourDotPaint.setColor(resources.getColor(R.color.analog_hour_markers));
            mAmbientHourDotPaint.setAntiAlias(false);

            mBitmapPaint = new Paint();
            mBitmapPaint.setFilterBitmap(true);
            mBitmapPaint.setDither(true);

            // Set up our bitmaps
            Drawable drawable;
            drawable = resources.getDrawable(R.drawable.medium_plus_small_gear, null);
            mBitmapMediumGear = ((BitmapDrawable) drawable).getBitmap();
            mBitmapMediumGear = mBitmapMediumGear.createScaledBitmap(mBitmapMediumGear, 168, 168, true);
            drawable = resources.getDrawable(R.drawable.big_gear, null);
            mBitmapBigGear = ((BitmapDrawable) drawable).getBitmap();
            mBitmapBigGear = mBitmapBigGear.createScaledBitmap(mBitmapBigGear, 283, 283, true);

            // Declare the calendar!
            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    // mHrMinHandPaint.setAntiAlias(!inAmbientMode);
                }
            }
            invalidate();
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            //updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background.
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            // Compute rotations
            float seconds = mCalendar.get(Calendar.SECOND) +
                    mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float secTick = mCalendar.get(Calendar.SECOND) / 60f * TWO_PI;
            // float secRot = seconds / 60f * TWO_PI;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float minRot = minutes / 60f * TWO_PI;
            float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;
            float hrRot = hours / 12f * TWO_PI;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            // If not in ambient mode, do all the fancy stuffs!
            if (!mAmbient) {
                // center of bitmap
                float bitmapCenterX;
                float bitmapCenterY;
                // offset
                float bitmapOffsetX;
                // position to draw bitmap
                float bitmapPosX;
                float bitmapPosY;

                // MEDIUM GEAR RIGHT:
                bitmapCenterX = mBitmapMediumGear.getWidth() / 2;
                bitmapCenterY = mBitmapMediumGear.getHeight() / 2;
                bitmapOffsetX = 160;
                bitmapPosX = centerX - bitmapCenterX + bitmapOffsetX;
                bitmapPosY = centerY - bitmapCenterY;
                canvas.save();
                canvas.rotate(seconds * -30, centerX + bitmapOffsetX, centerY);
                canvas.drawBitmap(mBitmapMediumGear, bitmapPosX, bitmapPosY, mBitmapPaint);
                canvas.restore();

                // MEDIUM GEAR LEFT:
                bitmapPosX = centerX - bitmapCenterX - bitmapOffsetX;
                canvas.save();
                canvas.rotate(seconds * -30, centerX - bitmapOffsetX, centerY);
                canvas.drawBitmap(mBitmapMediumGear, bitmapPosX, bitmapPosY, mBitmapPaint);
                canvas.restore();

                // BIG GEAR:
                bitmapCenterX = mBitmapBigGear.getWidth() / 2;
                bitmapCenterY = mBitmapBigGear.getHeight() / 2;
                bitmapPosX = centerX - bitmapCenterX;
                bitmapPosY = centerY - bitmapCenterY;
                canvas.save();
                canvas.rotate(seconds * 6, centerX, centerY);
                canvas.drawBitmap(mBitmapBigGear, bitmapPosX, bitmapPosY, mBitmapPaint);
                canvas.restore();

                // Draw hour dots
                for (int i = 0; i < 60; i += 5) {
                    float dotX = (float) Math.sin(i / 60f * TWO_PI) * (centerX);
                    float dotY = (float) -Math.cos(i / 60f * TWO_PI) * (centerX);
                    canvas.drawCircle(centerX + dotX, centerY + dotY, 7f, mHourDotPaint);
                }

                // Draw minute hand
                float minX = (float) Math.sin(minRot) * minLength;
                float minY = (float) -Math.cos(minRot) * minLength;
                canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mMinHandPaint);

                // Draw hour hand
                float hrX = (float) Math.sin(hrRot) * hrLength;
                float hrY = (float) -Math.cos(hrRot) * hrLength;
                canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHourHandPaint);

                // Draw second hand
                float secX = (float) Math.sin(secTick) * secLength;
                float secY = (float) -Math.cos(secTick) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mSecHandPaint);
                canvas.drawCircle(centerX + secX, centerY + secY, 5, mSecHandPaint);

            } else {
                // We are in ambient mode. Don't do fancy stuff plz!

                // Minute hand
                float minX = (float) Math.sin(minRot) * minLength;
                float minY = (float) -Math.cos(minRot) * minLength;
                canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mAmbientMinHandPaint);

                // Hour hand
                float hrX = (float) Math.sin(hrRot) * hrLength;
                float hrY = (float) -Math.cos(hrRot) * hrLength;
                canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mAmbientHrHandPaint);

                // Hour dots
                for (int i = 0; i < 60; i += 5) {
                    float dotX = (float) Math.sin(i / 60f * TWO_PI) * (centerX - 7);
                    float dotY = (float) -Math.cos(i / 60f * TWO_PI) * (centerX - 7);
                    canvas.drawCircle(centerX + dotX, centerY + dotY, 7f, mAmbientHourDotPaint);
                }
            }
            // Draw every frame as long as we're visible and in interactive mode.
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
            }
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            // updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SprocketWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SprocketWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }
    }
}
/**
 * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
 * or stops it if it shouldn't be running but currently is.
 * <p/>
 * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
 * only run when we're visible and in interactive mode.
 * <p/>
 * Handle updating the time periodically in interactive mode.
 *//*
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        *//**
 * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
 * only run when we're visible and in interactive mode.
 *//*
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        *//**
 * Handle updating the time periodically in interactive mode.
 *//*
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SprocketWatchFace.Engine> mWeakReference;

        public EngineHandler(SprocketWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SprocketWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
*/
