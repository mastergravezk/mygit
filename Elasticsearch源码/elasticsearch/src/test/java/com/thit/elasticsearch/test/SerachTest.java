package com.thit.elasticsearch.test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TypeAttributeImpl;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.Attribute;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse.FieldMappingMetaData;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.river.mongodb.Operation;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregator;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.thit.elasticsearch.common.ESConstant;
import com.thit.elasticsearch.common.ESUtil;
import com.thit.elasticsearch.orcldb.DbOperation;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SerachTest {
	private static Client client;
	public static Client initClient() throws UnknownHostException{
		Map<String,Object> analyze = new HashMap<String,Object>();
		Map<String,String> standard = new HashMap<String,String>();
		standard.put("tokenizer", "standard");
		Map<String,String> ik = new HashMap<String,String>();
		ik.put("tokenizer", "ik_max_word");
		List<Object> ls = new ArrayList<>();
		ls.add(standard);
		ls.add(ik);
		analyze.put("analyzer", ls);
		if(client==null){
			Settings settings = Settings.builder()
					.put("cluster.name", "mdmjt")//指定集群的名称
			        .put("client.transport.sniff", true)//如果有节点加入集群将自动检测加入
			        .put("analysis", ls)
			        .build(); 
			client = new PreBuiltTransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("192.168.2.33"), 9300));
//			.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9301));
		}
		return client;
	}
	public static void main(String[] args) throws Exception {
//		String name = SerachTest.class.getName();
//		System.out.println(name);
//		System.out.println("11");
//		client = ESInitializer.initClient();
//		client.admin().indices()
//		Settings build = Settings.builder().put("number_of_shards", 2)
//				.put("number_of_replicas", 1)
//				.build();
//		client.admin().indices().prepareCreate("mdmindex")
//		.setSettings(build)
//		.execute().actionGet();
		String index = "mdmindex";
		String tableName = "t_mat";
		
//		String clerkid="882";
//		String importseq="11";
		long start = System.currentTimeMillis();
			
		
//		SearchResponse searchRespo nse = client.prepareSearch(index.toLowerCase()).setTypes(tableName.toLowerCase())
//				.setQuery(QueryBuilders.constantScoreQuery(QueryBuilders.boolQuery()
//						.filter(QueryBuilders.matchQuery("CO_VALID", "1"))
////						.filter(QueryBuilders.matchQuery("CO_ATTRLANGUAGE","zh"))
//						.filter(QueryBuilders.queryStringQuery("圆螺母用止动垫圈")))
//						
//						)
//				.get();
//		GetMappingsResponse getMappingsResponse = client.admin().indices().getMappings(client.admin().indices().prepareGetMappings(index).setTypes(tableName).request()).get();
//		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = getMappingsResponse.getMappings();
//		
//		GetFieldMappingsResponse mapRes = client.admin().indices().prepareGetFieldMappings(index).setIndices(index).setTypes(tableName).get(new TimeValue(60000));
////		
//		 ImmutableOpenMap<String, MappingMetaData> mappings = client.admin().cluster().prepareState().execute()
//                 .actionGet().getState().getMetaData().getIndices().get(index).getMappings();
//        String string = mappings.get(tableName).source().toString();
//        System.out.println(string);
//        JSONObject json = new JSONObject();
//        JSONObject out = json.fromObject(string);
//        JSONObject obj = (JSONObject)out.get("t_mat");
//        JSONObject obj1 = (JSONObject)obj.get("properties");
//        JSONArray names = obj1.names();
//        Object[] array = names.toArray();
//        for(Object a :array){
//        	System.out.println(a.toString());
//        }
//        System.out.println(obj.toString());
//		Iterator<String> keysIt = mappings.keysIt();
//		while(keysIt.hasNext()){
//			System.out.println(keysIt.next());
//			
//			ImmutableOpenMap map = mappings.get(keysIt.next());
//			Iterator iterator = map.iterator();
//			JSONObject json = new JSONObject();
//			JSONObject out = json.fromObject(map);
//			System.out.println(out);
//			while(iterator.hasNext()){
//				System.out.println(iterator.next());
//			}
//		}
//		for(String field : fields.keySet()){
//			System.out.println(field+" : ");
//			FieldMappingMetaData key = fields.get(field);
//			String val = key.fullName();
//			System.out.println(val);
//		}
//		
		List<String> codes = new ArrayList<>();
		codes.add("M000000264519".toLowerCase());
		codes.add("M000000264520".toLowerCase());
		String query="gb/t2040-200812345";

//		System.out.println("queryOrg="+query);

//		System.out.println(query);
		query=QueryParser.escape(query);
//		tokenStream.addAttributeImpl(new TypeAttributeImpl());
		String querystr = "2Y1234567901234567890\\GB/T2040-200812345";
//		String querystr = "T2Y1234567901234567890\\GB/T2040-200812345";
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
//		QueryParser.escape(s)
		Map<String,Float> fields = new HashMap<String,Float>();
		fields.put("MATBMAT", 1.0f);
//		Aggregations aggs = 
		
//		client.prepareIndex("", "", "").setParent("").setWaitForActiveShards(1).
		SearchResponse searchResponse = client.prepareSearch(index.toLowerCase()).setTypes(tableName.toLowerCase())
				.setQuery(
//						.filter(QueryBuilders.queryStringQuery("gb/t2040-200812345").defaultOperator(Operator.AND).escape(true))
						 boolQuery
						 .filter(QueryBuilders.matchAllQuery())
						 //全文搜索较准确查询
						 .filter(QueryBuilders.queryStringQuery("*BWPCLNT800*").allowLeadingWildcard(true).analyzer("standard").useAllFields(true).defaultOperator(Operator.AND))
						 .filter(QueryBuilders.queryStringQuery("*HUCCLNT800*").allowLeadingWildcard(true).analyzer("standard").useAllFields(true).defaultOperator(Operator.AND))
						 .filter(QueryBuilders.queryStringQuery(QueryParser.escape("*\\GB/*")).allowLeadingWildcard(true).analyzer("standard").useAllFields(true).defaultOperator(Operator.AND))
////					
						 //条件搜索较准确查询
//						 .filter(QueryBuilders.nestedQuery(path, query, scoreMode))
//						 .filter(QueryBuilders.matchQuery("MATBMAT", "T2Y2\\GB/T4423").operator(Operator.AND))
//						 .filter(QueryBuilders.wildcardQuery("MATBMAT", "*t442*"))
						 /*-----------------------------------------------------------------*/
//						 .filter(QueryBuilders.rangeQuery("CO_ID.keyword").from(0).to(10))
//						.filter(QueryBuilders.wildcardQuery("MATBMAT.keyword", "*GB/T14976-2*"))
//						.filter(QueryBuilders.wildcardQuery("MATVOCH.keyword", "*51A-01-04*"))
//						 .filter(QueryBuilders.matchQuery("MATVOCH.ikword", "TCD0000001454").analyzer("ik_max_word"))
//						.filter(QueryBuilders.simpleQueryStringQuery("6005a-t"))
//						.filter(QueryBuilders.queryStringQuery("2040-20081").analyzer("ik_max_word"))
//						.mustNot(QueryBuilders.queryStringQuery("231").analyzer("ik_max_word"))
//						.mustNot(QueryBuilders.queryStringQuery("-").analyzer("ik_max_word"))
//						.filter(QueryBuilders.wildcardQuery("MATISDESC.keyword","*GB/T14976-20*"))
//						.filter(QueryBuilders.wildcardQuery("MATNUM","*m00000*000001*"))
//						.filter(QueryBuilders.matchPhrasePrefixQuery("MATBMAT",querystr))
//						.filter(QueryBuilders.wildcardQuery("OLDCODE.keyword","*CNR000020*"))
//						.filter(QueryBuilders.queryStringQuery(QueryParser.escape("T2Y1234567901234567890\\GB/".toLowerCase()))
//								.defaultOperator(Operator.AND).analyzeWildcard(true))
//						.filter(QueryBuilders.wildcardQuery("MATBMAT", "*2008123*"))
//						.filter(QueryBuilders.simpleQueryStringQuery(query))
//						.filter(QueryBuilders.wildcardQuery("APPLICANT", "*m0000000000*"))
//						.filter(QueryBuilders.queryStringQuery("板3"))
//						.filter(QueryBuilders.queryStringQuery("52X40X4X1200"))
//						.filter(QueryBuilders.queryStringQuery("中间右侧板3\\52X40X4X1200"))
						)
				.setSize(20)
				.get();
		SearchHit[] hits = searchResponse.getHits().getHits();
		System.out.println("查询结果总数："+searchResponse.getHits().totalHits);
		for (SearchHit hit : hits) {
			System.out.println(hit.getSourceAsString());
		}
		long end = System.currentTimeMillis();
		System.out.println("查询时间"+String.valueOf(end-start));
//		BoolQueryBuilder filter = QueryBuilders.boolQuery();
//		filter.filter(QueryBuilders.termQuery("CO_ID_OLD", "776869")).filter(QueryBuilders.termQuery("ORDERBY", "2"))
//				.filter(QueryBuilders.matchQuery("BATCH", "000000008735"));
//		System.out.println("执行了1");
//		//向es发送查询请求
//		SearchResponse responseSimilar = client.prepareSearch("mdmindex").setTypes("similarity_batch_temp").setQuery(filter)
//				.get();
//		SearchHit[] hits = responseSimilar.getHits().getHits();
//		for (SearchHit hit : hits) {
//			System.out.println(hit.getSourceAsString());
//		}
//		String sql = " SELECT COUNT(1)  FROM  SIMILARITY_BATCH_TEMP SBT WHERE SBT.ORDERBY=2";
//		ResultSet rs = DbOperation.executeQuery(sql);
//		while(rs.next()){
//			System.out.println("");
//		}
//		deleteDatas(sql);
		/*Map map = new HashMap<>();
		map.put("creatorName", "赵凯'1213'sdfs");
		map.put("CO_DESC", "赵凯'1211");1125774
		client.prepareUpdate("mdmindex", "t_mat", "771814").setDoc(map).get();*/
//		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
//	   SearchResponse resp = client.prepareSearch("mdmindex")
//	   	.setTypes("t_mat")
//	   	.setQuery(boolQuery
//	   			.filter(QueryBuilders.termsQuery("CO_ATTRLANGUAGE", "zh","all"))
//	   			.filter(QueryBuilders.matchQuery("CO_VALID", "1"))
//	   			)
//	   	.get();
//	   SearchHit[] hits = resp.getHits().getHits();
//	   for(SearchHit hit : hits){
////		   System.out.println(hit.getSource().get("creatorName")==null);
//		   System.out.println(hit.getSourceAsString());
//	   }
//	   SearchResponse resp1 = client.prepareSearch("mdmindex")
//			   	.setTypes("t_")
//			   	.setPostFilter(boolQuery)
//			   	.get();
//	   SearchHit[] hits1 = resp1.getHits().getHits();
//	   System.out.println("num = " + String.valueOf(resp1.getHits().getTotalHits()));
//	   for(SearchHit hit1 : hits1){
//		   System.out.println(hit1.getSource().get("CO_MODIFYTIME")==null); 
//		   System.out.println("".equals(hit1.getSource().get("CO_MODIFYTIME"))); 
//	   }
	   /*TermsAggregationBuilder field = AggregationBuilders.terms("CVPOSTAL_GB").field("CVPOSTAL.keyword");
		TermsAggregationBuilder field1 = AggregationBuilders.terms("CVCITY_GB").field("CVCITY.keyword");
		field.subAggregation(field1);
		SearchResponse searchResponse = client
				.prepareSearch("mdmindex")
				.setTypes("t_customer")
				.addAggregation(field)
				.get();
		Terms aggs = searchResponse.getAggregations().get("CVPOSTAL_GB");
		System.out.println(searchResponse);
		for(Terms.Bucket entry :aggs.getBuckets()){
			Object key = entry.getKey();
			long docCount = entry.getDocCount();
			System.out.println("key="+key +" 聚合数目=" +String.valueOf(docCount));
			StringTerms  terms  = entry.getAggregations().get("CVCITY_GB");
			for(StringTerms.Bucket subentry : terms.getBuckets()){
				System.out.println("subkey="+subentry.getKey()+"子聚合数目为="+String.valueOf(subentry.getDocCount()));
			}
		}*/
		/*String sql = "select * from tbmb where bmid=189";
		LinkedHashMap<String, Object> rs = DbOperation.executelinkMap(sql);
		System.out.println(rs.get("NAME_VE"));
		BulkRequestBuilder bulk = null;
		if(1==1){
			bulk = client.prepareBulk();
			bulk.add(client.prepareUpdate("", "", "").setDoc(rs));
			System.out.println(bulk);
		}
		System.out.println(bulk);*/
		
//		boolean a = existWordsOrNot("1213");
//		System.out.println(a);
//		System.out.println(Integer.valueOf("121"));
//		float rint = Math.round(12.523);
//		System.out.println(Double.valueOf("123"));
//	 System.out.println("四舍五入取整:(2.5)=" + new BigDecimal("2.567").setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
//		String op = ZK.TALL.getOP();
//		System.out.println(op);
		
 	}
	public static String getPropertyLimit(String str,int num,String regex) throws Exception{
		if(num>-1){
			if(str!=null){
				return str.split("\\"+regex)[num-1];
			}
		}else{
			throw new Exception("输入数字不正确，请重新输入！");
		}
		return str;
	}
	public static boolean existWordsOrNot(String inputstr){
		Pattern pattern = Pattern.compile(ESConstant.regex_word);
		Matcher matcher = pattern.matcher(inputstr);
		boolean flag = false;
		if(matcher.find()) flag = true;
		else flag = false;
		
		return flag;
	}
	public static void deleteDatas(String sql){
		PreparedStatement pre = null;
		Connection con = null;
		boolean flag = false;
		Properties pro = new Properties();
		try {
			InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("Database.properties");
			pro.load(in);
			if(in!= null){
				in.close();
			}
			String driver = pro.getProperty("OracleDriver");
			String url = pro.getProperty("ESDB_Url");// 127.0.0.1是本机地址，XE是精简版Oracle的默认数据库名
			String user = pro.getProperty("ESDB_User");// 用户名,系统默认的账户名
			String password = pro.getProperty("ESDB_Password");// 你安装时选设置的密码
			Class.forName(driver);
			con = DriverManager.getConnection(url, user, password);
			pre = con.prepareStatement(sql);
			flag = pre.execute();
			System.out.println(flag);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(con!=null){
				try {
					con.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(pre!= null){
				try {
					pre.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
	}
}
