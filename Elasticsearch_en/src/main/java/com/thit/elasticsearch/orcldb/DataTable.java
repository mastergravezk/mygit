package com.thit.elasticsearch.orcldb;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class DataTable {

	ArrayList<Object[]> datas;
	LinkedHashMap<String, Integer> columns;
	
	
	public ArrayList<Object[]> getDatas() {
		return datas;
	}

	public void setDatas(ArrayList<Object[]> datas) {
		this.datas = datas;
	}

	public LinkedHashMap<String, Integer> getColumns() {
		return columns;
	}

	public void setColumns(LinkedHashMap<String, Integer> columns) {
		this.columns = columns;
	}

	public DataTable(ResultSet rs) {
		try {
			datas = new ArrayList<Object[]>();
			columns = new java.util.LinkedHashMap<String, Integer>();
			ResultSetMetaData rsmd = rs.getMetaData();
			int count = rsmd.getColumnCount();
			for (int i = 1; i <= count; i++) {
				columns.put(rsmd.getColumnName(i), i - 1);
			}
			while (rs.next()) {
				Object[] row = new Object[count];
				datas.add(row);
				for (int i = 1; i <= count; i++) {
					row[i - 1] = rs.getObject(i);
					//System.out.println("MDM DEBUG:" + row[i - 1].toString());
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Object[] getRowDatas(int index) {
		return datas.get(index);
	}

	public String getValue(int rowIndex, String columnName) {
		Object[] rows = datas.get(rowIndex);
		if(!columns.containsKey(columnName))
			throw new RuntimeException("没有找到数据" + columnName);
		Object value = rows[columns.get(columnName)];
		if(value == null)
		{
			return null;
		}
		else {
			return value.toString();	
		}
	}

	public DataRow[] getRows() {
		DataRow[] result = new DataRow[datas.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = new DataRow(this, i);
		}
		return result;
	}
	
	public int GetDataSize()
	{
		return datas.size();
	}

}
