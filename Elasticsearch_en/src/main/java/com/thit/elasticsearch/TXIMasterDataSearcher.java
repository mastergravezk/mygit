package com.thit.elasticsearch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse.FieldMappingMetaData;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
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
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregatorFactory;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.ibm.icu.impl.duration.TimeUnit;
import com.ibm.icu.text.AlphabeticIndex.Bucket;
import com.thit.elasticsearch.common.ESConstant;
import com.thit.elasticsearch.common.ESUtil;
import com.thit.elasticsearch.orcldb.DataTable;
import com.thit.elasticsearch.orcldb.DbOperation;
import com.thit.elasticsearch.test.Test;
import com.xicrm.business.TXIBizException;
import com.xicrm.business.task.TXIBizProcDB;
import com.xicrm.business.util.JNDINames;
import com.xicrm.business.util.TXIBizSmallUtil;
import com.xicrm.common.TXIConfig;
import com.xicrm.common.TXISystem;
import com.xicrm.db.JetDBSelect;
import com.xicrm.exception.TXIException;
import com.xicrm.model.TXIDataModel;
import com.xicrm.model.TXIModel;
import com.xicrm.util.TXIUtil;

/**
 * 
 * @author zk
 * @version 2018-1-12 
 * @since  jdk1.8
 */
public class TXIMasterDataSearcher {
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
		//分解输入参数
		Vector outputParamVt = TXIUtil.parseInputParameterBySeperator(inputParams);
		String tableName=outputParamVt.get(0)==null?"":outputParamVt.get(0).toString();//表名
		String cl_id=outputParamVt.get(1)==null?"":outputParamVt.get(1).toString(); //分类
		String conditions=outputParamVt.get(2)==null?"":outputParamVt.get(2).toString(); //查询条件
		String iuserName=outputParamVt.get(3)==null?"":outputParamVt.get(3).toString();	//增加数据创建用户
		String icomanualcodes=outputParamVt.get(4)==null?"":outputParamVt.get(4).toString();	//手工编码
		String ilang=outputParamVt.get(5)==null?"":outputParamVt.get(5).toString();	//语言标记
		String[] page = pageInfo.split(";");
		String current = page[0], pageFlag = page[1],firstPage = page[2],prePage =page[3],nextPage =page[4],lastPage = page[5];
		detailNumber = detailNumber == null ? "" : detailNumber;
		if (detailNumber.equals("")) {
			detailNumber = "detail0";
		}
		String pageSizeTemp = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 1, model);
		String PageInfoControl = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 2, model);
		Integer pageNo = TXIBizSmallUtil.XiGetCurrentPage(PageInfoControl, model);
		String pageSize = TXIBizSmallUtil.XiGetPageSize(pageSizeTemp, model);
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
			String[] fields = ESUtil.getMappingMetaDatas(index, tableName);
			BoolQueryBuilder filter = new BoolQueryBuilder();
			filter.filter(qb).filter(termsQ);
			//构建聚合查询的条件
			TermsAggregationBuilder agg1 = AggregationBuilders.terms("code").field("CO_MANUALCODE.keyword");
			// 条件添加
			for(Object sysNumArray : sysNumArrays){
				String dsh = sysNumArray.toString();
				filter.should(QueryBuilders.wildcardQuery("SYS", "*"+sysNumArray.toString()+"*"));
			}
			model = ESFuzzySearchtoModelBySYS(index, detailNumber, tableName, filter,agg1, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
			/*************************新加的模糊查询*************************/
		}
		return model;
	}
	/**
	 * sysNum的es模糊查询
	 * @param index
	 * @param detailNumber
	 * @param tableName
	 * @param must
	 * @param aggs
	 * @param pageSize
	 * @param pageFlag
	 * @param current
	 * @param PageInfoControl
	 * @param pageNo
	 * @param model
	 * @return
	 */
	public static TXIModel ESFuzzySearchtoModelBySYS(String index,String detailNumber,String tableName, QueryBuilder must,AggregationBuilder aggs, String pageSize, String pageFlag,
			String current, String PageInfoControl, int pageNo, TXIModel model) {
		// 客户端发送查询请求
		initClient();
		SearchResponse searchResponse = client.prepareSearch(index.toLowerCase())
				.setTypes(tableName.toLowerCase())
				.setQuery(QueryBuilders.constantScoreQuery(must))//不进行分值计算
				.addAggregation(aggs)//group by 聚合查询
				.setTimeout(new TimeValue(60000))
				.execute()
				.actionGet();
		// 符合条件的总记录数
		int totalnum = (int) searchResponse.getHits().getTotalHits();
		//将编码放到数组中拼接查询条件使用
		Terms terms = searchResponse.getAggregations().get("code");
		List codes = new ArrayList<>(terms.getBuckets().size());
		//将group by查出来的co_manualcode分组桶中的值
		for(Terms.Bucket bucket : terms.getBuckets()){
		    //term查询将内容中的字母都改成小写
			codes.add(bucket.getKeyAsString().toLowerCase());
		}
		//查询要展示的数据
		if(codes.isEmpty()){
			model.getDetail(detailNumber).clear();
			model = TXIBizSmallUtil.setPageNo(PageInfoControl, 1, Integer.valueOf(pageSize), Integer.valueOf(0),
					model);
		}else{
			BoolQueryBuilder filter1 = new BoolQueryBuilder();
			SearchResponse searchResponse1 = client.prepareSearch(index.toLowerCase())
					.setTypes(tableName.toLowerCase())
					.addSort("CO_MANUALCODE.keyword", SortOrder.ASC)//按照哪个字段排序 ，keyword表示不进行分词
					.setScroll(new TimeValue(60000))
					.setQuery(filter1.filter(QueryBuilders.termsQuery("CO_MANUALCODE", codes)).filter(QueryBuilders.termQuery("CO_VALID", "1")))//不进行分值计算
					.setSize(Integer.valueOf(pageSize))
					.setTimeout(new TimeValue(60000))
					.execute()
					.actionGet();
			// 获得总页数
			int totalPage = getTotalPage(totalnum, pageSize);
			// 根据翻页格式确定取哪一页得数据，得把这20条数据的范围确定
			int ltnum = -1, gtnum = -1;
			// 载入时的标记为空首页的标记也为空
			int[] range = setSubidRange(ltnum, gtnum, totalnum, totalPage, pageFlag, pageSize, current);
			ltnum = range[0];
			gtnum = range[1];
			// 处理查询结果
			// 从model中得到页记录信息
			Hashtable detail0 = (Hashtable) model.getDetails().get(detailNumber);
			if(detail0==null){
				model.setDetail(detailNumber,new Hashtable());
			}
			int subid = 0;
			model.getDetail(detailNumber).clear();
			//对co_createtime进行long转date
			DateFormat timeParse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
			// 计算出要取那一页得数据（?<subid<?）
			do {
				for (SearchHit hit : searchResponse1.getHits().getHits()) {
					// 到分页的页数据之后就将数据放入到detail0中
					if (subid < gtnum && subid >= ltnum) {
						Map<String, Object> source = hit.getSource();
						TXIModel submodel = new TXIModel(model);
						// //由于hashtable中的键值均不能为null所以放弃用putAll()了
						Hashtable<String, Object> datas = new Hashtable<String, Object>();
						for (String key : source.keySet()) {
							Object obj = source.get(key);
							String s_col = key == null ? "" : key;
							Object s_val=null;
							//ES中存储的创建时间字段为long类型所以展示的时候转化成String
							if("CO_CREATETIME".equals(key)){
								if(obj!=null){
									s_val = timeParse.format(new Date(Long.parseLong(obj.toString())));
								}else{
									s_val = "";
								}
							}else{
								s_val = obj == null ? "" : obj;
							}
							datas.put(s_col, s_val);
						}
						submodel = getARdFromHashES(datas, submodel);
						model.addDetail(detailNumber, String.valueOf(subid), submodel);
					}
					subid++;
				}
				// 分页查询每pageSize做一次查询
				// 这里采用的是类似关系型数据库中的游标的方式避免了深度分页(from to 形式的分页会带来太多的资源消耗)带来的各种资源消耗
				// 通过游标id来遍历命中数据
				searchResponse = client.prepareSearchScroll(searchResponse1.getScrollId()).setScroll(new TimeValue(60000))//缓存数据存活时间
						.execute().actionGet();
			} while (subid < gtnum);
			if ("".equals(pageFlag)) {
				pageNo = 1;
			}
			//设置页面需要展示的东西 pageNo 当前需要展示的页数 pageSize 页面行显示数 totalnum 总记录数
			model = TXIBizSmallUtil.setPageNo(PageInfoControl, pageNo, Integer.valueOf(pageSize), Integer.valueOf(totalnum),
					model);
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
		
		/*******************全文检索入口*****************/
//		//分解输入参数
		Vector outputParamVt = TXIUtil.parseInputParameterBySeperator(inputParams);
		String tableName=outputParamVt.get(0)==null?"":outputParamVt.get(0).toString();//表名
		String cl_id=outputParamVt.get(1)==null?"":outputParamVt.get(1).toString(); //分类
		String conditions=outputParamVt.get(2)==null?"":outputParamVt.get(2).toString(); //查询条件
		String iuserName=outputParamVt.get(3)==null?"":outputParamVt.get(3).toString();	//增加数据创建用户
		String icomanualcodes=outputParamVt.get(4)==null?"":outputParamVt.get(4).toString();	//手工编码
		String ilang=outputParamVt.get(5)==null?"":outputParamVt.get(5).toString();	//语言标记
		System.out.println("语言标记="+ilang);
		String[] page = pageInfo.split(";");
		String current = page[0], pageFlag = page[1],firstPage = page[2],prePage =page[3],nextPage =page[4],lastPage = page[5];
//		
		/****************************************/
//		QueryBuilder qb = null;
		detailNumber = detailNumber == null ? "" : detailNumber;
		if (detailNumber.equals("")) {
			detailNumber = "detail0";
		}
		String pageSizeTemp = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 1, model);
		String PageInfoControl = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 2, model);
		Integer pageNo = TXIBizSmallUtil.XiGetCurrentPage(PageInfoControl, model);
		String pageSize = TXIBizSmallUtil.XiGetPageSize(pageSizeTemp, model);
