package com.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import android.os.StatFs;
import android.util.Log;

/**
 * Tools
 *
 */
public class Utils {
	private static final String TAG="com.proxy.utils";
	

	static protected String getSubString(String source,String startStr,String endStr){
		int startIndex=source.indexOf(startStr)+startStr.length();
		int endIndex=source.indexOf(endStr,startIndex);
		return source.substring(startIndex, endIndex);
	}
	
	/**
	 * Obtain a valid file name
	 * @param str
	 * @return
	 */
	static protected String getValidFileName(String str)
    {
        str=str.replace("\\","");
        str=str.replace("/","");
        str=str.replace(":","");
        str=str.replace("*","");
        str=str.replace("?","");
        str=str.replace("\"","");
        str=str.replace("<","");
        str=str.replace(">","");
        str=str.replace("|","");
        str=str.replace(" ","_");    //A space in front of the replacement will produce, and the last one will be replaced
        return str;
    }
	
	/**
	 * Access to external memory space available
	 * @return
	 */
	static protected long getAvailaleSize(String dir) {
		StatFs stat = new StatFs(dir);//path.getPath());
		long totalBlocks = stat.getBlockCount();// Get block number
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize; // Get the available size
	}
	
	/**
	 * Get a file folder, sorted by date, from the old to the new
	 * @param dirPath
	 * @return
	 */
	static private List<File> getFilesSortByDate(String dirPath) {
		List<File> result = new ArrayList<File>();
		File dir = new File(dirPath);
		File[] files = dir.listFiles();
		if(files==null || files.length==0)
			return result;
		
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Long.valueOf(f1.lastModified()).compareTo(
						f2.lastModified());
			}
		});

		for (int i = 0; i < files.length; i++){
			result.add(files[i]);
			Log.i(TAG, i+":"+files[i].lastModified() + "---" + files[i].getPath());
		}
		return result;
	}
	
	/**
	 * Remove the extra cache files 
	 * @param dirPath cache file folder path
	 * @param maximun maximum number of cached files
	 */
	static protected void asynRemoveBufferFile(final String dirPath,final int maximun) {
		new Thread() {
			public void run() {
				List<File> lstBufferFile = Utils.getFilesSortByDate(dirPath);
				while (lstBufferFile.size() > maximun) {
					Log.i(TAG, "---delete " + lstBufferFile.get(0).getPath());
					lstBufferFile.get(0).delete();
					lstBufferFile.remove(0);
				}
			}
		}.start();
	}
	
	public static String getExceptionMessage(Exception ex){
		String result="";
		StackTraceElement[] stes = ex.getStackTrace();
		for(int i=0;i<stes.length;i++){
			result=result+stes[i].getClassName() 
			+ "." + stes[i].getMethodName() 
			+ "  " + stes[i].getLineNumber() +"line"
			+"\r\n";
		}
		return result;
	}
}
