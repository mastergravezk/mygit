package com.thit.elasticsearch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.HasChildQueryBuilder;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.thit.elasticsearch.common.ESUtil;
import com.thit.elasticsearch.orcldb.DbOperation;
import com.xicrm.model.TXIModel;

import ch.qos.logback.core.util.TimeUtil;
/**
 * 
 * @author zk
 * @since jdk 1.8
 */
public class TXIAsyncImportChecker {
	private static Client client;
	
	public TXIAsyncImportChecker(){
		
	}					   				
	public static TXIModel asyncDuplicateCheck(String index, String co_ids, String importseq,
			String errorMesg ,TXIModel model) throws Exception {
		System.out.println("异步查重开始......");
		
		long start =TimeUtil.computeStartOfNextSecond(System.currentTimeMillis());
		System.out.println("开始时间："+String.valueOf(start));
		System.out.println("索引="+index);
		System.out.println("co_ids="+co_ids);
		System.out.println("importseq="+importseq);
		if(client == null){
			client = ESUtil.getClient();
		}
		SearchRequestBuilder prepareSearch = client.prepareSearch(index);
		//聚合查询
		String v_matgrup = null,
			   v_coids = null,
		       v_tabName = null,
			   v_clid = null,
			   v_coid = null,
			   v_ca_id = null,
			   v_value = null,
			   v_codeid = null,
			   v_matdxlg = null,
			   v_clerkid = null;
		int count = 0;
		/**聚合查询代替以下sql语句
		 * SELECT MT.MATGRUP,  WM_CONCAT(MT.CO_ID) CO_ID, MT.TABLENAME, MT.CL_ID,IMPORTUSER
		 * FROM T_MAT_TEMPYB MT
		 * WHERE MT.IMPORT_ERROR = ''0''
		 * AND MT.CO_ID IN ('||CO_IDS||')
		 * AND MT.PROCESS_BATCH = '||IIMPORTSEQ||'
		 * AND MT.MARKSIGN = ''1''
		 * GROUP BY MATGRUP, TABLENAME,CL_ID,IMPORTUSER';
		 */
		
		String[] coids = null;
		if (!co_ids.trim().equals("")) {
			coids = co_ids.split(",");
		} else {
			throw new Exception("no data import!");
		}
		//更新标记为处理中
		Map<String,Object> source = new HashMap<String,Object>();
		source.clear();
		source.put("PROCESS_STATE", "1");
		BulkRequestBuilder bulk = client.prepareBulk();
		for(String coid : coids){
			bulk.add(client.prepareUpdate(index, "t_mat_tempyb", coid).setDoc(source));
		}
		bulk.get();
		//coids拼接查询条件
		IdsQueryBuilder idbuilders = new IdsQueryBuilder();
		idbuilders.addIds(coids);
		//校验物料组是否存在
		SearchResponse searchResp = prepareSearch.setTypes("t_mat_tempyb").setQuery(idbuilders).get();
		SearchHit[] hits1 = searchResp.getHits().getHits();
		for (SearchHit hit1 : hits1) {
			Object id = hit1.getSource().get("CO_ID");
			v_coid = id == null?"":(String)id;
			Object matgroup = hit1.getSource().get("MATGRUP");
			if(matgroup!=null){
				SearchResponse searchResp1 = prepareSearch.setTypes("cl_catalog").setQuery(QueryBuilders.boolQuery()
						.filter(QueryBuilders.matchQuery("CL_CODE", (String)matgroup))
						.filter(QueryBuilders.matchQuery("CL_HASCHILD","0"))).get();
				count = (int)searchResp1.getHits().getTotalHits();
				if(count==0){
					updateErrorMesg(source, "", index, "t_mat", "物料组没有对应的分类", v_coid);
					System.out.println("t_mat_tempyb------"+v_coid+":物料组没有对应的分类");
				}else{
					Object clid = searchResp1.getHits().getHits()[0].getSource().get("CL_ID");
					v_clid = clid == null?"":(String)clid;
					source.clear();
					source.put("MATGRUP", (String)v_matgrup);
					source.put("CL_ID", v_clid);
					client.prepareUpdate(index, "t_mat_tempyb",v_coid).setDoc(source).get();
				}
			}else{
				updateErrorMesg(source, "", index, "t_mat", "物料组不能为空", v_coid);
				System.out.println("t_mat_tempyb------"+v_coid+":物料组不能为空");
			}
			
		}
		//自查重
		SearchResponse searchResp2 = prepareSearch.setTypes("t_mat_tempyb").setQuery(QueryBuilders.boolQuery()
				.filter(idbuilders)
				.mustNot(QueryBuilders.matchQuery("IMPORT_ERROR", "1"))).get();
		SearchHit[] hits2 = searchResp2.getHits().getHits();
		for (SearchHit hit2 : hits2) {
			Object coid = hit2.getSource().get("CO_ID");
			v_coid = coid == null ?"":(String)coid;
			Object clid = hit2.getSource().get("CL_ID");
			v_clid = clid == null ?"":(String)clid;
			Object matdxlg = hit2.getSource().get("MATDXLG");
			v_matdxlg = matdxlg == null ?"":(String)matdxlg;
			//判断大小量纲没有特殊字符
			if(v_matdxlg.indexOf("\'")>-1){
				updateErrorMesg(source, "", index, "t_mat", "大小量纲含有特殊字符", v_coid);
				System.out.println("t_mat_tempyb------"+v_coid+":大小量纲含有特殊字符");
			}
			SearchResponse searchResp3 = prepareSearch.setTypes("search_duplicates").setQuery(QueryBuilders.boolQuery()
					.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
					.mustNot(QueryBuilders.matchQuery("CA_ID", ""))).get();
			count = (int)searchResp3.getHits().getTotalHits();
			if(count>0){
				SearchHit[] hits4 = searchResp3.getHits().getHits();
				source.clear();
				for (SearchHit hit4 : hits4) {
					Object caid = hit4.getSource().get("CA_ID");
					v_ca_id = caid == null?"":(String)caid;
					Object codeid = hit4.getSource().get("CA_CODEID");
					v_codeid = codeid == null?"":(String)codeid;
					Object value = hit2.getSource().get(v_codeid);
					v_value = value == null ?"":(String)value;
					source.put(v_codeid, v_value);
				}
				if(!source.isEmpty()){
					BoolQueryBuilder filter = QueryBuilders.boolQuery();
					for(String key : source.keySet()){
						filter.filter(QueryBuilders.matchQuery(key, source.get(key)));
					}
					filter.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))
					.filter(idbuilders);
					SearchResponse searchResp6 = prepareSearch.setTypes("t_mat_tempyb").setQuery(filter).get();
					count = (int)searchResp6.getHits().getTotalHits();
					if(count>1){
						updateErrorMesg(source, "", index, "t_mat", "该记录在EXCEL中自身重复", v_coid);
						System.out.println("t_mat_tempyb------"+v_coid+":该记录在EXCEL中自身重复");
					}
				}
			}
		}
		//构建聚合查询的条件
		TermsAggregationBuilder field1 = AggregationBuilders.terms("MATGRUP").field("MATGRUP.keyword");
		TermsAggregationBuilder field2 = AggregationBuilders.terms("TABLENAME").field("TABLENAME.keyword");
		TermsAggregationBuilder field3 = AggregationBuilders.terms("CL_ID").field("CL_ID.keyword");
		TermsAggregationBuilder field4 = AggregationBuilders.terms("IMPORTUSER").field("IMPORTUSER.keyword");
		field1.subAggregation(field2).subAggregation(field3).subAggregation(field4);
		//先过滤后聚合 
		SearchResponse searchResponse = client.prepareSearch(index).setTypes("t_mat_tempyb")
			.setQuery(QueryBuilders.boolQuery()
					.filter(QueryBuilders.matchQuery("IMPORT_ERROR", "0"))
					.filter(idbuilders)
					.filter(QueryBuilders.matchQuery("PROCESS_BATCH", importseq))
					.filter(QueryBuilders.matchQuery("MARKSIGN", "1")))
			.addAggregation(field1).get();
		Terms terms  = searchResponse.getAggregations().get("MATGRUP");
		//聚合结果分析
		for(Terms.Bucket bucket : terms.getBuckets()){
			v_matgrup = bucket.getKeyAsString();
			Terms terms1 = bucket.getAggregations().get("TABLENAME");
			for(Terms.Bucket bucket1 : terms1.getBuckets()){
				v_tabName = bucket1.getKeyAsString();
				Terms terms2 = bucket1.getAggregations().get("CL_ID");
				for(Terms.Bucket bucket2 : terms2.getBuckets()) {
					v_clid = bucket2.getKeyAsString();
					Terms terms3 = bucket2.getAggregations().get("IMPORTUSER");
					for(Terms.Bucket bucket3 : terms3.getBuckets()){
						v_clerkid = bucket3.getKeyAsString();
						SearchResponse searchResponse2 = prepareSearch.setTypes("t_mat_tempyb")
								.setPostFilter(
										QueryBuilders.boolQuery().filter(QueryBuilders.matchQuery("MATGRUP", v_matgrup))
												.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))
												.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
												.filter(QueryBuilders.matchQuery("IMPORTUSER", v_clerkid)))
								.get();
						SearchHit[] hits = searchResponse2.getHits().getHits();
						count = (int)searchResponse2.getHits().getTotalHits();
						String[] ids = new String[count];
						for(int i=0 ; i<count ;i++){
							ids[i] = hits[i].getSourceAsMap().get("CO_ID")==null?"":(String)hits[i].getSourceAsMap().get("CO_ID");
						}
						//调用导入物料校验查重的方法
						matAsynImportChecker(index,importseq,v_matgrup,v_tabName,v_clid,v_clerkid,ids,prepareSearch,model);
					}
				}
			}
		}
		System.out.println("异步查重结束......");
		long end = TimeUtil.computeStartOfNextSecond(System.currentTimeMillis());
		System.out.println("结束时间时间："+String.valueOf(end));
		System.out.println("异步查重执行时间时间："+String.valueOf(end-start));
		return model;
	}
	/**
	 * 
	 * @param v_matgrup 物料组
	 * @param v_tabName 表名t_mat
	 * @param v_clid 分类id
	 * @param v_clerkid 员工id
	 * @param ids 物料表的co_id
	 * @param prepareSearch 预查询对象
	 * @param model 用户数据
	 * @throws Exception 
	 */
	private static TXIModel matAsynImportChecker(String index,String importseq,String v_matgrup, String v_tabName, String v_clid, String v_clerkid,
			String[] ids,SearchRequestBuilder prepareSearch, TXIModel model) throws Exception {
		/*************************获取用户编码和部门编码****************************/
		String iclerkcode = null, //人员编码
				v_bmdj = null,    //部门等级
				izz = null,       //部门编码
				v_sjbmid = null;  //上级部门id
		
		if(client == null){
			client = ESUtil.getClient();
		}
		//給	iclerkcode賦值 sql=SELECT CLERK_CODE FROM CLERK WHERE ROWNUM=1 AND CLERKID='||CLERKID;	
		SearchResponse searchResp = prepareSearch.setTypes("clerk").setQuery(QueryBuilders.matchQuery("CLERKID", v_clerkid)).get();
		Object obj = searchResp.getHits().getHits()[0].getSourceAsMap().get("CLERK_CODE");
		iclerkcode = obj==null?"":(String)obj;
		//SELECT count(BMDJ) INTO v_num1 FROM TBMB WHERE BMID IN ( SELECT  DISTINCT DEP_ID FROM USER_DEP_MAPPING WHERE USER_ID=CLERKID)
		SearchResponse searchResp2 = prepareSearch.setTypes("user_dep_mapping").setQuery(QueryBuilders.matchQuery("USER_ID", v_clerkid)).addAggregation(AggregationBuilders.terms("DEP_ID").field("DEP_ID")).get();
		Terms terms = searchResp2.getAggregations().get("DEP_ID");
		//从装有DEP_ID值的桶里取出来放到字符串数组中
		List<? extends Bucket> buckets = terms.getBuckets();
		String[] dep_ids = new String[buckets.size()]; 
		for(int i=0;i<buckets.size();i++){
			dep_ids[i] = buckets.get(i).getKeyAsString();
		}
		
		SearchResponse searchResp3 = prepareSearch.setTypes("tbmb").setQuery(QueryBuilders.boolQuery().filter(QueryBuilders.termsQuery("BMID", dep_ids)).mustNot(QueryBuilders.matchQuery("BMDJ", ""))).get();
		long count = searchResp3.getHits().getTotalHits();
		if(count==1){
			Object bmdj = searchResp3.getHits().getHits()[0].getSourceAsMap().get("BMDJ");
			v_bmdj = bmdj==null?"":(String)bmdj;
			if("3".equals(v_bmdj)||"4".equals(v_bmdj)){
			//3和4为部门等级其上一级就是2所以不用join
			// SELECT BMBM INTO IZZ FROM (SELECT * FROM TBMB START WITH BMID IN( SELECT  DISTINCT DEP_ID FROM USER_DEP_MAPPING WHERE USER_ID=CLERKID)  CONNECT BY PRIOR SJBMID = BMID)  TMP WHERE TMP.BMDJ=2;  
			//可参考的sql语句SELECT * FROM TBMB START WITH BMID IN (257) connect by prior  sjbmid = bmid	
				SearchResponse searchResp4 = prepareSearch.setTypes("tbmb").setQuery(QueryBuilders.termQuery("BMID",dep_ids[0])).get();
				Object sjobj = searchResp4.getHits().getHits()[0].getSourceAsMap().get("SJBMID");
				v_sjbmid = sjobj == null?"":(String)sjobj;
				
				SearchResponse searchResp5 = prepareSearch.setTypes("tbmb").setQuery(QueryBuilders.termQuery("BMID", v_sjbmid)).get();
				Object bmbmobj = searchResp5.getHits().getHits()[0].getSourceAsMap().get("BMBM");
				izz = bmbmobj == null?"":(String)bmbmobj;
			}else{
				Object bmbmojb = searchResp3.getHits().getHits()[0].getSourceAsMap().get("BMBM");
				izz = bmbmojb == null?"":(String)bmbmojb;
			}
		
		/*************************属性拆分大小量纲****************************/
		//SELECT COUNT(1) INTO V_COUNT1 FROM CL_ATTRASSEMBLE_MAPPING CL WHERE CL.ASSEMBLE_TARGETID='MATDXLG' AND CL_ID=ICLID
		String v_assemble_symble = null,  //属性组合之间的分隔符
				v_assemble_attrid = null, //属性组合id
				v_matdxlg = null,         //大小量纲
				v_model = null,        	//截取大小量纲的最后一位
//				v_pid = null,
//				v_pjsx = null,
//				v_pjsxz = null,
				v_dxlg = null;            //大小量纲(中间变量)
		int v_db1 = 0;
		int v_db2 = 0;
		BulkRequestBuilder bulkinsert = null;
		SearchResponse searchResp6 = prepareSearch.setTypes("cl_attrassemble_mapping")
				.setPostFilter(
						QueryBuilders.boolQuery().filter(QueryBuilders.matchQuery("ASSEMBLE_TARGETID", "MATDXLG"))
								.filter(QueryBuilders.termQuery("CL_ID", v_clid)))
				.get();
		long count1 = searchResp6.getHits().getTotalHits();
		if(count1>0){
			Object symbleobj = searchResp6.getHits().getHits()[0].getSource().get("ASSEMBLE_SYMBEL");
			v_assemble_symble = symbleobj == null?"":(String)symbleobj;
			Object attridobj = searchResp6.getHits().getHits()[0].getSource().get("ASSEMBLE_ATTRID");
			v_assemble_attrid = attridobj == null?"":(String)attridobj;
			if(!"".equals(v_assemble_attrid)){
				String[] columns =  v_assemble_attrid.split(",");
				v_db1 = columns.length;
				Map source = new HashMap<>();
				bulkinsert = client.prepareBulk();
				for(int i=0;i<v_db1;i++){
					source.clear();
					source.put("PC", importseq);//导入序列号
					source.put("CLERKCODE", iclerkcode);//员工编码
					source.put("CL_ID", v_clid);//分类id
					source.put("ID", String.valueOf(i));
					source.put("PJSX", columns[i]);//ASSEMBLE_ATTRID（字段名以逗号分割开）拼接属性
					bulkinsert.add(client.prepareIndex(index,"pjsxb", String.valueOf(i)).setSource(source));
				}
				bulkinsert.get();
			}
			//拆分循环
			IdsQueryBuilder idsterms = new IdsQueryBuilder();
			idsterms.addIds(ids);
			SearchResponse searchResp7 = prepareSearch.setTypes(v_tabName.toLowerCase()).setQuery(QueryBuilders.boolQuery()
					.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
					.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))
					.filter(QueryBuilders.matchQuery("PROCESS_BATCH", importseq))
					.filter(idsterms)).get();
			SearchHit[] hits = searchResp7.getHits().getHits();
			//主要处理逻辑
			Map source = new HashMap<>();
			String[] hit_ids = new String[hits.length];
			for(int j=0 ; j<hits.length; j++ ){
				hit_ids[j] = (String)hits[j].getSource().get("CO_ID");
				//更新时间戳
				source.clear();
				source.put("PROCESS_TIME", new SimpleDateFormat("yyyy-MM-dd HH:mi:ss").format(new Date()));
				//考虑要不要批量处理
				client.prepareUpdate(index,v_tabName.toLowerCase(),hit_ids[j]).setDoc(source).get();
//				updateTimeStamp(source, index, v_tabName, hit_ids[j]);
				//考虑要不要批量处理
				SearchResponse searchResp8 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.boolQuery()
						.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
						.filter(QueryBuilders.matchQuery("PROCESS_BATCH", importseq))
						.filter(QueryBuilders.matchQuery("CO_ID", hit_ids[j]))
						).get();
				//得到大小量纲的值
				Object dxlgobj = searchResp8.getHits().getHits()[0].getSource().get("MATDXLG");
				v_dxlg = dxlgobj==null?"":(String)dxlgobj;
				if(!"".equals(v_dxlg)){
					v_matdxlg = v_dxlg;
					v_db2 = v_matdxlg.split(v_assemble_symble).length;
					//如果最后一位是特殊分割符的话大小量纲的格式就会出现错误
					if(v_db1==v_db2){
						v_model = v_matdxlg.substring(v_matdxlg.length()-1, v_matdxlg.length());
						if(!"".equals(v_model)&&!"".equals(v_assemble_symble)){
							if(v_assemble_symble.equals(v_model)){
								updateErrorState(source, index, v_tabName, hit_ids[j]);
							}else{
								updateAssembleColAndVal(v_db2, source, importseq, iclerkcode, v_clid, v_matdxlg, v_assemble_symble, index, v_tabName, hit_ids[j], bulkinsert);
							}
							
						}else{
							updateAssembleColAndVal(v_db2, source, importseq, iclerkcode, v_clid, v_matdxlg, v_assemble_symble, index, v_tabName, hit_ids[j], bulkinsert);
						}
					}else{
						updateErrorState(source, index, v_tabName, hit_ids[j]);
					}
					deletePJSXZB(prepareSearch, bulkinsert, iclerkcode, v_clid, importseq, index, "pjsxzb");
				}
			}
			deletePJSXZB(prepareSearch, bulkinsert, iclerkcode, v_clid, importseq, index,"pjsxzb");
		}
		
		/********************************属性组合*************************************/
		String v_ca_codeid = null,//字段名
			v_ca_restrict = null,//字段的限制|s|0|200|0|不限|
			v_ca_name = null,
			v_co_id = null,
			v_pjstr = null,
			v_property_val = null,
			v_value = null,
			cl_default = null, //默认值
			v_ca_id = null,
			v_valueround = null,
			v_a = null, //是否必填
			v_b = null, //数据类型
			v_c = null, //最小长度
			v_d = null, //最大长度
			v_e = null, //数据下限
			v_f = null, //数据上线
			v_g = null, //是否大小写
			cl_id = null, //
			v_ca_valuetype = null, //字段值类型
			v_cl_code = null, //分类编码
			v_cl_name = null, //分类名称
			v_vs_id = null, //枚举id
			v_vsname = null, //枚举值
			v_tablename = null,
			v_co_id_mark = null,
			v_co_desc = null,
			v_vs_id1 = null,//对象id
			v_vsname1 = null,//对象值
			v_matname = null,
			v_matdescs = null,//物料简称
			v_matwljc = null;//物料简称
		long num = 0;
			
		//SELECT CC.COLUMN_NAME,C.CA_RESTRICT,C.CA_NAME,C.CA_ID FROM COLS CC,CL_CATALOGATTRVALUE C WHERE C.TABLENAME = ITABLENAME  AND C.CL_ID=ICLID
		// AND CC.TABLE_NAME=ITABLENAME||'_TEMPYB'
		// AND CC.COLUMN_NAME=C.CA_CODEID AND C.AT_ID NOT IN (11,12) AND C.CA_CODEID<>'MATDXLG'		
		SearchResponse searchResp11 = prepareSearch.setTypes("cols").setQuery(QueryBuilders.boolQuery()
				.filter(QueryBuilders.matchQuery("TABLE_NAME", v_tabName+"_tempyb"))
				).get();
		SearchHit[] hits = searchResp11.getHits().getHits();
		String[] col_name = new String[hits.length];
		for(int i = 0;i<hits.length ;i++){
			Object colobj = hits[i].getSource().get("COLUMN_NAME");
			col_name[i] = colobj == null?"":(String)colobj;
		}
		//确定数组有内容
		if(col_name.length>0){
			BoolQueryBuilder filter = QueryBuilders.boolQuery();
			for(String str : col_name){
				filter.filter(QueryBuilders.matchQuery("CA_CODEID", str));
			}
			filter.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))
			.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
			.filter(QueryBuilders.termsQuery("AT_ID", "11","12"))
			.mustNot(QueryBuilders.matchQuery("CA_CODEID", "MATDXLG"));
			SearchResponse searchResp12 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(filter).get();
			SearchHit[] hits2 = searchResp12.getHits().getHits();
			//第一层循环
			for (SearchHit hit2 : hits2) {
				Object ca_codeid = hit2.getSource().get("CA_CODEID");
				v_ca_codeid = ca_codeid == null ? "" : (String) ca_codeid;
				Object ca_restrict = hit2.getSource().get("CA_RESTRICT");
				v_ca_restrict = ca_restrict == null ? "" : (String) ca_restrict;
				Object name = hit2.getSource().get("CA_NAME");
				v_ca_name = name == null ? "" : (String) name;
				Object ca_id = hit2.getSource().get("CA_ID");
				v_ca_id = ca_id == null ? "" : (String) ca_id;
				SearchResponse searchResp13 = prepareSearch.setTypes(v_tabName.toLowerCase())
						.setQuery(QueryBuilders.boolQuery().filter(QueryBuilders.matchQuery("CL_ID", v_clid))
								.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))
								.mustNot(QueryBuilders.matchQuery("IMPORT_ERROR", "1"))
								.filter(QueryBuilders.matchQuery("PROCESS_BATCH", importseq)))
						.get();
				SearchHit[] hits3 = searchResp13.getHits().getHits();
				// 第二层循环
				for (SearchHit hit3 : hits3) {
					// 得到co_id
					v_co_id = hit3.getId();
					// 查询得到属性组合的值（哪些字段组合的）
					checkAssembleAttrs(prepareSearch, v_clid, v_assemble_symble, v_assemble_attrid, index, v_tabName,
							v_property_val, v_co_id, v_ca_codeid);
					// 另外两种特殊情况1是CO_DESC
					checkAssembleAttrs(prepareSearch, v_clid, v_assemble_symble, v_assemble_attrid, index, v_tabName,
							v_property_val, v_co_id, "CO_DESC");
					// 另外两种特殊情况1是CO_ID_MARK
					checkAssembleAttrs(prepareSearch, v_clid, v_assemble_symble, v_assemble_attrid, index, v_tabName,
							v_property_val, v_co_id, "CO_ID_MARK");
				}

			}
			/*******************************属性判断*******************************************/
			BoolQueryBuilder filter1 = QueryBuilders.boolQuery();
			for(String str : col_name){
				filter1.filter(QueryBuilders.matchQuery("CA_CODEID", str));
			}
			filter1.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))
			.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
			.filter(QueryBuilders.termsQuery("AT_ID", "11","12"));
			SearchResponse searchResp17 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(filter).get();
			SearchHit[] hits3 = searchResp17.getHits().getHits();
			for(SearchHit hit3 : hits3){
				Object ca_codeid = hit3.getSource().get("CA_CODEID");
				v_ca_codeid = ca_codeid == null ? "" : (String) ca_codeid;
				Object ca_restrict = hit3.getSource().get("CA_RESTRICT");
				v_ca_restrict = ca_restrict == null ? "" : (String) ca_restrict;
				Object name = hit3.getSource().get("CA_NAME");
				v_ca_name = name == null ? "" : (String) name;
				Object ca_id = hit3.getSource().get("CA_ID");
				v_ca_id = ca_id == null ? "" : (String) ca_id;
				Map source = new HashMap<>();
				for(String co_id : ids){
					updateTimeStamp(prepareSearch, source, index, v_tabName, co_id);
					SearchResponse searchResp18 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(new IdsQueryBuilder().addIds(co_id)).get();
					num = searchResp18.getHits().getTotalHits();
					if(num>0){
						Object value= searchResp18.getHits().getHits()[0].getSource().get(v_ca_codeid);
						v_value = value == null?"":(String)value;
					}else{
						v_value = "";
					}
					//如果字段值为空添加默认值
					if("".equals(v_value)){
						SearchResponse searchResp19 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.boolQuery()
								.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))
								.filter(QueryBuilders.matchQuery("CA_CODEID", v_ca_codeid))
								.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
								.mustNot(QueryBuilders.matchQuery("CL_DEFAULT", ""))).get();
						num = searchResp19.getHits().getTotalHits();
						if(num>0){
							Object def = searchResp19.getHits().getHits()[0].getSource().get("CL_DEFAULT");
							cl_default = def == null?"-1":(String)def;
						}else{
							cl_default = "-1";
						}
						source.clear();
						source.put(v_ca_codeid, cl_default);
						if(!"-1".equals(cl_default)){
							client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source).get();
						}
					}
					//判断属性是否为必填
					v_a = ESUtil.getPropertyLimit(v_ca_restrict, 1 , "|");
					v_b = ESUtil.getPropertyLimit(v_ca_restrict, 2 , "|");
					v_c = ESUtil.getPropertyLimit(v_ca_restrict, 3 , "|");
					v_d = ESUtil.getPropertyLimit(v_ca_restrict, 4 , "|");
					v_e = ESUtil.getPropertyLimit(v_ca_restrict, 5 , "|");
					v_f = ESUtil.getPropertyLimit(v_ca_restrict, 6 , "|");
					v_g = ESUtil.getPropertyLimit(v_ca_restrict, 7 , "|");
					//字段不能为空
					if("*".equals(v_a)){
						if("".equals(v_value)){
							source.clear();
							source.put("PROCESS_STATE", "3");
							source.put("IMPORT_ERROR", "1");
							source.put("IMPORT_ERROR_COMMENT", v_ca_name+"：字段不能为空;");
							updateTempYB(index, v_tabName, co_id, source);
						}
					}
					//
					if(!"".equals(v_value)){
						if("s".equalsIgnoreCase(v_b)){
							//大写
							if("A".equalsIgnoreCase(v_g)){
								source.clear();
								source.put(v_ca_codeid, v_value.toUpperCase());
								updateTempYB(index, v_tabName, co_id, source);
							}
							//字段长度校验
							if("0".equals(v_c)&&"0".equals(v_d)){
								if(v_value.length()<Integer.valueOf(v_c)){
									updateErrorMesg(source, v_ca_name, index, v_tabName, "：该字段输入值不能小于最小长度;", co_id);
								}else if(v_value.length()>Integer.valueOf(v_d)){
									updateErrorMesg(source, v_ca_name, index, v_tabName, "：该字段输入值不能大于最大长度;", co_id);
								}
							}
							//物料描述（中）、大小量纲、基本物料长度不校验  
							if("0".equals(v_c)&&!"0".equals(v_d)){
								if(v_d.length()>Integer.valueOf(v_d)){
									if("MATDESCC".equals(v_ca_codeid)||"MATDXLG".equals(v_ca_codeid)||"MATBMAT".equals(v_ca_codeid)){
										
									}else{
										updateErrorMesg(source, v_ca_name, index, v_tabName, "：该字段输入值长度不能大于最大长度;", co_id);
									}
								}
							}
						}
						//判断数值不为空
						if(!"0".equals(v_e)&&!"0".equals(v_f)){
							if(Integer.valueOf(v_value)<Integer.valueOf(v_e)){
								updateErrorMesg(source, v_ca_name, index, v_tabName, "：该字段值不能小于数值下限;", co_id);
							}
							if(Integer.valueOf(v_value)>Integer.valueOf(v_f)){
								updateErrorMesg(source, v_ca_name, index, v_tabName, "：该字段值不能大于数值上限;", co_id);
							}
						//整型字段校验
						}else if("i".equalsIgnoreCase(v_b)){//整型
							updateNumberTypetoTempYB(0,v_value, v_valueround, source, v_ca_codeid, index, v_tabName, co_id);
						}else if("f2".equalsIgnoreCase(v_b)){//保留2位小数
							updateNumberTypetoTempYB(2,v_value, v_valueround, source, v_ca_codeid, index, v_tabName, co_id);
						}else if("f3".equalsIgnoreCase(v_b)){//保留3位小数
							updateNumberTypetoTempYB(3,v_value, v_valueround, source, v_ca_codeid, index, v_tabName, co_id);
						}
						//分类检查
						SearchResponse searchResp20 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.boolQuery()
								.filter(QueryBuilders.matchQuery("AT_ID", "3"))
								.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
								.filter(QueryBuilders.matchQuery("CA_CODEID", v_ca_codeid)))
								.addSort("CA_VALUETYPE", SortOrder.DESC)
								.get();
						num = searchResp20.getHits().getTotalHits();
						if(num>0){
							Object ca_valuetype = searchResp20.getHits().getHits()[0].getSource().get("CA_VALUETYPE");
							v_ca_valuetype = ca_valuetype == null?"-1":(String)ca_valuetype;
							
							if(!"-1".equals(v_ca_valuetype)){
								SearchResponse searchResp21 = prepareSearch.setTypes("cl_classtypevalues").setQuery(QueryBuilders.matchQuery("CT_ID", v_ca_valuetype)).get();
								num = searchResp21.getHits().getTotalHits();
								if(num>0){
									Object clid = searchResp20.getHits().getHits()[0].getSource().get("CA_VALUETYPE");
									cl_id = clid == null?"-1":(String)clid;
								}
								
								if("-1".equals(cl_id)){
									String sql = "SELECT c.cl_code FROM cl_catalog c  start with CL_ID="+v_clid+
                      "connect by prior c.CL_ID=CL_PARENTID";
									List<String> clcodes = DbOperation.executeCO_IDList(sql);
									SearchResponse searchResp22 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.boolQuery()
											.filter(QueryBuilders.termsQuery(v_ca_codeid, clcodes))
											.filter(QueryBuilders.matchQuery(v_ca_codeid, v_value))
											.filter(QueryBuilders.matchQuery("CO_ID", co_id))
											.filter(QueryBuilders.matchQuery("CL_ID", cl_id))).get();
									num = searchResp22.getHits().getTotalHits();
									if(num>0){
										SearchResponse searchResp23 = prepareSearch.setTypes("cl_catalog").setQuery(QueryBuilders.matchQuery("CL_CODE", v_value)).get();
										Map<String, Object> source2 = searchResp23.getHits().getHits()[0].getSource();
										Object clcode = source2.get("CL_CODE");
										Object clname = source2.get("CL_NAME");
										v_cl_code = clcode == null ? "" : (String)clcode;
										v_cl_name = clname == null ? "" : (String)clname;
										source.clear();
										source.put(v_ca_codeid+"_ID", v_cl_code);
										source.put(v_ca_codeid, v_cl_name);
										client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source).get();
									}else{
										updateErrorMesg(source, v_ca_name, index, v_tabName, "：分类有问题，请检查！;", co_id);
									}
								}else{
									updateErrorMesg(source, v_ca_name, index, v_tabName, "：分类有问题，请检查！;", co_id);
								}
							}
						}
					}
					/**枚举属性**/
					SearchResponse searchResp24 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.boolQuery()
							.filter(QueryBuilders.termQuery("AT_ID", "2"))
							.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
							.filter(QueryBuilders.matchQuery("CO_CODEID", v_ca_codeid))).get();
				    num = searchResp24.getHits().getTotalHits();
				    if(num>0){
				    	//查询出导入时候填写的枚举属性值
		                //增加查询枚举属性是否存在
				    	SearchResponse searchResp25 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.boolQuery()
								.filter(QueryBuilders.termQuery("AT_ID", "2"))
								.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
								.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))
								.filter(QueryBuilders.matchQuery("CO_CODEID", v_ca_codeid))).get();
				    	Object valueType = searchResp25.getHits().getHits()[0].getSource().get("CA_VALUETYPE");
				    	v_ca_valuetype = valueType == null?"-1":(String)valueType;
				    	if(Integer.valueOf(v_ca_valuetype)>0){
				    		if(!"".equals(v_value)){
				    			SearchResponse searchResp26 = prepareSearch.setTypes("cl_valueset").setQuery(QueryBuilders.boolQuery()
				    					.filter(QueryBuilders.matchQuery("ET_ID", v_ca_valuetype))
				    					.filter(QueryBuilders.matchQuery("VS_CODE", v_value))).get();
				    			Object vsid = searchResp26.getHits().getHits()[0].getSource().get("VS_ID");
				    			v_vs_id = vsid == null ?"-1":(String)vsid;
				    			if(!"-1".equals(v_vs_id)){
				    				SearchResponse searchResp27 = prepareSearch.setTypes("cl_valueset").setQuery(QueryBuilders.termQuery("VS_ID", v_vs_id)).get();
				    				Object vsname = searchResp27.getHits().getHits()[0].getSource().get("VS_NAME");
				    				v_vsname = vsname == null?"":(String)vsname;
				    				if(!"-1".equals(v_vsname)){
				    					source.put(v_ca_codeid+"_ID", v_value);
										source.put(v_ca_codeid, v_vsname);
										client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source).get();
				    				}
				    			}else{
				    				updateErrorMesg(source, v_ca_name, index, v_tabName, "：该枚举属性不存在，请重新填写;", co_id);
				    			}
				    		}
				    	}
				    }
				    /****对象属性****/
				    SearchResponse searchResp28 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.boolQuery()
				    		.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
				    		.filter(QueryBuilders.matchQuery("CA_CODEID", v_ca_codeid))
				    		.filter(QueryBuilders.termsQuery("AT_ID", "8","14","15"))).get();
				    num = searchResp28.getHits().getTotalHits();
				    if(num>0){
				    	SearchResponse searchResp29 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.boolQuery()
					    		.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
					    		.filter(QueryBuilders.matchQuery("CA_CODEID", v_ca_codeid))
					    		.filter(QueryBuilders.termsQuery("AT_ID", "8","14","15"))
					    		.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))).get();
				    	Object valuetype = searchResp29.getHits().getHits()[0].getSource().get("CA_VALUETYPE");
				    	v_ca_valuetype = valuetype == null?"":(String)valuetype;
				    	if(Integer.valueOf(v_ca_valuetype)>0){
				    		SearchResponse searchResp30 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.termQuery("CL_ID", v_ca_valuetype)
				    				).get();
				    		Object tableName = searchResp30.getHits().getHits()[0].getSource().get("TABLENAME");
				    		v_tablename = tableName == null?"aaa":(String)tableName;
				    		if(!"aaa".equals(v_tablename)){
				    			if(!"".equals(v_value)){
				    				SearchResponse searchResp31 = prepareSearch.setTypes(v_tablename.toLowerCase()).setQuery(QueryBuilders.matchQuery("CO_ID_MARK", v_value))
				    					.addSort("CO_ID_MARK",SortOrder.DESC).get();
				    				Object co_id_mark = searchResp31.getHits().getHits()[0].getSource().get("CO_ID_MARK");
				    				v_co_id_mark = co_id_mark == null?"-1":(String)co_id_mark;
				    				if(!"-1".equals(v_co_id_mark)) {
					    				Object co_desc = searchResp31.getHits().getHits()[0].getSource().get("CO_DESC");
					    				v_co_desc = co_desc == null ? "-1" : (String)co_desc;
					    				source.clear();
					    				source.put(v_ca_codeid+"_ID", v_value);
					    				source.put(v_ca_codeid, v_co_desc);
					    				client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source).get();
				    				}else{
				    					updateErrorMesg(source, v_ca_name, index, v_tabName, "：对象属性不存在，请重新填写;", co_id);
				    				}
				    				
				    			}
				    		}
				    	}
				    }
				    /****复选框校验****/
				    SearchResponse searchResp31 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.boolQuery()
				    		.filter(QueryBuilders.matchQuery("AT_ID", "13"))
				    		.filter(QueryBuilders.matchQuery("CA_CODEID", v_ca_codeid))
				    		.filter(QueryBuilders.matchQuery("CL_ID", v_clid))).get();
				    num = searchResp31.getHits().getTotalHits();
				    if(num>0){
				    	SearchResponse searchResp32 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.boolQuery()
				    			.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))
				    			.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
				    			.filter(QueryBuilders.matchQuery("AT_ID", "13"))
				    			.filter(QueryBuilders.matchQuery("CA_CODEID", v_ca_codeid))).get();
				    	Object valuetype = searchResp32.getHits().getHits()[0].getSource().get("CA_VALUETYPE");
				    	v_ca_valuetype = valuetype == null?"-1":(String)valuetype;
				    	if(Integer.valueOf(v_ca_valuetype)>0){
				    		if(!"".equals(v_value)){
				    			v_vs_id1 = "";
				    			v_vsname1 = "";
				    			String[] values = v_value.split(":");
				    			BoolQueryBuilder filter2 = QueryBuilders.boolQuery();
				    			for(String val : values){
				    				filter2.filter(QueryBuilders.matchQuery("VS_NAME", val).operator(Operator.AND));
				    			}
				    			filter2.filter(QueryBuilders.termQuery("ET_ID", v_ca_valuetype));
				    			SearchResponse searchResp33 = prepareSearch.setTypes("cl_valueset").setQuery(filter2).get();
				    			num = searchResp33.getHits().getTotalHits();
				    			if(num>0){
				    				SearchHit[] hits4 = searchResp33.getHits().getHits();
				    				for(SearchHit hit4 : hits4){
				    					Object vs_name = hit4.getSource().get("VS_NAME");
				    					v_vsname = vs_name  == null?"":(String)vs_name;
				    					Object vs_id = hit4.getSource().get("VS_ID");
				    					v_vs_id = vs_id == null?"-1":(String)vs_id;
				    					if(Integer.valueOf(v_vs_id)>0){
				    						v_vs_id = v_vs_id + ":";
				    						v_vs_id1 = v_vs_id1 + v_vs_id;
				    						v_vsname = v_vsname + ":";
				    						v_vsname1 = v_vsname1 + v_vsname;
				    					}
				    				}
				    				if(!"".equals(v_vs_id1)){
				    					source.clear();
				    					source.put(v_ca_codeid+"_ID", v_vsname1);
				    					source.put(v_ca_codeid, v_vsname1);
				    					client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source).get();
				    				}else{
				    					updateErrorMesg(source, v_ca_name, index, v_tabName, ":该复选框属性不存在，请重新填写;", co_id);
				    				}
				    			}
				    		}
				    		
				    	}
				    }
				    /**增加物料名称简称***/
				    SearchResponse searchResp34 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.termQuery("CO_ID", co_id)).get();
				    if("T_MAT".equalsIgnoreCase(v_tabName)&&"MATNAME".equalsIgnoreCase(v_ca_codeid)){
				    	num = searchResp34.getHits().getTotalHits();
				    	if(num>0){
				    		Object matname = searchResp34.getHits().getHits()[0].getSource().get("MATNAME");
				    		v_matname = matname == null ? "" : (String)matname;
				    		SearchResponse searchResp35 = prepareSearch.setTypes("wlmc_wljc_mapiing").setQuery(QueryBuilders.matchQuery("WLMC", v_matname)).get();
				    		num = searchResp35.getHits().getTotalHits();
				    		if(num>0){
				    			v_matname = "".equals(v_matname)?"-1":v_matname;
				    			Object matdesc = searchResp34.getHits().getHits()[0].getSource().get("MATDESCS");
				    			v_matdescs = matdesc == null ? "" : (String)matdesc;
				    			
				    			Object wljc = searchResp35.getHits().getHits()[0].getSource().get("WLJC");
				    			v_matwljc = wljc == null ? "-1" : (String)wljc;
				    			if(!"-1".equals(v_matname)){
				    				source.clear();
				    				source.put("MATDESCS", v_matwljc);
				    				if("".equals(v_matdescs)){//1、名称有，简称空，对照表中有，简称取对照表
				    					if(!"-1".equals(v_matwljc)){
				    						client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source).get();
				    					}
				    				}else{//2.名称有，简称有，简称与对照表不符，简称取对照表
				    					if(!v_matdescs.equals(v_matwljc)){
				    						client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source).get();
				    					}
				    				}
				    			}else{
				    				if(!"".equals(v_matdescs)){//3.名称有，简称空，对照表中无，简称取名称
				    					if("-1".equals(v_matwljc)){
				    						source.clear();
				    						source.put("MATDESCS", v_matname);
				    						client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source).get();
				    					}
				    				}else{
				    					if("-1".equals(v_matwljc)){//名称有，简称有，对照表无，简称取填写
				    						source.clear();
				    						source.put("MATDESCS", v_matdescs);
				    						client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source).get();
				    					}
				    				}
				    			}
				    		}
				    	}
				    }
				    /**SYSCODE和SYSNUM**/
				    String v_syscodes = null, 
				    	   v_sysnum = null, 
				    	   v_qfcode = null,
				    	   v_import_err = null,
				    	   v_dztable = null;
