package com.proxy;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;

import com.proxy.Config.ProxyRequest;
import com.proxy.Config.ProxyResponse;

import android.text.TextUtils;
import android.util.Log;

public class HttpGetProxy{

	private static final int SIZE = 1024*1024;
	
	final static public String TAG = "HttpGetProxy";
	private int mBufferSize;
	private int mBufferFileMaximum;
	private int remotePort=-1;
	private String remoteHost;
	private int localPort;
	private String localHost;
	private ServerSocket localServer = null;
	private SocketAddress serverAddress;
	private DownloadThread downloadThread = null;
	private ProxyResponse proxyResponse=null;
	private String mBufferDirPath=null;
	private String mId,mUrl;
	private String mMediaUrl;
	private String mMediaFilePath;
	private boolean mEnable = false;
	
	private Proxy proxy=null;
	/**
	 * Initialize the proxy server, and start the proxy server
	 * @param dir Path cache folder
	 * @param size The size of the required pre-loaded
	 * @param maximum The maximum number of pre-loaded files
	 */
	public HttpGetProxy(String dirPath,int size,int maximum) {
		try {
			//Initialize proxy server
			mBufferDirPath = dirPath; 
			mBufferSize=size;
			mBufferFileMaximum = maximum;
			localHost = Config.LOCAL_IP_ADDRESS;
			localServer = new ServerSocket(0, 1,InetAddress.getByName(localHost));
			localPort =localServer.getLocalPort();//There ServerSocket automatically assigned port
			//Start Proxy Server
			Log.i(TAG, "1 start proxy");
					startProxy();
				
			
			mEnable = true;
		} catch (Exception e) {
			mEnable = false;
		}
	}
	
	/**
	 * Proxy server is available
	 * @return
	 */
	public boolean getEnable(){
		//External memory can be used to determine whether the
		File dir = new File(mBufferDirPath);
		mEnable=dir.exists();
		if(!mEnable)
			return mEnable;

		//Get free space
		long freeSize = Utils.getAvailaleSize(mBufferDirPath);
		mEnable = (freeSize > mBufferSize);
		
		return mEnable;
	}

	/**
	 * Stop downloading
	 */
	public void stopDownload(){
		if (downloadThread != null && downloadThread.isDownloading())
			downloadThread.stopThread();
	}
	
	/**
	 * Start pre-loaded, one time only preload a video
	 * @param id Video unique id, long effective
	 * @param url Video Link
	 * @param isDownload Whether to download
	 * @throws Exception
	 */
	public void startDownload(String id,String url,boolean isDownload) throws Exception {
		//Proxy servers are unavailable
		if(!getEnable())
			return;

		//Clear the cache file past
		Utils.asynRemoveBufferFile(mBufferDirPath, mBufferFileMaximum);
		
		mId=id;
		mUrl=url;
		String fileName = Utils.getValidFileName(mId);
		mMediaFilePath = mBufferDirPath + "/" + fileName;

		//To determine whether a file exists, ignore files already buffered
		File tmpFile = new File(mMediaFilePath);
		if(tmpFile.exists() && tmpFile.length()>=mBufferSize){
			Log.i(TAG, "----exists:" + mMediaFilePath+" size:"+tmpFile.length());
			return;
		}
		stopDownload();
		if (isDownload) {
			downloadThread = new DownloadThread(mUrl, mMediaFilePath,mBufferSize);
			downloadThread.startThread();
			Log.i(TAG, "----startDownload:" + mMediaFilePath);
		}
	}
	
