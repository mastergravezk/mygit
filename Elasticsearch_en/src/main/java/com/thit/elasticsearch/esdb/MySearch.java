package com.thit.elasticsearch.esdb;

import org.apache.lucene.util.StringHelper;

import com.xicrm.model.TXIModel;

public class MySearch implements SearchESOperator {
    //demo
	@Override
	public Object search(Object index, Object type, Object spare1, Object spare2, Object spare3, Object spare4,
			Object spare5, Object spare6) throws Exception {
		// TODO Auto-generated method stub
	    TXIModel model = (TXIModel)spare6;
		return model;
	}

}
