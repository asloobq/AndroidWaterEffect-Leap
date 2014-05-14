package jp.co.cyberagent.android.gpuimage;

import java.nio.FloatBuffer;
import java.util.Random;
import java.util.Vector;

import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;

public class WaterRippleEffect extends GPUImageFilter {

	/*
	 * Do one without touch, just use the center of the screen to show ripples.
	 * */
	
	/*
	 * gl_FragCoord = 
	 *  is an input variable that contains the window relative coordinate (x, y, z, 1/w)
	 *  values for the fragment.
	 * length = 
	 *  calculate the length of a vector ie. sqrt( x[0]^2 + x[1]^2 + .... ) 
	 * */
	public static final String WATER_RIPPLE_FRAGMENT_SHADER = "" +
			" \n" +
			" uniform highp float time;\n" +
			" uniform highp vec2 resolution;\n" +
			" uniform highp vec2 touch;\n" +
			" uniform sampler2D inputImageTexture;\n" +
			" \n" +
			" void main(void) {\n" +
			"		highp vec2 cPos = -1.0 + 2.0 * gl_FragCoord.xy / resolution.xy;\n" +
			"		cPos.x = cPos.x + touch.x; cPos.y = cPos.y + touch.y;" +
			"		highp float cLength = length(cPos*time);\n" +
			"		\n" +
			"		highp vec2 uv = gl_FragCoord.xy/resolution.xy+(cPos/cLength)*cos(cLength*12.0-time*4.0)*0.03;\n" +
			"		uv.y =1.0f - uv.y;\n" +
			"		highp vec3 col = texture2D(inputImageTexture,uv).xyz;\n" +
			"		\n" +
			"		gl_FragColor = vec4(col,1.0);\n" +
			" }";

	/*public static final String WATER_RIPPLE_FRAGMENT_SHADER = "" +
			" \n" +
			" uniform float time;\n" +
			" uniform vec2 resolution;\n" +
			" uniform sampler2D inputImageTexture;\n" +
			" \n" +
			" void main(void) {\n" +
			"		vec2 tc = gl_FragCoord.xy;\n" +
			"		vec2 p = -1.0 + 2.0 * tc;\n" +
			"		float len = length(p);\n" +
			"		\n" +
			"		vec2 uv = tc + (p/len) * cos(len*12.0 - time*4.0) * 0.03;\n" +
			"		vec3 col = texture2D(inputImageTexture,uv).xyz;\n" +
			"		\n" +
			"		gl_FragColor = vec4(col,1.0);\n" +
			" }";*/
	/*public static final String WATER_RIPPLE_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" + 
            " \n" + 
            " uniform sampler2D inputImageTexture;\n" + 
            " uniform lowp float contrast;\n" + 
            " \n" + 
            " void main()\n" + 
            " {\n" + 
            "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" + 
            "     \n" + 
            "     gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), textureColor.w);\n" + 
            " }";*/
	
	private long mStartTime;
	private float mTimeUniform;
	private int mTimeUniformLocation;
	
	private float mResUniformX;
	private float mResUniformY;
	private int mResUniformLocation;
	
	private float mTouchUniformX;
	private float mTouchUniformY;
	private int mTouchUniformLocation;
	
	private int count = 0;
	private int mTimer = 1000;
	private Random mRandom = new Random();
	
	private int mContrastLocation;
    private float mContrast;
	
	public WaterRippleEffect(float resX, float resY) {
		super(NO_FILTER_VERTEX_SHADER, WATER_RIPPLE_FRAGMENT_SHADER);
			mResUniformX = resX;
			mResUniformY = resY;
			mContrast = 1.8f;
			mTouchUniformX = 0.0f;
			mTouchUniformY = 0.0f;
	}

	@Override
	public void onInit() {
		super.onInit();
		//Uniform names initialized here
		mTimeUniformLocation = GLES20.glGetUniformLocation(getProgram(), "time");
		mResUniformLocation  = GLES20.glGetUniformLocation(getProgram(), "resolution");
		mTouchUniformLocation  = GLES20.glGetUniformLocation(getProgram(), "touch");
//		mContrastLocation = GLES20.glGetUniformLocation(getProgram(), "contrast");
		
		mStartTime = 0;//System.currentTimeMillis();
	}
	
	@Override
	public void onInitialized() {
		super.onInitialized();
		setUniforms();
	}


	@Override
	public void onDraw(int textureId, FloatBuffer cubeBuffer,
			FloatBuffer textureBuffer) {
		super.onDraw(textureId, cubeBuffer, textureBuffer);
		setUniforms();
	}
	
	public void setUniforms() {
		//mTimeUniform = Math.abs((mStartTime - System.currentTimeMillis())) / 1000;
		mTimeUniform = (System.currentTimeMillis() - mStartTime) / 100;
//		mTimeUniform = mRandom.nextFloat();
//		mTimeUniform *= mTimer;
//		if(mTimer > 0) {
//			--mTimer;
//		}
		setTime(mTimeUniform);
		setResolution(mResUniformX, mResUniformY);
		if (++count % 100 == 0) {
			Log.v(WaterRippleEffect.class.getName(), "setUniforms time " + System.currentTimeMillis() 
					+ "| time = " + mStartTime + "| time = "+ mTimeUniform);
		}
//		setFloat(mContrastLocation, mContrast);
	}
	
	public void setTime(final float time) {
		setFloat(mTimeUniformLocation, time);
	}
	
	public void setResolution(final float resX, final float resY) {
		setFloatVec2(mResUniformLocation, new float[] {resX, resY});
	}
	
	public void setTouches(final float touchX, final float touchY) {
		 mStartTime = System.currentTimeMillis();
		 setFloatVec2(mTouchUniformLocation, new float[] {touchX, touchY});
	}
}
