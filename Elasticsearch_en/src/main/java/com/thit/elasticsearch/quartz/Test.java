package com.thit.elasticsearch.quartz;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.FileAppender;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import jdk.nashorn.internal.runtime.regexp.joni.Regex;

public class Test {
	private static Client client;
	public static void main(String[] args) throws Exception {
//		java.util.Properties  pr =  new Properties();
//		try {
//			pr.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("Database.properties"));
//			Enumeration<Object> elements = pr.keys();
//			while(elements.hasMoreElements()){
//				Object name = elements.nextElement();
//				System.out.println(name+"=="+pr.get(name));
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		exportExcel();
		String sheetName = "中车统计表单";  
        String titleName = "中车数据统计表";  
        String fileName = "中车申请统计表单";  
        int columnNumber = 1;  
        int[] columnWidth = {30};  
        String[][] dataList = getCoids();  
        String[] columnName = { "主键CO_ID" }; 
		ExportWithNoResponse(sheetName, titleName, fileName, columnNumber, columnWidth, columnName, dataList);
		
	}
	
	public static String filterSpecialCharOfXml(String txt){  
	    String res = "";  
	        for(int i = 0; i < txt.length(); ++i){  
	            char ch = txt.charAt(i);  
	            if( Character.isDefined(ch) &&  
	                ch!= '&' && ch != '<' && ch != '>' &&   
	                !Character.isHighSurrogate(ch) &&  
	                !Character.isISOControl(ch) &&  
	                !Character.isLowSurrogate(ch)  
	               ){  
	                res = res + ch;  
	            }  
	        }  
	        return res;  
	}
	public static String[][] getCoids() throws UnknownHostException{
		if(client == null){
			client = com.thit.elasticsearch.test.Test.initClient();
		}
		//将token中分词后的内容，除了最后一个term进行模糊查询之外，另外的都进行准确匹配
//		AnalyzeResponse resp = client.admin().indices().prepareAnalyze("mdmindex", "adsfusd//asfasd//adf-ad/简单段").setAnalyzer("standard").get();
//		List<AnalyzeToken> tokens = resp.getTokens();
//		for (AnalyzeToken token : tokens) {
//			String term = token.getTerm();
//			System.out.println(term);
//		}
		
		//file要创建两次如果没有路径的话，先创建路径后创建文件(一般情况下是文件夹已经创建好了)
		File file = new File("e:/zkes/coids.xlsx");
		if(!file.exists()){
//			file.mkdirs();
//			String path = file.getPath();
//			file1 = new File(path+"/es.txt");
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		OutputStream out = null;
		OutputStreamWriter writer = null;
		StringBuffer sb = new StringBuffer();
		String[][] coids = null;
		try {
			out = new FileOutputStream(file);
			writer = new OutputStreamWriter(out);
			SearchResponse searchResponse = client.prepareSearch("mdmindex").setTypes("t_mat")
					.setScroll(new TimeValue(120*1000))
					.addSort("CO_ID.keyword", SortOrder.ASC)
					.setQuery(QueryBuilders.boolQuery()
							.filter(QueryBuilders.wildcardQuery("MATNUM","*m00000000*"))
//							.filter(QueryBuilders.existsQuery("CO_MANUALCODE"))
							)
//		.addDocValueField("CO_MANUALCODE.keyword")
					.setSize(50000)
					.get(new TimeValue(120*1000));
			SearchHit[] hits = searchResponse.getHits().getHits();
			int total = (int)searchResponse.getHits().getTotalHits();
			System.out.println(total);
			coids = new String[total][1];
			int num =0;
			
			do{
				for (SearchHit hit : searchResponse.getHits().getHits()) {
//					Object code = hit.getSourceAsMap().get("MATNUM");
//					sb.append(code).append(",");
					coids[num][0] = hit.getId();
					num++;
				}
				searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(60000)).get();
				System.out.println("循环id："+searchResponse.getScrollId());
				
			}while(searchResponse.getHits().getHits().length!=0);
//			writer.write(sb.substring(0, sb.length()-1));
//			out.flush();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(writer!=null){
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(out!=null){
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return coids;
	}
	//导出ES数据到excel中
	public static void exportExcel(){
		//先创建excel文件
		File file = new File("e:/zkes/coids.xlsx");
		if(!file.exists()){
//			file.mkdirs();
//			String path = file.getPath();
//			file1 = new File(path+"/es.txt");
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// 第一步，创建一个webbook，对应一个Excel文件  
		HSSFWorkbook wb = new HSSFWorkbook();  
        // 第二步，在webbook中添加一个sheet,对应Excel文件中的sheet  
        HSSFSheet sheet = wb.createSheet("物料编码"); 
        
        
	}
	@org.junit.Test
	public static void ExportWithNoResponse(String sheetName, String titleName,  
            String fileName, int columnNumber, int[] columnWidth,  
            String[] columnName, String[][] dataList 
           ) throws Exception {  
        if (columnNumber == columnWidth.length&& columnWidth.length == columnName.length) {  
            // 第一步，创建一个webbook，对应一个Excel文件  
            HSSFWorkbook wb = new HSSFWorkbook();  
            // 第二步，在webbook中添加一个sheet,对应Excel文件中的sheet  
            HSSFSheet sheet = wb.createSheet(sheetName);  
            // sheet.setDefaultColumnWidth(15); //统一设置列宽  
            for (int i = 0; i < columnNumber; i++)   
            {  
                for (int j = 0; j <= i; j++)   
                {  
                    if (i == j)   
                    {  
                        sheet.setColumnWidth(i, columnWidth[j] * 256); // 单独设置每列的宽  
                    }  
                }  
            }  
            // 创建第0行 也就是标题  
            HSSFRow row1 = sheet.createRow((int) 0);  
            row1.setHeightInPoints(50);// 设备标题的高度  
            // 第三步创建标题的单元格样式style2以及字体样式headerFont1  
            HSSFCellStyle style2 = wb.createCellStyle();  
         
            
  
            HSSFCell cell1 = row1.createCell(0);// 创建标题第一列  
//            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0,  
//                    columnNumber - 1)); // 合并列标题  
            cell1.setCellValue(titleName); // 设置值标题  
            cell1.setCellStyle(style2); // 设置标题样式  
  
            // 创建第1行 也就是表头  
            HSSFRow row = sheet.createRow((int) 1);  
            row.setHeightInPoints(37);// 设置表头高度  
  
            
            // 第四.一步，创建表头的列  
            for (int i = 0; i < columnNumber; i++)   
            {  
                HSSFCell cell = row.createCell(i);  
                cell.setCellValue(columnName[i]);  
            }  
  
            // 第五步，创建单元格，并设置值  
            for (int i = 0; i < dataList.length; i++)   
            {  
                row = sheet.createRow((int) i + 2);  
                // 为数据内容设置特点新单元格样式1 自动换行 上下居中  
//                HSSFCellStyle zidonghuanhang = wb.createCellStyle();  
//                zidonghuanhang.setWrapText(true);// 设置自动换行  
                HSSFCell datacell = null;  
                for (int j = 0; j < columnNumber; j++)   
                {  
                    datacell = row.createCell(j);  
                    datacell.setCellValue(dataList[i][j]);  
                }  
            }  
  
         // 第六步，将文件存到指定位置  
            try {  
                FileOutputStream fout = new FileOutputStream("D:\\coids.xlsx");  
                wb.write(fout);  
                String str = "导出" + fileName + "成功！";  
                System.out.println(str);  
                fout.close();  
            } catch (Exception e) {  
                e.printStackTrace();  
                String str1 = "导出" + fileName + "失败！";  
                System.out.println(str1);  
            }  
            
        } else {  
            System.out.println("列数目长度名称三个数组长度要一致");  
        }  
  
    }
	
}
