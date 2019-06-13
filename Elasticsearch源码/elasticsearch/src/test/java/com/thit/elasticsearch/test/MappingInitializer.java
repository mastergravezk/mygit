package com.thit.elasticsearch.test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.function.Function;
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
		
		String sql = "select distinct tablename from cl_catalog where tablename='T_MAT'";
		List<String> dt = DbOperation.executeArrayList(sql);
		//增加张雪飞的那几个字段
		Map<String,Object> fields = new HashMap<String,Object>();
		Map<String,Object> fields_child = new HashMap<String,Object>();
		fields_child.put("type", "keyword");
		fields_child.put("ignore_above", "2000");
		fields.put("keyword", fields_child);
		//添加ik自定义分词
		Map<String,Object> ik_fields_child = new HashMap<String,Object>();
		ik_fields_child.put("type", "text");
		ik_fields_child.put("analyzer", "ik_max_word");
		fields.put("ikword", ik_fields_child);
		
		
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
						.field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("SYS").field("type", "text").field("fields", fields).endObject()
//						.startObject("CO_SYSTEMID").field("type", "text").field("fields", fields)
//						.startObject("CO_DESC").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("CO_ID_MARK").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("CO_CREATER").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("CO_SYSTEMID").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("CO_MANUALCODE").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATNUM").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATDESCC").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATNAME").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATDESCS").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATBMAT").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATTYPE_ID").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATTYPE").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATBUNIT_ID").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATBUNIT").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATWUNIT_ID").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATWUNIT").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATVUNIT").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATDXLG").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("ZCZMS").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("ZCZBM").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATGRUP").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("ZADDINFO").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATVOCH").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("ZADDINFO").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("BACKUP2").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("BACKUP3").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("OLDCODE").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("ZMODEL").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("MATGRUP_ID").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("SYSNUM").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("SYSCODE").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("INDSDESC").field("type", "text").field("analyzer", "ik_max_word").endObject()
//						.startObject("ELASTICSEARCH").field("type", "text").field("fields", fields).endObject()
//						.startObject("ELASTICSEARCH1").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_ID_MARK").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_CREATER").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_SYSTEMID").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_MANUALCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("MATNUM").field("type", "text").field("fields", fields).endObject()
						.startObject("MATDESCC").field("type", "text").field("fields", fields).endObject()
						.startObject("MATNAME").field("type", "text").field("fields", fields).endObject()
						.startObject("MATDESCS").field("type", "text").field("fields", fields).endObject()
						.startObject("MATBMAT").field("type", "text").field("fields", fields).endObject()
						.startObject("MATTYPE_ID").field("type", "text").field("fields", fields).endObject()
						.startObject("MATTYPE").field("type", "text").field("fields", fields).endObject()
						.startObject("MATBUNIT_ID").field("type", "text").field("fields", fields).endObject()
						.startObject("MATBUNIT").field("type", "text").field("fields", fields).endObject()
						.startObject("MATWUNIT_ID").field("type", "text").field("fields", fields).endObject()
						.startObject("MATWUNIT").field("type", "text").field("fields", fields).endObject()
						.startObject("MATVUNIT").field("type", "text").field("fields", fields).endObject()
						.startObject("MATDXLG").field("type", "text").field("fields", fields).endObject()
						.startObject("ZCZMS").field("type", "text").field("fields", fields).endObject()
						.startObject("ZCZBM").field("type", "text").field("fields", fields).endObject()
						.startObject("MATGRUP").field("type", "text").field("fields", fields).endObject()
						.startObject("ZADDINFO").field("type", "text").field("fields", fields).endObject()
						.startObject("MATVOCH").field("type", "text").field("fields", fields).endObject()
						.startObject("ZADDINFO").field("type", "text").field("fields", fields).endObject()
						.startObject("BACKUP2").field("type", "text").field("fields", fields).endObject()
						.startObject("BACKUP3").field("type", "text").field("fields", fields).endObject()
						.startObject("OLDCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("ZMODEL").field("type", "text").field("fields", fields).endObject()
						.startObject("OLDCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("MATGRUP_ID").field("type", "text").field("fields", fields).endObject()
						.startObject("SYSNUM").field("type", "text").field("fields", fields).endObject()
						.startObject("SYSCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("INDSDESC").field("type", "text").field("fields", fields).endObject()
						
						//扩充的字段
//						.startObject("EXTRA").field("type", "long").endObject()
//						.startObject("EXTRB").field("type", "integer").endObject()
//						.startObject("EXTRC").field("type", "integer").endObject()
//						.startObject("EXTRD").field("type", "byte").endObject()
//						.startObject("EXTRE").field("type", "text").field("fields", fields).endObject()
//						.startObject("EXTRF").field("type", "text").field("fields", fields).endObject()
//						.startObject("EXTRG").field("type", "text").field("fields", fields).endObject()
//						.startObject("EXTRH").field("type", "text").field("fields", fields).endObject()
//						.startObject("EXTRI").field("type", "text").field("fields", fields).endObject()
//						.startObject("EXTRG").field("type", "text").field("fields", fields).endObject()
						.endObject()
						.endObject()
						.endObject();
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
						.startObject("CO_VALID").field("type", "byte").endObject()
						.startObject("IMPORTUSER").field("type", "integer").endObject()
						.startObject("IMPORTSEQ").field("type", "integer").endObject()
						.startObject("CO_CREATETIME").field("type", "long").endObject()
						.startObject("CO_PUBLISHTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_SUBMITTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_MODIFYTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_DELTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("SYS").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_SYSTEMID").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_DESC").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_ID_MARK").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_SYSTEMID").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_MANUALCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("CVNAME").field("type", "text").field("fields", fields).endObject()
						.startObject("VVDNUM").field("type", "text").field("fields", fields).endObject()
						.startObject("CVSEARCH1").field("type", "text").field("fields", fields).endObject()
						.startObject("CVSEARCH2").field("type", "text").field("fields", fields).endObject()
						.startObject("CVPOSTAL").field("type", "text").field("fields", fields).endObject()
						.startObject("CVCITY").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTEL").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTELEXT").field("type", "text").field("fields", fields).endObject()
						.startObject("CVFAX").field("type", "text").field("fields", fields).endObject()
						.startObject("CVMOBILE").field("type", "text").field("fields", fields).endObject()
						.startObject("VVDGP").field("type", "text").field("fields", fields).endObject()
						.startObject("CVSTREET").field("type", "text").field("fields", fields).endObject()
						.startObject("CVCOUN").field("type", "text").field("fields", fields).endObject()
						.startObject("CVSTATE").field("type", "text").field("fields", fields).endObject()
						.startObject("CVGCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTAXREG").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTAXREG").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTAX1").field("type", "text").field("fields", fields).endObject()
						.startObject("CHAR01").field("type", "text").field("fields", fields).endObject()
						.startObject("VGDPDESC").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_FLANS").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_MAPS").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_PLOGS").field("type", "text").field("fields", fields).endObject()
						.startObject("SYS").field("type", "text").field("fields", fields).endObject()
						.startObject("SYSCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("SYSNUM").field("type", "text").field("fields", fields).endObject()
						//扩充的字段
						.endObject()
						.endObject()
						.endObject();
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
						.startObject("CO_DELTIME").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
						.startObject("CO_SYSTEMID").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_DESC").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_CREATEDEPT").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_MODIFYORG").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_ID_MARK").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_MODIFYDEPT").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_MODIFYUSER").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_CREATER").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_CREATORG").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_MANUALCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("CVREMARK").field("type", "text").field("fields", fields).endObject()
						.startObject("CVCITY").field("type", "text").field("fields", fields).endObject()
						.startObject("CVFAX").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTELEXT").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTEL").field("type", "text").field("fields", fields).endObject()
						.startObject("CVEMAIL").field("type", "text").field("fields", fields).endObject()
						.startObject("CVNAME").field("type", "text").field("fields", fields).endObject()
						.startObject("CVOLDNAME").field("type", "text").field("fields", fields).endObject()
						.startObject("CVMOBILE").field("type", "text").field("fields", fields).endObject()
						.startObject("CVSEARCH1").field("type", "text").field("fields", fields).endObject()
						.startObject("CVSEARCH2").field("type", "text").field("fields", fields).endObject()
						.startObject("CVPOSTAL").field("type", "text").field("fields", fields).endObject()
						.startObject("CVVDNUM").field("type", "text").field("fields", fields).endObject()
						.startObject("CVCOUN_ID").field("type", "text").field("fields", fields).endObject()
						.startObject("CVSTREET").field("type", "text").field("fields", fields).endObject()
						.startObject("CCUNUM").field("type", "text").field("fields", fields).endObject()
						.startObject("CVVDNUM_ID").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTDPT_ID").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTDPT").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTAXREG").field("type", "text").field("fields", fields).endObject()
						.startObject("CHAR01").field("type", "text").field("fields", fields).endObject()
						.startObject("CVSTATE").field("type", "text").field("fields", fields).endObject()
						.startObject("CVGCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("CVTAX1").field("type", "text").field("fields", fields).endObject()
						.startObject("CVFAXEXT").field("type", "text").field("fields", fields).endObject()
						.startObject("CVBKACCT").field("type", "text").field("fields", fields).endObject()
						.startObject("CUGPDESC").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_FLANS").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_MAPS").field("type", "text").field("fields", fields).endObject()
						.startObject("CO_PLOGS").field("type", "text").field("fields", fields).endObject()
						.startObject("SYS").field("type", "text").field("fields", fields).endObject()
						.startObject("SYSCODE").field("type", "text").field("fields", fields).endObject()
						.startObject("SYSNUM").field("type", "text").field("fields", fields).endObject()
						//扩充的字段
						.endObject()
						.endObject()
						.endObject();
			}
			System.out.println(jsonBuilder.string());
			boolean flag = ESInitializer.createMappings(index, tablename.toLowerCase(),jsonBuilder);
			if(flag)
				System.out.println("成功创建字段映射！");
			else
				System.out.println("创建字段映射失败！");
		}
	}
//	public static void analyzeTest(){
//		String content = "凯神得分词";
//		AnalyzeRequestBuilder ikRequest = new AnalyzeRequestBuilder(ESInitializer.initClient(),AnalyzeAction.INSTANCE,"mdmindex",content);
//		ikRequest.setTokenizer("ik_max_word");
//		List<AnalyzeResponse.AnalyzeToken> ikTokenList = ikRequest.execute().actionGet().getTokens();
//		//循环赋值
//		List<String> searchTermList = new ArrayList<>();
//		
//		ikTokenList.forEach(ikToken -> { searchTermList.add(ikToken.getTerm()); 
//		
//		System.out.println(ikToken.getTerm());
//		});
//	}
//	
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
