package com.thit.elasticsearch.metadb;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.ibm.icu.text.SimpleDateFormat;

import net.sf.json.util.JSONBuilder;

public class InitializerTest {
	public static void main(String[] args) throws IOException, ParseException {
//		try {
//			ESInitializer.initDataToES();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		Client client = ESInitializer.initClient();
		Map source = new HashMap<>();
//		source.put("name", "zk");
//		source.put("age", 2);
//		client.prepareIndex("mdmindex", "user", "1").setSource(source).get();
////		
//		source.put("name", "mn");
//		source.put("age", 3);
//		source.put("birthday", new Date());
//		source.put("height", 174);
//		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");
//		String date1 = "20160418";
////		System.out.println(sdf1.parse(date1));
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		String date = "2017-01-11 12:12:12";
//		sdf.parse(date);
		
//		source.put("CO_FREEZE", 127);
//		source.put("CO_ID", 23244);
//		source.put("CO_PUBLISHTIME", date1);
//		client.prepareIndex("mdmindex", "t_mat", "1").setSource(source).get();
//		source.put("CO_FREEZE", 127);
//		source.put("CO_ID", 23245);
//		source.put("CO_PUBLISHTIME", date);
//		client.prepareIndex("mdmindex", "t_mat", "2").setSource(source).get();
		
		
		//删除索引
//		client.admin().indices().prepareDelete("mdmindex").get();
		//创建索引
//		Settings settings = Settings.builder().put("number_of_shards", "2")
//		.put("number_of_replicas","1").build();
//		client.admin().indices().prepareCreate("mdmindex").setSettings(settings).get();
		
		PutMappingRequest source2 = Requests.putMappingRequest("mdmindex").type("t_mat").source(getBuilder());
		try {
			client.admin().indices().putMapping(source2).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		//查询
//		BoolQueryBuilder filter = QueryBuilders.boolQuery();
//		filter.filter(QueryBuilders.rangeQuery("CO_PUBLISHTIME").gt("2016-05-11 12:12:12").lt("2018-6-11 12:12:12"));
//		
//		SearchResponse res = client.prepareSearch("mdmindex").setTypes("t_mat")
//		.setQuery(filter).get();
//		SearchHit[] hits = res.getHits().getHits();
//		for(SearchHit hit : hits){
//			System.out.println(hit.getSourceAsString());
//		}
	}
	
	
	static XContentBuilder getBuilder() throws IOException{
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject()
		.startObject("t_mat")
		.startObject("properties")
		.startObject("CO_FREEZE").field("type", "byte").endObject()
		.startObject("CO_HASCHILD").field("type", "byte").endObject()
		.startObject("CO_DEL").field("type", "byte").endObject()
		.startObject("CO_PID").field("type", "integer").endObject()
		.startObject("CO_VALID").field("type", "byte").endObject()
		.startObject("CO_ID").field("type", "integer").endObject()
		.startObject("CO_CREATETIME").field("type", "long").endObject()
		.startObject("CO_PUBLISHTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd||epoch_millis").endObject()
		.startObject("EXTRD").field("type", "byte").endObject()
		.endObject()
		.endObject()
		.endObject();
		return builder;
	}
	
}
