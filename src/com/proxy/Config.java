package com.proxy;

/**
 * Config
 *
 */
public class Config{

	final static public String LOCAL_IP_ADDRESS = "127.0.0.1";
	final static public int HTTP_PORT = 80;
	final static public String HTTP_BODY_END = "\r\n\r\n";
	final static public String HTTP_RESPONSE_BEGIN = "HTTP/";
	final static public String HTTP_REQUEST_BEGIN = "GET ";
	final static public String HTTP_REQUEST_LINE1_END = " HTTP/";
	

	static public class ProxyRequest{
		/**Http Request Content*/
		public String _body;
		/**RanageLocation*/
		public long _rangePosition;
	}
	
	static public class ProxyResponse{
		public byte[] _body;
		public byte[] _other;
		public long _currentPosition;
		public long _duration;
	}
}