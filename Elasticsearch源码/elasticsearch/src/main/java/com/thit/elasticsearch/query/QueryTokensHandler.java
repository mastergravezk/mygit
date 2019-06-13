package com.thit.elasticsearch.query;

import java.util.List;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;

import com.thit.elasticsearch.common.ESConstant;
import com.thit.elasticsearch.common.ESUtil;

public class QueryTokensHandler {
	
//	private String content;//传入的查询内容
	private MDMESQueryContext context;
//	private QueryTokens tokens;//将处理结果放入到tokens中
//	
//	private String indices;//对哪些索引进行处理
//	
//	private String analyzer;//使用哪种分词器
	
	public QueryTokensHandler() {
		super();
		// TODO Auto-generated constructor stub
	}

	public QueryTokensHandler(MDMESQueryContext context) {
		super();
		this.context = context;
	}

	public MDMESQueryContext getContext() {
		return context;
	}

	public void setContext(MDMESQueryContext context) {
		this.context = context;
	}

	public QueryTokens handleQueryStr(String content){
		if(content!=null&&!"".equals(content)){
			List<AnalyzeToken> tokenss = ESUtil.annalyzeStr(context.getIndex(), content, context.getAnalyzer());
			QueryTokens ts = new QueryTokens();
			
			int size = tokenss.size();
			AnalyzeToken startToken = tokenss.get(0);
			AnalyzeToken endToken = tokenss.get(size-1);
			
			
			//情况1：如果只有一个token且|1.含有中文用准确匹配2.不含中文那就直接模糊---》赋值到start
			if(size==1){
				ts = setStarttoken(ts, startToken.getTerm());
			
			}else if(size==2){ //情况2：如果有两个token同情况1--》赋值到start和end
				ts = setStarttoken(ts, startToken.getTerm());
				ts = setEndtoken(ts, endToken.getTerm());
				
			}else if(size>=3){//情况3: 如果有三个或者三个以上的token那么开始和结尾同情况1之外--》赋值到start和end，中间部分进行合并--》赋值到middle
				String mid = content.substring(startToken.getEndOffset(),endToken.getStartOffset());
				ts = setStarttoken(ts, startToken.getTerm());
				ts = setMiddletoken(ts, mid);
				ts = setEndtoken(ts, endToken.getTerm());
			}
			context.setTokens(ts);
			return ts;
		}else{
			return null;
		}
	}
	
	//设置开始位置的操作信息
	private QueryTokens setStarttoken(QueryTokens tokens , String start ){
		if(ESUtil.existChinese(start)){
			tokens.setStart(start,ESConstant.exact);
		}else{
			tokens.setStart(start,ESConstant.fuzzy);
		}
		return tokens;
	}
	//设置中间位置的操作信息
	private QueryTokens setMiddletoken(QueryTokens tokens , String middle ){
		tokens.setMiddle(middle, ESConstant.exact);
		return tokens;
	}
	/**设置结尾位置的操作信息**/
	private QueryTokens setEndtoken(QueryTokens tokens , String end ){
		if(ESUtil.existChinese(end)){
			tokens.setEnd(end,ESConstant.exact);
		}else{
			tokens.setEnd(end,ESConstant.fuzzy);
		}
		return tokens;
	}
	
	
}