//		Integer currentPage = TXIBizSmallUtil.XiGetCurrentPage(PageInfoControl, model);
		
		System.out.println("PageInfoControl="+PageInfoControl);
		System.out.println("pageNo="+String.valueOf(pageNo));
		System.out.println("pageSize="+pageSize);
		System.out.println("查询条件："+conditions);
//		System.out.println("currentPage="+String.valueOf(currentPage));
		BoolQueryBuilder must = QueryBuilders.boolQuery();
		//如果不是根节点的cl_id就应该加上cl_id的过滤条件
//		if(!isOrnotRootcl_id(cl_id, tableName)){
//			must = must.filter(QueryBuilders.termQuery("CL_ID", cl_id));
//		}
		//如果查询条件为空则全文匹配查询否则每一个字段都要模糊查询
		QueryBuilder qb = QueryBuilders.termQuery("CO_VALID", "1"); 
		if (conditions == null || "".equals(conditions.trim())) {
			//这里要改成查询co_valid=1的记录
			//避免用must会查询匹配分值（分值一般没用降低性能）
			must.filter(qb);
		} else {
//			must = ESUtil.splitChineseToFullSearch(tableName,conditions.toLowerCase(),must);
			//escape是去除特殊符号operator操作符匹配token里边的内容用and匹配不用or（默认设置是or）
			must = must.filter(QueryBuilders.queryStringQuery(QueryParser.escape(conditions.toLowerCase())).defaultOperator(Operator.AND))
					   .filter(qb);
		}
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
		/*************************新加的模糊查询*************************/
		//先判断
		initClient();
		SearchResponse searchResponse = client.prepareSearch(index.toLowerCase())
				.setTypes(tableName.toLowerCase())
				.setQuery(QueryBuilders.constantScoreQuery(must))//不进行分值计算
				.setSize(Integer.valueOf(pageSize))
				.setTimeout(new TimeValue(60000))
				.get();
		//符合条件的总记录数
		int totalnum = (int) searchResponse.getHits().getTotalHits();
		//如果查出来的内容是空的并且查询条件不为空且内容不全是中文
		if(totalnum==0&&!"".equals(conditions)&&!ESUtil.allChinese(conditions)){
			//得到
			String[] fields = ESUtil.getMappingMetaDatas(index, tableName);
			BoolQueryBuilder filter = new BoolQueryBuilder();
			filter.filter(qb).filter(termsQ);
			//构建聚合查询的条件
			TermsAggregationBuilder agg1 = AggregationBuilders.terms("code").field("CO_MANUALCODE.keyword");
			for(Object field : fields){
				//这两个字段不用模糊查询
				if("CO_CREATETIME".equalsIgnoreCase(field.toString())||"IMPORTSEQ".equalsIgnoreCase(field.toString())){
					
				}else{
					//后缀位_EN的则不进行模糊查询
					if(ESConstant.lang_zh.equalsIgnoreCase(ilang)){
						if(!field.toString().endsWith("_EN")){
							if(ESUtil.existChinese(conditions)){//内容有中文的直接用match查询
								filter.should(QueryBuilders.matchQuery(field.toString(),conditions.toLowerCase().trim()));
							}else{//无中文则用模糊
								filter.should(QueryBuilders.wildcardQuery(field.toString()+".keyword", "*"+conditions.trim()+"*"));
							}
						}
					}else{
						if(ESUtil.existChinese(conditions)){
							filter.should(QueryBuilders.matchQuery(field.toString(),conditions.toLowerCase().trim()));
						}else{
							filter.should(QueryBuilders.wildcardQuery(field.toString()+".keyword", "*"+conditions.trim()+"*"));
						}
					}
				}
			}
			model = ESFuzzySearchtoModel(index, detailNumber, tableName, filter,agg1, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
//			model = ESSearchtoModel(index,detailNumber,tableName, filter, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
			/*************************新加的模糊查询*************************/
		}else{
			//客户端发送查询请求
			model = ESSearchtoModel(index,detailNumber,tableName, must, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
		}
		return model;
	}
	
	/**
	 * es模糊查询
	 * @param index
	 * @param detailNumber
	 * @param tableName
	 * @param must
	 * @param aggs
	 * @param pageSize
	 * @param pageFlag
	 * @param current
	 * @param PageInfoControl
	 * @param pageNo
	 * @param model
	 * @return
	 */
	public static TXIModel ESFuzzySearchtoModel(String index,String detailNumber,String tableName, QueryBuilder must,AggregationBuilder aggs, String pageSize, String pageFlag,
			String current, String PageInfoControl, int pageNo, TXIModel model) {
		System.out.println("细表名称="+detailNumber);
		// 客户端发送查询请求
		initClient();
		SearchResponse searchResponse = client.prepareSearch(index.toLowerCase())
				.setTypes(tableName.toLowerCase())
				.setQuery(QueryBuilders.constantScoreQuery(must))//不进行分值计算
				.addAggregation(aggs)//group by 聚合查询
				.setTimeout(new TimeValue(60000))
				.execute()
				.actionGet();
		// 符合条件的总记录数
		int totalnum = (int) searchResponse.getHits().getTotalHits();
		//将编码放到数组中拼接查询条件使用
		Terms terms = searchResponse.getAggregations().get("code");
		
		List codes = new ArrayList<>(terms.getBuckets().size());
		//将group by查出来的co_manualcode分组桶中的值
		for(Terms.Bucket bucket : terms.getBuckets()){
		    //term查询将内容中的字母都改成小写
			codes.add(bucket.getKeyAsString().toLowerCase());
		}
		//查询要展示的数据
		if(codes.isEmpty()){
			model.getDetail(detailNumber).clear();
			model = TXIBizSmallUtil.setPageNo(PageInfoControl, 1, Integer.valueOf(pageSize), Integer.valueOf(0),
					model);
		}else{
			BoolQueryBuilder filter1 = new BoolQueryBuilder();
			SearchResponse searchResponse1 = client.prepareSearch(index.toLowerCase())
					.setTypes(tableName.toLowerCase())
					.addSort("CO_MANUALCODE.keyword", SortOrder.ASC)//按照哪个字段排序 ，keyword表示不进行分词
					.setScroll(new TimeValue(60000))
					.setQuery(filter1.filter(QueryBuilders.termsQuery("CO_MANUALCODE", codes)).filter(QueryBuilders.termQuery("CO_VALID", "1")))//不进行分值计算
					.setSize(Integer.valueOf(pageSize))
					.setTimeout(new TimeValue(60000))
					.execute()
					.actionGet();
			System.out.println("查询命中数:                       " + String.valueOf(totalnum));
			// 获得总页数
			int totalPage = getTotalPage(totalnum, pageSize);
			// 根据翻页格式确定取哪一页得数据，得把这20条数据的范围确定
			int ltnum = -1, gtnum = -1;
			// 载入时的标记为空首页的标记也为空
			int[] range = setSubidRange(ltnum, gtnum, totalnum, totalPage, pageFlag, pageSize, current);
			ltnum = range[0];
			gtnum = range[1];
			System.out.println("查询起始数:                       " + String.valueOf(ltnum));
			System.out.println("查询结束数:                       " + String.valueOf(gtnum));
			// 处理查询结果
			// 从model中得到页记录信息
			Hashtable detail0 = (Hashtable) model.getDetails().get(detailNumber);
			if(detail0==null){
				model.setDetail(detailNumber,new Hashtable());
			}
			int subid = 0;
			
			model.getDetail(detailNumber).clear();
//		detail0.clear();
			System.out.println(model.getDetails().get(detailNumber));
			//对co_createtime进行long转date
			DateFormat timeParse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
			// 计算出要取那一页得数据（?<subid<?）
			do {
				for (SearchHit hit : searchResponse1.getHits().getHits()) {
					// 到分页的页数据之后就将数据放入到detail0中
					if (subid < gtnum && subid >= ltnum) {
						Map<String, Object> source = hit.getSource();
						TXIModel submodel = new TXIModel(model);
						// //由于hashtable中的键值均不能为null所以放弃用putAll()了
						Hashtable<String, Object> datas = new Hashtable<String, Object>();
						for (String key : source.keySet()) {
							Object obj = source.get(key);
							String s_col = key == null ? "" : key;
							Object s_val=null;
							//ES中存储的创建时间字段为long类型所以展示的时候转化成String
							if("CO_CREATETIME".equals(key)){
//							System.out.println(obj);
								if(obj!=null){
									s_val = timeParse.format(new Date(Long.parseLong(obj.toString())));
								}else{
									s_val = "";
								}
							}else{
								s_val = obj == null ? "" : obj;
							}
							datas.put(s_col, s_val);
						}
						submodel = getARdFromHashES(datas, submodel);
						model.addDetail(detailNumber, String.valueOf(subid), submodel);
					}
					subid++;
					// System.out.println("subid="+String.valueOf(subid));
				}
				// 分页查询每pageSize做一次查询
				// 这里采用的是类似关系型数据库中的游标的方式避免了深度分页(from to 形式的分页会带来太多的资源消耗)带来的各种资源消耗
				// 通过游标id来遍历命中数据
				searchResponse = client.prepareSearchScroll(searchResponse1.getScrollId()).setScroll(new TimeValue(60000))//缓存数据存活时间
						.execute().actionGet();
			} while (subid < gtnum);
//		client.close();
			//
			// System.out.println("总记录数="+String.valueOf(totalnum));
			if ("".equals(pageFlag)) {
				pageNo = 1;
			}
			
			/**
			 * 设置页面需要展示的东西 pageNo 当前需要展示的页数 pageSize 页面行显示数 totalnum 总记录数
			 */
			model = TXIBizSmallUtil.setPageNo(PageInfoControl, pageNo, Integer.valueOf(pageSize), Integer.valueOf(totalnum),
					model);
			
		}
		// 不要关闭链接因为一旦关闭client要重新创建延迟很高而且单例创建client之后在高并发场景下requests是交给netty处理的netty已经实现了线程池
//		if(client!=null){
//			client.close();
//		}
		System.out.println("......ES查询结束");
		return model;
	}
	/**
	 * 分页查询并将结果放入detail0
	 * @param index 索引
	 * @param detailNumber 细表名称
	 * @param tableName 表名称
	 * @param must 需要过滤查询条件的布尔查询
	 * @param pageSize 一页显示多少条数据
	 * @param pageFlag 翻页格式:当前页;翻页标记(0:首页,1:前页,2:后页,3:末页,4:不翻页),''为重置
	 * @param current 当前页
	 * @param PageInfoControl 
	 * @param pageNo 
	 * @param model
	 * @return
	 */
	public static TXIModel ESSearchtoModel(String index,String detailNumber,String tableName, QueryBuilder must, String pageSize, String pageFlag,
			String current, String PageInfoControl, int pageNo, TXIModel model) {
		System.out.println("细表名称="+detailNumber);
		// 客户端发送查询请求
		initClient();
		SearchResponse searchResponse = client.prepareSearch(index.toLowerCase())
				.setTypes(tableName.toLowerCase())
				.addSort("CO_MANUALCODE.keyword", SortOrder.ASC)//按照哪个字段排序
				.setScroll(new TimeValue(60000))
				.setQuery(QueryBuilders.constantScoreQuery(must))//不进行分值计算
				.setSize(Integer.valueOf(pageSize))
				.setTimeout(new TimeValue(60000))
				.execute()
				.actionGet();
		// 符合条件的总记录数
		int totalnum = (int) searchResponse.getHits().getTotalHits();
		System.out.println("查询命中数:                       " + String.valueOf(totalnum));
		// 获得总页数
		int totalPage = getTotalPage(totalnum, pageSize);
		// 根据翻页格式确定取哪一页得数据，得把这20条数据的范围确定
		int ltnum = -1, gtnum = -1;
		// 载入时的标记为空首页的标记也为空
		int[] range = setSubidRange(ltnum, gtnum, totalnum, totalPage, pageFlag, pageSize, current);
		ltnum = range[0];
		gtnum = range[1];
		System.out.println("查询起始数:                       " + String.valueOf(ltnum));
		System.out.println("查询结束数:                       " + String.valueOf(gtnum));
		// 处理查询结果
		// 从model中得到页记录信息
		Hashtable detail0 = (Hashtable) model.getDetails().get(detailNumber);
		if(detail0==null){
			model.setDetail(detailNumber,new Hashtable());
		}
		int subid = 0;
		
		model.getDetail(detailNumber).clear();
//		detail0.clear();
		System.out.println(model.getDetails().get(detailNumber));
		//对co_createtime进行long转date
		DateFormat timeParse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		//计算出要取那一页得数据（?<subid<?）
		do {
			for (SearchHit hit : searchResponse.getHits().getHits()) {
				// 到分页的页数据之后就将数据放入到detail0中
				if (subid < gtnum && subid >= ltnum) {
					Map<String, Object> source = hit.getSource();
					TXIModel submodel = new TXIModel(model);
					//由于hashtable中的键值均不能为null所以放弃用putAll()了
					Hashtable<String, Object> datas = new Hashtable<String, Object>();
					for (String key : source.keySet()) {
						Object obj = source.get(key);
						String s_col = key == null ? "" : key;
						Object s_val=null;
						//ES中存储的创建时间字段为long类型所以展示的时候转化成String
						if("CO_CREATETIME".equals(key)){
//							System.out.println(obj);
							if(obj!=null){
								s_val = timeParse.format(new Date(Long.parseLong(obj.toString())));
							}else{
								s_val = "";
							}
						}else{
							s_val = obj == null ? "" : obj;
						}
						datas.put(s_col, s_val);
					}
					submodel = getARdFromHashES(datas, submodel);
					model.addDetail(detailNumber, String.valueOf(subid), submodel);
				}
				subid++;
				// System.out.println("subid="+String.valueOf(subid));
			}
			// 分页查询每pageSize做一次查询
			// 这里采用的是类似关系型数据库中的游标的方式避免了深度分页(from to 形式的分页会带来太多的资源消耗)带来的各种资源消耗
			// 通过游标id来遍历命中数据
			searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(60000))//缓存数据存活时间
					.execute().actionGet();
		} while (subid < gtnum);
//		if(client!=null){
//			client.close();
//		}
		if ("".equals(pageFlag)) {
			pageNo = 1;
		}
		
		/**
		 * 设置页面需要展示的东西 pageNo 当前需要展示的页数 pageSize 页面行显示数 totalnum 总记录数
		 */
		model = TXIBizSmallUtil.setPageNo(PageInfoControl, pageNo, Integer.valueOf(pageSize), Integer.valueOf(totalnum),
				model);
		// 关闭链接
		System.out.println("......ES查询结束");
		return model;
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
	 * 设置subid的范围取查询的数据的范围
	 * @param ltnum 范围比较的较小数
	 * @param gtnum 范围比较的较大数
	 * @param totalnum 总记录数
	 * @param totalPage 总页数
	 * @param pageFlag 翻页格式:当前页;翻页标记(0:首页,1:前页,2:后页,3:末页,4:不翻页),''为重置
	 * @param pageSize 每页的行设置
	 * @param current 当前页
	 */
	public static int[] setSubidRange(int ltnum,int gtnum,int totalnum,int totalPage,String pageFlag,String pageSize,String current){
		//载入时的标记为空首页的标记也为空
		if ("".equals(pageFlag) || "0".equals(pageFlag)) {
			ltnum = 0;
			if(Integer.valueOf(pageSize)<=totalnum){
				gtnum = Integer.valueOf(pageSize);//当分页行数小于总数时
			}else{
				gtnum = totalnum;//当分页行数大于总数时
			}
			System.out.println("gtnum="+gtnum);
		//上一页
		} else if ("1".equals(pageFlag)) {
			if("2".equals(current)){
				ltnum = 0;
				gtnum = Integer.valueOf(pageSize);
			}else{
				ltnum = (Integer.valueOf(current) - 2) * 20;
				gtnum = (Integer.valueOf(current) - 1) * 20;
			}
		//下一页
		} else if ("2".equals(pageFlag)) {
			//当前页的下一页是最后一页时
			if(String.valueOf(totalPage-1).equals(current)){
				ltnum = (totalPage-1)*20;
				gtnum = totalnum;
			}else{
				ltnum = (Integer.valueOf(current)) * 20;
				gtnum = (Integer.valueOf(current) + 1) * 20;
			}
		//尾页
		} else if ("3".equals(pageFlag)) {
			ltnum = (totalPage-1)*20;
			gtnum = totalnum;
		//go按钮
		} else if ("4".equals(pageFlag)) {
			ltnum = (Integer.valueOf(current) - 1) * 20;
			gtnum = (Integer.valueOf(current)) * 20;
		}
		int[] range= {ltnum,gtnum};
		return range;
	}
	/**讲记录添加到指定细表
	 * @param record  查询得到的数据记录
	 * @param detailModel 记录放到细表中
	 * @return
	 */
	public static TXIModel getARdFromHashES(Hashtable record, TXIModel detailModel) {
        Enumeration recordEn = record.keys();
        String outPutFieldName = "";
        Object result = "";
        while (recordEn.hasMoreElements()) {
            //取得输出字段名
            outPutFieldName = (String) recordEn.nextElement();
            //取得字段名标识的值
            result = record.get(outPutFieldName);
            //result = result == null ? "" : result;
            //若输出参数中包含"ID"字符串则将记录ID置入modelID中
            if (outPutFieldName.equalsIgnoreCase(JNDINames.ID)) {
                detailModel.setId(result.toString());
            }
            if (result != null) {
                detailModel.setView(outPutFieldName, result);
            }
        }
        return detailModel;
    }
	/**得到总页数
	 * @param totalnum 总记录数
	 * @param pageSize 每页的行设置
	 * @return
	 */
	public static int getTotalPage(int totalnum,String pageSize){
		int totalPage=0;
		if(!"0".equals(pageSize)){
			int flag = totalnum%Integer.valueOf(pageSize);
			if(flag!=0){
				totalPage = totalnum/Integer.valueOf(pageSize)+1;
			}else{
				totalPage = totalnum/Integer.valueOf(pageSize);
			}
		}
		return totalPage;
	}
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
	 * @param tableName
	 * @param str
	 * @param filter
	 * @return
	 */
	public static BoolQueryBuilder  getSearchConditionFilter(String splitor,String str,BoolQueryBuilder filter){
//		Map<String,String> map = new HashMap<String, String>();
		String[] split = ESUtil.splitConditions(str,";");
//		for(int i=0;i<split.length;i++){
//			split[i].
//		}
		String[] split2 = null;
		int first =-1,last=-1;
		String likeVal = null;
		//SYS LIKE '%3000MDM001;A4,3000ECC001;A4%';COUNT LIKE '%AD,安道尔%'
		for(int i=0;i<split.length;i++){
			split2 = split[i].split(splitor);
			first = split2[1].indexOf("%");
			last = split2[1].lastIndexOf("%");
			likeVal = split2[1].trim().substring(first, last-1);
//			filter = ESUtil.splitChineseToCombinSearch(tableName,split2[0].trim(), likeVal, filter);
			filter = filter.filter(QueryBuilders.matchQuery(split2[0].trim(), likeVal.toLowerCase()).operator(Operator.AND));
//			map.put(split2[0].trim(), likeVal);
			System.out.println(split2[0].trim()+"="+likeVal );
		}
		return filter;
	}
	/**
	 * 解析模糊 查询条件的sql字符串 
	 * @param tableName
	 * @param str
	 * @param filter
	 * @return
	 */
	public static BoolQueryBuilder  getFuzzySearchConditionFilter(String splitor,String str,BoolQueryBuilder filter){
//		Map<String,String> map = new HashMap<String, String>();
		String[] split = ESUtil.splitConditions(str,";");
//		for(int i=0;i<split.length;i++){
//			split[i].
//		}
		String[] split2 = null;
		int first =-1,last=-1;
		String likeVal = null;
		//SYS LIKE '%3000MDM001;A4,3000ECC001;A4%';COUNT LIKE '%AD,安道尔%'
		for(int i=0;i<split.length;i++){
			split2 = split[i].split(splitor);
			first = split2[1].indexOf("%");
			last = split2[1].lastIndexOf("%");
			likeVal = split2[1].trim().substring(first, last-1);
			if(ESUtil.existChinese(likeVal)){
				filter = filter.filter(QueryBuilders.matchQuery(split2[0].trim(), likeVal.toLowerCase()).operator(Operator.AND));
			}else{
				filter = filter.filter(QueryBuilders.wildcardQuery(split2[0].trim(), "*"+likeVal.toLowerCase()+"*"));
			}
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
		//编码分开
		String[] codes = conditions.split(",");
		String[] _codes = new String[codes.length];
		for(int i=0;i<codes.length;i++){
			_codes[i] = codes[i].substring(1, codes[i].length()-1).toLowerCase();
		}
		// 分页信息
		String[] page = pageInfo.split(";");
		String current = page[0], pageFlag = page[1], firstPage = page[2], prePage = page[3], nextPage = page[4],
				lastPage = page[5];

		detailNumber = detailNumber == null ? "" : detailNumber;
		if (detailNumber.equals("")) {
			detailNumber = "detail0";
		}
		String pageSizeTemp = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 1, model);
		String PageInfoControl = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 2, model);
		Integer pageNo = TXIBizSmallUtil.XiGetCurrentPage(PageInfoControl, model);
		String pageSize = TXIBizSmallUtil.XiGetPageSize(pageSizeTemp, model);
		Integer currentPage = TXIBizSmallUtil.XiGetCurrentPage(PageInfoControl, model);

		System.out.println("pageSizeTemp=" + pageSizeTemp);
		System.out.println("PageInfoControl=" + PageInfoControl);
		System.out.println("pageNo=" + String.valueOf(pageNo));
		System.out.println("pageSize=" + pageSize);
		System.out.println("currentPage=" + String.valueOf(currentPage));
		BoolQueryBuilder filter = new BoolQueryBuilder();
		filter.filter(QueryBuilders.matchQuery("CO_VALID", "1"));
		if(_codes.length>0){
			filter.filter(QueryBuilders.termsQuery("CO_MANUALCODE", _codes));
		}
		//此处不用分页,
		model = ESSearchtoModel(index, detailNumber, tableName, filter, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
		return model;
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
		BoolQueryBuilder must = QueryBuilders.boolQuery();
		BoolQueryBuilder fuzzy = QueryBuilders.boolQuery();
		QueryBuilder qb = null;
		Vector outputParamVt = TXIUtil.parseInputParameterBySeperator(inputParams);
		Enumeration elements = outputParamVt.elements();
		
		//有多少参数就创建多大的数组
		String[] input =  new String[outputParamVt.size()];
		int i=0;
		while(elements.hasMoreElements()){
			String val = (String)elements.nextElement();
			System.out.println(val);
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
				fuzzy.filter(idsbuliders);
			}else{
				return model;
			}
		}
		//得到输入参数个数-1用;号隔开
		System.out.println("cl_id="+cl_id+"  tableName="+tableName+" startTime ="+startTime+" endTime="+endTime);
		System.out.println(combinations);
		//如果不是根节点的cl_id加上cl_id过滤条件(勿删)
//		if(!isOrnotRootcl_id(cl_id, tableName)){
//			must.filter(QueryBuilders.termQuery("CL_ID", cl_id));
//		}
		//判断组合查询条件是否为空
		if(!"".equals(combinations)&&combinations!=null){
			must = getSearchConditionFilter("LIKE", combinations, must);
			fuzzy = getFuzzySearchConditionFilter("LIKE", combinations, fuzzy);
			
		}
		//放在should的后面
		fuzzy = fuzzy.filter(QueryBuilders.termQuery("CO_VALID", "1"));
		//设置时间范围
		if(!"".equals(startTime)||!"".equals(endTime)){
			must = setTimeRangeBulider(must, startTime, endTime);
			fuzzy = setTimeRangeBulider(fuzzy, startTime, endTime);
		}
		must.filter(QueryBuilders.termsQuery("CO_ATTRLANGUAGE", ilang,ESConstant.lang_all));
		fuzzy = fuzzy.filter(QueryBuilders.termsQuery("CO_ATTRLANGUAGE", ilang,ESConstant.lang_all));
        //分页信息
		String[] page = pageInfo.split(";");
		String current = page[0], pageFlag = page[1],firstPage = page[2],prePage =page[3],nextPage =page[4],lastPage = page[5];
		
		detailNumber = detailNumber == null ? "" : detailNumber;
		if (detailNumber.equals("")) {
			detailNumber = "detail0";
		}
		String pageSizeTemp = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 1, model);
		String PageInfoControl = TXIBizSmallUtil.XiGetValueOfSpecificalNumberBySemicolon(pageSizeTempSSS, 2, model);
		Integer pageNo = TXIBizSmallUtil.XiGetCurrentPage(PageInfoControl, model);
		String pageSize = TXIBizSmallUtil.XiGetPageSize(pageSizeTemp, model);
		Integer currentPage = TXIBizSmallUtil.XiGetCurrentPage(PageInfoControl, model);
		
		System.out.println("inputParams="+inputParams);
		System.out.println("pageSizeTemp="+pageSizeTemp);
		System.out.println("PageInfoControl="+PageInfoControl);
		System.out.println("pageNo="+String.valueOf(pageNo));
		System.out.println("pageSize="+pageSize);
		System.out.println("currentPage="+String.valueOf(currentPage));
		//判断查询结果是否存在
		initClient();
		SearchResponse searchResponse = client.prepareSearch(index.toLowerCase())
				.setTypes(tableName.toLowerCase())
				.setQuery(QueryBuilders.constantScoreQuery(must))//不进行分值计算
				.setSize(Integer.valueOf(pageSize))
				.get(new TimeValue(60000));
		int totalnum = (int) searchResponse.getHits().getTotalHits();
		if(totalnum==0){
			//构建聚合查询的条件
//			TermsAggregationBuilder agg1 = AggregationBuilders.terms("CO_MANUALCODE").field("CO_MANUALCODE.keyword");
			model = ESSearchtoModel(index, detailNumber, tableName, fuzzy,pageSize, pageFlag, current, PageInfoControl, pageNo, model);
		}else{
			//客户端发送查询请求
			model = ESSearchtoModel(index,detailNumber,tableName, must, pageSize, pageFlag, current, PageInfoControl, pageNo, model);
		}
		return model;
		
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
	 * 封装二级平台调用集团查询返回的xml
	 * @param source1 属性信息大小量纲=DXLG
	 *				   物料名称=MATNAME(查属性表)
	 * @param source0 MATNAME=AAAAAA
	 *				  DXLG=10（查主数据表）
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
	/**
	 * @param args
	 * @throws UnknownHostException
	 * @Description 考虑到扩展性创建一个索引（名字就定位mdmindex） type就指定为表名就可以了,id自增序号不用说了
	 *              type类型不适合 完全不同类型的数据 。如果两个类型的字段集是互不相同的，
	 *              这就意味着索引中将有一半的数据是空的（字段将是 稀疏的 ），最终将导致性能问题。 在这种情况下，最好是使用两个单独的索引
	 */
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		/**================================insert结束================================**/
		
		// 查询字段值要小写
		MatchQueryBuilder qb1 = QueryBuilders.matchQuery("ZCZMS","20111136");
		//term查询没有添加分词器所以中文查询查不到的
		SimpleQueryStringBuilder qb4 = QueryBuilders.simpleQueryStringQuery("上盖板");
		// 匹配前缀查询
