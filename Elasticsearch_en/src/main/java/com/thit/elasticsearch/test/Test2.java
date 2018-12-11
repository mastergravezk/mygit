package com.thit.elasticsearch.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.thit.elasticsearch.common.ESUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class Test2 {
	private static Client client;
	public static Client initClient() throws UnknownHostException{
		if(client==null){
			Settings settings = Settings.builder()
					.put("cluster.name", "mdmjt")//指定集群的名称
			        .put("client.transport.sniff", true)//如果有节点加入集群将自动检测加入
			        .build(); 
			client = new PreBuiltTransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("192.168.1.5"), 9300));
//			.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9301));
		}
		return client;
	}
	public static String[] getMappingMetaDatas(String index,String tablename){
		
	   ImmutableOpenMap<String, MappingMetaData> mappings = client.admin().cluster().prepareState().execute()
                .actionGet().getState().getMetaData().getIndices().get(index.toLowerCase()).getMappings();
       String string = mappings.get(tablename.toLowerCase()).source().toString();
       JSONObject json = new JSONObject();
       JSONObject out = json.fromObject(string);
       JSONObject t_mat = (JSONObject)out.get(tablename.toLowerCase());
       JSONObject properties = (JSONObject)t_mat.get("properties");
       JSONArray names = properties.names();
       int size = names.size();
       Object[] o = names.toArray();
       List<String> a = new ArrayList<>();
       String b = "";
       for(int i=0;i<size;i++){
    	   b = (String)o[i];
    	   if("IMPORTSEQ".equals(b)||"CO_CREATETIME".equals(b)){
    		   
    	   }else{
    		   a.add((String)o[i]);
    	   }
       }
       String[] c = new String[a.size()];
//       if(){}
       return  a.toArray(c);
       
	}
	public static void main(String[] args) throws UnknownHostException {
		client = initClient();
		
		String[] a =  getMappingMetaDatas("mdmindex", "t_mat");
		System.out.println(a);
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		SearchResponse Resp = client.prepareSearch("mdmindex")
			.setTypes("t_mat")
			.setSize(10000)
			.setQuery(
					boolQuery
					.should(QueryBuilders.wildcardQuery("ZCZBM.keyword", "*GB/T69*"))
//					.filter(QueryBuilders.multiMatchQuery("我的流程", a))
					).get();
		SearchHit[] hits = Resp.getHits().getHits();
		System.out.println(hits.length);
		for (SearchHit hit : hits) {
			System.out.println(hit.getSourceAsString());
		}
		// add();
		// String jsonstr =
		// "[{\"RESTYPE\":\"1\",\"RESDESC\":\"物料类型：字段不能为空;\",\"SYSAPPLYID\":\"3000MDM001:000000000762017\"},{\"RESTYPE\":\"2\",\"RESDESC\":\"null\",\"SYSAPPLYID\":\"3000MDM001:000000000762016\"}]";
		// paraseJson(jsonstr);
		
//		Client client = new RestClient(
//				RestClient.builder(new HttpHost("localhost", 9200, "http")));
//		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
//		searchSourceBuilder.aggregation(AggregationBuilders.terms("top_10_states").field("state").size(10));
//		SearchRequest searchRequest = new SearchRequest();
//		searchRequest.indices("social-*");
//		searchRequest.source(searchSourceBuilder);
//		SearchResponse searchResponse = client.search(searchRequest);
//		System.out.println(tableSizeFor(33));
//		System.out.println("abc".hashCode());
//		annalyzeStr("mdmindex", "0101ERP001;3333", "standard");
	}
	public static List annalyzeStr(String index,String termsStr,String analyzer){
		//将token中分词后的内容，除了最后一个term进行模糊查询之外，另外的都进行准确匹配
		AnalyzeResponse resp = client.admin().indices().prepareAnalyze(index, termsStr).setAnalyzer(analyzer).get();
		List<AnalyzeToken> tokens = resp.getTokens();
		List list = new ArrayList();
		for (AnalyzeToken token : tokens) {
			String term = token.getTerm();
			System.out.println(term);
			list.add(term);
		}
		return list;
	}
	static int tableSizeFor(int cap){
		int n = cap - 1;
        n |= n >>> 1;
        System.out.println(n);
        n |= n >>> 2;
        System.out.println(n);
        n |= n >>> 4;
        System.out.println(n);
        n |= n >>> 8;
        System.out.println(n);
        n |= n >>> 16;
        System.out.println(n);
        return (n < 0) ? 1 : (n >= 1<<30) ? 1<<30 : n + 1;
	}
	public static List<Map<String,Object>> paraseJson(String jsonstr){
		JSONArray jsonarray = JSONArray.fromObject(jsonstr);
		List<Map<String,Object>> json = new ArrayList<Map<String,Object>>();
		//"{\"name\":\"zhangsan\",\"password\":\"zhangsan123\",\"email\":\"10371443@qq.com\"}";
		for(int i=0;i<jsonarray.size();i++){
			JSONObject jsonObject = jsonarray.getJSONObject(i);
			Map<String, Object> map = new HashMap<String, Object>();
            for (Iterator<?> iter = jsonObject.keys(); iter.hasNext();)
            {
                String key = (String) iter.next();
                String value = jsonObject.get(key).toString();
                map.put(key, value);
            }
            json.add(map);
		}
		return json;
	}
	public static void search() throws UnknownHostException{
		client = initClient();
		SearchResponse resp = client.prepareSearch("test1").setTypes("user")
		.setQuery(QueryBuilders.matchAllQuery())
		.addSort("age.keyword", SortOrder.ASC)
		.setSize(200)
		.get();
		SearchHit[] hits = resp.getHits().getHits();
		for (SearchHit hit : hits) {
			System.out.println(hit.getSourceAsString());
		}
		client.close();
	}
//	public static void add() throws UnknownHostException{
//		JSONObject paraser = new JSONObject();
//		User  user = new User();
//		user.setAge(1);
//		user.setGender("man");
//		user.setJob("coder");
//		user.setName("zk");
//		JSONObject json = paraser.fromObject(user);
//		System.out.println(json.toString());
//		client = initClient();
//		System.out.println(client);
//		
////		Settings build = Settings.builder().put("number_of_shards", 1)
////				.put("number_of_replicas", 1)
////				.build();
//		client.admin().indices().prepareCreate("test1").get();
//		
//		Map source = new HashMap();
//		BulkRequestBuilder bulk = client.prepareBulk();
//		for(int i = 4 ;i<100 ;i++){
//			source.clear();
//			source.put("name", "zk"+String.valueOf(i));
//			source.put("age", String.valueOf(i));
//			source.put("gender", "man");
//			bulk.add(client.prepareIndex("test1", "user").setId(String.valueOf(i))
//			.setSource(source));
//		}
//		bulk.get();
//		client.close();
//		
//	}
	public static void delete() throws UnknownHostException{
		client = initClient();
		client.admin().indices().prepareDelete("test1").get();
		System.out.println("删除索引成功！");
	}
	public static void update() throws UnknownHostException{
		client = initClient();
		Map source = new HashMap();
		source.put("name", "wsp");
		client.prepareUpdate("test1", "user", "5").setDoc(source).get();
				
	}
}
