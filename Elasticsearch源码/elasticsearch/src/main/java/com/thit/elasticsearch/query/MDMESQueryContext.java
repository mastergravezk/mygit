package com.thit.elasticsearch.query;

import com.xicrm.model.TXIModel;

public class MDMESQueryContext {
	
	private QueryTokens tokens;//存储分词内容
	
	private String index;
	
	private String type;
	
	private String analyzer;
	
	private String detailNum;
	
	private TXIModel model;
	
	private PageInfo pageInfo;
	
	private String sortField;


	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public MDMESQueryContext() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MDMESQueryContext(QueryTokens tokens, String index, String type, String analyzer, String detailNum,
			TXIModel model, PageInfo pageInfo, String sortField) {
		super();
		this.tokens = tokens;
		this.index = index;
		this.type = type;
		this.analyzer = analyzer;
		this.detailNum = detailNum;
		this.model = model;
		this.pageInfo = pageInfo;
		this.sortField = sortField;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(String analyzer) {
		this.analyzer = analyzer;
	}

	public String getDetailNum() {
		return detailNum;
	}

	public void setDetailNum(String detailNum) {
		this.detailNum = detailNum;
	}

	public TXIModel getModel() {
		return model;
	}

	public void setModel(TXIModel model) {
		this.model = model;
	}

	public PageInfo getPageInfo() {
		return pageInfo;
	}

	public void setPageInfo(PageInfo pageInfo) {
		this.pageInfo = pageInfo;
	}

	public QueryTokens getTokens() {
		return tokens;
	}

	public void setTokens(QueryTokens tokens) {
		this.tokens = tokens;
	}

	
	
	
}
