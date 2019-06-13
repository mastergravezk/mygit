package com.thit.elasticsearch.query;

import java.util.List;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;

/**
 * 将查询内容切分成三部分
 * @author zk
 *
 */
public class QueryTokens {
	

	private List<AnalyzeToken> tokens;
	private PositionOperator start;//开始
	private PositionOperator middle;//中间
	private PositionOperator end;//尾部
	
//	private short queryflag;//查询
	public static final class PositionOperator{//各个位置的操作
		
		private String postion;
		private byte operator;//精确查询为1，模糊查询为0
		public PositionOperator(String postion, byte operator) {
			super();
			this.postion = postion;
			this.operator = operator;
		}
		public String getPostion() {
			return postion;
		}
		public void setPostion(String postion) {
			this.postion = postion;
		}
		public byte getOperator() {
			return operator;
		}
		public void setOperator(byte operator) {
			this.operator = operator;
		}
		
		
	}
	
	
	public QueryTokens() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	public QueryTokens(List<AnalyzeToken> tokens, PositionOperator start, PositionOperator middle,
			PositionOperator end) {
		super();
		this.tokens = tokens;
		this.start = start;
		this.middle = middle;
		this.end = end;
	}


	public PositionOperator getStart() {
		return start;
	}
	public void setStart(String start,byte operator) {
		this.start = new PositionOperator(start, operator);
		
	}
	public PositionOperator getMiddle() {
		return middle;
	}
	public void setMiddle(String middle,byte operator) {
		this.middle = new PositionOperator(middle, operator);
	}
	public PositionOperator getEnd() {
		return end;
	}
	public void setEnd(String end,byte operator) {
		this.end = new PositionOperator(end, operator);
	}

	public List<AnalyzeToken> getTokens() {
		return tokens;
	}

	public void setTokens(List<AnalyzeToken> tokens) {
		this.tokens = tokens;
	}
	public static void main(String[] args) {
		PositionOperator s = new PositionOperator("start", (byte)1);
		PositionOperator s1 = new PositionOperator("start", (byte)2);
		System.out.println(s.getOperator());
		System.out.println(s1.getOperator());
	}
	
	
}
