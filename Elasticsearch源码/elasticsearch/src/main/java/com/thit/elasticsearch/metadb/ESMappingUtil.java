package com.thit.elasticsearch.metadb;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.elasticsearch.common.xcontent.XContentBuilder;

import com.thit.elasticsearch.orcldb.DbOperation;

public class ESMappingUtil {
	
	
	/**
	 * 
	 * @param builder
	 * @param tableName
	 * @param ignore_cols
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder getMappingBuilder(XContentBuilder builder,String tableName,List<String> ignore_cols) throws IOException{
		String[] columnNames = DbOperation.getColumnNames(tableName);
		//增加其他索引方式（keyword不进行分词）
		Map fields = new TreeMap<>();
		Map child = new TreeMap<>();
		child.put("type", "keyword");
		child.put("ignore_above", "2000");
		fields.put("keyword", child);
		if(columnNames.length>0){
			for(String col : columnNames){
				if(ignore_cols.contains(col))
					continue;
				else
					builder = builder.startObject(col).field("type","text").field("analyzer","ik_smart").field("fields", fields).endObject();
				
			}
			//扩充的字段
			builder.startObject("EXTRA").field("type", "long").endObject()
			.startObject("EXTRB").field("type", "integer").endObject()
			.startObject("EXTRC").field("type", "integer").endObject()
			.startObject("EXTRD").field("type", "byte").endObject()
			.startObject("EXTRE").field("type", "text").field("analyzer","ik_max_word").field("fields", fields).endObject()
			.startObject("EXTRF").field("type", "text").field("analyzer","ik_max_word").field("fields", fields).endObject()
			.startObject("EXTRG").field("type", "text").field("analyzer","ik_smart").field("fields", fields).endObject()
			.startObject("EXTRH").field("type", "text").field("analyzer","ik_smart").field("fields", fields).endObject()
			.startObject("EXTRI").field("type", "text").field("analyzer","ik_smart").field("fields", fields).endObject()
			.startObject("EXTRG").field("type", "text").field("analyzer","ik_smart").field("fields", fields).endObject()
			.startObject("EXTRk").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||yyyyMMdd||yyyy/MM/dd HH:mm:ss||yyyy/MM/dd HH:mm||yyyy/MM/dd||epoch_millis").endObject()
			.endObject()
			.endObject()
			.endObject();
		}
		return builder;
	}

}