//		MatchPhrasePrefixQueryBuilder qb = QueryBuilders.matchPhrasePrefixQuery(volumn, matnum);
		// 精确匹配
		MatchQueryBuilder qb6 = QueryBuilders.matchQuery("CVSTREET", "北京市丰台区青塔小区春园4座B-503室");
		MatchQueryBuilder qb16 = QueryBuilders.matchQuery("INDSDESC", "机械工程");
		// 如果参数传的是空，那就用全文匹配
		MatchAllQueryBuilder qb8 = QueryBuilders.matchAllQuery();
		// 多个字段匹配一个模糊值
		// 可以先完全匹配然后三次模糊匹配如果是将所有的字段都加上模糊查询(查询之前一定要把字母大写转换成小写)
		// 可以循环字段进行添加模糊查询
//		WildcardQueryBuilder qb3 = QueryBuilders.wildcardQuery("SYS", "*3000mdm001;a1,3000mdm001;ts10*");
		TermQueryBuilder qb3 = QueryBuilders.termQuery("SYS", "3000mdm001;a1,3000mdm001;ts10");
		String title ="qch305-&&||!(){}[]^\"~*?:\\";
		title = "DGIXφCEST10".toLowerCase();
		title =org.apache.lucene.queryparser.classic.QueryParser.escape(title);
		MatchPhraseQueryBuilder matchPhraseQuery = QueryBuilders.matchPhraseQuery("MATDXLG", title);
		System.out.println(title.toLowerCase());
		WildcardQueryBuilder qb11 = QueryBuilders.wildcardQuery("BACKUP3", "*"+title+"*");
		
		PrefixQueryBuilder qb14 = QueryBuilders.prefixQuery("matgroup", "m0000001785");
		// 范围匹配的话
		RangeQueryBuilder qb12 = QueryBuilders.rangeQuery("CO_CREATETIME").gte(new Date());
		// QueryBuilders.wildcardQuery("", );
		// 求交集用boolean查询
		SimpleQueryStringBuilder qsp = QueryBuilders.simpleQueryStringQuery("m000000183815");
		QueryStringQueryBuilder qsq = QueryBuilders.queryStringQuery("zc");
		WildcardQueryBuilder qbw = QueryBuilders.wildcardQuery("MATNUM", "*m00000018381*");
		TermQueryBuilder term1 = QueryBuilders.termQuery("BACKUP3","QCH305\\-80\\-00\\-000".toLowerCase());
		
		BoolQueryBuilder filter = QueryBuilders.boolQuery();
		Calendar calender  = Calendar.getInstance();
		/*直接的id关联的将similarity_batch_temp中的co_id看做是parent_id,
		 * 
		*/
