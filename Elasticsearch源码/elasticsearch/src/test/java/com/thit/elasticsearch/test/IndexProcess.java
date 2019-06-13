package com.thit.elasticsearch.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

public class IndexProcess {
//	@Test
	public static void deleteIndex() throws FileNotFoundException {
//		String index = "mdmindex";
//		boolean deleteIndex = ESInitializer.deleteIndex(index);
//		System.out.println(deleteIndex);
		FileReader read = new FileReader("d:/webservice.xml");
		System.out.println(read.getEncoding());
//		read.
	}
	
	
	public static void createIndex() {
		String index = "mdmindex";
		Map<String,Object> meta = new TreeMap<>();
		meta.put("number_of_shards", "3");
		meta.put("number_of_replicas", "1");
		//自定义分词器
		Map<String,Object> analyze = new HashMap<String,Object>();
		Map<String,String> standard = new HashMap<String,String>();
		standard.put("tokenizer", "standard");
		Map<String,String> ik = new HashMap<String,String>();
		ik.put("tokenizer", "ik_max_word");
		List<Object> ls = new ArrayList<>();
		ls.add(standard);
		ls.add(ik);
		analyze.put("analyzer", ls);
//		meta.put("analysis", ls);
		String source = "{\"settings\": "
				+ "{\"number_of_shards\":\"3\","
				+ "\"number_of_replicas\":\"1\","
				+ "\"analysis\":"
				+ "{\"analyzer\":"
				+ "{\"standard\":{\"tokenizer\":\"standard\"}"
				+ ","
				+ "\"ik_max_word\":{\"tokenizer\":\"ik_max_word\"}"
				+ "}}}}";
		System.out.println(source);	
//		boolean flag = ESInitializer.createDefultindex(index, source);
		boolean flag =ESInitializer.createDefultindex(index, meta);
		if(flag){
			System.out.println("创建索引成功");
		}else{
			System.out.println("创建索引失败");
		}
	}
}
