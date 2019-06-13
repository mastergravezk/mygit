package com.thit.elasticsearch.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SyncDBtoESJob implements Job {
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		SyncDBtoESTimer timer = new SyncDBtoESTimer();
		timer.syncDatas();
	}

}
