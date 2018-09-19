package com.kdg.gnome.adx.test;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.httpkit.HeaderMap;

public class HeaderProcessing {
	public HeaderMap Header(Map<String, Object> headerFormReq) {
		HeaderMap header = new HeaderMap();

		Set<Entry<String, Object>> set1 = headerFormReq.entrySet();
		for (Iterator<Entry<String, Object>> iter = set1.iterator(); iter
				.hasNext();) {
			Entry<String, Object> entry = iter.next();
			String key = entry.getKey();
			Object value = entry.getValue();
			header.put(key, value);
		}

		return header;
	}
}
