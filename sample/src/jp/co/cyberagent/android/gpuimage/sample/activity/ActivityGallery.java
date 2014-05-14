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

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage.OnPictureSavedListener;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.WaterRippleEffect;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools.FilterAdjuster;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools.OnGpuImageFilterChosenListener;
import jp.co.cyberagent.android.gpuimage.sample.R;
import jp.co.cyberagent.android.gpuimage.sample.service.MessengerService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class ActivityGallery extends Activity implements OnSeekBarChangeListener,
        OnClickListener, OnPictureSavedListener {

    private static final int REQUEST_PICK_IMAGE = 1;
    private GPUImageFilter mFilter;
    private FilterAdjuster mFilterAdjuster;
    private GPUImageView mGPUImageView;
    protected static final String DEBUG_TAG = ActivityGallery.class.getName();
    private Point mWindowSize;
	private Messenger mService = null;
	private boolean mIsBound;

	private static Point mLeapSize = new Point(250, 500);

    @SuppressLint("NewApi")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
//        ((SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(this);
//        findViewById(R.id.button_choose_filter).setOnClickListener(this);
//        findViewById(R.id.button_save).setOnClickListener(this);
		doBindService();
        mGPUImageView = (GPUImageView) findViewById(R.id.gpuimage);

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE);
        
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
							float points[] = convertFromAndroidSpaceToOpenGL(event.getX(), event.getY());
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
//            case R.id.button_save:
//                saveImage();
//                break;

            default:
                break;
        }

    }

    @Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
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

    private float[] convertFromAndroidSpaceToOpenGL(float x, float y) {

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

    private static float[] convertFromLeapSpaceToOpenGL(float x, float y) {

    	float newX = 0, newY;
    	if(x != 0) {
    		newX = x / mLeapSize.x;
    	}

    	float halfHeight = (mLeapSize.y / 2);

    	newY = y - halfHeight;
    	if(newY != 0) {
    		newY = newY / halfHeight;
    	}
    	newX *= -1;
    	newY *= -1;
    	Log.v(DEBUG_TAG ,"newX = " + newX + "| newY = " + newY + "| y = " + y + "| halfheight = " + halfHeight);
    	return new float[] {newX, newY};
    }

    /**
	 * Handler of incoming messages from service.
	 */
	static class IncomingHandler extends Handler {
		private ActivityGallery mActivityGallery;

		public IncomingHandler(ActivityGallery activityGallery) {
			super();
			this.mActivityGallery = activityGallery;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MessengerService.MSG_SET_VALUE:
				float[] reading = (float[]) msg.obj;
				//TODO do something with values
				//mRenderer.setValues(reading[0], reading[1], reading[2], reading[3]);
				try {
					float points[] = convertFromLeapSpaceToOpenGL(reading[0], reading[1]);
					((WaterRippleEffect) mActivityGallery.mFilter).setTouches(points[0], points[1]);
				} catch (NullPointerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler(this));

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);
			Log.d("TAG", "Attached.");

			// We want to monitor the service for as long as we are
			// connected to it.
			try {
				Message msg = Message.obtain(null,
						MessengerService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			Log.d("TAG", "Disconnected.");
		}
	};

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		bindService(new Intent(ActivityGallery.this,
				MessengerService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		Log.d("TAG", "Binding.");
	}

	void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							MessengerService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}

			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
			Log.d("TAG", "Unbinding.");
		}
	}

}
