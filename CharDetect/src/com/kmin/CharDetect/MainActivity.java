package com.kmin.CharDetect;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.opencv.android.*;
import android.util.*;
import android.view.View.*;
import android.content.*;
import android.net.*;
import android.graphics.*;
import java.io.*;

public class MainActivity extends Activity
{
	private final String TAG = "opencv load";
	private String tip_msg = "";
	
	private int min_height = -1;
	private int max_height = -1;
	private int min_width = -1;
	
	Button btn_si,btn_proc;
	ImageView iv_src,iv_ret;
	TextView tv_msg;
	EditText et_min_height,et_max_height,et_min_width;
	private ProgressBar progressBar;
	
	public ProcTask mTask;
	
	Bitmap src_bmp,ret_bmp;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) 
	{
		public void onManagerConnected(int status) 
		{
			switch (status) 
			{
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(TAG, "OpenCV loaded successfully");
				} 
				break;
				default:
				{
					super.onManagerConnected(status);
				} 
				break;
			}
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
		initUI();
    }
	//界面初始化
	public void initUI()
	{
		iv_src = (ImageView)findViewById(R.id.iv_src);
		iv_ret = (ImageView)findViewById(R.id.iv_ret);
		tv_msg = (TextView)findViewById(R.id.tv_msg);
		
		et_max_height = (EditText)findViewById(R.id.et_max_height);
		et_min_height = (EditText)findViewById(R.id.et_min_height);
		et_min_width = (EditText)findViewById(R.id.et_min_width);
		
		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
		
		btn_si = (Button)findViewById(R.id.btn_si);
		btn_proc = (Button)findViewById(R.id.btn_proc);
		
		btn_si.setOnClickListener(new BtnClickListener());
		btn_proc.setOnClickListener(new BtnClickListener());
	}
	
	//按钮点击
	public class BtnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View p1)
		{
			// TODO: Implement this method
			switch(p1.getId())
			{
				case R.id.btn_si:
				{
					Intent intent = new Intent();
					intent.setType("image/*");
					intent.setAction(Intent.ACTION_GET_CONTENT);
					startActivityForResult(intent,1);
				}
				break;
				case R.id.btn_proc:
				{
					try{
						min_height = Integer.parseInt(et_min_height.getEditableText().toString());
					}catch(Exception e){
						min_height = -1;
					}
					try{
						max_height = Integer.parseInt(et_max_height.getEditableText().toString());
					}catch(Exception e){
						max_height = -1;
					}
					try{
						min_width = Integer.parseInt(et_min_width.getEditableText().toString());
					}catch(Exception e){
						min_width = -1;
					}
					mTask = new ProcTask();  
					mTask.execute();
					
					btn_proc.setEnabled(false);  
				}
				break;
				default: break;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// TODO: Implement this method
		if(resultCode == RESULT_OK)
		{
			Uri uri = data.getData();
			ContentResolver cr = this.getContentResolver();
			try{
				src_bmp = BitmapFactory.decodeStream(cr.openInputStream(uri));
				iv_src.setImageBitmap(src_bmp);
			}catch(FileNotFoundException e){
				Log.d("get src img error",e.toString());
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public void onResume()
    {
        super.onResume();
		OpenCVLoader.initDebug();
        Log.d(TAG, "OpenCV library found inside package. Using it!");
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }
    
	//取消一个正在执行的任务,onCancelled方法将会被调用
	//mTask.cancel(true);
    private class ProcTask extends AsyncTask<String, Integer, String> {  
	
		DetectChar tc;
        //onPreExecute方法用于在执行后台任务前做一些UI操作  
        @Override  
        protected void onPreExecute() {  
            Log.i(TAG, "onPreExecute() called");  
			tip_msg = "\n开始处理...";
            tv_msg.setText(tip_msg);  
        }  

        //doInBackground方法内部执行后台任务,不可在此方法内修改UI  
        @Override  
        protected String doInBackground(String... params) {  
			tc = new DetectChar();
			if(max_height >= 0)tc.maxHeight = max_height;
			if(min_height >= 0)tc.minHeight = min_height;
			if(min_width >= 0)tc.minWidth = min_width;
			ret_bmp = tc.detect(src_bmp,MainActivity.this);
			publishProgress(100);
			
			
            return null;  
        }  

        //onProgressUpdate方法用于更新进度信息  
        @Override  
        protected void onProgressUpdate(Integer... progresses) {  
			Log.i(TAG, "onProgressUpdate(Progress... progresses) called");
            progressBar.setProgress(progresses[0]);
			tv_msg.setText(tip_msg+"\n"+progresses[0]+"%");
        }  

        //onPostExecute方法用于在执行完后台任务后更新UI,显示结果  
        @Override  
        protected void onPostExecute(String result) {  
            Log.i(TAG, "onPostExecute(Result result) called"); 
			tip_msg += "\n100%";
			tv_msg.setText(tip_msg);
			tip_msg += "\n处理完成";
            tv_msg.setText(tip_msg);  
			
			iv_ret.setImageBitmap(ret_bmp);
			btn_proc.setEnabled(true);
        }  

        //onCancelled方法用于在取消执行中的任务时更改UI  
        @Override  
        protected void onCancelled() {  
            Log.i(TAG, "onCancelled() called");
			btn_proc.setEnabled(true);
        }
		
		public void displayProgress(int num)
		{
			publishProgress(num);
		}
    }  
}
