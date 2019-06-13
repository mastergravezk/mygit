package com.thit.elasticsearch.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.thit.elasticsearch.orcldb.DbOperation;

/**
 * @description 创建索引及数据同步的功能类
 * @author zk
 * 
 */
public class ESInitializer {
	private static Client client;
	private static Properties pro;
	
	public static Client initClient(){
		
		if(client==null){
			Settings settings = Settings.builder()
					.put("cluster.name", "mdmjt")//指定集群的名称
			        .put("client.transport.sniff", true)//如果有节点加入集群将自动检测加入
			        .build(); 
			try {
				client = new PreBuiltTransportClient(settings)
						.addTransportAddress(new InetSocketTransportAddress(
								InetAddress.getByName(
								getProperties().getProperty("ESServerIP")),
								Integer.valueOf(getProperties().getProperty("ESServerPort"))
								));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return client;
	}
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public static Properties getProperties(){
		java.util.Properties pro = new java.util.Properties();
		try {
			pro.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("test.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pro;
	}
	/**
	 * 关闭索引（当更新settings后可以再打开）
	 */
	public static boolean closeIndex(String... index){
		boolean flag = false;
		CloseIndexResponse actionGet = client.admin().indices().prepareClose(index).execute().actionGet();
		if(actionGet.isAcknowledged()){
			flag = true;
		}
		return flag;
	}
	
	/**
	 * 打开索引
	 * @param index
	 * @return
	 */
	public static boolean openIndex(String... index){
		boolean flag = false;
		OpenIndexResponse actionGet = client.admin().indices().prepareOpen(index).execute().actionGet();
		if(actionGet.isAcknowledged()){
			flag = true;
		}
		return flag;
	}
	/**
	 * 自定义通过json创建索引及定义settings
	 * @param json
	 * @param index
	 */
	public static boolean createMySelfIndex(String json,String index){
		client = initClient();
		Settings settings = Settings.builder()
				.loadFromSource(json, XContentType.JSON).build();
		boolean flag = false;
		try {
			CreateIndexResponse res = client.admin().indices().prepareCreate(index).setSettings(settings).execute().get();
			if(res.isAcknowledged()&&res.isShardsAcked())
				flag = true;
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;

	}
	/**
	 * 通过map方式创建索引
	 * 
	 */
	public static boolean createDefultindex(String index,Map source){
		client  = initClient();
		System.out.println("初始化client");
		Settings settings = Settings.builder().put(source).build();
		boolean flag = false;
		try {
			CreateIndexResponse res = client.admin().indices().prepareCreate(index).setSettings(settings).execute().get();
			if(res.isAcknowledged()&&res.isShardsAcked())
				flag = true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;
	}
	/**
	 * 删除索引
	 * @param indices
	 * @return
	 */
	public static boolean deleteIndex(String ...indices){
		client = initClient();
		DeleteIndexResponse res = client.admin().indices().prepareDelete(indices).get();
		return res.isAcknowledged();
			
	}
	/**
	 * 创建映射字段
	 * @param _index
	 * @param _type
	 * @param builder
	 * @return
	 */
	public static boolean createMappings(String _index,String _type, XContentBuilder builder){
		client = initClient();
		PutMappingRequest mapingReq = Requests.putMappingRequest(_index.toLowerCase()).type(_type.toLowerCase()).source(builder);
		boolean flag = false;
		try {
			PutMappingResponse res = client.admin().indices().putMapping(mapingReq).get();
			flag = res.isAcknowledged();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;
	}
	/**
	 * 初始化t_mat表数据
	 * @throws IOException
	 */
	public static void initT_MATDataToES() throws IOException{
		client = initClient();
		// 如果是时范围的可以
//		String[] tablenames = {"t_mat"};
//		String sql1 = "select distinct tablename from CL_CATALOG where tablename not in('T_MAT','T_VENDOR','T_CUSTOMER')";
//		List tabns = DbOperation.executeArrayList(sql1);
//		tabns.add("t_mat_temp");
//		tabns.add("t_customer_temp");
//		tabns.add("t_vendor_temp");
//		Object[] tablenames = tabns.toArray();
//		DataTable dt = DbOperation.executeDataTable(sql);
//		ArrayList<Object[]> datas = dt.getDatas();
//		System.out.println(datas.size());
		BulkRequestBuilder prepareBulk = client.prepareBulk();
		//将string类型的字段转化成date类型
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		Date time = null;
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		PreparedStatement pre =null;
		ResultSet re = null;
		Connection con = null;
		try {
//			pro = DbOperation.pro;
			pro = getProperties();
			String driver = pro.getProperty("OracleDriver");
			String url = pro.getProperty("ESDB_Url");// 127.0.0.1是本机地址，XE是精简版Oracle的默认数据库名
			String user = pro.getProperty("ESDB_User");// 用户名,系统默认的账户名
			String password = pro.getProperty("ESDB_Password");// 你安装时选设置的密码
			String limitsize = pro.getProperty("LimitSize");
			String tablename = pro.getProperty("Table");
			String pk = pro.getProperty("PK");
			int size = Integer.valueOf(limitsize);
//			String ltnum = pro.getProperty("ltnum");
//			String gtnum = pro.getProperty("gtnum");
			String sql = pro.getProperty("ESsql");
//			String sql = "select * from "+tablename+" where co_id>"+ltnum+ " and co_id <" +gtnum;
//			+" where co_id>660000 and co_id<780001";
			
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
					if("CO_ID".equals(col)&&col.equalsIgnoreCase(pk)){
						co_id = String.valueOf(val);
					}else if("ID".equals(col)&&col.equalsIgnoreCase(pk)){
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
			//以前是if(next)
			if(num>0){
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
	/**
	 * 初始化除t_mat以外的表数据
	 * @throws IOException
	 */
	
	public static void  initDataToES() throws IOException{
		client = initClient();
		// 如果是时范围的可以
//		String[] tablenames = {"similarity_batch_temp"};
//		String sql1 = "select distinct tablename from CL_CATALOG where tablename not in('T_MAT','T_SYSINFO')";
		String sql2 = "select distinct tablename from CL_CATALOG where tablename<>'T_MAT' and tablename is not null";
		List tabns = DbOperation.executeArrayList(sql2);
		tabns.add("t_mat_temp");
		tabns.add("t_customer_temp");
		tabns.add("t_vendor_temp");
		Object[] tablenames = tabns.toArray();
		
		for(Object tablename : tablenames){
			String sql = "select * from "+tablename;
			System.out.println(sql);
//		DataTable dt = DbOperation.executeDataTable(sql);
//		ArrayList<Object[]> datas = dt.getDatas();
//		System.out.println(datas.size());
			BulkRequestBuilder prepareBulk = client.prepareBulk();
			//将string类型的字段转化成date类型
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		Date time = null;
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
			PreparedStatement pre =null;
			ResultSet re = null;
			Connection con = null;
			try {
				pro = DbOperation.pro;
				String driver = pro.getProperty("OracleDriver");
				String url = pro.getProperty("ESDB_Url");// 127.0.0.1是本机地址，XE是精简版Oracle的默认数据库名
				String user = pro.getProperty("ESDB_User");// 用户名,系统默认的账户名
				String password = pro.getProperty("ESDB_Password");// 你安装时选设置的密码
				String limitsize = pro.getProperty("LimitSize");
				int size = Integer.valueOf(limitsize);
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
//					System.out.println(map);
					prepareBulk.add(client.prepareIndex("mdmindex",tablename.toString().toLowerCase(),co_id).setSource(map));
					num++;
					if(num>size){
						BulkResponse bulkResponse = prepareBulk.get(new TimeValue(600*1000));
						System.out.println("批量成功插入"+tablename+":"+String.valueOf(num)+"条");
						if(bulkResponse.hasFailures()){
//						TXISystem.log.error("错误数据详细信息：", bulkResponse.buildFailureMessage());
							System.err.println("错误数据详细信息："+bulkResponse.buildFailureMessage());
						}	
						num=0;
					}
				}
				if(num>0){
					BulkResponse bulkResponse = prepareBulk.setTimeout(new TimeValue(300*1000)).get();
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
		
	}
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
	public static void main(String[] args) throws ParseException, IOException {
//		client = initClient();
//		client.admin().indices().prepareDelete("mdmindex").get();
//		Settings build = Settings.builder().put("number_of_shards", 1)
//				.put("number_of_replicas", 1)
//				.build();
//		client.admin().indices().prepareCreate("mdmindex")
//		.setSettings(build)
//		.execute().actionGet();
//		initDataToES();
		initT_MATDataToES();
//		Properties pro = DbOperation.pro;
//		System.out.println(pro.getProperty("ESServerIP"));
//		String driver = pro.getProperty("OracleDriver");
//		String url = pro.getProperty("ESDB_Url");
//		String username = pro.getProperty("ESDB_User");
//		String passw = pro.getProperty("ESDB_Password");
//		String sql = "select c.EXATTRIBUTEA,c.WORK_ITEM_IID,c.PROCESS_INS_ID,c.ACTIVITY_INS_ID from wf_ins_work_item c where rownum<2";
//		try {
//			Class.forName(driver);
//			Connection con = DriverManager.getConnection(url, username, passw);
//			PreparedStatement pre = con.prepareStatement(sql);
//			ResultSet result = pre.executeQuery();
//			ResultSetMetaData metaData = result.getMetaData();
//			int count = metaData.getColumnCount();
//			while(result.next()){
//				for(int i = 1 ; i<=count ; i++){
//					System.out.println("字段类型："+metaData.getColumnType(i));
//					System.out.println("字段类型名称："+metaData.getColumnTypeName(i));
//					System.out.println("字段展示大小："+metaData.getColumnDisplaySize(i));
//					System.out.println("字段标签："+metaData.getColumnLabel(i));
//					System.out.println(metaData.getColumnName(i)+"="+result.getString(i));
//				}
//			}
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
			
		
		
		
		//建立父子关系,一个子文档只能对应一个父文档
//		String tabName = "t_customer";
//		XContentBuilder builder = XContentFactory.jsonBuilder()
//				.startObject()
//	            .startObject(tabName)
//	            .startObject("_parent").field("type", "similarity_batch_temp").endObject()
//	            .startObject("properties")
//	            .endObject()
//	            .endObject()
//	            .endObject();
//		PutMappingRequest mappingReq_child = Requests.putMappingRequest("mdmindex")
//				.type(tabName)
//				.source(builder);
//				
//		try {
//			client.admin().indices().putMapping(mappingReq_child).get();
//		} catch (InterruptedException | ExecutionException e) {
//			e.printStackTrace();
//		}
//		Map source = new HashMap<>();
//		source.put("CO_FREEZE_DESC", "zk测试冻结");
//		source.put("CO_FREEZE", "1");
//		UpdateResponse updateResponse = client.prepareUpdate("mdmindex", "t_mat","8034").setDoc(source).get();
//		System.out.println(updateResponse.getResult().toString());
	
//		SearchResponse searchResponse = client.prepareSearch("mdmindex")
//				.setTypes("similarity_batch_temp")
//				.setScroll(new TimeValue(20000))
//				.setSize(10)
//		.setQuery(QueryBuilders.matchAllQuery()).get();
//		SearchHit[] hits = searchResponse.getHits().getHits();
//		BulkRequestBuilder deleteBulk = client.prepareBulk();
//		for (SearchHit hit : hits) {
//			String id = hit.getId();
//			deleteBulk.add(client.prepareDelete("mdmindex", "similarity_batch_temp", id));
//		}
//		BulkResponse bulkResponse = deleteBulk.get();
		
	}
	
}
