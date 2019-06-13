package com.thit.elasticsearch.query;

public class PageInfo {
	
	private String pageSize; //分页的每一页记录数
	
	private String pageFlag; 
	
	private String currentPage;
	
	private String pageInfoControl;
	
	private int pageNo;
	
	private int totalPage; //总页数
	
	private String prePage;
	
	private String backPage;
	
	private String firstPage;
	
	private String lastPage;
	
	

	public PageInfo() {
		super();
		// TODO Auto-generated constructor stub
	}

	

	public PageInfo(String pageSize, String pageFlag, String currentPage, String pageInfoControl, int pageNo,
			int totalPage, String prePage, String backPage, String firstPage, String lastPage) {
		super();
		this.pageSize = pageSize;
		this.pageFlag = pageFlag;
		this.currentPage = currentPage;
		this.pageInfoControl = pageInfoControl;
		this.pageNo = pageNo;
		this.totalPage = totalPage;
		this.prePage = prePage;
		this.backPage = backPage;
		this.firstPage = firstPage;
		this.lastPage = lastPage;
	}



	public String getPageSize() {
		return pageSize;
	}



	public void setPageSize(String pageSize) {
		this.pageSize = pageSize;
	}



	public String getPageFlag() {
		return pageFlag;
	}



	public void setPageFlag(String pageFlag) {
		this.pageFlag = pageFlag;
	}



	public String getCurrentPage() {
		return currentPage;
	}



	public void setCurrentPage(String currentPage) {
		this.currentPage = currentPage;
	}



	public String getPageInfoControl() {
		return pageInfoControl;
	}



	public void setPageInfoControl(String pageInfoControl) {
		this.pageInfoControl = pageInfoControl;
	}



	public int getPageNo() {
		return pageNo;
	}



	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}



	public int getTotalPage() {
		return totalPage;
	}



	public void setTotalPage(int totalPage) {
		this.totalPage = totalPage;
	}



	public String getPrePage() {
		return prePage;
	}



	public void setPrePage(String prePage) {
		this.prePage = prePage;
	}



	public String getBackPage() {
		return backPage;
	}



	public void setBackPage(String backPage) {
		this.backPage = backPage;
	}



	public String getFirstPage() {
		return firstPage;
	}



	public void setFirstPage(String firstPage) {
		this.firstPage = firstPage;
	}



	public String getLastPage() {
		return lastPage;
	}



	public void setLastPage(String lastPage) {
		this.lastPage = lastPage;
	}



}
