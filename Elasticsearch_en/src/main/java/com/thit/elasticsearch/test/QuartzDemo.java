package com.thit.elasticsearch.test;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;


public class QuartzDemo {
	public static void run(){
		SchedulerFactory fac = new StdSchedulerFactory();
		Scheduler scheduler = null;
		long start  = 0l;
		try {
			scheduler = fac.getScheduler();
			JobDetail detailjob = JobBuilder.newJob(MyJob.class).withIdentity("zkjob", "zk1").build();
			Trigger zktrigger = TriggerBuilder.newTrigger()
					.withIdentity("zktrigger", "zk2")
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().repeatSecondlyForever(5))
					.build();
			scheduler.scheduleJob(detailjob, zktrigger);             
			scheduler.start();
//			long end = System.currentTimeMillis();
//			System.out.println("延迟时间为="+String.valueOf(end-start));
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		run();
	}

}
