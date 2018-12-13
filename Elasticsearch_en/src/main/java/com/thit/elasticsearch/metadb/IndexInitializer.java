package com.thit.elasticsearch.metadb;

import java.util.Map;
import java.util.TreeMap;

import com.thit.elasticsearch.ESInitializer;

public class IndexInitializer {
	public static void main(String[] args) {
		String index = "mdmindex";
		Map meta = new TreeMap<>();
		meta.put("number_of_shards", "2");
		meta.put("number_of_replicas", "1");
		ESInitializer.createDefultindex(index, meta);
	}
}
