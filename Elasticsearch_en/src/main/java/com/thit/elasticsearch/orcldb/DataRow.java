package com.thit.elasticsearch.orcldb;

public class DataRow {
	DataTable table;
	int rowIndex;

	public DataRow(DataTable table, int index) {
		this.table = table;
		rowIndex = index;
	}

	public Object getValue(String columnName) {

		return table.getValue(rowIndex, columnName);
	}

}
