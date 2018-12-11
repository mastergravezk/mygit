package com.thit.elasticsearch.orcldb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

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
	
	/*public static String joinKey(Collection<String> list,String seprator)
	{
		String result = null;
		int count = 0;
		for(String oneString : list)
		{
			if(count > 0)
			{
				if("CO_ID".equals(oneString)){
					result = result + seprator +"TEMP_CO_ID";
				}else{
					result = result + seprator + oneString;
				}				
			}else {
				//小心将第一个落下
				if("CO_ID".equals(oneString)){
					result = "TEMP_CO_ID";
				}else{
					//result = result + seprator + oneString;
					result = oneString;
				}
				
			}
			count ++;
		}
		
		return result;
	}*/
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
