package com.thit.elasticsearch.test;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

public class MongodbConnect {
	static String ADMIN_DATABASE_NAME = "admin";
	public static void main(String[] args) throws InterruptedException, ParseException, UnknownHostException {
//		
//		MongoClientOptions mco = MongoClientOptions.builder().connectTimeout(15000).socketTimeout(60000).build();
//        @SuppressWarnings("deprecation")
////		Mongo mongo = new MongoClient(new ServerAddress(Network.getLocalHost().getHostName(), ports[0]), mco);
//        
//        DB mongoAdminDB = mongo.getDB(ADMIN_DATABASE_NAME);
//
//        cr = mongoAdminDB.command(new BasicDBObject("isMaster", 1));
//        logger.debug("isMaster: " + cr);
//		Map<String,Object> data4 = new HashMap<String,Object>();
		DBObject data4 = new BasicDBObject();
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		date = format.parse(format.format(new Date()));
		Calendar cla = Calendar.getInstance();
		cla.setTime(date);
		cla.add(Calendar.HOUR_OF_DAY,8);
		data4.put("start_time",cla.getTime().getTime());
		
//		Mongo mongo = new Mongo("192.168.100.103", 10111);
		Mongo mongo = new MongoClient("127.0.0.1",27017);
		//连接名为xiaodb的数据库,假如数据库不存在的话,mongodb会自动建立
		DB db = mongo.getDB("test");
		//users表名
		DBCollection dbColl = db.getCollection("user");
		for(int i = 5; i < 10;i++){
	
		data4.put("_id",i);
		data4.put("cust_Id",i+"");
		data4.put("is_Show",i);
		DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date1 = null;
		date = format.parse(format.format(new Date()));
		Calendar cla1 = Calendar.getInstance();
		cla1.setTime(date);
		cla1.add(Calendar.HOUR_OF_DAY,8);
		data4.put("start_time",cla.getTime());
		System.out.println(cla.getTime().toString());
		dbColl.insert(data4);
		Thread.sleep(1000);
	}
	}
}