//		MatchQueryBuilder parchil = QueryBuilders.matchQuery("BATCH", "000000008898");
//		HasParentQueryBuilder hasParentQuery = JoinQueryBuilders.hasParentQuery("similarity_batch_temp",parchil, true);
//		
		MatchQueryBuilder chil = QueryBuilders.matchQuery("CO_ID", "13195");
		HasChildQueryBuilder hasChildQuery = JoinQueryBuilders.hasChildQuery("t_vendor",chil , ScoreMode.None).innerHit(new InnerHitBuilder());
		/**===========nested嵌套查询============**/
		//引用对象可以
//		QueryBuilders.nestedQuery("", query, ScoreMode.None);
		
		String starttime1 = "2017-06-24";
		String starttime = "2016-05-18";
//		String endtime = "2017-01-06 00:00:00";
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd ");
		Date start = sdf1.parse(starttime);
		calender.setTime(start);
		System.out.println(calender.getTime().toInstant().toString());
//		Date end = sdf.parse(endtime);
		RangeQueryBuilder test1 = QueryBuilders.rangeQuery("CO_CREATETIME")
				.gt(ESUtil.FormatTime(starttime1));
//		RangeQueryBuilder test = QueryBuilders.rangeQuery("CO_CREATETIME").format("yyyy-MM-dd")
//				.gt(starttime);
		filter.filter(test1);
		BoolQueryBuilder must = QueryBuilders.boolQuery()
				.filter(QueryBuilders.termQuery("ZCZMS","20111136"))
