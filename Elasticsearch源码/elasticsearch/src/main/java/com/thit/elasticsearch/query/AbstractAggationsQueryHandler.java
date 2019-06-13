package com.thit.elasticsearch.query;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;

public abstract class AbstractAggationsQueryHandler implements QueryHandler {
	
	protected AggregationBuilder aggbuilder;
	
	protected Client client;
	
	
	
	public AbstractAggationsQueryHandler(AggregationBuilder aggbuilder, Client client) {
		super();
		this.aggbuilder = aggbuilder;
		this.client = client;
	}

	@Override
	public MDMESQueryContext handle(MDMESQueryContext qContext, QueryBuilder builder) {
		// TODO Auto-generated method stub
		
		return handle(qContext, builder,aggbuilder);
	}
	
	public abstract MDMESQueryContext handle(MDMESQueryContext qContext, QueryBuilder builder, AggregationBuilder aggbuilder2);

	
	public AggregationBuilder getAggbuilder() {
		return aggbuilder;
	}

	public void setAggbuilder(AggregationBuilder aggbuilder) {
		this.aggbuilder = aggbuilder;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}
		
	

}
