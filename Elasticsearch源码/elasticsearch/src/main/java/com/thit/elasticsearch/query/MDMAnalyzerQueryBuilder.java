package com.thit.elasticsearch.query;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.thit.elasticsearch.common.ESUtil;
import com.thit.elasticsearch.query.QueryTokens.PositionOperator;

public class MDMAnalyzerQueryBuilder implements MDMQueryBuilder{
	
	private Client client;
	
	private BoolQueryBuilder builder;
	
	
	public MDMAnalyzerQueryBuilder() {
		super();
		// TODO Auto-generated constructor stub
	}
	

	public MDMAnalyzerQueryBuilder(Client client, BoolQueryBuilder builder) {
		super();
		this.client = client;
		this.builder = builder;
	}


	@Override
	public QueryBuilder buildeQuery(MDMESQueryContext context) {
		// TODO Auto-generated method stub
		
		return buildQuery(context, null);
	}
	
	public QueryBuilder buildQuery(MDMESQueryContext qContext,String field) {
		// TODO Auto-generated method stub
		
		QueryTokens tokens = qContext.getTokens();
		String index = qContext.getIndex();
		String type = qContext.getType();
		String analyzer = qContext.getAnalyzer();
		
		PositionOperator start = tokens.getStart();
		PositionOperator middle = tokens.getMiddle();
		PositionOperator end = tokens.getEnd();
		
		if(start!=null){
			PositionOperator test = null;
			//先判断开始和中间拼接以及开始和结尾拼接能不能查询出来(避免开始部分非中文一直用模糊查询)
			if(middle!=null){
				test = new PositionOperator(start.getPostion()+middle.getPostion(), (byte)1);
			}else if(end!=null){
				test = new PositionOperator(start.getPostion()+end.getPostion(), (byte)1);
			}else{
				
			}
			if(test!=null){
				BoolQueryBuilder start_filter = new BoolQueryBuilder();
//			start_filter = builder;
				start_filter = handle(test,start_filter,field,analyzer);
				
				SearchResponse res = client.prepareSearch(index.toLowerCase()).setTypes(type.toLowerCase())
						.setQuery(start_filter)
						.setTimeout(new TimeValue(6000))
						.get();
				if(res.getHits().getHits().length>0){
					//如果是一个中文字
//					analyzer = exchangeAnalyzer(start.getPostion(), analyzer,"standard");
					start = setOperator(start);
					builder = handle(start,builder,field,analyzer);
//				continue;
				}else{
//					analyzer = exchangeAnalyzer(start.getPostion(), analyzer,"standard");
					start = setOperator(start);
					builder = handle(start,builder,field,analyzer);
				}
				
			}else{
//				analyzer = exchangeAnalyzer(start.getPostion(), analyzer,"standard");
				start = setOperator(start);
				builder = handle(start,builder,field,analyzer);
			}
			System.out.println("-------------------------------------------");
			System.out.println("startWord:"+start.getPostion()+"  & operator : "+(start.getOperator()==0? "模糊查询":"精确查询"));
		}
		if(middle!=null){
//			analyzer = exchangeAnalyzer(middle.getPostion(), analyzer,"standard");
//			middle = setOperator(middle);
			builder = handle(middle, builder,field,analyzer);
			System.out.println("middleWord:"+middle.getPostion()+"  & operator : "+(middle.getOperator()==0? "模糊查询":"精确查询"));
		}
		if(end!=null){//将尾部非中文的内容进行模糊查询
//			analyzer = exchangeAnalyzer(end.getPostion(), analyzer,"standard");
			end = setOperator(end);
			builder = handle(end, builder,field,analyzer);
			System.out.println("endWord:"+end.getPostion()+"  & operator : "+(end.getOperator()==0? "模糊查询":"精确查询"));
		}
		
		return builder;
	}
	
	private PositionOperator setOperator(PositionOperator po){
		if(!ESUtil.existChinese(po.getPostion())){
			po.setOperator((byte)0);
		}else{
			po.setOperator((byte)1);
		}
		return po;
	}
	/**
	 * 只存在一个中文就用标准分词器进行查询
	 * @param str 
	 * @param analyzer 
	 * @return
	 */
	private String exchangeAnalyzer(String str,String sourceanalyzer,String targetanalyzer){
		
		if(str.length()==1&&ESUtil.existChinese(str)){
			return targetanalyzer;
		}else{
			return sourceanalyzer;
		}
	}

	public  BoolQueryBuilder handle(PositionOperator po, BoolQueryBuilder builder,String field,String analyzer) {
		// TODO Auto-generated method stub
		if(po.getOperator()==0){//模糊查询
			if(field!=null&&!"".equals(field)){
				builder.filter(QueryBuilders.queryStringQuery("*"+po.getPostion()+"*").field(field).allowLeadingWildcard(true).analyzer(analyzer).defaultOperator(Operator.AND));
			}else{
				builder.filter(QueryBuilders.queryStringQuery("*"+po.getPostion()+"*").allowLeadingWildcard(true).analyzer(analyzer).useAllFields(true).defaultOperator(Operator.AND));
				
			}
		}else if(po.getOperator()==1){//精确查询
			if(field!=null&&!"".equals(field))
				builder.filter(QueryBuilders.queryStringQuery(QueryParser.escape(po.getPostion())).field(field).allowLeadingWildcard(true).analyzer(analyzer).defaultOperator(Operator.AND));
			else
				builder.filter(QueryBuilders.queryStringQuery(QueryParser.escape(po.getPostion())).allowLeadingWildcard(true).analyzer(analyzer).useAllFields(true).defaultOperator(Operator.AND));
		}
		return builder;
	}

	

	
}
