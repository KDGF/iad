package com.kdg.gnome.adx.test;

import java.io.FileNotFoundException;
import java.util.Random;

public class TimeProcessing {
	
	public void TripartiteProcessingTime() {
		ConfigProperties cp;
		try {
			cp = new ConfigProperties();
		
		
			int max = Integer.parseInt(cp.getWaitTimeMax());
			int min = Integer.parseInt(cp.getWaitTimeMin());

	
			Random random = new Random();
	
			int s = random.nextInt(max) % (max - min + 1) + min;
	
			try {
				Thread.sleep(s);
//				System.out.println("[info]:等待" + s + "ms");
				
	
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
