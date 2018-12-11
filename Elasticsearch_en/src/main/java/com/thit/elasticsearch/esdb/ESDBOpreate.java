package com.thit.elasticsearch.esdb;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.thit.elasticsearch.TXIMasterDataSearcher;
import com.thit.elasticsearch.common.ESUtil;
import com.xicrm.common.TXISystem;
import com.xicrm.model.TXIModel;
import com.xicrm.util.TXIUtil;

public class ESDBOpreate {
	private static Client client;
	/**
	 * 同步数据到ES中(非发布数据)
	 * @param index 索引
	 * @param detailnum 细表名
	 * @param tableName 表名
	 * @param pk_col 主键（如果表中有主键的话那就以主键的字段名否则为""）
	 * @param errorMsg 
	 * @param model 用户数据含细表
	 * @throws Exception
	 */
	public static void  initClient(){
		if(client == null){
			client = ESUtil.getClient();
		}
	}
	public static TXIModel addDatasToES(String index,String detailnum,String tableName,String pk_col,String errorMsg,TXIModel model) throws Exception{
		initClient();
		//从model中得到发布信息
		Hashtable detail = (Hashtable)model.getDetails().get(detailnum);
		Enumeration  keys = detail.keys();
		
		String key=null;
		TXIModel submodel = null;
		IndexResponse indexResponse = null;
		//批量操作处理对象
		BulkRequestBuilder prepareBulk = client.prepareBulk().setRefreshPolicy("true");
		// 细表中数据处理
		while (keys.hasMoreElements()) {
			key = (String) keys.nextElement();
			submodel = (TXIModel) detail.get(key);
			Hashtable htDetail = submodel.getHtValues();// 从细表中得到详细数据java.util.Hashtable$Enumerator@36fd78c9
			Set<String> keySet = htDetail.keySet();
			String co_id = null,co_manualcode = null,co_valid = null,_lang = null;
			Iterator<String> iterator = keySet.iterator();
			
			while(iterator.hasNext()) {
				String col = iterator.next();
				Object val = htDetail.get(col);
				// 将创建时间转化成long类型以便时间范围查询
				if ("CO_CREATETIME".equalsIgnoreCase(col)) {
					if (val != null) {
						htDetail.put(col, ESUtil.FormatTime(val.toString()));
					}
				} else if (pk_col.equalsIgnoreCase(col)) {// 如果主键存在且与表中
					co_id = val == null ? "" : (String) val;
				}else if("CO_ATTRLANGUAGE".equals(col)){
					_lang = val == null?"":(String)val;
				}
			}
			System.out.println("主键="+co_id);
			//将所有需要发布的数据放入到BulkRequestBuilder中先不插入ES
			if (co_id != null) {//有主键的表
				prepareBulk.add(
						client.prepareIndex(index.toLowerCase(), tableName.toLowerCase(), co_id).setSource(htDetail));
			}else{//无主键的表
				prepareBulk.add(client.prepareIndex(index.toLowerCase(), tableName.toLowerCase()).setSource(htDetail));
			}
		}
		if(!detail.isEmpty()){
			BulkResponse bulkResponse = prepareBulk.get();
			if(bulkResponse.hasFailures()){
				TXISystem.log.info(tableName+"插入表数据错误详情:", bulkResponse.buildFailureMessage());
			}else{
				TXISystem.log.info(tableName+"添加数据：", "ES中添加数据成功!");
				System.out.println(tableName+"添加数据成功！");
			}
		}
		//批量操作插入
		return model;
	}
	/**
	 * 批量和单条发布数据插入到ES
	 * @param tableName 表名
	 * @param errorMsg 
	 * @param model 用户数据
	 * @return
	 * @throws Exception 
	 */
	public static TXIModel addPublishDatastoES(String index,String lang,String detailnum,String tableName,String errorMsg,TXIModel model) throws Exception{
		System.out.println("索引名称="+index);
		System.out.println("细表名称="+detailnum);
		System.out.println("表名="+tableName);
		//ES连接
		initClient();
		//从model中得到发布信息
		Hashtable detail = (Hashtable)model.getDetails().get(detailnum);
		Enumeration  keys = detail.keys();
		
		String key=null;
		TXIModel submodel = null;
		IndexResponse indexResponse = null;
		//批量操作处理对象
		BulkRequestBuilder prepareBulk = client.prepareBulk()
				.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);//设置立即刷新机制
		//细表中数据处理
		while(keys.hasMoreElements()){
			key = (String)keys.nextElement();                  
			submodel = (TXIModel)detail.get(key);
			Hashtable htDetail = submodel.getHtValues();//从细表中得到详细数据
			Set<String> keySet = htDetail.keySet();
			String co_id = null;
			String co_manualcode = null;
			String co_valid = null;
			String co_freeze = null;
			String _lang = null;
			for(String col : keySet){
				Object val = htDetail.get(col);
				//将创建时间转化成long类型以便时间范围查询
				if("CO_CREATETIME".equals(col)){
					if(val!=null){
						htDetail.put(col, ESUtil.FormatTime(val.toString()));
					}
				}else if("CO_ID".equals(col)){
					co_id = val==null?"":(String)val;
				}else if("CO_MANUALCODE".equals(col)){
					co_manualcode = val==null?"":(String)val;	
				}else if("CO_VALID".equals(col)){
					co_valid = val==null?"":(String)val;
					System.out.println("最新数据发布的数据状态="+co_valid);
				}else if("CO_FREEZE".equals(col)){
					co_freeze = val == null?"":(String)val;
				}else if("CO_ATTRLANGUAGE".equals(col)){
					_lang = val == null?"":(String)val;
				}
			}
			//查询发布状态的相同编码的数据然后将其状态改成9(历史数据)
			if(!"".equals(co_manualcode)&&co_manualcode!=null){
				BoolQueryBuilder filter = QueryBuilders.boolQuery()
						.filter(QueryBuilders.matchQuery("CO_MANUALCODE", co_manualcode))
						.filter(QueryBuilders.matchQuery("CO_VALID", "1"));
					SearchResponse publisdRes = client.prepareSearch(index.toLowerCase())
						.setTypes(tableName.toLowerCase())
						.setTimeout(new TimeValue(60*1000))
						.setQuery(filter)
						.get();
					long totalHits = publisdRes.getHits().getTotalHits();
					if(totalHits>0){
						Iterator<SearchHit> iterator = publisdRes.getHits().iterator();
						while(iterator.hasNext()){
							String coid = null;
							SearchHit next = iterator.next();
							Map<String, Object> source = next.getSource();
							for(String field : source.keySet()){
								if("CO_VALID".equals(field)){
									System.out.println("之前数据发布的co_valid的值="+(String)source.get(field));
									source.put(field, "9");
								}else if("CO_ID".equals(field)){
									coid = (String)source.get(field);
								}
							}
							if(_lang==null||"".equals(_lang)){
								source.put("CO_ATTRLANGUAGE", lang);
							}
							//切记一定要指定索引的_id否则会另外生成一条_id为一长串字符的id
							IndexResponse indexResponse2 = client.prepareIndex(index.toLowerCase(), tableName.toLowerCase(),coid)
								.setSource(source)
								.get();
							Result result = indexResponse2.getResult();
							System.out.println(tableName+"的"+coid+"更新结果为"+result.getLowercase());
						}
					}
			}
			//修改发布的时候改成解冻状态
			if("2".equals(co_freeze)){
				htDetail.put("CO_FREEZE", "0");
			}
			//将所有需要发布的数据放入到BulkRequestBuilder中先不插入ES
			if(!"1".equals(co_valid)){
				htDetail.put("CO_VALID", "1");
			}
			if(_lang==null||"".equals(_lang)){
				htDetail.put("CO_ATTRLANGUAGE", lang);
			}
			//将每一条即将发布的数据放到bulk
			if(co_id!=null){
				prepareBulk.add(client.prepareIndex(index.toLowerCase(),tableName.toLowerCase(),co_id)
						.setSource(htDetail));
			}
		}
		//批量插入ES
		BulkResponse bulkResponse = prepareBulk.get();
		//插入时的异常数据
		if(bulkResponse.hasFailures()){
			TXISystem.log.error("错误数据详细信息：", bulkResponse.buildFailureMessage());
//			bulkResponse.buildFailureMessage();
		}else{
			TXISystem.log.info(tableName+"添加数据：", "ES中添加数据成功！");
		}
		return model;
	}
	/**
	 *从es中删除数据
	 * @param index 索引名
	 * @param tableName 表名
	 * @param ids 主键拼接
	 */
	public static void deleteDatasfromES(String index,String tableName,String _ids,String errMsg,TXIModel model){
		initClient();
		BulkRequestBuilder deleteBulk = client.prepareBulk().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//				.setRefreshPolicy("true");//都可以
		String[] co_ids = _ids.split(",");
		for(String co_id : co_ids){
			deleteBulk.add(client.prepareDelete(index, tableName.toLowerCase(), co_id));
		}
		BulkResponse bulkResponse = deleteBulk.get(new TimeValue(60*1000));
		if(bulkResponse.hasFailures()){
			TXISystem.log.error("错误数据详细信息：", bulkResponse.buildFailureMessage());
		}else{
			TXISystem.log.info(tableName+"删除：", "成功删除！");
		}
	}
	/**
	 * 更新es数据
	 * @param index 索引
	 * @param inputParams 更新需要的输入参数
	 * @param updateContent 更新内容
	 * @param errMsg 出错信息
	 * @param model  用户数据
	 */
	public static void updateESDatas(String index,String inputParams,String updateContent,String errMsg,TXIModel model){
		initClient();
		//分解输入参数
		Vector inputparams = TXIUtil.parseInputParameterBySeperator(inputParams);
		String tableName = inputparams.get(0)==null?"":inputparams.get(0).toString();
		String ids = inputparams.get(1)==null?"":inputparams.get(1).toString();
		//首先要明确更新内容不为空
		if(!"".equals(updateContent)&&updateContent!=null){
			//将需要更新的内容加入到集合
			Map source = new HashMap<>();
			String[] content = updateContent.split(";");
			for(String key_val : content){
				String[] split = key_val.split("=");
				source.put(split[0], split[1]);
			}
			//循环主键将每一条都加入到批量更新的请求中去
			if(!"".equals(ids)){
				BulkRequestBuilder updatebulk = client.prepareBulk().setRefreshPolicy("true");
				String[] co_ids = ids.split(",");
				for(String co_id :co_ids){
					updatebulk.add(client.prepareUpdate(index, tableName.toLowerCase(), co_id).setDoc(source));
				}
				BulkResponse updateResp = updatebulk.get(new TimeValue(60*1000));
				//异常处理
				if(updateResp.hasFailures()){
					TXISystem.log.error("批量更新出错信息：", updateResp.buildFailureMessage());
				}else{
					TXISystem.log.info(tableName+"elasticsearch更新结果：", "更新成功!");
				}
			}
		}
	}
   //增加一个根据查询条件跟新的方法
	/**
	 * @param index 索引名
	 * @param tableN 表名
	 * @param updateParams 更新内容
	 * @param conditions 查询内容 SYS='3000MDM001;A4,3000ECC001;A4';COUNT ='AD,安道尔'
	 * @param detailnum 如果用
	 * @param errorMsg 
	 * @param model
	 * @return
	 */
	/*public static TXIModel updatebySearch(String index,String tableN, String pk,String updateParams,String conditions,String detailnum,String errorMsg,TXIModel model){
		//更新的内容
		BoolQueryBuilder filter = new BoolQueryBuilder();
		TXIMasterDataSearcher.getSearchConditionFilter("=", conditions, filter);
		SearchResponse searchResponse1 = client.prepareSearch(index.toLowerCase()).setTypes(tableN.toLowerCase()+"_temp")
				.setQuery(QueryBuilders.boolQuery()
//						.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))
						.filter(QueryBuilders.matchQuery("MARKSIGN", "9"))
						.filter(QueryBuilders.matchQuery("IMPORTSEQ",importseq))
						.filter(QueryBuilders.matchQuery("TABLENAME", tableName)))
				.get();
		SearchHit[] hits1 = searchResponse1.getHits().getHits();
		int totalHits2 = (int)searchResponse1.getHits().getTotalHits();
		String co_id = null;
		if(totalHits2>0){
			BulkRequestBuilder bulk1 = client.prepareBulk();
			Map source = new HashMap<>();
			source.put("IMPORTUSER", clerkid);
			source.put("IMPORT_ERROR", "0");
			for (SearchHit hit : hits1) {
				//如果表中无主键则
				if(pk==null||"".equals(pk)){
					co_id = hit.getId();
				}else{  //如果表中有主键
					Object obj = hit.getSource().get(pk);
					co_id = obj == null?"":obj.toString(); 
				}
				bulk1.add(client.prepareUpdate(index, tableN.toLowerCase(), co_id).setDoc(source));
			}
			BulkResponse bulkResponse1 = bulk1.get();
			if(bulkResponse1.hasFailures()){
				TXISystem.log.error("批量插入相似度临时表时出错:",bulkResponse1.buildFailureMessage());
			}else{
				System.out.println("删除"+tableN);
			}
		}
		return model;
	}*/
	
   
}
