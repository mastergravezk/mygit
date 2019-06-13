package com.thit.elasticsearch.query;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import com.thit.elasticsearch.common.ModelUtil;
import com.xicrm.business.util.TXIBizSmallUtil;
import com.xicrm.model.TXIModel;


public class MDMQueryHandler implements QueryHandler{
	
	private Client client;
	
//	private String field;
	
	
	public MDMQueryHandler(Client client) {
		super();
		this.client = client;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}
	
	@Override
	public MDMESQueryContext handle(MDMESQueryContext context, QueryBuilder builder) {
		// TODO Auto-generated method stub
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
				.addSort(context.getSortField(), SortOrder.ASC)//按照哪个字段排序
				.setScroll(new TimeValue(60000))
				.setQuery(QueryBuilders.constantScoreQuery(builder))//不进行分值计算
				.setSize(Integer.valueOf(pageSize))
				.setTimeout(new TimeValue(60000))
				.execute()
				.actionGet();
		// 符合条件的总记录数
		int totalnum = (int) searchResponse.getHits().getTotalHits();
		System.out.println("查询命中数:                       " + String.valueOf(totalnum));
		// 获得总页数
		int totalPage = processor.getTotalPage(totalnum, pageInfo);
		// 根据翻页格式确定取哪一页得数据，得把这20条数据的范围确定
		int ltnum = -1, gtnum = -1;
		// 载入时的标记为空首页的标记也为空
		int[] range = processor.setSubidRange(ltnum, gtnum, totalnum, totalPage,pageInfo);
		ltnum = range[0];
		gtnum = range[1];
		System.out.println("查询起始数:                       " + String.valueOf(ltnum));
		System.out.println("查询结束数:                       " + String.valueOf(gtnum));
		// 处理查询结果
		// 从model中得到页记录信息
		Hashtable detail0 = (Hashtable) model.getDetails().get(detailNum);
		if(detail0==null){
			model.setDetail(detailNum,new Hashtable());
		}
		int subid = 0;
		
		model.getDetail(detailNum).clear();
//		detail0.clear();
		System.out.println(model.getDetails().get(detailNum));
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
					submodel = ModelUtil.getARdFromHashES(datas, submodel);
					model.addDetail(detailNum, String.valueOf(subid), submodel);
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
		model = TXIBizSmallUtil.setPageNo(pageInfoControl, pageNo, Integer.valueOf(pageSize), Integer.valueOf(totalnum),
				model);
		// 关闭链接
		
		System.out.println("......ES查询结束");
		context.setModel(model);
		return context;
		
	}
	


	
	
}
