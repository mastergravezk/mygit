package com.thit.elasticsearch.test;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


public class MatInitializer {
	
	public static void main(String[] args) {
		try {
			ESInitializer.initT_MATDataToES();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
