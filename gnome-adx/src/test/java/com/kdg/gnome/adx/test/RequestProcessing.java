package com.kdg.gnome.adx.test;

import java.io.UnsupportedEncodingException;

import org.httpkit.server.HttpRequest;

import com.kdg.gnome.share.Constants;
import com.kdg.gnome.share.OriginRequest;

public class RequestProcessing {
	public OriginRequest Request(HttpRequest request) {
		OriginRequest originReq = new OriginRequest();
		if (request.method.KEY == request.method.GET.KEY) {
			originReq.type = Constants.HTTP_REQ_TYPE_GET;
			originReq.getQuery = request.queryString;
//			System.out.println("[info]:get请求body为："
//					+ originReq.getQuery);
			
		} else if (request.method.KEY == request.method.POST.KEY) {
			originReq.type = Constants.HTTP_REQ_TYPE_POST;
//			System.out.println("[info]:请求的类型为："
//					+ originReq.type);

			originReq.postBody = request.getPostBody();
			byte[] postb = originReq.postBody;

			try {
				String postBodyString = new String(postb,
						"UTF-8");
//				System.out.println("[info]:post请求body为："
//						+ postBodyString);
	
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
	
			}
		} else {
			originReq.type = Constants.HTTP_REQ_TYPE_OTHER;
		}
		return originReq;
	}
	

}
