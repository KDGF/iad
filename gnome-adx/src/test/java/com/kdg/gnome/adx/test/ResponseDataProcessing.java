package com.kdg.gnome.adx.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ResponseDataProcessing {
	


	public String readFile(String filePath)
			throws UnsupportedEncodingException, FileNotFoundException {

		File file = new File(filePath);
		// System.err.println("响应数据文件为："+filePath);
		// InputStream in = new FileInputStream(file);
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(in)，"UTF-8");

		InputStreamReader isr = new InputStreamReader(
				new FileInputStream(file), "UTF-8");
		BufferedReader reader = new BufferedReader(isr);
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				isr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String responseDate = sb.toString();
		return responseDate;

	}

	public String Weight(List<String> responseDatafiles, List<Integer> weight) {
		int sumWeight = 0; // 得到总权重
		for (Integer weightEach : weight) {
			sumWeight += weightEach;
		}

		Random rand = new Random();

//		System.out.println("[info]:总权重为：" + sumWeight);
		int x = rand.nextInt(sumWeight); // 生成随机数
//		System.out.println("[info]:随机数为：" + x);
		int start = 0;
		int end = 0;
		for (int i = 1; i < weight.size() + 1; i++) {
			// 计算各个权重对应的起止数值段
			if (i == 1) {
				start = 0;
				end = weight.get(i - 1) - 1;
			} else {
				start = end + 1;
				end = end + weight.get(i - 1);
			}
			// 根据随机数落入的范围返回对应几率的广告id
			if (x >= start && x <= end) {
				return responseDatafiles.get(i - 1);
			}
			// else if(x<=start){
			// return responseDatafiles.get(i-2);
			// }
			else {
				continue;
			}
		}

		String respData = null;

		return respData;

	}

	public int CodeWeight(List<Integer> statusCode, List<Integer> weight) {
		int sumWeight = 0; // 得到总权重
		for (Integer weightEach : weight) {
			sumWeight += weightEach;
		}

		Random rand = new Random();

//		System.out.println("[info]:总权重为：" + sumWeight);
		int x = rand.nextInt(sumWeight); // 生成随机数
//		System.out.println("[info]:随机数为：" + x);
		int start = 0;
		int end = 0;
		for (int i = 1; i < weight.size() + 1; i++) {
			// 计算各个权重对应的起止数值段
			if (i == 1) {
				start = 0;
				end = weight.get(i - 1) - 1;
			} else {
				start = end + 1;
				end = end + weight.get(i - 1);
			}
			// 根据随机数落入的范围返回对应几率的广告id
			if (x >= start && x <= end) {
				return statusCode.get(i - 1);
			}
			// else if(x<=start){
			// return responseDatafiles.get(i-2);
			// }
			else {
				continue;
			}
		}

		int status = 0;

		return status;

	}

	
	

	
	
	
	public void ReqAndResp(ByteBuffer[] bytes){
		
		Charset charset = null;
		CharsetDecoder decoder = null;
		CharBuffer charBuffer = null;

		charset = Charset.forName("UTF-8");
		decoder = charset.newDecoder();
//		System.out
//				.println("++++++++++++++++++++++++++++++++++");
		try {
			for (int index = 0; index < bytes.length; index++) {

				// charBuffer =
				// decoder.decode(bytes[index]);//用这个的话，只能输出来一次结果，第二次显示为空
				charBuffer = decoder.decode(bytes[index]
						.asReadOnlyBuffer());
//				System.out.println(charBuffer.toString());
			}

		} catch (CharacterCodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		System.out
//				.println("++++++++++++++++++++++++++++++++++");
		
	}

}
