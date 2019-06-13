package com.thit.elasticsearch.query;

import org.elasticsearch.index.query.QueryBuilder;

public interface MDMQueryBuilder {
	//构建mdm的es查询
	QueryBuilder buildeQuery(MDMESQueryContext context);
	
	
}
