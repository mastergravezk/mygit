package com.thit.elasticsearch.common;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.thit.elasticsearch.orcldb.DbOperation;
import com.xicrm.common.TXISystem;
import com.xicrm.util.TXIUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
/**
 * 
 * @author zk
 * @version 2018-1-24
 * @since 1.8
 */
public class ESUtil {
	//静态代码快方式单例创建Client连接(也可以用内部类方式实现)
	private  static Client client = null;
	public  static final String ClusterName = TXISystem.config.getProperty("ClusterName", "mdmjt");//集群名称
	public  static final String ESNodeServerIP = TXISystem.config.getProperty("ESServerIP", "192.168.2.33"); //ES服务主节点ip
	public  static final String ESNodeServerPort = TXISystem.config.getProperty("ESNodeServerPort", "9300"); //主节点端口
	public  static final String XpackUserPassWord = TXISystem.config.getProperty("XpackUserPassWord", "elastic:changeme");
	private ESUtil(){
		
	}
	static{
		try {
			client = initClient(getMasterNodeTransAddress());
		} catch (NumberFormatException e) {
			System.err.println("字符串转整型时出错......");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			System.err.println("ip地址不存在，重新检查......");
			e.printStackTrace();
		}
	}
	//对外提供方法得到实例
	public static Client getClient() {
		return client;
	}
	//1.全文搜索：将前面的值
	/**
	 * 单例模式创建
	 * @param transaddess
	 * @return
	 */
	public static Client initClient(List<InetSocketTransportAddress> transaddess){
		if(client==null){
			Settings settings = Settings.builder()
					.put("cluster.name", ClusterName)//指定集群的名称
					.put("client.transport.sniff", true)//如果有节点加入集群将自动检测加入
					.put("xpack.security.user", XpackUserPassWord)//登录验证
					.build();
			client = new PreBuiltXPackTransportClient(settings)
					.addTransportAddresses(transaddess.toArray(new InetSocketTransportAddress[transaddess.size()]));
					
//			client = new PreBuiltTransportClient(settings)
//					.addTransportAddresses(transaddess.toArray(new InetSocketTransportAddress[transaddess.size()]));
//						.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("192.168.2.11"), 9300))
//						.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ESNodeServerIP),Integer.valueOf(ESNodeServerPort)));
			System.out.println("客户端创建完成与ES建立连接......");
		}
		
		return client;
	}
	/**
	 * ES集群里边可能有多个主节点
	 * @return
	 * @throws NumberFormatException
	 * @throws UnknownHostException
	 */
	public static List<InetSocketTransportAddress> getMasterNodeTransAddress() throws NumberFormatException, UnknownHostException{
		List<InetSocketTransportAddress> ip_ports = new ArrayList<InetSocketTransportAddress>();
		String[] ips = ESNodeServerIP.split(";");
		String[] ports = ESNodeServerPort.split(";");
		for(int i=0; i<ips.length; i++){
			ip_ports.add(new InetSocketTransportAddress(InetAddress.getByName(ips[i]), Integer.valueOf(ports[i])));
		}
		return ip_ports;
	}
	/**
	 * 四舍五入计算
	 * @param number 输入数字
	 * @param roundindex 保留几位小数
	 * @return
	 */
	public static String getRoundNumber(String number,int roundindex){
		BigDecimal roundstr = new BigDecimal(number).setScale(roundindex, BigDecimal.ROUND_HALF_UP);
		return roundstr.toString();
	}
	/**
	 * 判断字符串里边有没有字母
	 * @param inputstr
	 * @return
	 */
	public static boolean existWordsOrNot(String inputstr){
		Pattern pattern = Pattern.compile(ESConstant.regex_word);
		Matcher matcher = pattern.matcher(inputstr);
		boolean flag = false;
		if(matcher.find()){
			flag = true;
		} else {
			flag = false;
		}
		return flag;
	}
//	public static QueryBulider get
	/**
	 * 是否存在中文
	 * @param inputstr
	 * @return
	 */
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
	/**
	 * 是否全部是中文
	 * @param conditions
	 * @return
	 */
	public static boolean allChinese(String conditions) {
		// TODO Auto-generated method stub
		Pattern pattern = Pattern.compile(ESConstant.regex);
		boolean flag = true;
		for(char c : conditions.toCharArray()){
			Matcher matcher = pattern.matcher(Character.toString(c));
			if(matcher.find()){
				
			}else{
				return false;
			}
		}
		return true;
	}
	/**
	 * 测试分词效果工具
	 * @param index 索引
	 * @param content 要分词的内容 
	 * @param tokenizer 分析器 ik分词器中的ik_smart和ik_max_word的区别是分词的粒度不同ik_max_word更细一些
	 * @param charFilter 字符过滤器
	 * @param tokenFilter 分词过滤器
	 * @return
	 */
	public static List analyzeTest(String index,String content,String tokenizer,String charFilter,String tokenFilter){
		AnalyzeRequestBuilder ikRequest = new AnalyzeRequestBuilder(client,AnalyzeAction.INSTANCE,index,content);
        ikRequest.setTokenizer(tokenizer);//分析器
        ikRequest.addCharFilter(charFilter);//字符过滤器
        ikRequest.addTokenFilter(tokenFilter);//分词过滤器
        List<AnalyzeResponse.AnalyzeToken> ikTokenList = ikRequest.execute().actionGet().getTokens();
        //循环赋值
        List<String> searchTermList = new ArrayList<>();
        System.out.println("分词结果：");
        ikTokenList.forEach(ikToken -> { searchTermList.add(ikToken.getTerm()); 
        System.out.println(ikToken.getTerm());
        });
        return searchTermList;
	}
//	public static BoolQueryBuilder 
    /**
     * @param regex 分隔符
     * @param str 要分割的字符串 如：0|1|2|3|4
     * @param num 要截取的那一段的
     * @return
     * @throws Exception
     */
	public static String getPropertyLimit(String str,int num,String regex) throws Exception{
		if(num>-1){
			if(str!=null){
				//记得要转译特殊字符
				return str.split("\\"+regex)[num-1];
			}
		}else{
			throw new Exception("输入数字不正确，请重新输入！");
		}
		return str;
	}
	/**
	 * 分析器分析字符串
	 * @param index 索引
	 * @param termsStr 要分析的字符串
	 * @return
	 */
	public static List annalyzeStr(String index,String termsStr,String analyzer){
		//将token中分词后的内容，除了最后一个term进行模糊查询之外，另外的都进行准确匹配
		AnalyzeResponse resp = client.admin().indices().prepareAnalyze(index, termsStr).setAnalyzer(analyzer).get();
		List<AnalyzeToken> tokens = resp.getTokens();
//		List list = new ArrayList();
//		for (AnalyzeToken token : tokens) {
//			String term = token.getTerm();
//			System.out.println(term);
//			list.add(term);
//		}
		return tokens;
	}
	
//	public static void analyzerTest(){
//		AnalyzeRequestBuilder analyzerbuilder = new AnalyzeRequestBuilder(client, action, index, text);
//		analyzerbuilder.
//	}
	/**
	 * 全文搜索
	 * @param str 查询条件
	 * @param must 将分词之后的bulider加入到布尔查询的的builder中
	 * @return 返回布尔查询的builder
	 */
	@Deprecated
	public static BoolQueryBuilder splitChineseToFullSearch(String tableName,String str,BoolQueryBuilder must){
//		String str = "ZZE_钢板件rtwer门板hret8yha校验adsiufgy997kdsna_回家sdfa噶三个";
		//匹配中文正则表达式
//		String regex = ESConstant.regex;
		
		String[] split = str.split("");
		Pattern pattern = Pattern.compile(ESConstant.regex);
		int start = -1,end = -1;
		boolean rs = false;
		OK:
		for(String sp : split){
			rs = pattern.matcher(sp).find();
			if(rs){
				break OK;
			}
		}
		if(rs){
			must.filter(QueryBuilders.queryStringQuery(str).escape(true));
			//以字符形式分开
			/*str = filterSpecialChars(str);
			String[] split1 = str.split("");
			for (int i = 0;i < split1.length;i++) {
				if(i==0){
					start = end=0;
				}else{
					start = end;
				}
				//查找字符串中是否有匹配正则表达式的字符/字符串
				rs = pattern.matcher(split1[i]).find();
				if (rs) {
					end = i;
					//第一个如果为中文的话输出就与下面的
					//i+1<split.length&&!pattern.matcher(split[i+1]).find()冲突
					//所以当第一个为中文时不
					if(i==0){
						
					}else{
						//特殊字符处理
						String sub = str.substring(start,end);
////						boolean exist = existSpecialChar(sub);
//						if(exist){
//							LinkedHashMap<String, Object> map = getLinkMap(tableName);
//							sub = QueryParser.escape(sub);
//							for(String col :map.keySet()){
//								MatchPhraseQueryBuilder specialchar = QueryBuilders.matchPhraseQuery(col, sub.toLowerCase());
//								must.filter(specialchar);
//							}
//						}else{
//						}
						QueryStringQueryBuilder qbNoChinese = QueryBuilders.queryStringQuery(sub.toLowerCase());
						must.filter(qbNoChinese);
					}
					//将非中文之前的中文与非中文分离（"人wqrew分成：人，wqrew"）
					if(i+1<split1.length&&!pattern.matcher(split1[i+1]).find()){
						end =i+1;
						QueryStringQueryBuilder qbChinese = QueryBuilders.queryStringQuery(split1[i].toLowerCase());
						must.filter(qbChinese);
						System.out.println(split1[i]);
						//将最后一个为中文的字符显示出来	
					}else if(i+1==split1.length){
						QueryStringQueryBuilder qbChinese = QueryBuilders.queryStringQuery(split1[i].toLowerCase());
						must.filter(qbChinese);
						System.out.println(split1[i]);
					}
				}
				//如果最后是非中文的话，将最后部分查询条件也加上
				if(end+1<split1.length){
					QueryStringQueryBuilder qbNoChinese = QueryBuilders.queryStringQuery(str.toLowerCase());
					must.filter(qbNoChinese);
					System.out.println(str.substring(end));
				}
			}*/
			
		}else{
			//如果是没有中文的查询内容那么遍历字段对每个字段进行模糊查询
			LinkedHashMap<String, Object> map = getLinkMap(tableName);
//			boolean exist = existSpecialChar(str);
			//含有特殊字符处理
//			str = QueryParser.escape(str);
			for(String col :map.keySet()){
				MatchPhraseQueryBuilder specialchar = QueryBuilders.matchPhraseQuery(col, str.toLowerCase());
				must.should(specialchar);
			}
			/*if(exist){
			//不含特殊字符
			}else{
				for(String col : map.keySet()){
					WildcardQueryBuilder qbNoChinese = QueryBuilders.wildcardQuery(col, "*"+str.toLowerCase()+"*");
					must.should(qbNoChinese);
				}
			}*/
		}
		return must;
	}
	//主要用于得到表的字段
	public static LinkedHashMap<String, Object> getLinkMap(String tableName){
		String sql = "select * from "+tableName+" where rownum<2"; 
		LinkedHashMap<String, Object> map = DbOperation.executelinkMap(sql);
		return map;
	}
	/**
	 * 组合条件搜索
	 * @param col 组合条件的字段
	 * @param val 组合条件的值
	 * @param must 布尔查询的bulider
	 * @return
	 */
	@Deprecated
	public static BoolQueryBuilder splitChineseToCombinSearch(String tableName,String col,String val,BoolQueryBuilder must){
		//特殊字符处理
//		String title ="+-&&||!(){}[]^\"~*?:\\";
//		val = org.apache.lucene.queryparser.classic.QueryParser.escape(title); // 主要就是这一句把特殊字符都转义,那么lucene就可以识别
		//现将字段值隔离
//		val = filterSpecialChars(val);
		String[] split = val.split("");
		//中文正则表达式分离中文,如果有特殊符号去除掉
		Pattern pattern = Pattern.compile(ESConstant.regex);
		int start = -1,end = -1;
		boolean rs = false;
		OK:
		for(String sp : split){
			rs = pattern.matcher(sp).find();
			if(rs){
				break OK;
			}
		}
		if(rs){
			//以字符形式分开
			val = filterSpecialChars(val);
			String[] split1 = val.split("");
			for (int i = 0;i < split1.length;i++) {
				if(i==0){
					start = end=0;
				}else{
					start = end;
				}
				//查找字符串中是否有匹配正则表达式的字符/字符串
				rs = pattern.matcher(split1[i]).find();
				if (rs) {
					end = i;
					//第一个如果为中文的话输出就与下面的
					//i+1<split.length&&!pattern.matcher(split[i+1]).find()冲突
					//所以当第一个为中文时不
					if(i==0){
						
					}else{
						//特殊字符处理
						String sub = val.substring(start,end);
//						boolean exist = existSpecialChar(sub);
//						if(exist){
//							LinkedHashMap<String, Object> map = getLinkMap(tableName);
//							sub = QueryParser.escape(sub);
//							for(String str :map.keySet()){
//								MatchPhraseQueryBuilder specialchar = QueryBuilders.matchPhraseQuery(str, sub.toLowerCase());
//								must.filter(specialchar);
//							}
//						}else{
//						}
						WildcardQueryBuilder qbNoChinese = QueryBuilders.wildcardQuery(col, "*"+sub.toLowerCase()+"*");
						must.filter(qbNoChinese);
					}
					//将非中文之前的中文与非中文分离（"人wqrew分成：人，wqrew"）
					if(i+1<split1.length&&!pattern.matcher(split1[i+1]).find()){
						end =i+1;
						WildcardQueryBuilder qbChinese = QueryBuilders.wildcardQuery(col, "*"+split1[i].toLowerCase()+"*");
						must.filter(qbChinese);
						System.out.println(split1[i]);
						//将最后一个为中文的字符显示出来	
					}else if(i+1==split1.length){
						WildcardQueryBuilder qbChinese = QueryBuilders.wildcardQuery(col, "*"+split1[i].toLowerCase()+"*");
						must.filter(qbChinese);
						System.out.println(split1[i]);
					}
				}
				//如果最后是非中文的话，将最后部分查询条件也加上
				if(end+1<split1.length){
					QueryStringQueryBuilder qbNoChinese = QueryBuilders.queryStringQuery(val.toLowerCase());
					must.filter(qbNoChinese);
					System.out.println(val.substring(end));
				}
			}
		}else{
			//查询内容没有中文
			boolean exist = existSpecialChar(val);
			//含有特殊字符处理
			if(exist){
				val = QueryParser.escape(val);
				MatchPhraseQueryBuilder specialchar = QueryBuilders.matchPhraseQuery(col, val.toLowerCase());
				must.filter(specialchar);
			//不含特殊字符
			}else{
//				val = replaceSpecialChars(val);
//				WildcardQueryBuilder qbNoChinese = QueryBuilders.wildcardQuery(col, "*"+val.toLowerCase()+"*");
//				must.filter(qbNoChinese);
//				FuzzyQueryBuilder fuzzyQuery = QueryBuilders.fuzzyQuery(col, val.toLowerCase());
//				must.should(fuzzyQuery);
//				MatchPhraseQueryBuilder specialchar = QueryBuilders.matchPhraseQuery(col, val.toLowerCase());
//				must.should(specialchar);
				MatchPhraseQueryBuilder matchNoCh = QueryBuilders.matchPhraseQuery(col, val.toLowerCase());
				must.filter(matchNoCh);
			}
		}
		return must;
	}
//    private static String replaceSpecialChars(String str) {
//		if(str.indexOf(",")!=-1){
//			str = str.replaceAll(",", "?");
//		}
//		if(str.indexOf(";")!=-1){
//			str = str.replaceAll(";", "?");
//		}
//		return str;
//	}
//	public static String[] splitNoChStr(String str){
//		if(str.indexOf(",")!=-1&&str.indexOf(";")==-1){
//			return str.split(",");
//		}else if(str.indexOf(";")!=-1&&str.indexOf(";")==-1){
//			return str.split(";");
//		}else if(str.indexOf(";")!=-1&&str.indexOf(";")!=-1){
//			str.split(",")
//		}else{
//			return new String[]{str};
//		}
//	}
	//将条件拼成json然后从json取出来
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
	/**查询条件中特殊字符处理
	 *  if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
        || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
        || c == '*' || c == '?' || c == '|' || c == '&' || c == '/') {
	 * @param condition
	 * @return
	 */
	public static boolean existSpecialChar(String condition){
		boolean exist = false;
		if(condition.indexOf("\\")!=-1||condition.indexOf("+")!=-1||condition.indexOf("-")!=-1
		   ||condition.indexOf("!")!=-1||condition.indexOf("(")!=-1||condition.indexOf(")")!=-1
		   ||condition.indexOf(":")!=-1||condition.indexOf("^")!=-1||condition.indexOf("[")!=-1
		   ||condition.indexOf("]")!=-1||condition.indexOf("\"")!=-1||condition.indexOf("{")!=-1
		   ||condition.indexOf("}")!=-1||condition.indexOf("~")!=-1||condition.indexOf("*")!=-1
		   ||condition.indexOf("?")!=-1||condition.indexOf("|")!=-1||condition.indexOf("&")!=-1
		   ||condition.indexOf("/")!=-1){
			exist = true;
		}
		return exist;
	}
	/**
	 * 得到索引中相应类型的字段名称()
	 * @param index 索引名称
	 * @param tablename 类型名
	 * @return
	 */
	public static String[] getMappingMetaDatas(String index,String tablename){
		if(client==null){
			client = getClient();
		}
		ImmutableOpenMap<String, MappingMetaData> mappings = client.admin().cluster().prepareState().execute()
                .actionGet().getState().getMetaData().getIndices().get(index.toLowerCase()).getMappings();
		String string = mappings.get(tablename.toLowerCase()).source().toString();
       JSONObject json = new JSONObject();
       JSONObject out = json.fromObject(string);
       JSONObject t_mat = (JSONObject)out.get(tablename.toLowerCase());
       JSONObject properties = (JSONObject)t_mat.get("properties");
       JSONArray names = properties.names();
       //
       Object[] array = names.toArray();
       int size = names.size();
       List<String> a = new ArrayList<>();
       String attr = "";
       for(int i=0;i<size;i++){
    	   attr = (String)array[i];
    	   if("IMPORTSEQ".equals(attr)||"CO_CREATETIME".equals(attr)||"CO_DEL".equals(attr)||"CO_FREEZE".equals(attr)||"CO_HASCHILD".equals(attr)||"CO_VALID".equals(attr)||"CO_PID".equals(attr)||"CO_ID".equals(attr))
    		   continue;
    	   else
    		   a.add(attr);
       }
       String[] pro = new String[a.size()];
       return  a.toArray(pro);
       
	}
	/**这个不太靠谱,如果连续连个都有;就不行了，改成两个%一拼
	 * 将模糊查询条件分开  如：SYS LIKE '%3000MDM001;A4,3000ECC001;A4%';COUNT LIKE '%AD,安道尔%'
	 * @param str  模糊查询的str
	 * @param splitStr 
	 * @return
	 */
	public static String[] splitConditions(String str,String splitStr){
		String[] split = null;
		List<String> array =  new ArrayList<String>();
		if(!"".equals(str)){
			split = str.split(splitStr);
			String regex = "%";
			StringBuffer sb = new StringBuffer();
			for(int i=0;i<split.length;i++){
				//这里考虑到查询的内容里可能有;号，因为字符串是以;号隔开的，所以以隔开的串中的%的个数进行串的整合
				int charCount = getCharCount(split[i], "%");
//				System.out.println("第"+String.valueOf(i)+"个的个数="+String.valueOf(charCount));
				if(charCount<2){
					sb.append(split[i]+splitStr);
				}else{
					if(!"".equals(sb.toString())){
						array.add(sb.toString().substring(0,sb.toString().length()-1));
					}
					sb.delete(0, sb.length());
					array.add(split[i]);
				}
			}
			if(!"".equals(sb.toString())){
				array.add(sb.toString().substring(0,sb.toString().length()-1));
			}
		}
		String[] cons = new String[array.size()];
		
		for(int i=0;i<array.size();i++){
			cons[i] = array.get(i);
		}
		return cons;
	}
	/**
	 * 统计字符串中特殊符号的个数
	 * @param instr 字符串
	 * @param regex 特殊字符
	 * @return
	 */
	public static int getCharCount(String instr,String regex){
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(instr);
		int count = 0;
		while(matcher.find()){
			count++;
		}
		return count;
	}
	//将输入参数拆分放入到数组中
	public static String[] splitInputs(String inputs){
		Vector outputParamVt = TXIUtil.parseInputParameterBySeperator(inputs);
		Enumeration elements = outputParamVt.elements();
		//有多少参数就创建多大的数据
		String[] input =  new String[outputParamVt.size()];
		int i=0;
		while(elements.hasMoreElements()){
			String val = (String)elements.nextElement();
			System.out.println(val);
			input[i] = val;
			i++;
		}
		return input;
	}
	/**
	 * 过滤特殊字符
	 * @param str
	 * @return
	 */
	public static String filterSpecialChars(String str){
		
		str = str.replaceAll(",", "")
				.replaceAll("\\\\", "")
				.replaceAll("\\+", "")
				.replaceAll("_", "")
				.replaceAll("!", "")
				.replaceAll("\\(", "")
				.replaceAll("\\)", "")
				.replaceAll(":", "")
				.replaceAll("^", "")
				.replaceAll("\\[", "")
				.replaceAll("\\]", "")
				.replaceAll("\"", "")
				.replaceAll("\\{", "")
				.replaceAll("\\}", "")
				.replaceAll("~", "")
				.replaceAll("\\*", "")
				.replaceAll("\\?", "")
				.replaceAll("\\|", "")
				.replaceAll("\\&", "")
				.replaceAll("/", "")
				.replaceAll(";", "");
		
		return str;
	}
	/**
	 * 
	 * @param dateStr 根据输入的日期的格式取得它的时间毫秒数long
	 * @return
	 * @throws Exception 
	 */
	public static long FormatTime(String dateStr) throws Exception{
		HashMap<String, String> dateRegFormat = new HashMap<String, String>();
	    dateRegFormat.put(
	        "^\\d{4}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D*$",
	        "yyyy-MM-dd-HH-mm-ss");//2014年3月12日 13时5分34秒，2014-03-12 12:05:34，2014/3/12 12:5:34
	    dateRegFormat.put("^\\d{4}\\D+\\d{2}\\D+\\d{2}\\D+\\d{2}\\D+\\d{2}$",
	        "yyyy-MM-dd-HH-mm");//2014-03-12 12:05
	    dateRegFormat.put("^\\d{4}\\D+\\d{2}\\D+\\d{2}\\D+\\d{2}$",
	        "yyyy-MM-dd-HH");//2014-03-12 12
	    dateRegFormat.put("^\\d{4}\\D+\\d{2}\\D+\\d{2}$", "yyyy-MM-dd");//2014-03-12
	    dateRegFormat.put("^\\d{4}\\D+\\d{2}$", "yyyy-MM");//2014-03
	    dateRegFormat.put("^\\d{4}$", "yyyy");//2014
	    dateRegFormat.put("^\\d{14}$", "yyyyMMddHHmmss");//20140312120534
	    dateRegFormat.put("^\\d{12}$", "yyyyMMddHHmm");//201403121205
	    dateRegFormat.put("^\\d{10}$", "yyyyMMddHH");//2014031212
	    dateRegFormat.put("^\\d{8}$", "yyyyMMdd");//20140312
	    dateRegFormat.put("^\\d{6}$", "yyyyMM");//201403
	    dateRegFormat.put("^\\d{2}\\s*:\\s*\\d{2}\\s*:\\s*\\d{2}$",
	        "yyyy-MM-dd-HH-mm-ss");//13:05:34 拼接当前日期
	    dateRegFormat.put("^\\d{2}\\s*:\\s*\\d{2}$", "yyyy-MM-dd-HH-mm");//13:05 拼接当前日期
	    dateRegFormat.put("^\\d{2}\\D+\\d{1,2}\\D+\\d{1,2}$", "yy-MM-dd");//14.10.18(年.月.日)
	    dateRegFormat.put("^\\d{1,2}\\D+\\d{1,2}$", "yyyy-dd-MM");//30.12(日.月) 拼接当前年份
	    dateRegFormat.put("^\\d{1,2}\\D+\\d{1,2}\\D+\\d{4}$", "dd-MM-yyyy");//12.21.2013(日.月.年)
	  
	    String curDate =new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	    DateFormat formatter1 =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    DateFormat formatter2;
	    String dateReplace;
	    String strSuccess="";
	    long date = 0L;
	    try {
	      for (String key : dateRegFormat.keySet()) {
	        if (Pattern.compile(key).matcher(dateStr).matches()) {
	          formatter2 = new SimpleDateFormat(dateRegFormat.get(key));
	          if (key.equals("^\\d{2}\\s*:\\s*\\d{2}\\s*:\\s*\\d{2}$")
	              || key.equals("^\\d{2}\\s*:\\s*\\d{2}$")) {//13:05:34 或 13:05 拼接当前日期
	            dateStr = curDate + "-" + dateStr;
	          } else if (key.equals("^\\d{1,2}\\D+\\d{1,2}$")) {//21.1 (日.月) 拼接当前年份
	            dateStr = curDate.substring(0, 4) + "-" + dateStr;
	          }
	          dateReplace = dateStr.replaceAll("\\D+", "-");
	          // System.out.println(dateRegExpArr[i]);
//	          System.err.println(formatter2.parse(dateReplace));
//	          strSuccess = formatter1.format(formatter2.parse(dateReplace));
//	          break;
	          return date = formatter2.parse(dateReplace).getTime();
	        }
	      }
	    } catch (Exception e) {
	      System.err.println("-----------------日期格式无效:"+dateStr);
	      throw new Exception( "日期格式无效");
	    } 
		return date;
	}
	/**
	 * 表数据全同步
	 * @param tablename 要同步的表名
	 * @throws UnknownHostException
	 */
	public static void initDataToES(String tablename,java.util.Properties pro) throws UnknownHostException{
		
		// 如果是时范围的可以
//		String sql1 = "select distinct tablename from CL_CATALOG where tablename not in('T_MAT','T_VENDOR','T_CUSTOMER')";
//		List tabns = DbOperation.executeArrayList(sql1);
//		tabns.add("t_mat_temp");
//		tabns.add("t_customer_temp");
//		tabns.add("t_vendor_temp");
//		Object[] tablenames = tabns.toArray();
		
		// 给所有的数据添加索引
		String sql = "select * from "+tablename;
//					+" where co_id>660000 and co_id<780001";
		BulkRequestBuilder prepareBulk = client.prepareBulk();
		//将string类型的字段转化成date类型
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		PreparedStatement pre =null;
		ResultSet re = null;
		Connection con = null;
		String driver = null;
		String url = null;
		String user = null;
		String password = null;
		String limitsize = null;
		int size = 0;
		try {
//			java.util.Properties pro = new java.util.Properties();
			if(pro!=null){
//				pro.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("Database.properties"));
			}else{
				pro = new java.util.Properties();
				pro.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("Database.properties"));
				
			}
			driver = pro.getProperty("OracleDriver");
			url = pro.getProperty("ESDB_Url");// 127.0.0.1是本机地址，XE是精简版Oracle的默认数据库名
			user = pro.getProperty("ESDB_User");// 用户名,系统默认的账户名
			password = pro.getProperty("ESDB_Password");// 你安装时选设置的密码
			limitsize = pro.getProperty("LimitSize");
			size = Integer.valueOf(limitsize);
			//连接查询
			Class.forName(driver);// 加载Oracle驱动程序
			con = DriverManager.getConnection(url, user, password);// 获取连接
			pre = con.prepareStatement(sql);
			re = pre.executeQuery();
			
			ResultSetMetaData metaData = re.getMetaData();
			int count = metaData.getColumnCount();
			int num = 0;
			boolean next = re.next();
			//结果集处理
			while(re.next()){
				String co_id = null;
				int  importseq = 0;
				for(int i=1;i<=count;i++){
					String col = metaData.getColumnName(i);
					Object val = re.getObject(i);
					if("CO_CREATETIME".equals(col)){
						if(val!=null){
							map.put(col, FormatTime(val.toString()));
						}
					}else{
						map.put(col, val);
					}
					if("CO_ID".equals(col)){
						co_id = String.valueOf(val);
					}
					if("importseq".equalsIgnoreCase(col)){
						importseq = Integer.valueOf(String.valueOf(val)).intValue();
						map.put(col, importseq);
					}
				}
//				DeleteRequestBuilder prepareDelete = client.prepareDelete(tablename.toLowerCase()+"index",tablename.toLowerCase() ,co_id);
//				prepareBulk = prepareBulk.add(prepareDelete);
				prepareBulk.add(client.prepareIndex("mdmindex",tablename.toString().toLowerCase(),co_id).setSource(map));
				num++;
				if(num>size){
					BulkResponse bulkResponse = prepareBulk.get(new TimeValue(60*1000));
					System.out.println("批量成功插入"+tablename+":"+String.valueOf(num)+"条");
					if(bulkResponse.hasFailures()){
//						TXISystem.log.error("错误数据详细信息：", bulkResponse.buildFailureMessage());
						System.err.println("错误数据详细信息："+bulkResponse.buildFailureMessage());
					}	
					num=0;
				}
			}
			if(next){
				BulkResponse bulkResponse = prepareBulk.setTimeout(new TimeValue(30*1000)).get();
				System.out.println("批量成功插入"+tablename+":"+String.valueOf(num)+"条");
				if(bulkResponse.hasFailures()){
//				TXISystem.log.error("错误数据详细信息：", bulkResponse.buildFailureMessage());
					System.err.println(tablename+"错误数据详细信息："+bulkResponse.buildFailureMessage());
				}
				map.clear();
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally{
			try {
				if(pre!=null){
					pre.close();
				}
				if(re!=null){
					re.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args) throws Exception {
//		long formatTime = FormatTime("20180224");
//		Calendar date = Calendar.getInstance();
//		date.setTime(new Date());
//		date.getTime();
//		Date da = new Date(date.getTime().getTime());
//		String str = "SYS LIKE '%3000MDM001;A4,3000ECC001;A4%';COUNT LIKE '%AD,安道尔%'";
//		System.out.println(splitConditions(str, ";"));
		
	}
	
	
	
	
}
