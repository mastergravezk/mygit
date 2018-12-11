package com.thit.elasticsearch.test;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.logging.Log4J2LoggerFactory;

public class MyJob implements Job {
	static long start = System.currentTimeMillis();
	private static Logger _log = LoggerFactory.getLogger(MyJob.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		System.out.println("间隔秒数="+String.valueOf(System.currentTimeMillis()-start));
		start = System.currentTimeMillis();
//		System.out.println("结束时间="+String.valueOf(end));
		System.out.println("定时调度执行了");
		_log.debug("oh shit!");
		if(context.isRecovering()){
			_log.debug("oh repeate!");
		}
		
	}
	public static void main(String[] args) {
		
	}
	
}