	/**
	 * Get playing links
	 * 
	 * @param URL url Network
	 */
	public String getLocalURL(String id) {
		if(TextUtils.isEmpty(mId)		//No pre-loaded too
				|| mId.equals(id)==false)//Id and does not meet the pre-loaded
			return "";
		
		//Proxy servers are unavailable
		if(!getEnable())
			return mUrl;
		
		//Log.e("Http mUrl",mUrl);
		//HTTP exclude special, such as redirection
		//mMediaUrl = Utils.getRedirectUrl(mUrl);
		mMediaUrl = mUrl;
		//Log.e("Http mMediaUrl",mMediaUrl);
		// ----Local proxy server to obtain the corresponding link----//
		String localUrl="";
		URI originalURI = URI.create(mMediaUrl);
		remoteHost = originalURI.getHost();
		if (originalURI.getPort() != -1) {// URL with Port
			serverAddress = new InetSocketAddress(remoteHost, originalURI.getPort());// Use the default port
			remotePort = originalURI.getPort();// Save port, replacement transit
			localUrl = mMediaUrl.replace(remoteHost + ":" + originalURI.getPort(), localHost + ":" + localPort);
		} else {// URL without Port
			//serverAddress = new InetSocketAddress(remoteHost, Config.HTTP_PORT);// Use port 80
			remotePort = -1;
			localUrl = mMediaUrl.replace(remoteHost, localHost + ":" + localPort);
		}

		return localUrl;
	}

	private void startProxy() {
		new Thread() {
			public void run() {
		while (true) {  
			// --------------------------------------
			// MediaPlayer's request listening, MediaPlayer-> proxy server
			// --------------------------------------
			Log.i(TAG, "2--------------");
			try {
				Socket s = localServer.accept();
				Log.i(TAG, "2.1---------------");
				if(proxy!=null){
					proxy.closeSockets();
					Log.i(TAG, "2.2-------------close sockets");
				}
				Log.i(TAG, "3---------------");
				proxy = new Proxy(s);
				Log.i(TAG, "3.1---------------");
				/*new Thread(){
					public void run(){
						Log.i(TAG, "4------------------");
						try {
							Socket s = localServer.accept();
							Log.i(TAG, "4.1------------------");
							proxy.closeSockets();
							Log.i(TAG, "5--------------------");
							proxy = new Proxy(s);
							Log.i(TAG, "5.1---------------------");
							//proxy.run();
							Log.i(TAG, "5.2------------------------");
						} catch (IOException e) {
							Log.i(TAG, "6--------------------");
							Log.e(TAG, e.toString());
							Log.e(TAG, Utils.getExceptionMessage(e));
						}
						
					}
				}.start();*/
				Log.i(TAG, "6.1---------------");
				proxy.run();
				Log.i(TAG, "6.2----------------");
			} catch (IOException e) {
				Log.i(TAG, "7--------------------");
				Log.e(TAG, e.toString());
				Log.e(TAG, Utils.getExceptionMessage(e));
			}
		}
			}
		}.start();
	}
	
	private class Proxy{
		/** Socket receive requests Media Player */
		private Socket sckPlayer = null;
		/** Socket transceiver Media Server requests */
		private Socket sckServer = null;
		
		public Proxy(Socket sckPlayer){
			this.sckPlayer=sckPlayer;
		}
		
		/**
		 * Shut down the existing links
		 */
		public void closeSockets(){
			try {// Before starting a new request to close the past Socket
				if (sckPlayer != null){
					sckPlayer.close();
					sckPlayer=null;
				}
				
				if (sckServer != null){
					sckServer.close();
					sckServer=null;
				}
			} catch (IOException e1) {}
		}
		
