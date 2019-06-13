package com.thit.elasticsearch.quartz;

import java.io.IOException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.thit.elasticsearch.common.ESUtil;

public class SyncDBtoESTimer {
	private  Client client;
	
	public SyncDBtoESTimer() {
		super();
	}
	//思路：将es中的主键字段取出来到数据库里边查询没有同步的数据然后将查出来的未同步数据同步到es，如果没有主键就重新把数据都同步一遍。
	//待修改
	public void syncDatas(){
//		if(client == null){
//			client = ESUtil.getClient();
//		}
		String driver = null,
				url = null,
				user = null,
				password = null,
				tables = null,
				ids = null; 
		java.util.Properties pro = new java.util.Properties();
		try {
			pro.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("Database.properties"));
//			driver = pro.getProperty("OracleDriver");
//			url = pro.getProperty("ESDB_Url");// 127.0.0.1是本机地址，XE是精简版Oracle的默认数据库名
//			user = pro.getProperty("ESDB_User");// 用户名,系统默认的账户名
//			password = pro.getProperty("ESDB_Password");// 你安装时选设置的密码
			tables = pro.getProperty("Tables");//要监听同步的表
//			ids = pro.getProperty("PK");//要监听同步的表
			//表名拼接
			String[] tableNames = tables.split(";");
			//主键拼接
//			String[] splitids = ids.split(";");
			
			for(int i=0;i<tableNames.length;i++){
				//没有主键的表
				ESUtil.initDataToES(tableNames[i],pro);
//				if("".equals(splitids)){
//				}else{//有主键
//					SearchResponse searchResponse = client.prepareSearch("mdmindex").setTypes(tableNames[i].toLowerCase())
//							.setQuery(QueryBuilders.matchAllQuery()).get(new TimeValue(60 * 1000));
//					SearchHit[] hits = searchResponse.getHits().getHits();
//					
//					for (SearchHit hit : hits) {
//						hit.getId();
//					}
//					
//				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
