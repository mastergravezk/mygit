package com.thit.elasticsearch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.HasChildQueryBuilder;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.thit.elasticsearch.common.ESUtil;
import com.thit.elasticsearch.esdb.ESDBOpreate;
import com.thit.elasticsearch.orcldb.DataTable;
import com.thit.elasticsearch.orcldb.DbOperation;
import com.xicrm.common.TXISystem;
import com.xicrm.model.TXIModel;
import com.xicrm.util.TXIUtil;
/**
 * @author zk
 * @version 2018-03-05 
 * @since jdk1.8
 */
public class TXIDuplicateChecker {
	private static Client client;
	
	//反射创建对象时用
	public TXIDuplicateChecker() {
	}
	
	public static void initClient(){
		if(client == null){
			client = ESUtil.getClient();
		}
	}
	/**
	 * 
	 * @param co_ids 要查重数据的主键
	 * @param tableName 查重数据表
	 * @param batch 批次号
	 * @param model 用户数据
	 * @return
	 */
	public static TXIModel dupliacteCheck(String index, String inputParams, String detailnum, String errorMsg,
			TXIModel model) {

		// 有多少参数就创建多大的数据
		String[] input = ESUtil.splitInputs(inputParams);

		// 声明变量
		String co_ids = null, //
				tableName = null, //
				batch = null, //
				flag = null;

		co_ids = input[0];
		tableName = input[1];
		batch = input[2];
		flag = input[3]; // 1代表批量导入的查重，2代表流程中的查重
		// 测试用的将detail0中数据取出
		// DateFormat timeParse1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Hashtable detail = (Hashtable) model.getDetails().get(detailnum);
		Enumeration keys = detail.keys();
		// int subid = 1;
		// 创建客户端
		initClient();
		// 将主键id分割并放入数组用于ids查询
		String[] ids = co_ids.split(",");
		IdsQueryBuilder idsbuliders = new IdsQueryBuilder();
		idsbuliders.addIds(ids);
		// 构建一个查询的builder(我只建了一个索引就是mdmindex,以后数据多的话就得将这个索引名称加到输入参数里了)
		SearchRequestBuilder searchBuilder = client.prepareSearch(index.toLowerCase());
		// id查询
		String tab = null;

		if ("1".equals(flag)) {
			System.out.println("......批量导入查重开始");
			//批量导入的查重
			tab = tableName.toLowerCase() + "_temp";// 申请批量导入
			// 一次操作结束后再关闭连接
//			System.out.println(tab+"的主键="+co_ids);
			//这里有一个现象：就是数据插入到——temp时不会立即刷新所以，后来查出来的数据还没有
			SearchResponse response = searchBuilder.setTypes(tab)
					.addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC).setQuery(idsbuliders).setSize(100).get(new TimeValue(60*1000));
			// 遍历命中记录
			SearchHit[] hits = response.getHits().getHits();
			for (SearchHit hit : hits) {
				Map<String, Object> source = hit.getSource();
				model = commonDupCheck(index, source, tableName, batch, searchBuilder, model);
			}
			System.out.println("......批量导入查重结束");
		} else if("2".equals(flag)){
			System.out.println("......流程中查重开始");
			//流程中的查重
			while (keys.hasMoreElements()) {
				String subkey = (String) keys.nextElement();
				TXIModel submodel = (TXIModel) detail.get(subkey);
				Hashtable source = submodel.getHtValues();
				System.out.println("查重入口");
				model = commonDupCheck(index, source, tableName, batch, searchBuilder, model);
			}
			System.out.println("......流程中查重结束");
		}
		// client.close();
		return model;
	}
