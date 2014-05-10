/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage.sample.activity;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.GINGERBREAD;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage.OnPictureSavedListener;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.WaterRippleEffect;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools.FilterAdjuster;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools.OnGpuImageFilterChosenListener;
import jp.co.cyberagent.android.gpuimage.sample.utils.CameraHelperBase;
import jp.co.cyberagent.android.gpuimage.sample.utils.CameraHelperGB;
import jp.co.cyberagent.android.gpuimage.sample.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class ActivityGallery extends Activity implements OnSeekBarChangeListener,
        OnClickListener, OnPictureSavedListener {

    private static final int REQUEST_PICK_IMAGE = 1;
	protected static final String DEBUG_TAG = ActivityGallery.class.getName();
    private GPUImageFilter mFilter;
    private FilterAdjuster mFilterAdjuster;
    private GPUImageView mGPUImageView;
    private Point mWindowSize;

    @SuppressLint("NewApi")
	@Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        //Get height, width
        mWindowSize = new Point();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
        	getWindowManager().getDefaultDisplay().getSize(mWindowSize);
        } else {
        	Display display = getWindowManager().getDefaultDisplay();
        	mWindowSize.x = display.getWidth();
        	mWindowSize.y = display.getHeight();
        }
        Log.v(DEBUG_TAG, "Window Size  = " + mWindowSize.x + "  = " + mWindowSize.y);
        //((SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(this);
        //findViewById(R.id.button_choose_filter).setOnClickListener(this);
        //findViewById(R.id.button_save).setOnClickListener(this);

        mGPUImageView = (GPUImageView) findViewById(R.id.gpuimage);

        //Drawable water = getResources().getDrawable(R.drawable.water_over_coral_reef_303786);
        //BitmapDrawable = getResources().getDrawable(R.drawable.water_over_coral_reef_303786);
        Bitmap water = BitmapFactory.decodeResource(getResources(), R.drawable.water_over_coral_reef_303786);
        mGPUImageView.setImage(water);

        /*Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE);*/

        switchFilterTo( new WaterRippleEffect(mWindowSize.x, mWindowSize.y));

        mGPUImageView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = MotionEventCompat.getActionMasked(event);

			    switch(action) {
			        case (MotionEvent.ACTION_DOWN) :
			            Log.d(DEBUG_TAG,"Action was DOWN X = " + event.getX() + " | Y =" + event.getY());
			        	//if(mFilter.getClass() == WaterRippleEffect.class) {
					try {
						float points[] = convertFromAndroiSpaceToOpenGL(event.getX(), event.getY());
						((WaterRippleEffect)mFilter).setTouches(points[0], points[1]);
					} catch (NullPointerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			        	//}

			            return true;
			        case (MotionEvent.ACTION_MOVE) :
			            Log.d(DEBUG_TAG,"Action was MOVE");
			            return true;
			        case (MotionEvent.ACTION_UP) :
			            Log.d(DEBUG_TAG,"Action was UP");
			            return true;
			        case (MotionEvent.ACTION_CANCEL) :
			            Log.d(DEBUG_TAG,"Action was CANCEL");
			            return true;
			        case (MotionEvent.ACTION_OUTSIDE) :
			            Log.d(DEBUG_TAG,"Movement occurred outside bounds " +
			                    "of current screen element");
			            return true;
			        default :
			            return true;
			    }
			}
		});

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    handleImage(data.getData());
                } else {
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.button_choose_filter:
                GPUImageFilterTools.showDialog(this, new OnGpuImageFilterChosenListener() {

                    @Override
                    public void onGpuImageFilterChosenListener(final GPUImageFilter filter) {
                        switchFilterTo(filter);
                        mGPUImageView.requestRender();
                    }

                });
                break;
            /*case R.id.button_save:
                saveImage();
                break;
             */
            default:
                break;
        }

    }

    @Override
    public void onPictureSaved(final Uri uri) {
        Toast.makeText(this, "Saved: " + uri.toString(), Toast.LENGTH_SHORT).show();
    }

    private void saveImage() {
        String fileName = System.currentTimeMillis() + ".jpg";
        mGPUImageView.saveToPictures("GPUImage", fileName, this);
    }

    private void switchFilterTo(final GPUImageFilter filter) {
        if (mFilter == null
                || (filter != null && !mFilter.getClass().equals(filter.getClass()))) {
            mFilter = filter;
            mGPUImageView.setFilter(mFilter);
            mFilterAdjuster = new FilterAdjuster(mFilter);
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if (mFilterAdjuster != null) {
            mFilterAdjuster.adjust(progress);
        }
        mGPUImageView.requestRender();
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
    }

    private void handleImage(final Uri selectedImage) {
        mGPUImageView.setImage(selectedImage);
    }

    private float[] convertFromAndroiSpaceToOpenGL(float x, float y) {

    	float newX, newY;
    	float halfWidth = mWindowSize.x / 2;
    	float halfHeight = mWindowSize.y / 2;
    	newX = x - halfWidth;
    	newY = y - halfHeight;
    	if(newX != 0) {
    		newX = newX / halfWidth;
    	}
    	if(newY != 0) {
    		newY = newY / halfHeight;
    	}
    	newX *= -1;
    	Log.v(DEBUG_TAG ,"newX = " + newX + "| newY = " + newY);
    	return new float[] {newX, newY};
    }
}
