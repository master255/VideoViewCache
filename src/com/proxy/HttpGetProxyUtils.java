package com.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import com.proxy.Config.ProxyResponse;

import android.util.Log;
/**
 * Proxy Tools
 *
 */
public class HttpGetProxyUtils {
	final static public String TAG = "HttpGetProxy";
	
	/** Socket receive requests Media Player */
	private Socket mSckPlayer = null;

	/**Address Server*/
	private SocketAddress mServerAddress;
	
	public HttpGetProxyUtils(Socket sckPlayer,SocketAddress address){
		mSckPlayer=sckPlayer;
		mServerAddress=address;
	}
	
	/**
	 * Send pre-loaded to the server
	 * @param fileName Pre-loaded files
	 * @param range skip Size
	 * @return Size size has been sent, excluding skip the
	 * @throws Exception
	 */
	public int sendPrebufferToMP(String fileName,long range){
		final int MIN_SIZE= 100*1024;
		int fileBufferSize=0;

		byte[] file_buffer = new byte[1024];
		int bytes_read = 0;
		long startTimeMills = System.currentTimeMillis();

		File file = new File(fileName);
		if (file.exists() == false) {
			Log.i(TAG, ">>>Pre-loaded file does not exist");
			return 0;
		}
		if (range > (file.length())) {// Range is too small cache size exceeds a pre-
			Log.i(TAG,">>>Do not read the pre-loaded files range:" + range + ",buffer:" + file.length());
			return 0;
		}

		if (file.length() < MIN_SIZE) {// Pre-cache available is too small, no need to read and re-issued Request
			Log.i(TAG, ">>>Pre-loaded file is too small, does not read the pre-loaded");
			return 0;
		}
		
		FileInputStream fInputStream = null;
		try {
			fInputStream = new FileInputStream(file);
			if (range > 0) {
				byte[] tmp = new byte[(int) range];
				long skipByteCount = fInputStream.read(tmp);
				Log.i(TAG, ">>>skip:" + skipByteCount);
			}

			while ((bytes_read = fInputStream.read(file_buffer)) != -1) {
				mSckPlayer.getOutputStream().write(file_buffer, 0, bytes_read);
				fileBufferSize += bytes_read;//Was successfully sent computing
			}
			mSckPlayer.getOutputStream().flush();
			
			long costTime = (System.currentTimeMillis() - startTimeMills);
			Log.i(TAG, ">>>Read preloading time:" + costTime);
			Log.i(TAG, ">>>Read the complete ... Download:" + file.length() + ",Read:"+ fileBufferSize);
		} catch (Exception ex) {
		} finally {
			try {
				if (fInputStream != null)
					fInputStream.close();
			} catch (IOException e) {}
		}
		return fileBufferSize;
	}

	/**
	 * The Response Header remove server
	 * @throws IOException 
	 */
	public ProxyResponse removeResponseHeader(Socket sckServer,HttpParser httpParser)throws IOException {
		ProxyResponse result = null;
		int bytes_read;
		byte[] tmp_buffer = new byte[1024];
		while ((bytes_read = sckServer.getInputStream().read(tmp_buffer)) != -1) {
			result = httpParser.getProxyResponse(tmp_buffer, bytes_read);
			if (result == null)
				continue;// No Header exit this cycle

			// Response received the Header
			if (result._other != null) {// Send the remaining data
				sendToMP(result._other);
			}
			break;
		}
		return result;
	}
	
	public void sendToMP(byte[] bytes, int length) throws IOException {
		mSckPlayer.getOutputStream().write(bytes, 0, length);
		mSckPlayer.getOutputStream().flush();
	}

	public void sendToMP(byte[] bytes) throws IOException{
		if(bytes.length==0)
			return;
		mSckPlayer.getOutputStream().write(bytes);
		mSckPlayer.getOutputStream().flush();	
	}
	
	public Socket sentToServer(String requestStr) throws IOException{
		Socket sckServer = new Socket();
		sckServer.connect(mServerAddress);
		sckServer.getOutputStream().write(requestStr.getBytes());// MediaPlayer's request
		sckServer.getOutputStream().flush();
		return sckServer;
	}
}
