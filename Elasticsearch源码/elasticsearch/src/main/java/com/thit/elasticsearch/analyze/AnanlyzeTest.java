package com.thit.elasticsearch.analyze;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.segments.IndexSegments;
import org.elasticsearch.action.admin.indices.segments.IndexShardSegments;
import org.elasticsearch.action.admin.indices.segments.IndicesSegmentResponse;
import org.elasticsearch.action.admin.indices.segments.ShardSegments;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.engine.Segment;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;

//import com.thit.elasticsearch.test.ESInitializer;



public class AnanlyzeTest {
	
//	private static Client client;
	/**
	 * 测试分词效果工具
	 * @param index 索引
	 * @param content 要分词的内容 
	 * @param tokenizer 分析器 ik分词器中的ik_smart和ik_max_word的区别是分词的粒度不同ik_max_word更细一些
	 * @param charFilter 字符过滤器
	 * @param tokenFilter 分词过滤器
	 * @return
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static List analyzeTest(Client client,String index,String content,String tokenizer,String charFilter,String tokenFilter) throws InterruptedException, ExecutionException{
//		Settings settings = Settings.builder().put("cluster.name", "mdmjt")//指定集群的名称
//				.put("client.transport.sniff", true)
//				.put("path.home", "D:/Apacheitems/elasticsearch-5.6.3.zk/plugins/ik/config/IKAnalyzer.cfg.xml")
//				.build();//如果有节点加入集群将自动检测加入
//	    //加载字典
//		Configuration config = new Configuration(new Environment(settings), settings);
//		Dictionary dict = Dictionary.getSingleton();
//		
//		List<String> words = new ArrayList<>();
//		words.add("ro-s-s");
//		words.add("o-s-s");
//        words.add("芳香-L-氨基酸脱羧酶类");
//        words.add("atp柠檬酸（pro-S）裂合酶");
        IndicesSegmentResponse res = client.admin().indices().prepareSegments("mdmindex").execute().get();
        Map<String, IndexSegments> indices = res.getIndices();
        for(String key : indices.keySet()){
        	IndexSegments indexSegments = indices.get(key);
        	Iterator<IndexShardSegments> iterator = indexSegments.iterator();
        	while(iterator.hasNext()){
        		IndexShardSegments next = iterator.next();
        		Iterator<ShardSegments> iterator2 = next.iterator();
        		while(iterator2.hasNext()){
        			ShardSegments next2 = iterator2.next();
        			List<Segment> segments = next2.getSegments();
        			for(Segment seg : segments){
//        				seg.readFrom();
        			}
        		}
        	}
        }
		AnalyzeRequestBuilder ikRequest = new AnalyzeRequestBuilder(client,AnalyzeAction.INSTANCE,index,content);
		
		ikRequest.setTokenizer(tokenizer);//分析器
//        ikRequest.addCharFilter("stopwords").addCharFilter(new HashMap<String, String>().put("stopwords", ","));//字符过滤器
//        ikRequest.addCharFilter("lowercase");//字符过滤器
//        ikRequest.
//        ikRequest.addTokenFilter(tokenFilter);//分词过滤器
//        dict.addWords(words);//添加分词集合
        List<AnalyzeResponse.AnalyzeToken> ikTokenList = ikRequest.execute().actionGet().getTokens();
        
        //循环赋值
        List<String> searchTermList = new ArrayList<>();
//        System.out.println("middl结果："+content.substring(ikTokenList.get(0).getEndOffset(), ikTokenList.get(ikTokenList.size()-1).getStartOffset()));
        ikTokenList.forEach(ikToken -> { 
        	searchTermList.add(ikToken.getTerm()); 
        System.out.println(ikToken.getTerm());
        });
        return searchTermList;
	}
	public static void main(String[] args) throws UnknownHostException, InterruptedException, ExecutionException {
//		try {
//			Client client = SerachTest.initClient();
//			List<String> strs = analyzeTest(client, "mdmindex", "sadf-dsaf", "standard", "stop", "stop");
//			for(String token : strs){
//				System.out.println(token);
//			}
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		List<String> res = queryContent("志向(心理学)芳香-L-氨基酸脱羧酶类，atp柠檬酸（pro-S）裂合酶");
//		List<String> res = queryContent("pro-s-sdf/adsd(hs)we-q");
//		List<String> res = queryContent("我是中国人质");
//		analyzeTest(SerachTest.initClient(), "mdmindex", "prO-s-sdf/adsd(hs)we-qw-eq", "ik_max_word", null, null);
//		analyzeTest(ESInitializer.initClient(), "mdmindex", "BWPCLNT800;HUCCLNT800", "standard", null, null);
//		queryContent("今天qqro-s-s");
	}
	
	public static List<String> queryContent(String content){
		Settings settings = Settings.builder().put("cluster.name", "mdmjt")//指定集群的名称
		.put("client.transport.sniff", true)
//		.put("path.home", "D:/Apacheitems/elasticsearch-5.6.3.zk/plugins/ik/config/IKAnalyzer.cfg.xml")
		.put("path.home", "D:/Apacheitems/plugins/ik")
		.build();//如果有节点加入集群将自动检测加入
		//加载字典
		Configuration config = new Configuration(new Environment(settings), settings);
		//添加字典分词
		Dictionary dict = Dictionary.getSingleton();
		
		List<String> words = new ArrayList<>();
		words.add("ro-s-s");
		words.add("f/ads");
		words.add("今天qq");
//        words.add("芳香-L-氨基酸脱羧酶类");
//        words.add("atp柠檬酸（pro-S）裂合酶");
        dict.addWords(words);//添加分词集合
		
//		List<String> words2 = new ArrayList<>();
//		words2.add("-");
//		words2.add("推出");
//		dict.dis
        //做的工作就是如果查询的内容在词库内如果有的话就会命中hit
		Hit hit = dict.matchInMainDict("t2y2".toCharArray());
		//如果查询的内容没有在词库中就可以先将内容添加到词库中
		if(!hit.isMatch()){
			
		}
        System.out.println(hit.isMatch());
		List<String> res = new ArrayList<>();
		StringReader reader = new StringReader(content);
		IKSegmenter iks = new IKSegmenter(reader, config);
//		Lexeme next = iks.next();
		try{
			System.out.println("-------------------分词结果-----------------");
			for(Lexeme pri = iks.next(); pri!=null;pri = iks.next()){
				res.add(pri.getLexemeText());
				System.out.print(pri.getLexemeText()+"  ");
			}
			
		}catch(Exception e ){
			e.printStackTrace();
		}
		return res;
	}
	
	
	
}
