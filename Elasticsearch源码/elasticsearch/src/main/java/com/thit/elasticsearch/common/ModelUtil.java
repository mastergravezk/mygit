package com.thit.elasticsearch.common;

import java.util.Enumeration;
import java.util.Hashtable;

import com.xicrm.business.util.JNDINames;
import com.xicrm.model.TXIModel;

public class ModelUtil {
	/**讲记录添加到指定细表
	 * @param record  查询得到的数据记录
	 * @param detailModel 记录放到细表中
	 * @return
	 */
	public static TXIModel getARdFromHashES(Hashtable record, TXIModel detailModel) {
        Enumeration recordEn = record.keys();
        String outPutFieldName = "";
        Object result = "";
        while (recordEn.hasMoreElements()) {
            //取得输出字段名
            outPutFieldName = (String) recordEn.nextElement();
            //取得字段名标识的值
            result = record.get(outPutFieldName);
            //result = result == null ? "" : result;
            //若输出参数中包含"ID"字符串则将记录ID置入modelID中
            if (outPutFieldName.equalsIgnoreCase(JNDINames.ID)) {
                detailModel.setId(result.toString());
            }
            if (result != null) {
                detailModel.setView(outPutFieldName, result);
            }
        }
        return detailModel;
    }
}
