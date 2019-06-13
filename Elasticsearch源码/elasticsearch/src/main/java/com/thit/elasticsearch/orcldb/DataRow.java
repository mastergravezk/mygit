package com.thit.elasticsearch.orcldb;

public class DataRow {
	DataTable table;
	int rowIndex;
	
	public DataRow(DataTable table, int rowIndex) {
		super();
		this.table = table;
		this.rowIndex = rowIndex;
	}
	
	public Object getValue(String columnName){
		return table.getValue(rowIndex,columnName);
	}
	
	
}
