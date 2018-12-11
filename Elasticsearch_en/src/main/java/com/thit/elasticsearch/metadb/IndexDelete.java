package com.thit.elasticsearch.metadb;

import com.thit.elasticsearch.ESInitializer;

public class IndexDelete {
	public static void main(String[] args) {
		String index = "mdmindex";
		boolean deleteIndex = ESInitializer.deleteIndex(index);
		System.out.println(deleteIndex);
	}
}
