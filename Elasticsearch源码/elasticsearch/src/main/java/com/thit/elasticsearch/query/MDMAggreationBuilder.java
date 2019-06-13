package com.thit.elasticsearch.query;

import org.elasticsearch.search.aggregations.AggregationBuilder;

public interface MDMAggreationBuilder {
	//构建聚合
	AggregationBuilder buildAggs(MDMESQueryContext context);
}