//				.mustNot(QueryBuilders.matchQuery("CO_ID", "778203"))
//				.filter(QueryBuilders.matchQuery("CO_VALID", "1"))
//				.filter(QueryBuilders.matchQuery("MATDXLG", "DGIXφCEST10"))
//				.filter(QueryBuilders.matchQuery("ZCZBM", "GB 2432-2036"))
//				.filter(QueryBuilders.matchQuery("MATISDESC", "GB 2312-2026"))
				;                                                                                                                                                                                                                                                                                                                                                                                                                             
//		AggregationBuilders.terms("aggs").                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
//				.must(qb9);
//				.must(qb11).must(qb3).must(qb6);
		// 有没有取交集的
//		must.filter();"MATGRUP"
//		String[] split = "铜锭".split("");
//		for(String str : split){
////			QueryStringQueryBuilder qb9 = QueryBuilders.queryStringQuery(str);
//			WildcardQueryBuilder qb9 = QueryBuilders.wildcardQuery("MATGRUP", "*"+str+"*");
//			must.filter(qb9);
//		}
		
		// QueryBuilders.moreLikeThisQuery()
		// MoreLikeThisQueryBuilder.Item
		client = Test.initClient();
//		CreateIndexResponse actionGet = client.admin().indices().prepareCreate(tablename.toLowerCase() + "index").addMapping("t_mat",mapping).execute().actionGet();
//		SearchRequestBuilder searchbulider = client.prepareSearch(tablename.toLowerCase() + "index").setTypes(tablename.toLowerCase())
//							                       .addFieldDataField("");
		String tablename = "t_vendor";
		String[] ids= {"13195","13197"};
		IdsQueryBuilder idsQuery = new IdsQueryBuilder();
		idsQuery.addIds(ids);
		MatchPhraseQueryBuilder test = QueryBuilders.matchPhraseQuery("CO_SYSTEMID", "BWPCLNT800;,CNRSRM;,3000ECC001");
		MatchQueryBuilder testt = QueryBuilders.matchQuery("OD", "5000YY0100;");
