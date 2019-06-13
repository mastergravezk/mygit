package com.thit.elasticsearch;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.join.query.HasChildQueryBuilder;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thit.elasticsearch.common.ESConstant;
import com.thit.elasticsearch.common.ESUtil;
import com.thit.elasticsearch.orcldb.DbOperation;
import com.thit.elasticsearch.query.*;
import com.xicrm.business.util.TXIBizSmallUtil;
import com.xicrm.common.TXISystem;
import com.xicrm.exception.TXIException;
import com.xicrm.model.TXIModel;
import com.xicrm.util.TXIUtil;

/**
 * 
 * @author zk
 * @version 2018-1-12 
 * @since  jdk1.8
 */
public class TXIMasterDataSearcher {
	
	//private static Logger logger = LoggerFactory.getLogger(TXIMasterDataSearcher.class);
	private static Client client;
	
	
	public TXIMasterDataSearcher() {
		
	}
	
	public static void initClient(){
		if(client == null){
			client = ESUtil.getClient();
		}
		
	}
	/**
	 * 全文检索SYS中sysNum
	 * @param inputParams ES查询所需要的参数
	 * @param errMsg	ES查询报错信息
	 * @param outputTotalnum ES查询记录总数
	 * @param pageSizeTempSSS 页大小
	 * @param detailNumber 细表名
	 * @param model 用户信息
	 * @return
	 * @throws UnknownHostException
	 * @throws TXIException 
	 */
	 //全文检索：考虑到扩展性创建一个索引（名字就定位mdmindex） type就指定为表名就可以了,id自增序号不用说了
	 //       type类型不适合 完全不同类型的数据 。如果两个类型的字段集是互不相同的， 这就意味着索引中将有一半的数据是空的（字段将是
	 //       稀疏的 ），最终将导致性能问题。 在这种情况下，最好是使用两个单独的索引
	 //       中文一般采取分词搜索，英文，字母或者其他的可以正常搜索
	@SuppressWarnings("null")
	public static TXIModel fullSearchBySYS(String index,String inputParams,String pageInfo,String errMsg, String outputTotalnum,String pageSizeTempSSS,String detailNumber,TXIModel model) throws UnknownHostException, TXIException {
		/*******************全文检索入口*****************/
		//System.err.println("编译过了");
		initClient();
		//分解输入参数
		Vector outputParamVt = TXIUtil.parseInputParameterBySeperator(inputParams);
		String tableName=outputParamVt.get(0)==null?"":outputParamVt.get(0).toString();//表名
		String cl_id=outputParamVt.get(1)==null?"":outputParamVt.get(1).toString(); //分类
		String conditions=outputParamVt.get(2)==null?"":outputParamVt.get(2).toString(); //查询条件
		String iuserName=outputParamVt.get(3)==null?"":outputParamVt.get(3).toString();	//增加数据创建用户
		String icomanualcodes=outputParamVt.get(4)==null?"":outputParamVt.get(4).toString();	//手工编码
		String ilang=outputParamVt.get(5)==null?"":outputParamVt.get(5).toString();	//语言标记
//		
		PageInfo pageinfo = setPageInfo(pageInfo, pageSizeTempSSS, model);
		MDMESQueryContext context = new MDMESQueryContext(null, index, tableName, "ik_smart", detailNumber, model, pageinfo,"CO_MANUALCODE.keyword");
		
		BoolQueryBuilder must = QueryBuilders.boolQuery();
		//如果查询条件为空则全文匹配查询否则每一个字段都要模糊查询
		QueryBuilder qb = QueryBuilders.termQuery("CO_VALID", "1"); 
		if (conditions == null || "".equals(conditions.trim())) {
			//避免用must会查询匹配分值（分值一般没用降低性能）,要改成查询co_valid=1的记录
			must.filter(qb);
		} else {
			//escape是去除特殊符号operator操作符匹配token里边的内容用and匹配不用or（默认设置是or）
			must = must.filter(QueryBuilders.queryStringQuery(QueryParser.escape(conditions.toLowerCase())).defaultOperator(Operator.AND))
					   .filter(qb);
		}
		//增加数据创建用户
		if(!"".equals(iuserName)){
			must.filter(QueryBuilders.matchQuery("CO_CREATER", iuserName));
		}
		// sysNum拼接数组
		ArrayList sysNumArrays = new ArrayList();
		if(!"".equals(icomanualcodes)){
			String[] manualcodes = icomanualcodes.split(",");
			for (String code : manualcodes) {
				// 查询sys中sysNum，模糊匹配
				StringBuilder str = new StringBuilder();
				str.append(";");
				String regexp = "\'";
				code = code.replaceAll(regexp, "");
				String sysNum = str.append(code).toString();
				sysNumArrays.add(sysNum);
				must.should(QueryBuilders.matchPhraseQuery("SYS", sysNum));
			}
		}
		//过滤语言标记当前语言标记和为all的数据查出来
		TermsQueryBuilder termsQ = QueryBuilders.termsQuery("CO_ATTRLANGUAGE.keyword", ilang,ESConstant.lang_all);
		must.filter(termsQ);
		/*************************新加的模糊查询*************************/
		//先判断
		initClient();
		// 模糊查询
		if(!"".equals(icomanualcodes)){
			//得到
			//String[] fields = ESUtil.getMappingMetaDatas(index, tableName);
			BoolQueryBuilder filter = new BoolQueryBuilder();
			filter.filter(qb).filter(termsQ);
			//构建聚合查询的条件
			TermsAggregationBuilder agg1 = AggregationBuilders.terms("code").field("CO_MANUALCODE.keyword");
			//过滤桶去重cardinality和percentiles算法
//			AggregationBuilders.filter("", filter).subAggregation(AggregationBuilders.cardinality("").field("").precisionThreshold(1000000))
//			AggregationBuilders.filter("", filter).subAggregation(AggregationBuilders.percentiles("").field("").)
			//按时间段聚合
//			AggregationBuilders.dateHistogram("").field("").interval(1000).subAggregation(aggregation)
			//条件添加
			for(Object sysNumArray : sysNumArrays){
				String dsh = sysNumArray.toString();
				filter.should(QueryBuilders.wildcardQuery("SYS", "*"+sysNumArray.toString()+"*"));
			}
			QueryHandler handler = new MDMSYSAggreationQueryHandler(agg1, client);
			context = handler.handle(context, filter);
//			model = ESFuzzySearchtoModelBySYS(index, detailNumber, tableName, filter,agg1, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
			/*************************新加的模糊查询*************************/
			model = context.getModel();
		}
		return model;
	}
	/**
	 * 2018/5/4 升级到英文
	 * @param inputParams ES查询所需要的参数
	 * @param errMsg	ES查询报错信息
	 * @param outputTotalnum ES查询记录总数
	 * @param pageSizeTempSSS 页大小
	 * @param detailNumber 细表名
	 * @param model 用户信息
	 * @return
	 * @throws UnknownHostException
	 * @throws TXIException 
	 */
	//全文检索：考虑到扩展性创建一个索引（名字就定位mdmindex） type就指定为表名就可以了,id自增序号不用说了
	 //          type类型不适合 完全不同类型的数据 。如果两个类型的字段集是互不相同的， 这就意味着索引中将有一半的数据是空的（字段将是
	 //         稀疏的 ），最终将导致性能问题。 在这种情况下，最好是使用两个单独的索引
	//中文一般采取分词搜索，英文，字母或者其他的可以正常搜索
	@SuppressWarnings("null")
	public static TXIModel fullSearch(String index,String inputParams,String pageInfo,String errMsg, String outputTotalnum,String pageSizeTempSSS,String detailNumber,TXIModel model) throws UnknownHostException, TXIException {
		System.out.println("输入参数："+inputParams);
		/*******************全文检索入口*****************/
		initClient();
//		//分解输入参数
		Vector outputParamVt = TXIUtil.parseInputParameterBySeperator(inputParams);
		int size = outputParamVt.size();
		String tableName=outputParamVt.get(0)==null?"":outputParamVt.get(0).toString();//表名
		String cl_id=outputParamVt.get(1)==null?"":outputParamVt.get(1).toString(); //分类
		String conditions=outputParamVt.get(2)==null?"":outputParamVt.get(2).toString(); //查询条件
		//条件分割成几块了
		int i = 1;
		while(i+5!=size){
			conditions = conditions +";"+ outputParamVt.get(2+i);
			i++;
		}
		String iuserName=outputParamVt.get(size-3)==null?"":outputParamVt.get(size-3).toString();	//增加数据创建用户
		String icomanualcodes=outputParamVt.get(size-2)==null?"":outputParamVt.get(size-2).toString();	//手工编码
		String ilang=outputParamVt.get(size-1)==null?"":outputParamVt.get(size-1).toString();	//语言标记
		if (detailNumber.equals("")) {
			detailNumber = "detail0";
		}
		System.out.println("语言标记="+ilang);
		PageInfo pageinfo = setPageInfo(pageInfo, pageSizeTempSSS, model);
		/*构建全文查询的上下文*/
		MDMESQueryContext context =  new MDMESQueryContext(null, index, tableName, "ik_smart", detailNumber, model, pageinfo,"CO_MANUALCODE.keyword");
//		System.out.println("currentPage="+String.valueOf(currentPage));
		BoolQueryBuilder must = QueryBuilders.boolQuery();
		//如果不是根节点的cl_id就应该加上cl_id的过滤条件
		//如果查询条件为空则全文匹配查询否则每一个字段都要模糊查询
		QueryBuilder qb = QueryBuilders.termQuery("CO_VALID", "1"); 
		must.filter(qb);
		//增加数据创建用户
		if(!"".equals(iuserName)){
			must.filter(QueryBuilders.matchQuery("CO_CREATER", iuserName));
		}
		//手工编码拼接
		if(!"".equals(icomanualcodes)){
			String[] manualcodes = icomanualcodes.split(",");
			for (String code : manualcodes) {
				must.should(QueryBuilders.matchQuery("CO_MANUALCODE", code));
			}
		}
		//过滤语言标记当前语言标记和为all的数据查出来
		TermsQueryBuilder termsQ = QueryBuilders.termsQuery("CO_ATTRLANGUAGE.keyword", ilang,ESConstant.lang_all);
		must.filter(termsQ);
		
		if (conditions == null || "".equals(conditions.trim())) {
			//这里要改成查询co_valid=1的记录
			//避免用must会查询匹配分值（分值一般没用降低性能）
//			must.filter(qb);
		} else {
			//对查询内容进行分词处理
			QueryTokensHandler handler = new QueryTokensHandler(context);
			QueryTokens tokens = handler.handleQueryStr(conditions);
			context.setTokens(tokens);
			//构建查询的builder
			MDMQueryBuilder builder = new MDMAnalyzerQueryBuilder(client,must);
			must = (BoolQueryBuilder) builder.buildeQuery(context);
		}
		//对构建的builder进行查询处理
		QueryHandler search = new MDMQueryHandler(client);
		context = search.handle(context,must);
		/*************************新加的模糊查询*************************/
		System.out.println(must.toString());
	//客户端发送查询请求
//		model = ESSearchtoModel(index,detailNumber,tableName, must, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
		return context.getModel();
	}
	
