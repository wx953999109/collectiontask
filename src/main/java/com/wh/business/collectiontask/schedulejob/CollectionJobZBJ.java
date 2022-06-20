package com.wh.business.collectiontask.schedulejob;

import org.quartz.*;

@DisallowConcurrentExecution
public class CollectionJobZBJ implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap jobDetailMap = context.getJobDetail().getJobDataMap();
        JobDataMap triggerMap = context.getTrigger().getJobDataMap();
        JobDataMap mergeMap = context.getMergedJobDataMap();

        System.out.println("zbj:" + System.currentTimeMillis());
    }
}