//		BoolQueryBuilder zk = QueryBuilders.boolQuery();
		TermQueryBuilder termQuery = QueryBuilders.termQuery("CO_SYSTEMID", "BWPCLNT800;,CNRSRM;,3000ECC001");
		
		MatchQueryBuilder zk_ik = QueryBuilders.matchQuery("Tags", "我是饭桶客户端").analyzer("standard")
		.operator(Operator.AND);
		QueryStringQueryBuilder queryStringQuery = QueryBuilders.queryStringQuery("").defaultOperator(Operator.AND);
		
		/*SearchResponse scrollResp = client.prepareSearch("mdmindex")
				.setTypes(tablename)                                                                                                                                                                                                                                                                                        
				.addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
				.setScroll(new TimeValue(60000))
				.setQuery(queryStringQuery)
//				.setTypes("similarity_batch_temp")
//				.addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC).setScroll(new TimeValue(60000))
//				.setQuery(idsQuery)
				.execute()
				.actionGet();
		
		
//		TimeUnit.MILLISECOND.smaller();
		System.out.println(scrollResp.getHits().totalHits);*/
//		String index = "t_vendorindex";
//		String type = "t_vendor"; 
		
		/*创建一个空指针CVSTREET加上中文分词并指定分片数量
		 * number_of_shards，及副本数量number_of_replicas
		 * 
		 */
		Settings build = Settings.builder().put("number_of_shards", 1)
						  .put("number_of_replicas", 1)
						  .build();
		 client.admin().indices().prepareCreate("mdm_en")
		 	  .setSettings(build)
		      .execute().actionGet();
		 XContentBuilder endObject = XContentFactory.jsonBuilder()
		 .startObject()
		 .startObject("t_mat")
		 .startObject("properties")
		 .startObject("MATVOCH").field("type","string").field("index", "not_analyzed")
		 .endObject()
		 .endObject()
		 .endObject()
		 .endObject();