	/**
	 * 判断cl_id是否为根节点的cl_id
	 * @param cl_id
	 * @param tableName
	 * @return
	 */
	/*public static boolean isOrnotRootcl_id(String cl_id,String tableName){
		String sql = "select min(cl_id) from cl_catalog where tablename='" + tableName +"'" ;
		Object root = DbOperation.executeObject(sql);
		boolean flag = false;
		if (root!=null&&cl_id.equals(root.toString()))
			flag = true;
		
		return flag;
	}*/
	
	/**
	 * 时间范围字段用rangeBulider单独处理 查询的字段的是创建时间的范围区间
	 * @return
	 * @throws Exception 
	 * @throws ParseException 
	 */
	public static BoolQueryBuilder setTimeRangeBulider(BoolQueryBuilder must,String startTime,String endTime) throws Exception {
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		Date start = sdf.parse(startTime);
//		Date end = sdf.parse(endTime);
		//将字段的值转换成可以比较范围的long
		RangeQueryBuilder range = QueryBuilders.rangeQuery("CO_CREATETIME");
		if(!"".equals(startTime)){
			range = range.gte(ESUtil.FormatTime(startTime));//gte大于等于
			must.filter(range);
		}
		if(!"".equals(endTime)){
			range = range.lte(ESUtil.FormatTime(endTime)); //lte小于等于
			must.filter(range);
		}
		return must;
	}
	/**
	 * 解析模糊 查询条件的sql字符串 
	 * @param str
	 * @param filter
	 * @return
	 */
	public static BoolQueryBuilder  getSearchConditionFilter(MDMESQueryContext context,String splitor,String str,BoolQueryBuilder filter){
//		Map<String,String> map = new HashMap<String, String>();
		String[] split = ESUtil.splitConditions(str,";");
		String[] split2 = null;
		int first =-1,last=-1;
		String likeVal = null;
		
		QueryTokensHandler handler = new QueryTokensHandler(context);
		MDMAnalyzerQueryBuilder builder = new MDMAnalyzerQueryBuilder(client,filter);
		//SYS LIKE '%3000MDM001;A4,3000ECC001;A4%';COUNT LIKE '%AD,安道尔%'
		for(int i=0;i<split.length;i++){
			split2 = split[i].split(splitor);
			first = split2[1].indexOf("%");
			last = split2[1].lastIndexOf("%");
			likeVal = split2[1].trim().substring(first, last-1);
			QueryTokens ts = handler.handleQueryStr(likeVal);
			filter = (BoolQueryBuilder) builder.buildQuery(context,split2[0].trim());
//			filter = filter.filter(QueryBuilders.matchQuery(split2[0].trim(), likeVal.toLowerCase()).operator(Operator.AND));
			System.out.println(split2[0].trim()+"="+likeVal );
		}
		return filter;
	}
	
