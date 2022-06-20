package com.wh.business.collectiontask.controll;

import com.wh.business.collectiontask.schedulejob.CollectionJobA5;
import com.wh.business.collectiontask.schedulejob.CollectionJobYPWK;
import com.wh.business.collectiontask.schedulejob.CollectionJobZBJ;
import org.quartz.*;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@Component
@RestController
@RequestMapping("job")
public class TaskManageControll {

    Scheduler scheduler;

    String defaultGroup = "default-group";
    int intervalInSeconds = 1;
    //int intervalInSeconds = 60 * 10;

    @PostConstruct
    void init() throws SchedulerException {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
    }

    @PostMapping("getTaskStatus")
    String getTaskStatus(String platfrom) {
        return platfrom;
    }

    @PostMapping("start")
    String startJob(String platfrom) throws Exception {
        Class jobClass = null;
        if ("a5".equalsIgnoreCase(platfrom)) {
            jobClass = CollectionJobA5.class;
        } else if ("zbj".equalsIgnoreCase(platfrom)) {
            jobClass = CollectionJobZBJ.class;
        } else if ("ypwk".equalsIgnoreCase(platfrom)) {
            jobClass = CollectionJobYPWK.class;
        }
        Assert.notNull(jobClass, "平台代码错误");

        JobKey jobKey = JobKey.jobKey(platfrom, defaultGroup);
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        if (jobDetail == null) {
            jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(platfrom, defaultGroup)  //设置job的名字和组
                    .usingJobData("key", "value")  //可以放入信息数据，在业务逻辑中获取
                    .requestRecovery(true)  //job可恢复，在其执行的时候，scheduler发生硬关闭，则当scheduler重新启动的时候，该job会被重新执行，此时，该job的JobExecutionContext.isRecovering()返回true
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(platfrom, defaultGroup)
                    .usingJobData("key", "value")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(intervalInSeconds).repeatForever() //也可以用cron
                    ).build();

            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.start();
        }
        return "ok";
    }

    @PostMapping("stop")
    String stop(String platfrom) throws Exception {

        JobKey jobKey = JobKey.jobKey(platfrom, "default-group");
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        if (jobDetail != null) {
            scheduler.deleteJob(jobKey);
            return "ok";
        }else{
            return "任务不存在";
        }
    }
}