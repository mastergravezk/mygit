package com.thit.elasticsearch.metadb;

import java.io.IOException;

import com.thit.elasticsearch.ESInitializer;

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