//				    	   v_syscode = null;
				    if("T_MAT".equalsIgnoreCase(v_tabName)&&"SYSCODE".equalsIgnoreCase(v_ca_codeid)){
				    	num = searchResp34.getHits().getTotalHits();
				    	if(num>0){
				    		Object syscode = searchResp34.getHits().getHits()[0].getSource().get("SYSCODE");
				    		v_syscodes = syscode == null?"":(String)syscode;
				    		Object sysnum = searchResp34.getHits().getHits()[0].getSource().get("SYSNUM");
				    		v_sysnum = sysnum == null ?"":(String)sysnum;
				    		Object err_comment = searchResp34.getHits().getHits()[0].getSource().get("IMPORT_ERROR_COMMENT");
				    		v_import_err = err_comment == null ?"":(String)err_comment;
				    		
				    		//判断系统编码知否在相应的组织下
				    		if(!"".equals(v_syscodes)&&!"".equals(v_sysnum)){
				    			String[] syscodes = v_syscodes.split(",");
				    			for(String v_syscode : syscodes){
				    				SearchResponse searchResp36 = prepareSearch.setTypes("qualified_syss").setQuery(QueryBuilders.matchQuery("QF_ORG", izz)).get();
				    				num = searchResp36.getHits().getTotalHits();
				    				if(num>0){
				    					Object qf_code = searchResp36.getHits().getHits()[0].getSource().get("QF_CODE");
				    					v_qfcode = qf_code == null?"-1":(String)qf_code;
				    					if(!"-1".equals(v_qfcode)){
				    						SearchResponse searchResp37 = prepareSearch.setTypes("qualified_syss").setQuery(QueryBuilders.boolQuery()
				    								.filter(QueryBuilders.matchQuery("QF_ORG", izz))
				    								.filter(QueryBuilders.matchQuery("QF_TYPE", "36"))
				    								.filter(QueryBuilders.matchQuery("QF_CODE", v_syscode))).get();
				    						num = searchResp37.getHits().getTotalHits();
				    						if(num>0){
				    							updateErrorMesg(source, "", index, v_tabName, v_import_err+";编码"+v_syscode+"系统不存在该组织机构下", co_id);
				    						}
				    					}
				    					//与多值表中数据对比判断是否存在
				    					SearchResponse searchResp38 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(QueryBuilders.boolQuery()
				    							.filter(QueryBuilders.matchQuery("CA_CODEID", "SYS"))
				    							.filter(QueryBuilders.matchQuery("TABLENAME", "T_MAT"))).get();
				    					String val_type = null;
				    					Object valtype = searchResp38.getHits().getHits()[0].getSource().get("CA_VALUETYPE");
				    					val_type = valtype == null ?"":(String)valtype;
				    					SearchResponse searchResp39 = prepareSearch.setTypes("cl_catalogattrvalue").setQuery(
				    							QueryBuilders.matchQuery("CL_ID", val_type)).get();
				    					Object tabname = searchResp39.getHits().getHits()[0].getSource().get("TBALENAME");
				    					v_dztable = tabname == null?"":(String)tabname;
				    					SearchResponse searchResp40 = prepareSearch.setTypes(v_dztable).setQuery(QueryBuilders.boolQuery()
				    							.filter(QueryBuilders.matchQuery("SYSCODE", v_syscode))
				    							.filter(QueryBuilders.matchQuery("SYSNUM", v_sysnum))
				    							.filter(QueryBuilders.termsQuery("CO_VALID", "0","1","2","8","11"))).get();
				    					num = searchResp40.getHits().getTotalHits();
				    					if(num>0){
				    						updateErrorMesg(source, "", index, v_tabName, v_import_err+";系统代码,系统内部编号已经存在", co_id);
				    					}else{
				    						SearchResponse searchResp41 = prepareSearch.setTypes(v_dztable).setQuery(QueryBuilders.boolQuery()
					    							.filter(QueryBuilders.matchQuery("SYSTCODE", v_syscode))
					    							.filter(QueryBuilders.matchQuery("OLDNUM", v_sysnum))).get();
				    						num = searchResp41.getHits().getTotalHits();
				    						if(num>0){
				    							updateErrorMesg(source, "", index, v_tabName, v_import_err+";系统代码和内部编号已经发布过", co_id);
				    						}
				    					}
				    				}
				    			
				    			}
				    		}else if(!"".equals(v_sysnum)&&"".equals(v_syscodes)){
				    			updateErrorMesg(source, "", index, v_tabName, "系统代码,系统内部编号必须同时存在或同时为空", co_id);
				    		}else if("".equals(v_sysnum)&&!"".equals(v_syscodes)){
				    			updateErrorMesg(source, "", index, v_tabName, "系统代码,系统内部编号必须同时存在或同时为空", co_id);
				    		}
				    		SearchResponse searchResp42 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.boolQuery()
				    				.filter(QueryBuilders.matchQuery("SYSNUM", v_sysnum))
				    				.filter(QueryBuilders.matchQuery("PROCESS_BATCH", importseq))
				    				.filter(QueryBuilders.matchQuery("IMPORTUSER", v_clerkid))
				    				.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))).get();
				    		num = searchResp42.getHits().getTotalHits();
				    		if(num>1){
				    			updateErrorMesg(source, "", index, v_tabName, v_import_err + ";EXCEL系统内部编号重复，请核查", co_id);
				    		}
				    		
				    	}
				    }
				    /***毛重、净重、体积、与单位校验*****/
				    String v_mz = null, //毛重
				    		v_jz = null, //净重
				    		v_wu = null, //重量单位
				    		v_tj = null, //体积
				    		v_vu = null; //体积单位
				    if("T_MAT".equals(v_tabName)&&"MATGWEIGHT".equals(v_ca_codeid)){
				    	num = searchResp34.getHits().getTotalHits();
				    	if(num>0){
				    		Map source3 = searchResp34.getHits().getHits()[0].getSource();
				    		v_mz = source3.get("MATGWEIGHT") == null?"":(String)source3.get("MATGWEIGHT");
				    		v_jz = source3.get("MATNWEIGHT") == null?"":(String)source3.get("MATNWEIGHT");
				    		v_wu = source3.get("MATWUNIT") == null?"":(String)source3.get("MATWUNIT");
				    		v_tj = source3.get("MATVOLUME") == null?"":(String)source3.get("MATVOLUME");
				    		v_vu = source3.get("MATVUNIT") == null?"":(String)source3.get("MATVUNIT");
				    		if(!"".equals(v_mz)&&!"".equals(v_jz)){
				    			if("".equals(v_wu)){
				    				updateErrorMesg(source, "", index, v_tabName, v_import_err + ";重量单位不能为空",co_id);
				    			}
				    		}else if(!"".equals(v_wu)){
				    			updateErrorMesg(source, "", index, v_tabName,  v_import_err + ";重量单位应该为空",co_id);
				    		}
				    		if(!"".equals(v_tj)){
				    			if("".equals(v_vu)){
				    				updateErrorMesg(source, "", index, v_tabName, v_import_err + ";体积单位不能为空",co_id);
				    			}
				    		}else if(!"".equals(v_vu)){
				    			updateErrorMesg(source, "", index, v_tabName,  v_import_err + ";体积单位应该为空",co_id);
				    		}
				    	}
				    }
				    
				}
			}
			
		}
		/*******************************自查重********************************/
		String v_string = null;
		SearchResponse searchResp4 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.boolQuery()
				.mustNot(QueryBuilders.matchQuery("IMPORT_ERROR", "1"))
				.filter(QueryBuilders.matchQuery("CL_ID", v_clid))).get();
		num = searchResp4.getHits().getTotalHits();
		if(num>0){
			SearchResponse searchResp5 = prepareSearch.setTypes("search_duplicates").setQuery(QueryBuilders.boolQuery()
					.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
					.mustNot(QueryBuilders.matchQuery("CA_ID", ""))).get();
			num = searchResp5.getHits().getTotalHits();
			if(num>0){
				Map<String,Object> source = new HashMap<String,Object>();
				for(String id : ids){
					SearchResponse searchResp7 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.boolQuery()
							.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
							.filter(QueryBuilders.matchQuery("CO_ID", id))
							.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))
							.mustNot(QueryBuilders.matchQuery("IMPORT_ERROR", "1"))).get();
					num = searchResp7.getHits().getTotalHits();
					if(num>0){
						SearchResponse searchResp8 = prepareSearch.setTypes("search_duplicates").setQuery(QueryBuilders.matchQuery("CL_ID", v_clid)).get();
						num = searchResp8.getHits().getTotalHits();
						if(num>0){
							SearchHit[] hits2 = searchResp8.getHits().getHits();
							for(SearchHit hit2 : hits2){
								v_ca_id = hit2.getSource().get("CA_ID") == null?"":(String)hit2.getSource().get("CA_ID");
								v_ca_codeid = hit2.getSource().get("CA_CODEID") == null?"": (String)hit2.getSource().get("CA_CODEID");
								SearchResponse searchResp9 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.boolQuery()
										.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
										.filter(QueryBuilders.matchQuery("CO_ID", id))
										.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))
										.filter(QueryBuilders.matchQuery("IMPORTUSER", v_clerkid))
										.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))
										.filter(QueryBuilders.matchQuery("PROCESS_BATCH", importseq))
										.mustNot(QueryBuilders.matchQuery("IMPORT_ERROR", "1"))).get();
								num = searchResp9.getHits().getTotalHits();
								if(num>0){
									Object ca_codeid = searchResp9.getHits().getHits()[0].getSource().get(v_ca_codeid);
									v_value = ca_codeid == null?"":(String)ca_codeid;
									if("".equals(v_value)){
									}else{
										source.clear();
										source.put(v_ca_codeid, v_value);
									}
//									v_string = v_string + " = " +v_ca_codeid;
								}
							}
						}
					}
					if(!source.isEmpty()){
						//临时表中已处理的正确数据大于0,表示改条数据已存在
						BoolQueryBuilder filter = QueryBuilders.boolQuery();
						String col = null;
						for(String key : source.keySet()){
							col = source.get(key)==null?"":(String)source.get(key);
							filter.filter(QueryBuilders.matchQuery(key, col));
						}
						filter.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))
						.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))
						.filter(QueryBuilders.termsQuery("PROCESS_STATE", "2","3"))
						.filter(QueryBuilders.termsQuery("IMPORT_ERROR", "0","2"));
						SearchResponse searchResp10 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(filter).get();
						num = searchResp10.getHits().getTotalHits();
						Map doc = new HashMap<>();
						if(num>0){
							updateErrorMesg(doc, "", index, v_tabName,  "跟集团内待申请数据重复;",id);
						}
						//数据库数据查重
						BoolQueryBuilder filter1 = QueryBuilders.boolQuery();
						String col1 = null;
						for(String key : source.keySet()){
							col1 = source.get(key)==null?"":(String)source.get(key);
							filter.filter(QueryBuilders.matchQuery(key, col1));
						}
						filter.filter(QueryBuilders.termQuery("CO_VALID", "1"));
						SearchResponse searchResp12 = prepareSearch.setTypes(v_tabName.toLowerCase()).setQuery(filter).get();
						num = searchResp12.getHits().getTotalHits();
						if(num>0){
							updateErrorMesg(doc, "", index, v_tabName, "该记录在数据库中已经存在;", id);
						}
					}
							
				}
			}
		}
		/***************************唯一性校验*****************************/
		SearchResponse searchResp13 = prepareSearch.setTypes("cl_validate_rules").setQuery(QueryBuilders.boolQuery()
				.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
				.filter(QueryBuilders.matchQuery("VR_TYPE", "1"))).get();
		num = searchResp13.getHits().getTotalHits();
		if(num>0){
			SearchHit[] hits2 = searchResp13.getHits().getHits();
			int count2 = hits2.length;
			//聚集查询条件 
 			String[] vr_ids = new String[count2];
			for(int i=0;i<count2;i++){
				Object id = hits2[i].getSource().get("ID");
				if(id!=null){
					vr_ids[i] = (String)id;
				}
			}
//			AggregationBuilders.terms("VR_ID").field("VR_ID")
			SearchResponse searchResp14 = prepareSearch.setTypes("cl_validate_rules_detail").setQuery(QueryBuilders.termsQuery("VR_ID", vr_ids)).get();
			num = searchResp14.getHits().getTotalHits();
			if(num>0){
				//聚集查询出来的vr_id进行循环
				Set<String> vrids = new HashSet<String>();
				SearchHit[] hits3 = searchResp14.getHits().getHits();
				for(SearchHit hit3 :hits3){
					Object vrid = hit3.getSource().get("VR_ID");
					if(vrid!=null){
						vrids.add((String)vrid);
					}
				}
				//my_cur2
				Map<String,Object> source = new HashMap<>();
				for(String coid : ids){
					source.clear();
					for(String vr_id: vrids){
						SearchResponse searchResp15 = prepareSearch.setTypes("cl_validate_rules_detail").setQuery(QueryBuilders.matchQuery("ID", vr_id)).get();
						num = searchResp15.getHits().getTotalHits();
						if(num>0){
							Object vrdcode = searchResp15.getHits().getHits()[0].getSource().get("VRD_CODE");
							v_ca_codeid = vrdcode ==  null ? "" : (String) vrdcode;
							SearchResponse searchResp16 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.boolQuery()
									.filter(QueryBuilders.matchQuery("CO_ID", coid))
									.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
									.filter(QueryBuilders.matchQuery("PROCESS_BATCH", importseq))
									.filter(QueryBuilders.matchQuery("MARKSIGN", "1"))
									.filter(QueryBuilders.matchQuery("IMPORTUSER", v_clerkid))
									.filter(QueryBuilders.matchQuery("TABLENAME", v_tabName))).get();
							num = searchResp16.getHits().getTotalHits();
							if(num>0){
								Object codeid = searchResp16.getHits().getHits()[0].getSource().get(v_ca_codeid);
								if(codeid!=null){
									source.put(v_ca_codeid, (String)codeid);
								}
								
							}
						}
						SearchResponse searchResp18 = prepareSearch.setTypes("cl_validate_rules_detail").setQuery(QueryBuilders.matchQuery("VR_ID", vr_id)).get();
						//说明校验的属性值不全为空，全为空则不校验
						if(!source.isEmpty()){
							BoolQueryBuilder filter = QueryBuilders.boolQuery();
							for(String key : source.keySet()){
								filter.filter(QueryBuilders.matchQuery(key, source.get(key)).operator(Operator.AND));
							}
							filter.filter(QueryBuilders.matchQuery("CO_VALID", "1"));
							SearchResponse searchResp17 = prepareSearch.setTypes(v_tabName.toLowerCase()).setQuery(filter).get();
							num = searchResp17.getHits().getTotalHits();
							String vrd_name = "";
							if(num>0){
								SearchHit[] hits4 = searchResp18.getHits().getHits();
								for(SearchHit hit4 : hits4){
									Object vrdname = hit4.getSource().get("VRD_NAME");
									if(vrdname!=null){
										vrd_name = vrd_name +","+ vrdname;
									}
								}
								vrd_name = vrd_name.substring(1);
								updateErrorMesg(source, "", index, v_tabName, vrd_name+",违法唯一性校验!", coid);
							}
						}
					}
				}
				
			}
		}
		//删除相应拼接属性表和拼接属性值表的数据
		deletePJSXZB(prepareSearch, bulkinsert, iclerkcode, v_clid, importseq, index, "pjsxb");
		deletePJSXZB(prepareSearch, bulkinsert, iclerkcode, v_clid, importseq, index, "pjsxzb");
		
		IdsQueryBuilder idsterms = new IdsQueryBuilder();
		idsterms.addIds(ids);
		SearchResponse searchResp1 = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.boolQuery()
				.filter(QueryBuilders.matchQuery("PROCESS_STATE", "1"))
				.filter(idsterms)).get();
		long num1 = searchResp1.getHits().getTotalHits();
		if(num1>0){
			Map<String,Object> source = new HashMap<String,Object>();
			BulkRequestBuilder bulk = client.prepareBulk();
			source.put("PROCESS_STATE", "2");
			SearchHit[] hits1 = searchResp1.getHits().getHits();
			for (SearchHit hit : hits1) {
				UpdateRequestBuilder prepareUpdate = client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", hit.getId());
				bulk.add(prepareUpdate);
			}
			bulk.get();
		}
		
	}else{
		Map<String,Object> source = new HashMap<String,Object>();
		for(String co_id : ids ){
			updateErrorMesg(source, "", index, v_tabName, "该人员没有分配组织",co_id);
		}
	}
	return model;	
	}
	/**
	 * 
	 * @param source
	 * @param v_ca_name
	 * @param index
	 * @param v_tabName
	 * @param errorMes
	 * @param co_ids
	 */
	public static void updateErrorMesg(Map source,String v_ca_name, String index, String v_tabName, String errorMes,String ...co_ids){
		if(client == null){
			client = ESUtil.getClient();
		}
		BulkRequestBuilder prepareBulk = client.prepareBulk();
		source.clear();
		source.put("PROCESS_STATE", "3");
		source.put("IMPORT_ERROR", "1");
		source.put("IMPORT_ERROR_COMMENT", v_ca_name+errorMes);
		for(String co_id : co_ids){
			prepareBulk.add(client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", co_id).setDoc(source));
		}
		prepareBulk.get();
	}
	
	/**
	 * 更新含有数字类型字段的TempYB表数据
	 * @param v_value 属性值
	 * @param v_valueround 属性值变量
	 * @param source 更新数据集合
	 * @param v_ca_codeid 属性名
	 * @param index 索引
	 * @param v_tabName 
	 * @param co_id
	 */
	public static void updateNumberTypetoTempYB(int roundindex,String v_value, String v_valueround, Map source, String v_ca_codeid, String index, String v_tabName, String co_id){
		if(ESUtil.existWordsOrNot(v_value)){
			v_valueround = ESUtil.getRoundNumber(v_value,roundindex);
			if(roundindex!=0){
				if(Double.valueOf(v_valueround)<1&&Double.valueOf(v_valueround)>0){
					v_valueround = "0"+v_valueround;
				}
			}
		}else{
			if(roundindex==0){
				v_valueround = "0";
			}else {
				v_valueround = "0.";
				for(int i=0;i<roundindex; i++){
					v_valueround = v_valueround+"0";
				}
			}
		}
		source.clear();
		source.put(v_ca_codeid, v_valueround);
		updateTempYB(index, v_tabName, co_id, source);
	}
	/**
	 * 更新TempYB表数据
	 * @param index 索引
	 * @param v_tabName 类型
	 * @param _id 数据唯一标识
	 * @param source 数据集合
	 */
	public static void updateTempYB(String index,String v_tabName,String _id,Map source){
		if(client == null){
			client = ESUtil.getClient();
		}
		client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", _id).setDoc(source).get();
	}
	/**
	 * 检查属性组合
	 * @param prepareSearch 查询操作的builder
	 * @param v_clid 分类id
	 * @param v_assemble_symble 分隔符
	 * @param v_assemble_attrid 属性名逗号拼接
	 * @param index 索引
	 * @param v_tabName 类型
	 * @param v_pjstr 中间值 
	 * @param v_property_val 拼接属性值
	 * @param v_co_id 数据唯一标识
	 * @param v_ca_codeid 属性组合目标字段名
	 */
	public static void checkAssembleAttrs(SearchRequestBuilder prepareSearch,String v_clid,String v_assemble_symble,
			String v_assemble_attrid,String index,String v_tabName,String v_property_val,String v_co_id,String v_ca_codeid){
		if(client == null){
			client = ESUtil.getClient();
		}
		String v_pjstr = null;
		SearchResponse searchResp14 = prepareSearch.setTypes("cl_attrassemble_mapping").setQuery(QueryBuilders.boolQuery()
				.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
				.filter(QueryBuilders.matchQuery("ASSEMBLE_TARGETID", v_ca_codeid))
				.mustNot(QueryBuilders.matchQuery("ASSEMBLE_TARGETID", ""))).get();
		long num = searchResp14.getHits().getTotalHits();
		if(num>0){
			//给v_assemble_symble赋值
			SearchHit[] hits4 = searchResp14.getHits().getHits();
			Object symble = hits4[0].getSource().get("ASSEMBLE_SYMBEL");
			v_assemble_symble  = symble==null?"":(String)symble;
			//得到拼接字段名以逗号隔开
			Object attrid = hits4[0].getSource().get("ASSEMBLE_ATTRID");
			v_assemble_attrid = attrid == null?"":(String)attrid;
			String[]  attrids = v_assemble_attrid.split(",");
			if(attrids.length>0){
				Map source = new HashMap<>();
				//属性值拼接逻辑
				for(String v_property : attrids){
					//更新时间戳
					SearchResponse searchResp15 = updateTimeStamp(prepareSearch, source, index, v_tabName, v_co_id);
					SearchHit[] hits5 = searchResp15.getHits().getHits();
					Object pjstr = hits5[0].getSource().get(v_property);
					v_pjstr = pjstr == null? "":(String)pjstr;
					if(!"".equals(v_pjstr)){
						v_pjstr = v_pjstr + v_assemble_symble;
						v_property_val  = v_property_val + v_pjstr;
					}
				}
				//如果有特殊字符去除最后的特殊字符
				String laststr = v_property_val.substring(v_property_val.length()-1);
				if(v_assemble_symble.equals(laststr)){
					v_property_val = v_property_val.substring(0, v_property_val.length()-1);
				}
				//更新属性值
				source.clear();
				source.put(v_ca_codeid, v_property_val);
				client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb", v_co_id).setDoc(source).get();
				v_property_val = "";
			}
			
		}
	}
	/**
	 * 更新时间戳
	 * @param source  集合
	 * @param index 索引名称
	 * @param v_tabName 索引类型（表名）
	 * @param _id 数据id
	 */
	public static SearchResponse updateTimeStamp(SearchRequestBuilder prepareSearch,Map source,String index,String v_tabName,String _id){
		if(client == null){
			client = ESUtil.getClient();
		}
		SearchResponse searchResp = prepareSearch.setTypes(v_tabName.toLowerCase()+"_tempyb").setQuery(QueryBuilders.boolQuery()
				.filter(QueryBuilders.matchQuery("CO_ID", _id))
				.mustNot(QueryBuilders.matchQuery("PROCESS_TIME", ""))).get();
		long totalHits = searchResp.getHits().getTotalHits();
		if(totalHits>0){
			source.clear();
			source.put("PROCESS_TIME", new SimpleDateFormat("yyyy-MM-dd HH:mi:ss").format(new Date()));
			//考虑要不要批量处理
			client.prepareUpdate(index,v_tabName.toLowerCase(),_id).setDoc(source).get();
		}
		return searchResp;
	}
	/**
	 * 删除拼接属性值表
	 * @param prepareSearch 
	 * @param bulkinsert 
	 * @param iclerkcode 
	 * @param v_clid 
	 * @param importseq 
	 * @param index
	 */
	public static void deletePJSXZB(SearchRequestBuilder prepareSearch,BulkRequestBuilder bulkinsert, String iclerkcode, String v_clid, String importseq, String index,String tabName){
		if(client == null){
			client = ESUtil.getClient();
		}
		if(bulkinsert!= null){
			bulkinsert = client.prepareBulk();
		}
		SearchResponse searchResp10 = prepareSearch.setTypes(tabName).setQuery(QueryBuilders.boolQuery()
				.filter(QueryBuilders.matchQuery("CLERKCODE", iclerkcode))
				.filter(QueryBuilders.matchQuery("CL_ID", v_clid))
				.filter(QueryBuilders.matchQuery("PC", importseq)))
				.get();
		SearchHit[] hits2 = searchResp10.getHits().getHits();
		for(SearchHit hit2 : hits2){
			bulkinsert.add(client.prepareDelete(index, tabName, hit2.getId()));
		}
		bulkinsert.get();
	}
	/**
	 * 更新大小量纲错误数据
	 * @param source 更新或者插入所需数据集合
	 * @param index 数据索引
	 * @param v_tabName 数据类型
	 * @param _id 数据唯一标识
	 */
	public static void updateErrorState(Map source,String index,String v_tabName,String _id){
		if(client == null){
			client = ESUtil.getClient();
		}
		source.clear();
		source.put("PROCESS_STATE", "3");
		source.put("IMPORT_ERROR", "1");
		source.put("IMPORT_ERROR_COMMENT", "大小量纲不正确，请按标准进行填写");
		client.prepareUpdate(index, v_tabName.toLowerCase()+"_tempyb",_id).setDoc(source).get();
		
	}
	/**
	 * 
	 * @param v_db2  大小量纲拆分之后数组长度
	 * @param source 更新或者插入数据所需数据集合
	 * @param importseq 导入序列号
	 * @param iclerkcode 人员编码
	 * @param v_clid 分类id
	 * @param v_matdxlg 大小量纲
	 * @param v_assemble_symble 大小量纲分割字符
	 * @param index  数据索引
	 * @param v_tabName 数据类型
	 * @param _id 数据唯一标识
	 * @param bulkinsert 批操作对象
	 */
	public static  void updateAssembleColAndVal(int v_db2,Map source,String importseq,String iclerkcode,String v_clid,
			String v_matdxlg,String v_assemble_symble,String index,String v_tabName,String _id,BulkRequestBuilder bulkinsert){
		String	v_pid = null,//id
				v_pjsx = null,//拼接属性
				v_pjsxz = null;//拼接属性值
		if(client == null){
			client = ESUtil.getClient();
		}
		for(int i = 0; i<v_db2 ;i++){
			source.clear();
			source.put("PC", importseq);//导入序列号
			source.put("CLERKCODE", iclerkcode);//员工编码
			source.put("CL_ID", v_clid);//分类id
			source.put("ID", String.valueOf(i));
			source.put("PJSXZ", v_matdxlg.split(v_assemble_symble)[i]);//拼接属性值
			bulkinsert.add(client.prepareIndex(index, v_tabName.toLowerCase(), String.valueOf(i)).setSource(source));
		}
		bulkinsert.get();//批量
		HasChildQueryBuilder innerHit = JoinQueryBuilders.hasChildQuery(
				"pjsxb",//子文档的类型(拼接属性表)
				null, //子文档的搜索条件
				ScoreMode.None) //不进行评分
				.innerHit(new InnerHitBuilder());//只用于嵌套查询有内部join关系的以便能把父子文档都能查询出来
		SearchResponse searchResp9 = client.prepareSearch(index).setTypes("pjsxzb").setQuery(innerHit).get();
		SearchHit[] hits2 = searchResp9.getHits().getHits();
		for(SearchHit hit2 : hits2){
			//现将父文档查出来
			v_pjsxz = hit2.getSource().get("PJSXZ")==null?"":(String)hit2.getSource().get("PJSXZ");
			Map<String, SearchHits> innerHits = hit2.getInnerHits();//得到子文档
			for(String key : innerHits.keySet()){
				Iterator<SearchHit> iterator = innerHits.get(key).iterator();
				while(iterator.hasNext()){
					SearchHit next = iterator.next();
					v_pid = next.getSource().get("ID") == null?"":(String)next.getSource().get("ID"); 
					v_pjsx = next.getSource().get("pjsx") == null?"":(String)next.getSource().get("pjsx");
				}
				
			}
			source.clear();
			source.put(v_pjsx,v_pjsxz);
			client.prepareUpdate(index, v_tabName.toLowerCase(),_id).setDoc(source);
		}
		bulkinsert.get();
	}
	
}
