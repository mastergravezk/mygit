package com.thit.elasticsearch.metadb;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.thit.elasticsearch.common.ESConstant;
import com.thit.elasticsearch.orcldb.DbOperation;

/**
 * 扩充字段的方法，其实就是将添加的字段重新添加到mapping中重新put一下就行了
 * @author zk
 *
 */
public class MappingInitializer {
	public static void main(String[] args) throws IOException{
		
		String index = "mdmindex";
		
		String sql = "select distinct tablename from cl_catalog";
		List<String> dt = DbOperation.executeArrayList(sql);
		
		//增加张雪飞的那几个字段
		Map<String,Object> fields = new HashMap<String,Object>();
		Map<String,Object> fields_child = new HashMap<String,Object>();
		fields_child.put("type", "keyword");
		fields_child.put("ignore_above", "2000");
		fields.put("keyword", fields_child);
		//添加ik自定义分词
//		Map<String,Object> ik_fields_child = new HashMap<String,Object>();
//		ik_fields_child.put("type", "text");
//		ik_fields_child.put("analyzer", "ik_max_word");
//		fields.put("ikword", ik_fields_child);
//		fields.put("", "");
		
		for(String tablename : dt){
			System.out.println("表名："+tablename);
			if("".equals(tablename)||tablename==null){
				continue;
			}
			XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
			if("T_MAT".equalsIgnoreCase(tablename)||"T_CUSTOMER".equalsIgnoreCase(tablename)||"T_VENDOR".equalsIgnoreCase(tablename)){
				jsonBuilder.startObject()
						.startObject(tablename.toLowerCase())
						.startObject("properties")
						.startObject("CO_FREEZE").field("type", "byte").endObject()
						.startObject("CO_HASCHILD").field("type", "byte").endObject()
						.startObject("CO_DEL").field("type", "byte").endObject()
						.startObject("CO_PID").field("type", "integer").endObject()
						.startObject("CO_VALID").field("type", "byte").endObject()
						.startObject("CO_ID").field("type", "integer").endObject()
						.startObject("CO_CREATETIME").field("type", "long").endObject()
						.startObject("CO_PUBLISHTIME").field("type", "date")
						.field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_SUBMITTIME").field("type", "date")
						.field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_MODIFYTIME").field("type", "date")
						.field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_DELTIME").field("type", "date")
						.field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject();
//						.startObject("CO_CREATEDEPT").field("type","text").field("analyzer", "ik_max_word").field("fields",fields).endObject()
//						.startObject("CO_CREATEORG").field("type","text").field("analyzer", "ik_max_word").field("fields",fields).endObject()
//						.startObject("CO_CREATE").field("type","text").field("analyzer", "ik_max_word").field("fields",fields).endObject()
//						.startObject("CO_MODIFYORG").field("type","text").field("analyzer", "ik_max_word").field("fields",fields).endObject()
//						.startObject("CO_MODIFYDEPT").field("type","text").field("analyzer", "ik_max_word").field("fields",fields).endObject();
						
				List<String> ignore_cols = new ArrayList<>();
				ignore_cols.add("CO_FREEZE");
				ignore_cols.add("CO_HASCHILD");
				ignore_cols.add("CO_DEL");
				ignore_cols.add("CO_PID");
				ignore_cols.add("CO_VALID");
				ignore_cols.add("CO_ID");
				ignore_cols.add("CO_CREATETIME");
				ignore_cols.add("CO_PUBLISHTIME");
				ignore_cols.add("CO_SUBMITTIME");
				ignore_cols.add("CO_MODIFYTIME");
				ignore_cols.add("CO_DELTIME");
				
				jsonBuilder = ESMappingUtil.getMappingBuilder(jsonBuilder, tablename, ignore_cols);
			}else if(tablename.endsWith("_temp")){
				jsonBuilder.startObject()
						.startObject(tablename.toLowerCase())
						.startObject("properties")
						.startObject("CO_FREEZE").field("type", "byte").endObject()
						.startObject("CO_HASCHILD").field("type", "byte").endObject()
						.startObject("CO_DEL").field("type", "byte").endObject()
						.startObject("CO_PID").field("type", "integer").endObject()
						.startObject("CO_VALID").field("type", "byte").endObject()
						.startObject("CO_ID").field("type", "integer").endObject()
//						.startObject("CO_VALID").field("type", "byte").endObject()
						.startObject("IMPORTUSER").field("type", "integer").endObject()
						.startObject("IMPORTSEQ").field("type", "integer").endObject()
						.startObject("CO_CREATETIME").field("type", "long").endObject()
						.startObject("CO_PUBLISHTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_SUBMITTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_MODIFYTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_DELTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject();
				
				List<String> ignore_cols = new ArrayList<>();
				ignore_cols.add("CO_FREEZE");
				ignore_cols.add("CO_HASCHILD");
				ignore_cols.add("CO_DEL");
				ignore_cols.add("CO_PID");
				ignore_cols.add("CO_VALID");
				ignore_cols.add("CO_ID");
				ignore_cols.add("IMPORTUSER");
				ignore_cols.add("IMPORTSEQ");
				ignore_cols.add("CO_CREATETIME");
				ignore_cols.add("CO_PUBLISHTIME");
				ignore_cols.add("CO_SUBMITTIME");
				ignore_cols.add("CO_MODIFYTIME");
				ignore_cols.add("CO_DELTIME");
				
				jsonBuilder = ESMappingUtil.getMappingBuilder(jsonBuilder, tablename, ignore_cols);
				
			}else {
				jsonBuilder.startObject()
						.startObject(tablename.toLowerCase())
						.startObject("properties")
						.startObject("CO_FREEZE").field("type", "byte").endObject()
						.startObject("CO_HASCHILD").field("type", "byte").endObject()
						.startObject("CO_DEL").field("type", "byte").endObject()
						.startObject("CO_PID").field("type", "integer").endObject()
						.startObject("CO_VALID").field("type", "byte").endObject()
						.startObject("CO_ID").field("type", "integer").endObject()
						.startObject("CO_CREATETIME").field("type", "long").endObject()
						.startObject("CO_PUBLISHTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_SUBMITTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_MODIFYTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_DELTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject();
						
				List<String> ignore_cols = new ArrayList<>();
				ignore_cols.add("CO_FREEZE");
				ignore_cols.add("CO_HASCHILD");
				ignore_cols.add("CO_DEL");
				ignore_cols.add("CO_PID");
				ignore_cols.add("CO_VALID");
				ignore_cols.add("CO_ID");
				ignore_cols.add("CO_CREATETIME");
				ignore_cols.add("CO_PUBLISHTIME");
				ignore_cols.add("CO_SUBMITTIME");
				ignore_cols.add("CO_MODIFYTIME");
				ignore_cols.add("CO_DELTIME");		
				jsonBuilder = ESMappingUtil.getMappingBuilder(jsonBuilder, tablename, ignore_cols);
			}
			System.out.println(jsonBuilder.string());
			boolean flag = ESInitializer.createMappings(index, tablename.toLowerCase(),jsonBuilder);
			if(flag)
				System.out.println("成功创建字段映射！");
			else
				System.out.println("创建字段映射失败！");
		}
	}
	public static void analyzeTest(){
		String content = "凯神得分词";
		AnalyzeRequestBuilder ikRequest = new AnalyzeRequestBuilder(ESInitializer.initClient(),AnalyzeAction.INSTANCE,"mdmindex",content);
		ikRequest.setTokenizer("ik_max_word");
		List<AnalyzeResponse.AnalyzeToken> ikTokenList = ikRequest.execute().actionGet().getTokens();
		//循环赋值
		List<String> searchTermList = new ArrayList<>();
		
		ikTokenList.forEach(ikToken -> { searchTermList.add(ikToken.getTerm()); 
		
		System.out.println(ikToken.getTerm());
		});
	}
	
	public static boolean existChinese(String inputstr){
		Pattern pattern = Pattern.compile(ESConstant.regex);
		Matcher matcher = pattern.matcher(inputstr);
		boolean flag = false;                  
		if(matcher.find()){
			flag = true;
		} else {
			flag = false;
		}
		return flag;
	}
}
