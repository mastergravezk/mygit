package com.thit.elasticsearch.orcldb;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public class DbOperation {
	static Connection con;
	static int errorCount = 0;

	public static String url = null;
	public static String user = null;
	public static String password = null;
	public static String driver = null;
	public static java.util.Properties pro = new java.util.Properties();
	
	static{
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			pro.load(cl.getResourceAsStream("application.properties"));
			String path = (String)pro.getProperty("databasepath");
			pro.clear();
			pro.load(cl.getResourceAsStream(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	public static Properties getPro(){
//		setPro();
//		return pro;
//	}
	static void init() throws Exception {
		if (con != null && !con.isClosed())
			con.close();
		if (url == null) {
			driver = pro.getProperty("OracleDriver");
			url = pro.getProperty("ESDB_Url");// 127.0.0.1是本机地址，XE是精简版Oracle的默认数据库名
			user = pro.getProperty("ESDB_User");// 用户名,系统默认的账户名
			password = pro.getProperty("ESDB_Password");// 你安装时选设置的密码
			System.out.println(url);
			System.out.println(user);
			System.out.println(password);
		}
		//Log.info("开始尝试连接数据库！");
		Class.forName(driver);// 加载Oracle驱动程序
		con = DriverManager.getConnection(url, user, password);// 获取连接
		//Log.info("连接成功！");
	}

  
	static void open() throws Exception {
		if (con == null || con.isClosed()) {
			init();
		}
	}
	public static Object executeObject(String sql, Object... args) {
		PreparedStatement pre = null;
		ResultSet result = null;
		try {
			open();
			pre = Format(sql, args);
			result = pre.executeQuery();
			while (result.next()) {
				return result.getObject(1);
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (result != null)
					result.close();
				if (pre != null)
					pre.close();
			} catch (Exception e) {
				//Log.error(e);
			}
		}
	}
	static PreparedStatement Format(String sql, Object... args) throws Exception {
		//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			PreparedStatement pre = con.prepareStatement(sql);
			int i = 1;
			for (Object value : args) {
				if (value instanceof java.util.Date) {					
					pre.setTimestamp(i, new java.sql.Timestamp(((java.util.Date) value).getTime()));
				} else {
					pre.setObject(i, value);
				}
				i++;
			}
			return pre;
		} catch (SQLException e) {
			errorCount++;
			if (errorCount > 10) {
				init();
			}
			throw e;
		}

	}
	public static Connection getConnection() {
		try {
			open();
		} catch (Exception e) {
			// TODO 自动生成的 catch 块
			//Log.error(e);
		}
		return con;
	}
	public static void insert(String tableName, LinkedHashMap<String, Object> dict) {
		PreparedStatement pre = null;
		dict = clearDict(dict);
		try {
			open();
			ArrayList<String> list = new ArrayList<String>();
			for (@SuppressWarnings("unused") String col : dict.keySet()) {
				list.add("?");
			}
			String cols = StringJoin.joinKey(dict.keySet(), ",");
//			TXISystem.log.info("字段名拼接：", cols);
			String fakeValues = StringJoin.joinVal(list, ",");
//			TXISystem.log.info("字段名拼接：", cols);
			String sql = "insert into " + tableName + " (" + cols + ") values (" + fakeValues + ")"; 
//			if(TXISystem.config.getDebugSwitch()){
//				TXISystem.log.info("sql语句为:", sql);
//			}
			Object[] values = dict.values().toArray();
			pre = Format(sql,values);
			pre.executeUpdate();
			//String sql1 = "update "+tableName+" set sysapplyid =";
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (pre != null)
					pre.close();
			} catch (Exception e) {
				//Log.error(e);
			}
		}
	}
	
	public static LinkedHashMap<String, Object> clearDict(LinkedHashMap<String, Object> dict)
	{
		ArrayList<String> list = new ArrayList<String>();
		for (String col : dict.keySet()) {
			list.add(col);
		}
		for(String key : list)
		{
			if(dict.get(key) == null || dict.get(key).toString().trim().equals(""))
			{
				dict.remove(key);
			}
		}
		return dict;
	}
	public static LinkedHashMap<String, Object> executelinkMap(String sql){
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		PreparedStatement pre =null;
		ResultSet re = null;
		try {
			open();
			pre = con.prepareStatement(sql);
			re = pre.executeQuery();
			ResultSetMetaData metaData = re.getMetaData();
			int count = metaData.getColumnCount();
			while(re.next()){
				for(int i=1;i<=count;i++){
					map.put(metaData.getColumnName(i), re.getObject(i));
				}
			}
			return map;
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
	public static String[] getColumnNames(String tableName){
		PreparedStatement pre = null;
		ResultSet res = null;
		String[] columns = null;
		try {
			pre = con.prepareStatement("select * from "+tableName+" where rownum=1");
			res = pre.executeQuery();
			ResultSetMetaData metaData = res.getMetaData();
			int columnCount = metaData.getColumnCount();
			columns = new String[columnCount];
			for(int i = 1;i<=columnCount;i++){
				columns[i-1] = metaData.getColumnName(i);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return columns;
	}
	public static int executeUpdate(String sql, Object... args) {
		PreparedStatement pre = null;
		try {
			open();
			pre = Format(sql, args);
			return pre.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (pre != null)
					pre.close();
			} catch (Exception e) {
				//Log.error(e);
			}
		}
	}
	//调用ts_keygenerator存储过对TS_FLOWACTIVITIES表d
	public static int executeProdure(String tablename,int i_addstep) {
		CallableStatement stmt=null;
		String sql=null;
		
		try {
			sql="{call ts_keygenerator(?,?,?)}";
			open();
			stmt = con.prepareCall(sql);
			stmt.setString(1, tablename);
			stmt.setInt(2, i_addstep);
			stmt.registerOutParameter(3, Types.INTEGER);
			stmt.executeQuery();
			//pre = Format(sql, args);
			return stmt.getInt(3);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (Exception e) {
				//Log.error(e);
			}
		}
	}
	public static List executeArrayList(String sql, Object... args) {
		PreparedStatement pre = null;
		ResultSet result = null;
		//HashMap<String,Object> map = new HashMap<String,Object>();
		List<String> list = new ArrayList<String>();
		try {
			open();
			pre = Format(sql, args);
			result = pre.executeQuery();
			
			while (result.next()) {
				 list.add(result.getString(1));
//				 list.add(result.getString(2));
//				 list.add(result.getString(3));
			}
			return list;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (result != null)
					result.close();
				if (pre != null)
					pre.close();
			} catch (Exception e) {
				//Log.error(e);
			}
		}
	}
	public static List executeCO_IDList(String sql, Object... args) {
		PreparedStatement pre = null;
		ResultSet result = null;
		//HashMap<String,Object> map = new HashMap<String,Object>();
		List<String> list = new ArrayList<String>();
		try {
			open();
			pre = Format(sql, args);
			result = pre.executeQuery();
			
			while (result.next()) {
				 list.add(result.getString(1));
			}
			return list;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (result != null)
					result.close();
				if (pre != null)
					pre.close();
			} catch (Exception e) {
				//Log.error(e);
			}
		}
	}
	public static DataTable executeDataTable(String sql) {
		PreparedStatement pre = null;
		ResultSet result = null;
		try {
			open();
			pre = con.prepareStatement(sql);
			result = pre.executeQuery();
			return new DataTable(result);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (result != null)
					result.close();
				if (pre != null)
					pre.close();
			} catch (Exception e) {
				//Log.error(e);
			}
		}
	}
	public static ResultSet executeQuery(String sql, Object... args) {
		PreparedStatement pre = null;
		int int1=-1;
		try {
			open();
			pre = Format(sql, args);
			return pre.executeQuery();
			
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (pre != null)
					pre.close();
			} catch (Exception e) {
				//Log.error(e);
			}
		}
		
	}

	
	public static boolean remove(String sql) {
		PreparedStatement pre = null;
		boolean flag = false;
		try {
			open();
			pre = con.prepareStatement(sql);
			flag = pre.execute(sql);
//			flag = pre.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				if (pre != null)
					pre.close();
			} catch (Exception e) {
				//Log.error(e);
			}
		}
		return flag;
	}
	//public static HashMap 
	public static void close() {

		try {
			if (con != null)
				con.close();
			con = null;
			System.out.println("数据库连接已关闭！");
		} catch (SQLException e) {
			// TODO 自动生成的 catch 块
			//Log.error(e);
		}
	}
}