	/**
	 * 批量导入查询
	 * @param index 索引名
	 * @param tableName 表名
	 * @param conditions 编码用逗号拼接而成
	 * @param model
	 * @return
	 * @throws TXIException 
	 */
	public static TXIModel batchImportSearch(String index, String tableName, String conditions, String pageInfo,
			String errMsg, String outputTotalnum, String pageSizeTempSSS, String detailNumber, TXIModel model)
					throws TXIException {
		initClient();
		//编码分开
		String[] codes = conditions.split(",");
		String[] _codes = new String[codes.length];
		for(int i=0;i<codes.length;i++){
			_codes[i] = codes[i].substring(1, codes[i].length()-1).toLowerCase();
		}
		if (detailNumber.equals("")) {
			detailNumber = "detail0";
		}
		detailNumber = detailNumber == null ? "" : detailNumber;
		PageInfo pageinfo = setPageInfo(pageInfo, pageSizeTempSSS, model);
		//构建context
		MDMESQueryContext context = new MDMESQueryContext(null, index, tableName, "ik_smart", detailNumber, model, pageinfo,"CO_MANUALCODE.keyword");
		//构建builder
		BoolQueryBuilder filter = new BoolQueryBuilder();
		filter.filter(QueryBuilders.matchQuery("CO_VALID", "1"));
		if(_codes.length>0){
			filter.filter(QueryBuilders.termsQuery("CO_MANUALCODE", _codes));
		}
		
		//处理builder
		QueryHandler handler = new MDMQueryHandler(client);
		context = handler.handle(context, filter);
		//此处不用分页,
//		model = ESSearchtoModel(index, detailNumber, tableName, filter, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
		return context.getModel();
	}
	/**
	 * 
	 * @param inputParams
	 * @param pageInfo
	 * @param errMsg
	 * @param outputTotalnum
	 * @param pageSizeTempSSS
	 * @param detailNumber
	 * @param model
	 * @return
	 * @throws Exception 
	 */
	public static TXIModel combinSearch(String index,String inputParams,String combinations,String pageInfo,String errMsg, String outputTotalnum,String pageSizeTempSSS,String detailNumber,TXIModel model) throws Exception{
		initClient();
		System.out.println("--------------条件查询---------------");
		BoolQueryBuilder must = QueryBuilders.boolQuery();
		Vector outputParamVt = TXIUtil.parseInputParameterBySeperator(inputParams);
		Enumeration elements = outputParamVt.elements();
		System.out.println("inputParams="+inputParams);
		
		//有多少参数就创建多大的数组
		String[] input =  new String[outputParamVt.size()];
		int i=0;
		while(elements.hasMoreElements()){
			String val = (String)elements.nextElement();
//			System.out.println(val);
			input[i] = val;
			i++;
		}
		//[String:cl_id];[String:TABLENAME];[String:combin];[String:SCO_CREATETIME];[String:ECO_CREATETIME]'
		//从model主表中取出存储的键值
//		Hashtable htValues = model.getHtValues();
//		Enumeration keys = htValues.keys();
		String cl_id = null,tableName = null,startTime = null,endTime = null,co_creatororg = null,ilang = null;
		cl_id=input[0];
		tableName=input[1];
		startTime=input[2];
		endTime=input[3];
//		conditions=input[4];
		co_creatororg = input[4];
		ilang = input[5];
		//对查询条件进行解码
//		byte[] combytes = combinations.getBytes("ISO-8859-1"); 
//		combinations = new String(combytes, "utf-8").toString();
//		List<String> number = new ArrayList<String>();
		//必须加上这个条件
		must = must.filter(QueryBuilders.termQuery("CO_VALID", "1"));
		//首先弄清楚父子关系
	    //将TBMB部门表中的bmbm通过term查询结果取出bmbm查出来的值，再将与qualified_syss建立父子关系，然后
		//
		if(!"".equals(co_creatororg)){
			String sql = "select co_id from "+tableName+" a  where a.CO_MANUALCODE in (select m.CO_MANUALCODE from "+tableName+"MAPS m where m.SYSTCODE in"
                         +" (select qs.qf_code"
                         +"  from qualified_syss qs"
                         +"  where qs.qf_org in"
                         +"  (select tb.bmbm from TBMB tb  where tb.bmmc like '%"+co_creatororg.trim()+"%')))";
			TXISystem.log.info("sql=", sql);
			List<String> number = DbOperation.executeCO_IDList(sql);
			if(!number.isEmpty()){
				//查出来有多少个id就创建多少空间
				String[] ids = new String[number.size()];
				ids = number.toArray(ids);
				//将id放到相应的bulider中
				IdsQueryBuilder idsbuliders  = new IdsQueryBuilder();
				idsbuliders.addIds(ids);
				must.filter(idsbuliders);
//				fuzzy.filter(idsbuliders);
			}else{
				return model;
			}
		}
		//得到输入参数个数-1用;号隔开
		System.out.println("cl_id="+cl_id+"  tableName="+tableName+" startTime ="+startTime+" endTime="+endTime);
		System.out.println(combinations);
		//如果不是根节点的cl_id加上cl_id过滤条件(勿删)
		//放在should的后面
		//设置时间范围
		if(!"".equals(startTime)||!"".equals(endTime)){
			must = setTimeRangeBulider(must, startTime, endTime);
		}
		must.filter(QueryBuilders.termsQuery("CO_ATTRLANGUAGE", ilang,ESConstant.lang_all));
		detailNumber = detailNumber == null ? "" : detailNumber;
		if (detailNumber.equals("")) {
			detailNumber = "detail0";
		}
		
		PageInfo PageInfo = setPageInfo(pageInfo,pageSizeTempSSS,model);
		MDMESQueryContext context = new MDMESQueryContext(null, index, tableName, "ik_smart", detailNumber, model, PageInfo,"CO_MANUALCODE.keyword");
		//判断组合查询条件是否为空
		if(!"".equals(combinations)&&combinations!=null){
			must = getSearchConditionFilter(context,"LIKE", combinations, must);
		}
		QueryHandler handler = new MDMQueryHandler(client);
		context = handler.handle(context,must);
		//model = ESSearchtoModel(index,detailNumber,tableName, must, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
		System.out.println(must);
		return context.getModel();
		
	}
	/**
	 * 
	 * @param pageInfo 
	 * @param pageSizeTempSSS 分页大小
	 * @param model
	 * @return
	 * @throws TXIException
	 */
	private static PageInfo setPageInfo(String pageInfo,String pageSizeTempSSS,TXIModel model) throws TXIException {
		// TODO Auto-generated method stub
		 //分页信息
		String[] page = pageInfo.split(";");
		String current = page[0], pageFlag = page[1],firstPage = page[2],prePage =page[3],nextPage =page[4],lastPage = page[5];
		
		
		String pageSizeTemp = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 1, model);
		String pageInfoControl = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 2, model);
		Integer pageNo = TXIBizSmallUtil.XiGetCurrentPage(pageInfoControl, model);
		String pageSize = TXIBizSmallUtil.XiGetPageSize(pageSizeTemp, model);
		Integer currentPage = TXIBizSmallUtil.XiGetCurrentPage(pageInfoControl, model);
		
		System.out.println("pageSizeTemp="+pageSizeTemp);
		System.out.println("PageInfoControl="+pageInfoControl);
		System.out.println("pageNo="+String.valueOf(pageNo));
		System.out.println("pageSize="+pageSize);
		System.out.println("currentPage="+String.valueOf(currentPage));
		
		return new PageInfo(pageSize, pageFlag, current, pageInfoControl, pageNo, 0, prePage, nextPage, firstPage, lastPage);
	}

	/**
	 * 拼接查询条件
	 * @param model
	 * @return
	 */
	public static TXIModel jointSearchConditions(String combin,TXIModel model){
		Hashtable htValues = model.getHtValues();
		Enumeration keys = htValues.keys();
		StringBuilder sb = new StringBuilder();
		//将查询条件不为空的字段拼接起来放入model主表里
		while(keys.hasMoreElements()){
			Object key = keys.nextElement();
			Object val = htValues.get(key);
			//这两个不作为拼接条件后三位是_ID的去除掉
//			if((!key.toString().endsWith("_ID"))&&!"CO_CREATEORG".equalsIgnoreCase(key.toString())&&!"TTABLENAME".equalsIgnoreCase(key.toString())&&!"CO_ATTRLANGUAGE".equalsIgnoreCase(key.toString())&&!"SCO_CREATETIME".equalsIgnoreCase(key.toString())&&!"ECO_CREATETIME".equalsIgnoreCase(key.toString())){
			if(!"TTABLENAME".equalsIgnoreCase(key.toString())&&!"CO_ATTRLANGUAGE".equalsIgnoreCase(key.toString())&&!"SCO_CREATETIME".equalsIgnoreCase(key.toString())&&!"ECO_CREATETIME".equalsIgnoreCase(key.toString())){
				if(val!=null && !"".equals(val)){
					sb.append(key.toString()+ " LIKE '%" +val.toString()+"%';");
				}
			}
		}
		//如果拼接条件为空那么就不用截取了
		if("".equals(sb.toString())){
			htValues.put(combin,"");
		}else{
			htValues.put(combin,sb.toString().substring(0, sb.toString().length()-1));
		}
//		System.out.println("combin="+);
		model.setDetail(htValues);
		return model;
	}
	/**
	 * 集团数据查询返回的xml
	 * @param pageInfo 页信息
	 * @param model 用户数据
	 * @return
	 */
	public static String getESSearchBackXML(String pageInfo,TXIModel model){
		//从model中detail1得到相应返回属性信息
		Hashtable details = model.getDetails();
		Hashtable detail1 = (Hashtable)details.get("detail1");
		
		//从model中的detail0中得到查询内容
		Hashtable detail0 = (Hashtable)details.get("detail0");
		
		return jointSearchXML(pageInfo,detail1,detail0);
	}
	/**
	 * 二级平台调用集团查询返回的xml
	 *  属性信息大小量纲=DXLG
	 *	 物料名称=MATNAME(查属性表)
	 *  MATNAME=AAAAAA
	 *	 DXLG=10（查主数据表）
	 * @return
	 */
	public static String jointSearchXML(String pageInfo,Hashtable detail1,Hashtable detail0){
		StringBuilder sb = new StringBuilder("&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;");
		sb.append("&lt;MODELMsg&gt;");
		sb.append("&lt;properties&gt;");
		/**************页信息***************/
		sb.append("&lt;page-info&gt;");
		//首页<!--首页标记，值为1/0（1-有，0-无）-->
		sb.append("&lt;firstpage&gt;");
		
		sb.append("&lt;/firstpage&gt;");
		//前一页<!--前页，值为1/0（1-有，0-无）-->
		sb.append("&lt;prepage&gt;");
		
		sb.append("&lt;/prepage&gt;");
		//末页 <!--末页，值为1/0（1-有，0-无）-->
		sb.append("&lt;endpage&gt;");
		
		sb.append("&lt;/endpage&gt;");
		//下一页<!--下一页，值为1/0（1-有，0-无）-->
		sb.append("&lt;nextpage&gt;");
		
		sb.append("&lt;/nextpage&gt;");
		//页标记 <!--页标记，值为0/1/2/3（0-首页，1-前页，2-后页，3-末页，客户端发来）-->
		sb.append("&lt;pageflag&gt;");
		
		sb.append("&lt;/pageflag&gt;");
		//当前页<!--当前页，客户端发来，服务器也提供-->
		sb.append("&lt;currentpage&gt;");
		
		sb.append("&lt;/currentpage&gt;");
		//总页数<!--总页数-->
		sb.append("&lt;total-pages&gt;");
		
		sb.append("&lt;/total-pages&gt;");
		//总记录数 <!--总记录数-->
		sb.append("&lt;total-records-count&gt;");
		
		sb.append("&lt;/total-records-count&gt;");
		
		sb.append("&lt;/page-info&gt;");
		sb.append("&lt;/properties&gt;");
		/****************detail0主表信息*****************/
		sb.append("&lt;DETAIL0Msg&gt;");
		Enumeration keys0 = detail0.keys();
		while(keys0.hasMoreElements()){
			sb.append("&lt;MAINMsg&gt;");
			Object key0 = keys0.nextElement();
			//每一个detail里边
			TXIModel submodel1 = (TXIModel)detail0.get(key0);
			Hashtable source0 = submodel1.getHtValues();
			Enumeration keys = source0.keys();
			while (keys.hasMoreElements()) {
				Object key =keys.nextElement();
				sb.append("&lt;"+key.toString()+"&gt;");
				sb.append(source0.get(key).toString());
				sb.append("&lt;/"+key.toString()+"&gt;");
			}
			sb.append("&lt;/MAINMsg&gt;");
		}
		sb.append("&lt;/DETAIL0Msg&gt;");
		/********************detail1属性信息************************/
		sb.append("&lt;DETAIL1Msg&gt;");
		Enumeration keys1 = detail1.keys();
		while(keys1.hasMoreElements()){
			sb.append("&lt;MAINMsg&gt;");
			Object key = keys1.nextElement();
			//每一个detail里边
			TXIModel submodel1 = (TXIModel)detail1.get(key);
			Hashtable source1 = submodel1.getHtValues();
			while (keys1.hasMoreElements()) {
				Object key1 = keys1.nextElement();
				sb.append("&lt;"+key1.toString()+"&gt;");
				sb.append(source1.get(key1).toString());
				sb.append("&lt;/"+key1.toString()+"&gt;");
			}
			sb.append("&lt;/MAINMsg&gt;");
		}
		sb.append("&lt;/DETAIL1Msg&gt;");
		
		sb.append("&lt;/MODELMsg&gt;");
		
		return sb.toString();
	}
	public static void main(String[] args) {
		System.out.println(11);
	}
	
}
