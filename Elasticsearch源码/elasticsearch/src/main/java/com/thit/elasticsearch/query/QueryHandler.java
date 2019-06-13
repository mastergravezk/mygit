package com.thit.elasticsearch.query;

import org.elasticsearch.index.query.QueryBuilder;

public interface QueryHandler {
	//对构建成的查询进行处理
	MDMESQueryContext handle(MDMESQueryContext qContext,QueryBuilder builder);
	
}
