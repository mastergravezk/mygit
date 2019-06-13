package com.thit.elasticsearch.query;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
//打个样没用
public class DemoAggreationBuilder implements MDMAggreationBuilder {

	@Override
	public AggregationBuilder buildAggs(MDMESQueryContext context) {
		// TODO Auto-generated method stub
		AvgAggregationBuilder subAggregation = AggregationBuilders.avg("").field("").subAggregation(AggregationBuilders.terms("").field(""));
		return subAggregation;
	}

}
