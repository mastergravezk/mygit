package com.thit.elasticsearch;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;

public enum ZK {
	TALL("YES"),SHORT("OR");
	private String op;
	ZK(String op){
		this.op = op;
	}
    String getOP(){
		return op;
	}
	public static void main(String[] args) {
//		String name = null;
//		System.out.println("123".substring(1));
		System.out.println(ZK.TALL.getOP());
//		System.out.println(BooleanClause.Occur.MUST.toString());
	
	}
}
