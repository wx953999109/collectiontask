package com.wh.business.collectiontask.controller;

import com.wh.business.collectiontask.domain.JobInfo;
import com.wh.business.collectiontask.domain.R;
import com.wh.business.collectiontask.schedulejob.CollectionJobA5;
import com.wh.business.collectiontask.schedulejob.CollectionJobYPWK;
import com.wh.business.collectiontask.schedulejob.CollectionJobZBJ;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@RestController
@RequestMapping("job")
public class TaskManageControll {

    Scheduler scheduler;

    public TaskManageControll(Scheduler scheduler){
        this.scheduler = scheduler;
    }

    public static Map<String, JobInfo> jobs = new HashMap<>();
    String platforms[] = {"a5", "zbj", "ypwk"};

    String defaultGroup = "default-group";
    //int intervalInSeconds = 1;
    int intervalInSeconds = 60 * 10; //10分钟

    @PostConstruct
    void init() throws SchedulerException {
//        scheduler = StdSchedulerFactory.getDefaultScheduler();
        jobs.put("a5", new JobInfo("a5"));
        jobs.put("zbj", new JobInfo("zbj"));
        jobs.put("ypwk", new JobInfo("ypwk"));
    }

    @PostMapping("getJobStatus")
    R getJobStatus(String platform) throws Exception {
        Assert.isTrue(Arrays.asList(platforms).contains(platform), "平台代码错误");

        JobInfo jobInfo = jobs.get(platform);
        //因为任务job内部也有线程, 部分线程无法立即结束掉, 会出现日志滞后, 所以这里把日志内容固定为已停止
        if (!jobInfo.isRunning()) {
            jobInfo.setLog("已停止");
        }
        return R.success(jobInfo, "获取任务状态成功");
    }

    @PostMapping("start")
    R startJob(String platform) throws Exception {
        Class jobClass = null;
        if ("a5".equalsIgnoreCase(platform)) {
            jobClass = CollectionJobA5.class;
        } else if ("zbj".equalsIgnoreCase(platform)) {
            jobClass = CollectionJobZBJ.class;
        } else if ("ypwk".equalsIgnoreCase(platform)) {
            jobClass = CollectionJobYPWK.class;
        }
        Assert.notNull(jobClass, "平台代码错误");

        JobKey jobKey = JobKey.jobKey(platform, defaultGroup);
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        JobInfo jobInfo = jobs.get(platform);
        if (jobDetail == null) {
            jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(platform, defaultGroup)
                    .usingJobData("platform", platform)
                    .requestRecovery(false)  //job可恢复，在其执行的时候，scheduler发生硬关闭，则当scheduler重新启动的时候，该job会被重新执行，此时，该job的JobExecutionContext.isRecovering()返回true
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(platform, defaultGroup)
                    .usingJobData("key", "value")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(intervalInSeconds).repeatForever() //也可以用cron
                    ).build();

            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.start();
            jobInfo.setRunning(true);
        }
        return R.success(jobInfo, "已启动");
    }

    @PostMapping("stop")
    R stop(String platform) throws Exception {

        JobKey jobKey = JobKey.jobKey(platform, "default-group");
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        JobInfo jobInfo = jobs.get(platform);
        jobInfo.setRunning(false);
        if (jobDetail != null) {
            scheduler.interrupt(jobKey);
            scheduler.deleteJob(jobKey);
            jobInfo.setLog("已停止");
            return R.success(jobInfo, "已停止");
        } else {
            return R.error("任务不存在");
        }
    }


}