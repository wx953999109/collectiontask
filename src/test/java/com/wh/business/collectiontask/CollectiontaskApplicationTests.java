package com.wh.business.collectiontask;

import com.wh.business.collectiontask.schedulejob.CollectionJobA5;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CollectiontaskApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void taskTest() throws Exception {
        JobDetail jobDetail = JobBuilder.newJob(CollectionJobA5.class)
                .withIdentity("job1", "group1")  //设置job的名字和组
                .usingJobData("job", "jobDetail")  //可以放入信息数据，在业务逻辑中获取
                .requestRecovery(true)  //job可恢复，在其执行的时候，scheduler发生硬关闭，则当scheduler重新启动的时候，该job会被重新执行，此时，该job的JobExecutionContext.isRecovering()返回true
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger1", "trigger1")
                .usingJobData("trigger", "trigger")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever() //也可以用cron
                ).build();
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
        Thread.sleep(5000);

        Thread.sleep(Long.MAX_VALUE);
    }

}
