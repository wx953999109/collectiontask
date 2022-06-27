package com.wh.business.collectiontask.schedulejob;

import org.quartz.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@DisallowConcurrentExecution
public class CollectionJobYPWK implements InterruptableJob {

    ScheduledExecutorService scheduExec = Executors.newScheduledThreadPool(1);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap jobDetailMap = context.getJobDetail().getJobDataMap();
        JobDataMap triggerMap = context.getTrigger().getJobDataMap();
        JobDataMap mergeMap = context.getMergedJobDataMap();

        System.out.println("ypwk:" + System.currentTimeMillis());
    }

    /**
     * 中断
     *
     * @throws UnableToInterruptJobException
     */
    @Override
    public void interrupt() {
        scheduExec.shutdownNow();
    }
}