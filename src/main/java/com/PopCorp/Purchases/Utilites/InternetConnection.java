package com.PopCorp.Purchases.Utilites;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class InternetConnection {
	
	private URL url;
	private InputStreamReader streamReader;
	private HttpURLConnection connection;
	private StringBuilder pageInStringBuilder;
	private int ConnectTimeout = 10000;
	private int ReadTimeout = 10000;
	
	public InternetConnection(String page, int ConnTime,int ReadTime) throws MalformedURLException{
		url = new URL(page);
		ConnectTimeout = ConnTime;
		ReadTimeout = ReadTime;
	}
	
	public InternetConnection(String page) throws MalformedURLException{
		url = new URL(page);
	}
	
	public InputStream getStream() throws IOException{
		connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(ConnectTimeout);
		connection.setReadTimeout(ReadTimeout);
		connection.setRequestMethod("GET");
        connection.setDoInput(true);
		connection.connect();
		return connection.getInputStream();
	}
	
	public StringBuilder getPageInStringBuilder() throws IOException {
		streamReader = new InputStreamReader(getStream());
		pageInStringBuilder = new StringBuilder();
		int n = 0;
		char[] buffer = new char[80000];
		while (n >= 0) {
			n = streamReader.read(buffer, 0, buffer.length);
			if (n > 0) {
				pageInStringBuilder.append(buffer, 0, n);
			}
		}
		connection.disconnect();
		return pageInStringBuilder;
	}
	
	public void disconnect(){
		if (connection!=null){
			connection.disconnect();
		}
	}

}
