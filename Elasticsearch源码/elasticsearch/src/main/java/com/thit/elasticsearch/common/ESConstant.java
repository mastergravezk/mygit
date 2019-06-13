package com.thit.elasticsearch.common;

public class ESConstant {
	//匹配中文的正则表达式
	public static final String regex = "^[\u0391-\uFFE5]+$";
	//字母正则表达式
	public static final String regex_word = "[a-zA-z]";
	//语言标记英文
	public static final String lang_en = "en";
	//语言标记中文
	public static final String lang_zh = "zh";
	//语言标记中英文
	public static final String lang_all = "all";
	//模糊查询
	public static final byte fuzzy = 0;
	//精确查询
	public static final byte exact = 1;
}
