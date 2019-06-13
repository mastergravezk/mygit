package com.thit.elasticsearch.test;

import java.io.IOException;


public class OtherInitializer {
	public static void main(String[] args) {
		try {
			ESInitializer.initDataToES();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
