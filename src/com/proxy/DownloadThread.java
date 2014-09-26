package com.proxy;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;
/**
 * Download module, supports breakpoints download
 * 
 */
public class DownloadThread extends Thread {
	static private final String TAG="DownloadThread";
	private String mUrl;
	private String mPath;
	private long mDownloadSize;
	private int mTargetSize;
	private boolean mStop;
	private boolean mDownloading;
	private boolean mStarted;
	private boolean mError;
	
	public DownloadThread(String url, String savePath,int targetSize) {
		mUrl = url;
		mPath = savePath;
		
		//If the file exists, then continue
		File file=new File(mPath);
		if(file.exists()){
			mDownloadSize =  file.length();
		}else{
			mDownloadSize = 0;
		}
		
		mTargetSize=targetSize;
		mStop = false;
		mDownloading = false;
		mStarted = false;
		mError=false;
	}

	@Override
	public void run() {
		mDownloading = true;
		download();
	}
	
	/** Start downloading thread */
	public void startThread() {
		if (!mStarted) {
			this.start();

			// Only start once
			mStarted = true;
		}
	}

	/** Stop downloading thread*/
	public void stopThread() {
		mStop = true;
	}

	/** Are Downloading */
	public boolean isDownloading() {
		return mDownloading;
	}

	/**
	 * Whether to download an exception
	 * @return
	 */
	public boolean isError(){
		return mError;
	}
	
	public long getDownloadedSize() {
		return mDownloadSize;
	}

	/** Whether the download was successful */
	public boolean isDownloadSuccessed() {
		return (mDownloadSize != 0 && mDownloadSize >= mTargetSize);
	}

	private void download() {
		//Download success is closed
		if(isDownloadSuccessed()){
			Log.i(TAG,"...DownloadSuccessed...");
			return;
		}
		InputStream is = null;
		FileOutputStream os = null;
		if (mStop) {
			return;
		}
		try {
			URL url = new URL(mUrl);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setInstanceFollowRedirects(true);//Allows the redirection
			is = urlConnection.getInputStream();
			if(mDownloadSize==0){//The new file
				os = new FileOutputStream(mPath);
				Log.i(TAG,"download file:"+mPath);
			}
			else{//Additional data
				os = new FileOutputStream(mPath,true);
				Log.i(TAG,"append exists file:"+mPath);
			}
			int len = 0;
			byte[] bs = new byte[1024];
			if (mStop) {
				return;
			}
			while (!mStop //Not forced to stop
					&& mDownloadSize<mTargetSize //Not downloaded enough
					&& ((len = is.read(bs)) != -1)) {//Not all read
				os.write(bs, 0, len);
				mDownloadSize += len;
			}
		} catch (Exception e) {
			mError=true;
			Log.i(TAG,"download error:"+e.toString()+"");
			Log.i(TAG,Utils.getExceptionMessage(e));
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {}
			}

			if (is != null) {
				try {
					is.close();
				} catch (IOException e){}
			}
			mDownloading = false;
			
			//Clear empty file
			File nullFile = new File(mPath);
			if(nullFile.exists() && nullFile.length()==0)
				nullFile.delete();
			
			Log.i(TAG,"mDownloadSize:"+mDownloadSize+",mTargetSize:"+mTargetSize);
		}
	}
}