//		client.prepareDelete("t_vendorindex", type,"").get();
		Map<String,Object> fields = new HashMap<String,Object>();
		Map<String,Object> fields_child = new HashMap<String,Object>();
		fields_child.put("type", "keyword");
		fields_child.put("ignore_above", "256");
		fields.put("keyword", fields_child);
		
		XContentBuilder builder1 = XContentFactory.jsonBuilder()
		.startObject()
//		.startObject("similarity_batch_temp")
//		.endObject()
        .startObject("t_vendor")
//        .startObject("_parent").field("type", "t_vendor").endObject()
        .startObject("properties")
        .startObject("CO_ID").field("type", "integer").field("store", "yes").endObject()
        .startObject("CO_FREEZE").field("type", "integer").field("store", "yes").endObject()
        .startObject("CO_HASCHILD").field("type", "integer").field("store", "yes").endObject()
        .startObject("CO_DEL").field("type", "integer").field("store", "yes").endObject()
        .startObject("CO_PID").field("type", "integer").field("store", "yes").endObject()
        .startObject("CO_VALID").field("type", "integer").field("store", "yes").endObject()
        .startObject("CO_CREATETIME").field("type", "long").field("store", "yes").endObject()
        .startObject("CVCOUNT").field("type", "string").field("store", "true").endObject()
        .startObject("CO_FLANS").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_MODIFYTIME").field("type", "text").field("fields", fields).endObject()
        .startObject("CO_DESC").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_DESC_SRC").field("type", "text").field("fields",fields).endObject()
        .startObject("QXREN").field("type", "text").field("fields",fields).endObject()
        .startObject("SIMILARITY").field("type", "text").field("fields",fields).endObject()
        .startObject("LYXT").field("type", "text").field("fields",fields).endObject()
        .startObject("QXSHIJIAN").field("type", "text").field("fields",fields).endObject()
        .startObject("LSBM").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_CREATEDEPT").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_XSDTQSJ").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_MODIFYORG").field("type", "text").field("fields",fields).endObject()
        .startObject("CZSJ").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_ID_MARK").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_TREEBACKFILL").field("type", "text").field("fields",fields).endObject()
        .startObject("SJSXSJ").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_SUBMITUSER").field("type", "text").field("fields",fields).endObject()
        .startObject("CL_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_PUBLISHTIME").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_SUBMITTIME").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_MODIFYDEPT").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_MODIFYUSER").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_CREATER").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_CREATEORG").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_PUBLISHUSER").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_SYSTEMID").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_MANUALCODE").field("type", "text").field("fields",fields).endObject()
        .startObject("CVNAME").field("type", "text").field("fields",fields).endObject()
        .startObject("VVDNUM").field("type", "text").field("fields",fields).endObject()
        .startObject("CVSEARCH1").field("type", "text").field("fields",fields).endObject()
