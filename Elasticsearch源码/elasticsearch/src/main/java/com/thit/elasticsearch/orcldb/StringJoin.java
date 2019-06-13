package com.thit.elasticsearch.orcldb;

import java.util.Collection;

public class StringJoin {
	public static String joinKey(Collection<String> list,String seprator)
	{
		String result = null;
		int count = 0;
		for(String oneString : list)
		{
			if(count > 0)
			{
				result = result + seprator + oneString;
			}
			else {
				result = oneString;
			}
			count ++;
		}
		
		return result;
	}
	

	public static String joinVal(Collection<String> list,String seprator)
	{
		String result = null;
		int count = 0;
		for(String oneString : list)
		{
			if(count > 0)
			{
				
				result = result + seprator + oneString;
								
			}
			else {
				result = oneString;
			}
			count ++;
		}
		
		return result;
	}
}