		public void run() {
			HttpParser httpParser = null;
			HttpGetProxyUtils utils = null;
			int bytes_read;

			byte[] local_request = new byte[1024];
			byte[] remote_reply = new byte[1024 * 50];

			boolean sentResponseHeader = false;
			
			try {
				if ((sckPlayer != null) && (sckPlayer.isClosed()==false)) {
				Log.i(TAG, "8--------------------");
				//stopDownload();
				Log.i(TAG, "8.1------------------");
				httpParser = new HttpParser(remoteHost, remotePort, localHost,
						localPort);
				Log.i(TAG, "8.2------------------------");
				ProxyRequest request = null;
				while ((bytes_read = sckPlayer.getInputStream().read(
						local_request)) != -1) {
					Log.i(TAG, "8.3-----------------");
					byte[] buffer = httpParser.getRequestBody(local_request,
							bytes_read);
					if (buffer != null) {
						Log.i(TAG, "8.4-------------------");
						request = httpParser.getProxyRequest(buffer);
						break;
					}
				}
				Log.i(TAG, "9-----------------------");
				serverAddress = new InetSocketAddress(remoteHost, Config.HTTP_PORT);
				utils = new HttpGetProxyUtils(sckPlayer, serverAddress);
				boolean isExists = new File(mMediaFilePath).exists();
				if (request != null) {// MediaPlayer's request effective
					sckServer = utils.sentToServer(request._body);// Send MediaPlayer's request
				} else {// MediaPlayer's request is invalid
					closeSockets();
					return;
				}
				// ------------------------------------------------------
				// The feedback network server sent to the MediaPlayer, network server -> Proxy -> MediaPlayer
				// ------------------------------------------------------
				Log.i(TAG, "9.1---------------");
				while (sckServer != null 
						&& ((bytes_read = sckServer.getInputStream().read(
								remote_reply)) != -1)&& (sckPlayer.isClosed()==false)) {
					//Log.i(TAG, "9.2--------------------");
					if ((sentResponseHeader)&& (sckServer.isClosed()==false)) {
						//Log.i(TAG, "9.3----------------");
						try {// When you drag the progress bar, easy this exception, to disconnect and reconnect
							//Log.i(TAG, "9.4------------------------");
							utils.sendToMP(remote_reply, bytes_read);
						} catch (Exception e) {
							Log.i(TAG, "10------------------");
							Log.e(TAG, e.toString());
							Log.e(TAG, Utils.getExceptionMessage(e));
							break;// Send abnormal exit while
						}
						//Log.i(TAG, "10.1-------------------------");
						if (proxyResponse == null)
							continue;// No Response Header exit this cycle

						// Has finished reading
						if (proxyResponse._currentPosition > proxyResponse._duration - SIZE) {
							//Log.i(TAG, "11....ready....over....");
							proxyResponse._currentPosition = -1;
						} else if (proxyResponse._currentPosition != -1) {// Did not finish reading the
							proxyResponse._currentPosition += bytes_read;
						}

						continue;// Exit this while
					}
					proxyResponse = httpParser.getProxyResponse(remote_reply,
							bytes_read);
					if (proxyResponse == null)
						continue;// No Response Header exit this cycle

					sentResponseHeader = true; 
					// send http header to mediaplayer
					utils.sendToMP(proxyResponse._body);

					if (isExists) {//Send pre-loaded file to MediaPlayer
						Log.i(TAG, "12----------------->Need to send pre-loaded into the MediaPlayer");
						isExists = false;
						int sentBufferSize = 0; 
						sentBufferSize = utils.sendPrebufferToMP(
								mMediaFilePath, request._rangePosition);
						if (sentBufferSize > 0) {// Preloading successfully sent, the retransmission request to the server
							// After modifying Range Request sent to the server
							int newRange = (int) (sentBufferSize + request._rangePosition);
							String newRequestStr = httpParser
									.modifyRequestRange(request._body, newRange);
							Log.i(TAG, "12.0"+newRequestStr);
							try {
								if (sckServer != null)
									sckServer.close();
							} catch (IOException ex) {
							}
							Log.i(TAG, "12.1-----------------");
							sckServer = utils.sentToServer(newRequestStr);
							Log.i(TAG, "12.2-----------------");
							// The Response Header remove server
							proxyResponse = utils.removeResponseHeader(
									sckServer, httpParser);
							Log.i(TAG, "12.3-----------------");
							continue;
						}
					}

					// Send the binary data
					if (proxyResponse._other != null) {
						utils.sendToMP(proxyResponse._other);
					}
				}
				Log.i(TAG, "12.4-----------------");
				// Close 2 SOCKET
				closeSockets();
				}
			} catch (Exception e) {
				Log.i(TAG, "13--------------");
				Log.e(TAG, e.toString());
				Log.e(TAG, Utils.getExceptionMessage(e));
			}
		}
	}

}