//        .startObject("CVSEARCH2").field("analyzer","ik_smart").field("search_analyzer","ik_smart").field("type", "text").field("fields",fields).endObject()
        .startObject("CVSEARCH2").field("type", "text").field("fields",fields).endObject()
        .startObject("CVPOSTAL").field("type", "text").field("fields",fields).endObject()
        .startObject("CVCITY").field("type", "text").field("fields",fields).endObject()
        .startObject("CVTEL").field("type", "text").field("fields",fields).endObject()
        .startObject("CVTELEXT").field("type", "text").field("fields",fields).endObject()
        .startObject("CVFAX").field("type", "text").field("fields",fields).endObject()
        .startObject("CVMOBILE").field("type", "text").field("fields",fields).endObject()
        .startObject("CVEMAIL").field("type", "text").field("fields",fields).endObject()
        .startObject("CVREMARK").field("type", "text").field("fields",fields).endObject()
        .startObject("VVDGP").field("type", "text").field("fields",fields).endObject()
        .startObject("CVOLDNAME").field("type", "text").field("fields",fields).endObject()
        .startObject("CVSTREET").field("type", "text").field("fields",fields).endObject()
        .startObject("CVCOUN").field("type", "text").field("fields",fields).endObject()
        .startObject("CVSTATE").field("type", "text").field("fields",fields).endObject()
        .startObject("VCCUNUM").field("type", "text").field("fields",fields).endObject()
        .startObject("CVTDPT").field("type", "text").field("fields",fields).endObject()
        .startObject("CVGCODE").field("type", "text").field("fields",fields).endObject()
        .startObject("CVTAXREG").field("type", "text").field("fields",fields).endObject()
        .startObject("CVTAX1").field("type", "text").field("fields",fields).endObject()
        .startObject("CHAR01").field("type", "text").field("fields",fields).endObject()
        .startObject("CDEL").field("type", "text").field("fields",fields).endObject()
        .startObject("VCCUNUM_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("CVTDPT_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("CVCOUN_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("CVSTATE_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("CVGCODE_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("CVFAXEXT").field("type", "text").field("fields",fields).endObject()
        .startObject("CBKFRE").field("type", "text").field("fields",fields).endObject()
        .startObject("CPUBIND").field("type", "text").field("fields",fields).endObject()
        .startObject("CPLOG").field("type", "text").field("fields",fields).endObject()
        .startObject("CMAP").field("type", "text").field("fields",fields).endObject()
        .startObject("COM_CODE").field("type", "text").field("fields",fields).endObject()
        .startObject("OLDNUM").field("type", "text").field("fields",fields).endObject()
        .startObject("SYSTEM_CODE").field("type", "text").field("fields",fields).endObject()
        .startObject("CVBKACCT").field("type", "text").field("fields",fields).endObject()
        .startObject("VVDGP_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("MDMVENDORSX1").field("type", "text").field("fields",fields).endObject()
        .startObject("VDGPDESC").field("type", "text").field("fields",fields).endObject()
        .startObject("VMAP").field("type", "text").field("fields",fields).endObject()
        .startObject("VPLOG").field("type", "text").field("fields",fields).endObject()
        .startObject("VDEL").field("type", "text").field("fields",fields).endObject()
        .startObject("VPUBIND").field("type", "text").field("fields",fields).endObject()
        .startObject("VBKFRE").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_FLANS").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_MAPS").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_PLOGS").field("type", "text").field("fields",fields).endObject()
        .startObject("WORK_ITEM_IDS").field("type", "text").field("fields",fields).endObject()
        .startObject("DZCO_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("DZCL_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("MDMVENDORSX2_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("MDMVENDORSX2").field("type", "text").field("fields",fields).endObject()
        .startObject("SYS_ID").field("type", "text").field("fields",fields).endObject()
        .startObject("SYS").field("type", "text").field("fields",fields).endObject()
        .startObject("SYSCODE").field("type", "text").field("fields",fields).endObject()
        .startObject("SYSNUM").field("type", "text").field("fields",fields).endObject()
        .startObject("VENLEVEL").field("type", "text").field("fields",fields).endObject()
        .startObject("ZJVENCZ").field("type", "text").field("fields",fields).endObject()
        .startObject("VENLEVEL3").field("type", "text").field("fields",fields).endObject()
        .startObject("CO_ATTRLANGUAGE").field("type", "text").field("fields",fields).endObject()
        .startObject("SYSAPPLYID").field("type", "text").field("fields",fields).endObject()
        .endObject()
        .endObject()
        .endObject();
		XContentBuilder builder = XContentFactory.jsonBuilder()
				.startObject()
//				.startObject("similarity_batch_temp")
//				.endObject()
	            .startObject("similarity_batch_temp")
	            .startObject("_parent").field("type", "t_mat").endObject()
	            .startObject("properties")
	            .endObject()
	            .endObject()
	            .endObject();
		/*XContentBuilder simbuilder = XContentFactory.jsonBuilder()
				.startObject()
				.startObject("similarity_batch_temp")
				.startObject("properties")
				.startObject("ID").field("type", "integer").field("store", "yes").endObject()
				.startObject("CO_ID_OLD").field("type", "integer").field("store", "yes").endObject()
				.startObject("CO_ID").field("type", "integer").field("store", "yes").endObject()
				.startObject("CL_ID").field("type", "integer").field("store", "yes").endObject()
				.startObject("SIMILARITY").field("type", "text").field("fields",fields).endObject()
				.startObject("BATCH").field("type", "text").field("fields",fields).endObject()
				.startObject("TABLENAME").field("type", "text").field("fields",fields).endObject()
				.startObject("ORDERBY").field("type", "text").field("fields",fields).endObject()
				.startObject("MAXSIGN").field("type", "text").field("fields",fields).endObject()
				.startObject("XH").field("type", "text").field("fields",fields).endObject()
				.endObject()
				.endObject()
				.endObject();*/
				
		/*XContentBuilder t_matbuilder = XContentFactory.jsonBuilder()
				.startObject()
				.startObject("t_mat")
				.startObject("_parent").field("type", "similarity_batch_temp").endObject()
				.startObject("properties")
				.endObject()
				.endObject()
				.endObject();
//		XContentBuilder builder_child = XContentFactory.jsonBuilder().startObject()
//				.startObject()
//				.startObject("t_vendormaps")
//				.startObject("_parent").field("type", "t_vendor").endObject()
//				.startObject("properties")
//				.startObject("CO_ID").field("type", "integer").field("store", "yes").endObject()
//				.endObject()
//				.endObject()
//				.endObject();*/
//		IndexResponse indexResponse = client.prepareIndex("t_vendorindex", "t_vendor").setSource(builder).get();
		PutMappingRequest mappingReq_child = Requests.putMappingRequest("mdm_en")
				.type("t_mat")
				.source(endObject);
				
		try {
			client.admin().indices().putMapping(mappingReq_child).get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// 按照索引名称以及DOC_FIELD_NAME和升序排列
//		System.out.println("总计路数="+String.valueOf(scrollResp.getHits().totalHits));
		
		// Scroll until no hits are returned
		/*int num = 1;
		DateFormat dateparse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		scrollResp.getHits().
		do {
			for (SearchHit hit : scrollResp.getHits()) {
//				Map<String, SearchHits> innerHits = hit.getInnerHits();
//				for(String key : innerHits.keySet()){
//					Iterator<SearchHit> iterator = innerHits.get(key).iterator();
//					while(iterator.hasNext()){
//						SearchHit next = iterator.next();
//						System.out.println(next.getSourceAsString());
//					}
//					
//				}
//				Object object = hit.getSource().get("CVCOUN");
				System.out.println(hit.getSourceAsString());
//				Iterator<java.util.Map.Entry<String, Object>> iterator = hit.getSource().entrySet().iterator();
//				Map<String, Object> key_val = new HashMap<String, Object>();
//				while(iterator.hasNext()){
//					Entry<String, Object> next = iterator.next();
//					if(next.getKey().equals("CO_CREATETIME")){
//						Date cr = new Date((long)next.getValue());
//						System.out.println(dateparse.format(cr));
//					}
//				}
				num++;
			}
			scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute()
					.actionGet();
			// num++;
			// System.out.println(num);
		} while (scrollResp.getHits().getHits().length != 0);*/
		// 混合查询
		// 可以加多个查询条件
	}
}
