package com.thit.elasticsearch.metadb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;


public class IndexInitializer {
	
	private static Client client;
	public static void main(String[] args) {
		String index = "mdmindex";
		Map<String,Object> meta = new TreeMap<>();
		meta.put("number_of_shards", "3");
		meta.put("number_of_replicas", "1");
		
//		Map name = new TreeMap<>();
//		name.put("analysis-ik", new TreeMap<>().put("tokenizer", "ik_smart"));
//		Map analyzer = new TreeMap<>();
//		analyzer.put("analyzer", name);
//		String analysis = "analysis:{\"analyzer\":{\"analysis-ik\":{\"tokenizer\":\"ik_smart\"}}}";
//		meta.put("settings", analysis);
		
		//自定义分词器
	
//		System.out.println(source);	
		boolean flag = ESInitializer.createDefultindex(index, meta);
//		boolean flag =createDefultindex(index, meta);
		if(flag){
			System.out.println("创建索引成功");
		}else{
			System.out.println("创建索引失败");
		}
	}
	
	public static boolean createDefultindex(String index,String source){
		client = ESInitializer.initClient();
//		String source = "{\"settings\": "
//				+ "{\"number_of_shards\":\"3\","
//				+ "\"number_of_replicas\":\"1\","
//				+ "\"analysis\":"
//				+ "{\"analyzer\":"
//				+ "{\"standard\":{\"tokenizer\":\"standard\"}"
//				+ ","
//				+ "\"ik_max_word\":{\"tokenizer\":\"ik_max_word\"}"
//				+ "}}}}";
		System.out.println("初始化client");
//		Diction
//		Settings.builder().put(properties)
		Settings settings = Settings.builder().loadFromSource(source, XContentType.JSON).build();
//				.put("analysis", new HashMap<>().put("analyzer", new HashMap<>().put("ik_max_word", new HashMap<>().put("tokenizer", "ik_max_word")))).build();
		boolean flag = false;
		try {
			CreateIndexResponse res = client.admin().indices().prepareCreate(index).setSettings(settings).execute().get();
			if(res.isAcknowledged()&&res.isShardsAcked())
				flag = true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;
	}
}
