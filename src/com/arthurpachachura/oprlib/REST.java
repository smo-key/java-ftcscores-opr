package com.arthurpachachura.oprlib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.Gson;

final class REST {

	private static final String USER_AGENT = "Mozilla/5.0";
	
	static enum Method {
		GET,
		POST;
	}
	
	static class Response {
		private int responseCode;
		private String result;
		
		Response() {
			this.responseCode = 400;
			this.result = null;
		}
		Response(String error) {
			this.responseCode = 400;
			this.result = error;
		}
		Response(String result, int responseCode) {
			this.result = result;
			this.responseCode = responseCode;
		}
		
		String raw() {
			return result;
		}
		int status() {
			return responseCode;
		}
		boolean good() {
			return (responseCode < 400);
		}
		boolean bad() {
			return !good();
		}
		@SuppressWarnings("unchecked")
		Object json(Class type) {
			Gson gson = new Gson();
			return gson.fromJson(result.toString(), type);
		}
	}
	
	static Response request(String url, Method method) {
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			
			con.setRequestMethod(method.name()); //GET or POST
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.setUseCaches(false);
			
			int responseCode = con.getResponseCode();
			
			InputStream stream = responseCode >= 400 ? con.getErrorStream() : con.getInputStream();
			
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(stream));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			return new Response(response.toString(), responseCode);
		} catch(IOException e) {
			e.printStackTrace();
			return new Response(e.getMessage());
		}
		
	}
	
}
