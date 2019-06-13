package com.thit.elasticsearch.test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.thit.elasticsearch.test.SerachTest;

public class Testmapping {
	private static Client client;

	public static void main(String[] args) throws IOException {
		client = SerachTest.initClient();
		Settings build = Settings.builder().put("number_of_shards", 3).put("number_of_replicas", 1).build();
		client.admin().indices().prepareCreate("mdm_en").setSettings(build).execute().actionGet();
		XContentBuilder endObject = XContentFactory.jsonBuilder().startObject().startObject("t_mat")
				.startObject("properties").startObject("MATVOCH").field("type", "string").field("index", "not_analyzed")
				.endObject().endObject().endObject().endObject();
		PutMappingRequest mappingReq_child = Requests.putMappingRequest("mdm_en")
				.type("t_mat")
				.source(endObject);
				
		try {
			client.admin().indices().putMapping(mappingReq_child).get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