/**
 * 
 * @param index 索引
 * @param source 用户数据
 * @param tableName 传入的表名
 * @param batch 批次号
 * @param searchBuilder 
 * @param model 
 * @return
 */
	public static TXIModel commonDupCheck(String index,Map source,String tableName,String batch,SearchRequestBuilder searchBuilder,TXIModel model){
		// 声明变量
		String co_id = null, matvoch = "", // 凭证号
				zmodel = "", // 模型号
				mattdxlg = "", // 大小量纲
				zczms = "", // 材质
				zczbm = "", // 材质标准
				matisdesc = "", // 工业标准
				v_cvtax1 = "", // 组织机构代码
				v_cvname = "", // 供应商名称
				co_manualcode = ""; // 编码
				
		for(Object key : source.keySet()){
			String val = source.get(key)==null?"":source.get(key).toString();
			if("MATVOCH".equals(key)){
				matvoch = val;
			}else if("ZMODEL".equals(key)){
				zmodel  = val;
			}else if("MATDXLG".equals(key)){
				mattdxlg = val;
			}else if("ZCZMS".equals(key)){
				zczms = val; 
			}else if("ZCZBM".equals(key)){
				zczbm = val; 
			}else if("MATISDESC".equals(key)){
				matisdesc = val; 
			}else if("CVTAX1".equals(key)){
				v_cvtax1 = val; 
			}else if("CVNAME".equals(key)){
				v_cvname = val; 
			}else if("CO_MANUALCODE".equals(key)){
				co_manualcode = val; 
			}else if("CO_ID".equals(key)){
				co_id = val;
			}
		}
		BoolQueryBuilder filter = null;
		if("T_MAT".equalsIgnoreCase(tableName)){
			//判断凭证号相同的数据
			filter = QueryBuilders.boolQuery();
			if(!"".equals(matvoch)){
				filter.filter(QueryBuilders.matchQuery("MATVOCH", matvoch).operator(Operator.AND))
				.filter(QueryBuilders.matchQuery("CO_VALID", "1"));
				SearchResponse responseVoch = searchBuilder
						.setTypes(tableName.toLowerCase())
						.setScroll(new TimeValue(60000))
						.setQuery(filter)
						.execute()
						.actionGet();
				//记录超过10将大小量纲的比较也加进去
				long hitnum = responseVoch.getHits().getTotalHits();
				if(hitnum>10){
					//删除相似度临时表
					deleteSimilarTemp(index,co_id, batch);
					//如果凭证号不为空且查出有重复数据就会将ORDERBY=2和当前批次号的临时表数据删除
					filter.filter(QueryBuilders.matchQuery("MATDXLG", mattdxlg).operator(Operator.AND));
					SearchResponse responseDxlg = searchBuilder.setTypes(tableName.toLowerCase())
							.setQuery(filter)
							.setSize(20)
							.execute()
							.actionGet();
					//
					if(responseDxlg.getHits().getTotalHits()>0){
						handleExistDupDatas(index,co_manualcode, co_id, tableName, batch, filter, searchBuilder, model);
					}
					//如果记录数在0到10之间
				}else if(0<hitnum&&hitnum<=10){
					deleteSimilarTemp(index,co_id, batch);
					handleExistDupDatas(index,co_manualcode, co_id, tableName, batch, filter, searchBuilder, model);
					//如果查重数据为空那么将相似度数据都删除
				}else if(hitnum==0){
					deleteSimilarTemp(index,co_id, batch);
				}
				//如果模型号不为空
			}else if(!"".equals(zmodel)){
				filter.filter(QueryBuilders.matchQuery("ZMODEL", zmodel).operator(Operator.AND))
				.filter(QueryBuilders.matchQuery("CO_VALID", "1"));
				SearchResponse responseZmodel = searchBuilder.setTypes(tableName.toLowerCase())
						.setQuery(filter)
						.setSize(20)
						.execute()
						.actionGet();
				long totalHits = responseZmodel.getHits().getTotalHits();
				//且查出有重复数据就会将ORDERBY=2和当前批次号的临时表数据删除
				if(totalHits!=0){
					deleteSimilarTemp(index,co_id, batch);
					handleExistDupDatas(index,co_manualcode, co_id, tableName, batch, filter, searchBuilder, model);
				}else{
					deleteSimilarTemp(index,co_id, batch);
				}
				//大小量纲,材质，材质标准，工业标准都不为空
			}else if("".equals(zmodel)&&"".equals(matvoch)&&!"".equals(mattdxlg)&&!"".equals(zczms)&&!"".equals(zczbm)&&!"".equals(matisdesc)){
				filter
				.mustNot(QueryBuilders.matchQuery("CO_ID", co_id))
				.filter(QueryBuilders.matchQuery("CO_VALID", "1"))
				.filter(QueryBuilders.matchQuery("MATDXLG", mattdxlg).operator(Operator.AND))
				.filter(QueryBuilders.matchQuery("ZCZMS", zczms).operator(Operator.AND))
				.filter(QueryBuilders.matchQuery("ZCZBM", zczbm).operator(Operator.AND))
				.filter(QueryBuilders.matchQuery("MATISDESC", matisdesc).operator(Operator.AND));
				System.out.println(mattdxlg);
				System.out.println(zczms);
				System.out.println(zczbm);
				System.out.println(matisdesc);
				SearchResponse otherresponse = searchBuilder
						.setTypes(tableName.toLowerCase())
						.addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC).setScroll(new TimeValue(60000))
						.setQuery(filter)
						.execute()
						.actionGet();
				long totalHits = otherresponse.getHits().getTotalHits();
				if(totalHits!=0){
					deleteSimilarTemp(index,co_id, batch);
					handleExistDupDatas(index,co_manualcode, co_id, tableName, batch, filter, searchBuilder, model);
				}else{
					deleteSimilarTemp(index,co_id, batch);
				}
			}
			
		}else if("T_VENDOR".equalsIgnoreCase(tableName)||"T_CUSTOMER".equalsIgnoreCase(tableName)){
			filter = QueryBuilders.boolQuery();
			//供应商或者客户组织机构代码
			if(!"".equals(v_cvtax1)){
				filter.filter(QueryBuilders.matchQuery("CO_VALID", "1"))
				.filter(QueryBuilders.matchQuery("CVTAX1", v_cvtax1).operator(Operator.AND));
				SearchResponse responseCvtax1 = searchBuilder.setTypes(tableName.toLowerCase())
						.setQuery(filter)
						.setSize(0)//此处如果只用来判断命中数的话可以设置为0默认是返回10条
						.execute()
						.actionGet(); 
				long totalHits = responseCvtax1.getHits().getTotalHits();
				System.out.println("查重数据个数："+String.valueOf(totalHits));
				if(totalHits!=0){
					deleteSimilarTemp(index,co_id, batch);
					handleExistDupDatas(index,co_manualcode, co_id, tableName, batch, filter, searchBuilder, model);
				}else{
					deleteSimilarTemp(index,co_id, batch);
				}
				//供应商或者客户名称	
			}else if(!"".equals(v_cvname)){
				filter.filter(QueryBuilders.matchQuery("CO_VALID","1"))
				.filter(QueryBuilders.matchQuery("CVNAME", v_cvname).operator(Operator.AND));
				SearchResponse responseVname = searchBuilder.setTypes(tableName.toLowerCase())
						.setQuery(filter)
						.setSize(0)
						.execute()
						.actionGet();
				if(responseVname.getHits().getTotalHits()!=0){
					deleteSimilarTemp(index,co_id, batch);
					handleExistDupDatas(index,co_manualcode, co_id, tableName, batch, filter, searchBuilder, model);
				}else{
					deleteSimilarTemp(index,co_id, batch);
				}
				
			}
			
		}
		return model;
	}
	/**
	 * 删除order=2以当前批次号的
	  
	 */
	public static void deleteSimilarTemp(String index, String co_id, String batch) {
		initClient();
		//builder拼接查询条件,玛德es内部同步数据需要时间的
		/**
		 * 默认情况下索引的refresh_interval为1秒,这意味着数据写1秒后就可以被搜索到,每次索引的 refresh 会产生一个新的 lucene 段,这会导致频繁的 segment merge 行为,如果你不需要这么高的搜索实时性,应该降低索引refresh 周期
		 */
//		BoolQueryBuilder filter = QueryBuilders.boolQuery();
//		filter.filter(QueryBuilders.termQuery("CO_ID_OLD", co_id)).filter(QueryBuilders.termQuery("ORDERBY", "2"))
//				.filter(QueryBuilders.matchQuery("BATCH", batch));
//		//向es发送查询请求
//		SearchResponse responseSimilar = client.prepareSearch(index).setTypes("similarity_batch_temp").setQuery(filter)
//				.get();
//		long totalHits = responseSimilar.getHits().totalHits;
		//1s也不够实时所以只能用oracle来搜索了
		String sql = "select * from similarity_batch_temp where co_id_old = "+co_id+" and orderby=2 and batch = "+batch;
		DataTable dt = DbOperation.executeDataTable(sql);
		ArrayList<Object[]> datas = dt.getDatas();
		LinkedHashMap<String, Integer> columns = dt.getColumns();
		String[] co_ids = null;
		
		if(!datas.isEmpty()){
			// 遍历删除
		    BulkRequestBuilder prepareBulk = client.prepareBulk().setRefreshPolicy("true");
		    StringBuilder sb = new StringBuilder();
			co_ids = new String[datas.size()];
			int i=0;
			for(Object[] data : datas){
				for(String col : columns.keySet()){
					if ("ID".equals(col)) {
						co_ids[i] = String.valueOf(data[columns.get(col)]);
						sb.append(co_ids[i]).append(",");
						//批量添加到操作对象中暂时不去get
						prepareBulk.add(client.prepareDelete(index.toLowerCase(), "similarity_batch_temp", co_ids[i]));
						break;
					}
				}
				i++;
			}
			//批量get
			BulkResponse bulkResponse = prepareBulk.get();
			if (bulkResponse.hasFailures()) {
				TXISystem.log.error("批量删除相似度临时表出错：", bulkResponse.buildFailureMessage());
			}
			//同时删除数据库中的相应数据(sql最后不加;)
			String sql1 = "delete similarity_batch_temp where id in (" + sb.toString().substring(0, sb.length() - 1)
					+ ")";
			int updateCount= DbOperation.executeUpdate(sql1);
			System.out.println("成功删除similarity_batch_temp表数据:"+String.valueOf(updateCount));
		}
		

	}
	//处理存在重复数据的
	public static TXIModel handleExistDupDatas(String index,String co_manualcode,String co_id,String tableName,String batch,BoolQueryBuilder filter, SearchRequestBuilder searchBuilder,TXIModel model){
		//将本身这条除去termQuery直接取匹配不分词
		filter.mustNot(QueryBuilders.termQuery("CO_ID", co_id));
		if(!"".equals(co_manualcode)){
			//如果是修改的要除去本身的这一条
			filter.mustNot(QueryBuilders.termQuery("CO_MANUALCODE", co_manualcode.toLowerCase()));
			batchInsertSimilarTemp(index,co_id, tableName, batch, filter, model);
		}else{
			batchInsertSimilarTemp(index,co_id, tableName, batch, filter, model);
		}
		return model;
	}
	
	/**
	 * 批量插入相似度临时表
	 * @param co_id
	 * @param tableName
	 * @param batch
	 * @param filter
	 * @param searchBuilder
	 * @param prepareBulk
	 * @param model
	 * @return
	 */
	public static TXIModel batchInsertSimilarTemp(String index,String co_id,String tableName,String batch,QueryBuilder filter, TXIModel model){
		initClient();
		SearchResponse responsenoCode = client.prepareSearch(index).setTypes(tableName.toLowerCase())
				.setQuery(filter)
				.execute()
				.actionGet();
		//long类型转换
		DateFormat timeParse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		//将重复数据批量插入到similarity_batch_temp表中
		if(responsenoCode.getHits().getTotalHits()>0){
			BulkRequestBuilder prepareBulk = client.prepareBulk().setRefreshPolicy("true");
			for(SearchHit hit1 :responsenoCode.getHits().getHits()){
				Map<String, Object> source2 = hit1.getSource();
				LinkedHashMap<String,Object> similarMap = new LinkedHashMap<String, Object>();
				String newid = null;
				for(String key : source2.keySet()){
					if("CO_ID".equals(key)){
						newid = (String)source2.get(key);
						break;
					}
				}
				similarMap.put("CO_ID", newid);
				similarMap.put("ID", "0");
				similarMap.put("CO_ID_OLD", co_id);
//				similarMap.put("SIMILARITY", "");
				similarMap.put("BATCH", batch);
				similarMap.put("TABLENAME", tableName);
				similarMap.put("ORDERBY", "3");
				similarMap.put("MAXSIGN", "3");
				
//				model = putdatastoDetail("detail7",Integer.valueOf(newid), timeParse, source2, model);
				DbOperation.insert("similarity_batch_temp", similarMap);
				//别忘了将source内容加上
//				prepareBulk.add(client.prepareIndex(index.toLowerCase(),"similarity_batch_temp",newid).setParent(newid).setSource(similarMap));	
				prepareBulk.add(client.prepareIndex(index.toLowerCase(),"similarity_batch_temp",newid).setSource(similarMap));	
					
			}
			BulkResponse bulkResponse = prepareBulk.get();
			if(bulkResponse.hasFailures()){
				TXISystem.log.error("批量插入相似度临时表时出错:",bulkResponse.buildFailureMessage());
			}else{
				System.out.println("有重复数据"+String.valueOf(responsenoCode.getHits().getTotalHits())+"条！");
			}
		}
		return model;
	}
	/**
	 * 同步_temp临时表
	 * @param index  ES索引
	 * @param tableName 表名
	 * @param clerkid 员工id
	 * @param importseq 批次号
	 * @throws Exception 
	 */
	public static TXIModel syncT_mat_temp(String index,String inputParams,String detailnum,String errorMsg,TXIModel model) throws Exception{
		initClient();
		Vector params = TXIUtil.parseInputParameterBySeperator(inputParams);
		
		String tableName = params.get(0) == null?"":(String)params.get(0),
			   clerkid = params.get(1) == null ? "" : (String)params.get(1),
			   importseq = params.get(2) == null ? "" : (String)params.get(2),
			   pk_col = params.get(3) == null ? "" : (String)params.get(3),
			   marksign = params.get(4) == null ? "" : (String)params.get(4);//增加灵活性
		//将小于当前批次号的数据都删除（没用）,查询的话也是从当前批次号的数据中查询
		SearchResponse searchResponse = client.prepareSearch(index.toLowerCase()).setTypes(tableName.toLowerCase()+"_temp")
				.setQuery(QueryBuilders.boolQuery().filter(QueryBuilders.matchQuery("MARKSIGN", marksign))
						.filter(QueryBuilders.matchQuery("IMPORTUSER", clerkid))
						.filter(QueryBuilders.rangeQuery("IMPORTSEQ").lt(importseq))
						.filter(QueryBuilders.matchQuery("TABLENAME", tableName)))
				.get();
		int totalHits = (int)searchResponse.getHits().getTotalHits();
		System.out.println(totalHits);
		if(totalHits>0){
			SearchHit[] hits = searchResponse.getHits().getHits();
			BulkRequestBuilder bulk = client.prepareBulk().setRefreshPolicy("true");//增加及时刷新策略
			for (SearchHit hit : hits) {
				Object obj = hit.getSource().get(pk_col);
				String co_id = obj == null?"":obj.toString(); 
				bulk.add(client.prepareDelete(index, tableName.toLowerCase()+"_temp", co_id));
			}
			BulkResponse bulkResponse = bulk.get();
			if(bulkResponse.hasFailures()){
				TXISystem.log.error("批量删除相似度临时表时出错:",bulkResponse.buildFailureMessage());
			}else{
				System.out.println("批量删除"+tableName+"_temp");
			}
			
		}
		//调用（插入非发布数据逻辑）
		model = ESDBOpreate.addDatasToES(index, detailnum, tableName+"_temp", pk_col, errorMsg, model);
		
		SearchResponse searchResponse1 = client.prepareSearch(index.toLowerCase()).setTypes(tableName.toLowerCase()+"_temp")
				.setQuery(QueryBuilders.boolQuery()
//						.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))
						.filter(QueryBuilders.matchQuery("MARKSIGN", marksign))
						.filter(QueryBuilders.matchQuery("IMPORTSEQ",importseq))
						.filter(QueryBuilders.matchQuery("TABLENAME", tableName)))
				.get();
		SearchHit[] hits1 = searchResponse1.getHits().getHits();
		int totalHits2 = (int)searchResponse1.getHits().getTotalHits();
		if(totalHits2>0){
			BulkRequestBuilder bulk1 = client.prepareBulk().setRefreshPolicy("true");
			Map source = new HashMap<>();
			source.put("IMPORTUSER", clerkid);
			source.put("IMPORT_ERROR", "0");
			for (SearchHit hit : hits1) {
				Object obj = hit.getSource().get(pk_col);
				String co_id = obj == null?"":obj.toString(); 
				bulk1.add(client.prepareUpdate(index, tableName.toLowerCase()+"_temp", co_id).setDoc(source));
			}
			BulkResponse bulkResponse1 = bulk1.get();
			if(bulkResponse1.hasFailures()){
				TXISystem.log.error("批量插入相似度临时表时出错:",bulkResponse1.buildFailureMessage());
			}else{
				System.out.println("删除"+tableName+"_temp当前批次号的数据");
			}
		}
		return model;
		
	}
	/**
	 * 根据查询条件更新数据
	 * @param index
	 * @param updateParams 更新的字段和内容
	 * @param conditions 查询
	 * @param detailnum
	 * @param errorMsg
	 * @param model
	 * @return
	 */
	public static TXIModel updatebySearch(String index,String updateParams,String conditions,String detailnum,String errorMsg,TXIModel model){
		
		return model;
	}
	/**
	 * 查询SIMILARITY_BATCH_TEMP
	 * @param tableName
	 * @param batch
	 * @param errMsg
	 * @param model
	 */
	public static TXIModel similarityBatchSearch(String index,String tableName,String batch,String errMsg,TXIModel model){
		initClient();
		long count = 0l;
		SearchResponse searchResp = client.prepareSearch(index.toLowerCase()).setTypes("similarity_batch_temp").setQuery(QueryBuilders.boolQuery()
				.filter(QueryBuilders.matchQuery("BATCH", batch)).filter(QueryBuilders.termQuery("ORDERBY", "1")))
				.get(new TimeValue(60 * 1000));
		count = searchResp.getHits().getTotalHits();
		String co_id_old = null,
			   v_clid =  null,
			   v_code = null;
		if(count>0){
			SearchHit[] hits = searchResp.getHits().getHits();
			for (SearchHit hit : hits) {
				Object obj = hit.getSource().get("CO_ID_OLD");
				co_id_old = obj == null?"":obj.toString();
				//先將需要更新的數據的主键查詢出來
				SearchResponse searchResp4 = client.prepareSearch(index).setTypes(tableName.toLowerCase())
						.setQuery(QueryBuilders.boolQuery()
								.filter(QueryBuilders.termsQuery("ORDERBY","1","2"))
								.filter(QueryBuilders.matchQuery("BATCH", batch)))
						.get(new TimeValue(60 * 1000));
				count = searchResp4.getHits().getTotalHits();
				//得到主键
				String[] ids = null;
				if(count>0){
					ids = new String[(int)count];
					SearchHit[] hits2 = searchResp4.getHits().getHits();
					for(int i=0;i<count;i++){
						Object coid = hits2[i].getSource().get("CO_ID");
						ids[i] = coid==null?"":coid.toString();
					}
				}
				
				SearchResponse searchResp1 = client.prepareSearch(index).setTypes(tableName.toLowerCase())
						.setQuery(QueryBuilders.termQuery("CO_ID", co_id_old)).get(new TimeValue(60 * 1000));
				count = searchResp1.getHits().getTotalHits();
				if(count>0){
					SearchHit[] hits2 = searchResp1.getHits().getHits();
					Map source = new HashMap<>();
					for (SearchHit hit2 : hits2) {
						Object co_mual = hit2.getSource().get("CO_MANUALCODE");
						v_code = co_mual == null?"":co_mual.toString();
						Object clid = hit2.getSource().get("CL_ID");
						v_clid = clid == null?"":clid.toString();
						//批处理对象
						BulkRequestBuilder updateBulk = client.prepareBulk().setRefreshPolicy("true");
						if(!"".equals(v_code)){
							SearchResponse searchResp2 = client.prepareSearch(index).setTypes(tableName.toLowerCase())
									.setQuery(QueryBuilders.boolQuery()
											.filter(QueryBuilders.matchQuery("CO_MANUALCODE", v_code))
											.filter(QueryBuilders.termQuery("CO_VALID", "1")))
									.get(new TimeValue(60 * 1000));
							count = searchResp2.getHits().getTotalHits();
							if(count==0){
								source.clear();
								source.put("MAXSIGN", "1");
								for(String id :ids){
									updateBulk.add(client.prepareUpdate(index, tableName.toLowerCase(), id).setDoc(source));
								}
								updateBulk.get();
							}else{
								Object coid = searchResp2.getHits().getHits()[0].getSource().get("CO_ID");
								if(coid!=null){
									client.prepareDelete(index, tableName.toLowerCase(), coid.toString()).get();
								}
								source.clear();
								source.put("MAXSIGN", "1");
								for(String id :ids){
									updateBulk.add(client.prepareUpdate(index, tableName.toLowerCase(), id).setDoc(source));
								}
								updateBulk.get();
							}
						}else{
							source.clear();
							source.put("MAXSIGN", "1");
							for(String id :ids){
								updateBulk.add(client.prepareUpdate(index, tableName.toLowerCase(), id).setDoc(source));
							}
							updateBulk.get();
						}
						
					}
				}
			}
		}
		
		return model;
		
	}
	/**
	 * 还差一个父子join查询将插入到similarity_batch_temp表中数据
	 * 建立父子查询将similarity_batch_temp设置为父文档，其他的主表如：t_mat
	 * 设置成子文档，如果similarity_batch_temp中没有co_id的数据而t_mat中有
	 * co_id对应的数据，那么初始化数据的时候把将similarity_batch_temp中的co_id
	 * 都查出来然后循环co_id作为t_mat的parent_id，没有的co_id，t_mat就没有parent_id
	 * 
	 * @param tableName 表名
	 * @param batch 批次号
	 * @param model 用户数据
	 * @return
	 */
	 
	public static TXIModel searchSimilarDatas(String tableName,String batch,String errorMsg,TXIModel model){
		initClient();
		//第一种情况拼接
		BoolQueryBuilder boolQuery1 = QueryBuilders.boolQuery();
		//在ES中添加数据的时候如果某一个字段数据为空的话那么mapping中就没有这一个字段
		boolQuery1.filter(QueryBuilders.matchQuery("BATCH", batch))
			.filter(QueryBuilders.matchQuery("TABLENAME", tableName.toLowerCase()))
			.should(QueryBuilders.matchQuery("ORDERBY", "1"))
			.should(QueryBuilders.matchQuery("MAXSIGN", "3"));
			
		//子文档的搜索条件
//		MatchAllQueryBuilder childSearchCon = QueryBuilders.matchAllQuery();
		//ScoreMode.None
		HasChildQueryBuilder innerHit = JoinQueryBuilders.hasChildQuery(
				"similarity_batch_temp",//子文档的类型
				boolQuery1, //子文档的搜索条件
				ScoreMode.None) //不进行评分
			.innerHit(new InnerHitBuilder());//只用于嵌套查询有内部join关系的以便能把父子文档都能查询出来
		
//		boolQuery1.filter(innerHit);
		model = insertDetail7(tableName,innerHit, model);
		//第二种情况拼接
		BoolQueryBuilder boolQuery2_child = QueryBuilders.boolQuery();
		//去重查询
//		 AggregationBuilder aggregation =  
//	                AggregationBuilders  
//	                        .terms("agg").field("CO_ID_OLD")  
//	                        .subAggregation(  
//	                                AggregationBuilders.topHits("top").size(1)  
//	                        );  
		boolQuery2_child.filter(QueryBuilders.matchQuery("BATCH", batch))
			.filter(QueryBuilders.matchQuery("MAXSIGN", "3"));
		SearchResponse chil_res = client.prepareSearch("mdmindex")
			.setTypes("similarity_batch_temp")
			.setQuery(boolQuery2_child)
//			.addAggregation(aggregation)
			.get();
		long count = chil_res.getHits().totalHits;
		String[] co_id_olds = new String[(int) count];
//		Terms agg = chil_res.getAggregations().get("agg");
		int num = 0;
		for(SearchHit hit : chil_res.getHits().getHits()){
			Map<String, Object> sourceAsMap = hit.getSourceAsMap();
			for(String key : sourceAsMap.keySet()){
				if("CO_ID_OLD".equals(key)){
					co_id_olds[num] = sourceAsMap.get(key)==null?"":sourceAsMap.get(key).toString();
				}
			}
			num ++;
		}
		System.out.println(co_id_olds);
		
		BoolQueryBuilder boolQuery2 = QueryBuilders.boolQuery();
		//在ES中添加数据的时候如果某一个字段数据为空的话那么mapping中就没有这一个字段
		boolQuery2.filter(QueryBuilders.matchQuery("BATCH", batch))
			.filter(QueryBuilders.matchPhraseQuery("TABLENAME", tableName))
			.mustNot(QueryBuilders.termQuery("ID", "0"))
			.mustNot(QueryBuilders.termsQuery("CO_ID_OLD", co_id_olds))//terms可以添加多个值
			.should(QueryBuilders.termsQuery("MAXSIGN", "1","2"))
			.should(QueryBuilders.termQuery("ORDERBY", "1"));
		
		HasChildQueryBuilder innerHit1 = JoinQueryBuilders.hasChildQuery(
				"similarity_batch_temp",//子文档的类型
				boolQuery2, //子文档的搜索条件
				ScoreMode.None) //不进行评分
			.innerHit(new InnerHitBuilder());
		model = insertDetail7(tableName,innerHit1, model);
				
		
		return model;
	}
	public static TXIModel insertDetail7(String tablename, QueryBuilder filter,TXIModel model){
		initClient();
//		client = TXIMasterDataSearch.getClient();
		SearchResponse searchResponse = client.prepareSearch("mdmindex")
				.setTypes(tablename.toLowerCase())
				.addSort(FieldSortBuilder.DOC_FIELD_NAME,SortOrder.ASC)
//				.addSort("CO_ID_OLD", SortOrder.DESC)
//				.addSort("ORDERBY", SortOrder.DESC)
//				.addSort("SIMILARITY", SortOrder.DESC)
//				.setScroll(new TimeValue(60*1000))//这里不需要分页查询
				.setQuery(filter)
				.get();
		
		long totalHits = searchResponse.getHits().totalHits;
		int num = 0;
		//对co_createtime进行long转date
		DateFormat timeParse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		for(SearchHit hit : searchResponse.getHits()){
			//先得到父文档的记录
			Map<String, Object> parent = hit.getSourceAsMap();
			model = putdatastoDetail("detail0",num, timeParse, parent, model);
			//得到子文档的记录
			Map<String, SearchHits> innerHits = hit.getInnerHits();
			for(String key : innerHits.keySet()){
				Iterator<SearchHit> iterator = innerHits.get(key).iterator();
				while(iterator.hasNext()){
					SearchHit next = iterator.next();
					System.out.println(next.getSourceAsString());
					
					Map<String, Object> child = next.getSourceAsMap();
					model = putdatastoDetail("detail0",num, timeParse, child, model);
				}
			}
		}
		return model;
	}
	
	public static TXIModel putdatastoDetail(String detailnum,int num,DateFormat timeParse,Map<String, Object> map ,TXIModel model){
		TXIModel submodel = new TXIModel(model);
		// 由于hashtable中的键值均不能为null所以放弃用putAll()了
		Hashtable<String, Object> datas = new Hashtable<String, Object>();
		for (String key1 : map.keySet()) {
			Object obj = map.get(key1);
			String s_col = key1 == null ? "" : key1;
			Object s_val=null;
			//ES中存储的创建时间字段为long类型所以展示的时候转化成String
			if("CO_CREATETIME".equals(key1)){
				System.out.println(obj);
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
		submodel = TXIMasterDataSearcher.getARdFromHashES(datas, submodel);
		model.addDetail(detailnum, String.valueOf(num), submodel);
		return model;
	}
}
