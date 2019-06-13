package com.thit.elasticsearch.query;

public class PageProcessor {
	
	/**得到总页数
	 * @param totalnum 总记录数
	 * @param pageSize 每页的行设置
	 * @return
	 */
	public static int getTotalPage(int totalnum,PageInfo pageinfo){
		int totalPage=0;
		String pageSize = pageinfo.getPageSize();
		if(!"0".equals(pageSize)){
			int flag = totalnum%Integer.valueOf(pageSize);
			if(flag!=0){
				totalPage = totalnum/Integer.valueOf(pageSize)+1;
			}else{
				totalPage = totalnum/Integer.valueOf(pageSize);
			}
		}
		return totalPage;
	}
	
	/**
	 * 设置subid的范围取查询的数据的范围
	 * @param ltnum 范围比较的较小数
	 * @param gtnum 范围比较的较大数
	 * @param totalnum 总记录数
	 * @param totalPage 总页数
	 * @param pageFlag 翻页格式:当前页;翻页标记(0:首页,1:前页,2:后页,3:末页,4:不翻页),''为重置
	 * @param pageSize 每页的行设置
	 * @param current 当前页
	 */
	public static int[] setSubidRange(int ltnum,int gtnum,int totalnum,int totalPage,PageInfo pageinfo){
		
		String pageFlag = pageinfo.getPageFlag();
		String pageSize = pageinfo.getPageSize();
		String current = pageinfo.getCurrentPage();
		
		//载入时的标记为空首页的标记也为空
		if ("".equals(pageFlag) || "0".equals(pageFlag)) {
			ltnum = 0;
			if(Integer.valueOf(pageSize)<=totalnum){
				gtnum = Integer.valueOf(pageSize);//当分页行数小于总数时
			}else{
				gtnum = totalnum;//当分页行数大于总数时
			}
			System.out.println("gtnum="+gtnum);
		//上一页
		} else if ("1".equals(pageFlag)) {
			if("2".equals(current)){
				ltnum = 0;
				gtnum = Integer.valueOf(pageSize);
			}else{
				ltnum = (Integer.valueOf(current) - 2) * 20;
				gtnum = (Integer.valueOf(current) - 1) * 20;
			}
		//下一页
		} else if ("2".equals(pageFlag)) {
			//当前页的下一页是最后一页时
			if(String.valueOf(totalPage-1).equals(current)){
				ltnum = (totalPage-1)*20;
				gtnum = totalnum;
			}else{
				ltnum = (Integer.valueOf(current)) * 20;
				gtnum = (Integer.valueOf(current) + 1) * 20;
			}
		//尾页
		} else if ("3".equals(pageFlag)) {
			ltnum = (totalPage-1)*20;
			gtnum = totalnum;
		//go按钮
		} else if ("4".equals(pageFlag)) {
			ltnum = (Integer.valueOf(current) - 1) * 20;
			gtnum = (Integer.valueOf(current)) * 20;
		}
		int[] range= {ltnum,gtnum};
		return range;
	}
	
}
