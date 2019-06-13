package com.thit.elasticsearch.query;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;

import com.thit.elasticsearch.common.ModelUtil;
import com.xicrm.business.util.TXIBizSmallUtil;
import com.xicrm.model.TXIModel;

public class MDMSYSAggreationQueryHandler extends AbstractAggationsQueryHandler {
	
	
	
	public MDMSYSAggreationQueryHandler(AggregationBuilder aggbuilder, Client client) {
		super(aggbuilder, client);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public MDMESQueryContext handle(MDMESQueryContext context, QueryBuilder builder,
			AggregationBuilder aggs) {
		
		String index = context.getIndex();
		String type = context.getType();
		TXIModel model = context.getModel();
		
		String detailNum = context.getDetailNum();
		PageInfo pageInfo = context.getPageInfo();
		String pageFlag = pageInfo.getPageFlag();
		String pageInfoControl = pageInfo.getPageInfoControl();
		int pageNo = pageInfo.getPageNo();
		String pageSize = pageInfo.getPageSize();
		
		PageProcessor processor = new PageProcessor();
		
		SearchResponse searchResponse = client.prepareSearch(index.toLowerCase())
				.setTypes(type.toLowerCase())
				//查询和聚合是并列的
				.setQuery(QueryBuilders.constantScoreQuery(builder))//不进行分值计算
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
			model.getDetail(detailNum).clear();
			model = TXIBizSmallUtil.setPageNo(pageInfoControl, 1, Integer.valueOf(pageSize), Integer.valueOf(0),
					model);
		}else{
			BoolQueryBuilder filter1 = new BoolQueryBuilder();
			SearchResponse searchResponse1 = client.prepareSearch(index.toLowerCase())
					.setTypes(type.toLowerCase())
					.addSort("CO_MANUALCODE.keyword", SortOrder.ASC)//按照哪个字段排序 ，keyword表示不进行分词
					.setScroll(new TimeValue(60000))
					.setQuery(filter1.filter(QueryBuilders.termsQuery("CO_MANUALCODE", codes)).filter(QueryBuilders.termQuery("CO_VALID", "1")))//不进行分值计算
					.setSize(Integer.valueOf(pageSize))
					.setTimeout(new TimeValue(60000))
					.execute()
					.actionGet();
			// 获得总页数
			int totalPage = processor.getTotalPage(totalnum, pageInfo);
			// 根据翻页格式确定取哪一页得数据，得把这20条数据的范围确定
			int ltnum = -1, gtnum = -1;
			// 载入时的标记为空首页的标记也为空
			int[] range = processor.setSubidRange(ltnum, gtnum, totalnum, totalPage, pageInfo);
			ltnum = range[0];
			gtnum = range[1];
			// 处理查询结果
			// 从model中得到页记录信息
			Hashtable detail0 = (Hashtable) model.getDetails().get(detailNum);
			if(detail0==null){
				model.setDetail(detailNum,new Hashtable());
			}
			int subid = 0;
			model.getDetail(detailNum).clear();
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
						submodel = ModelUtil.getARdFromHashES(datas, submodel);
						model.addDetail(detailNum, String.valueOf(subid), submodel);
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
			model = TXIBizSmallUtil.setPageNo(pageInfoControl, pageNo, Integer.valueOf(pageSize), Integer.valueOf(totalnum),
					model);
		}
		
		context.setModel(model);
		return context;
	}

}
