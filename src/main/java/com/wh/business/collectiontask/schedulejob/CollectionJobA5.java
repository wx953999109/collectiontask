package com.wh.business.collectiontask.schedulejob;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.business.collectiontask.controller.TaskManageControll;
import com.wh.business.collectiontask.domain.IMyRunnable;
import com.wh.business.collectiontask.domain.JobInfo;
import com.wh.business.collectiontask.entity.TaskDO;
import com.wh.business.collectiontask.service.TaskService;
import com.wh.business.collectiontask.util.ApplicationContextUtils;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.quartz.DelegatingJob;
import org.springframework.transaction.TransactionDefinition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@DisallowConcurrentExecution
@Log4j2
public class CollectionJobA5 implements InterruptableJob {

    final String Host = "https://www.a5.cn/";
    List<String> taskList = new ArrayList<String>();
    JobInfo jobInfo;
    TaskService taskService;
    ScheduledExecutorService scheduExec = Executors.newScheduledThreadPool(1);
    private DataSourceTransactionManager dataSourceTransactionManager;
    private TransactionDefinition transactionDefinition;

    CollectionJobA5() {
        taskService = (TaskService) ApplicationContextUtils.getBean(TaskService.class);
        transactionDefinition = (TransactionDefinition) ApplicationContextUtils.getBean(TransactionDefinition.class);
        dataSourceTransactionManager = (DataSourceTransactionManager) ApplicationContextUtils.getBean(DataSourceTransactionManager.class);
    }

    int delay = 3000;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        System.out.println("a5:" + System.currentTimeMillis());

        JobDataMap jobDetailMap = context.getJobDetail().getJobDataMap();
        JobDataMap triggerMap = context.getTrigger().getJobDataMap();
        JobDataMap mergeMap = context.getMergedJobDataMap();

        String platform = jobDetailMap.getString("platform");
        jobInfo = TaskManageControll.jobs.get(platform);
        jobInfo.setCollectionCount(0);
        jobInfo.setRunning(true);
        try {
            //读取任务列表页数
            jobInfo.setLog("读取页数");
            Document doc = Jsoup.connect(Host + "tasklist-o-1-page-1.html").get();
            Elements tmpLis = doc.select(".m-page-nums .pagination li");
            int pageCount = Integer.parseInt(tmpLis.get(tmpLis.size() - 2).text());
            for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                jobInfo.setLog("加载第" + pageIndex + "页数据");
                //过会再访问
                Thread.sleep(delay);
                ReadTaskList(Host + "tasklist-o-1-page-" + pageIndex + ".html");
            }

            jobInfo.setLog("读取所有任务详细数据");
            //过会再访问
            Thread.sleep(delay);
            //读取所有任务详细数据
            ReadDetailRead();
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    /**
     * 读取列表页中的所有任务链接
     *
     * @param url
     * @throws Exception
     */
    void ReadTaskList(String url) throws Exception {
        Document doc = Jsoup.connect(url).get();

        Elements links = doc.selectFirst(".m-tk-list").select("h3 a");
        if (links.size() == 0) return;
        for (Element link : links) {
            String path = link.attr("href").toString();
            String detailTaskUrl = Host + path;
            //将任务详细页面的链接存放到集合里面
            taskList.add(detailTaskUrl);
        }
    }

    /**
     * 读取任务详细页面
     *
     * @throws Exception
     */
    private void ReadDetailRead() throws Exception {
        for (int i = 0; i < taskList.size(); i++) {
            jobInfo.setLog("加载任务详细, 第" + (i + 1) + "个/" + taskList.size());
            IMyRunnable<String> task = new IMyRunnable<String>() {
                String detailTaskUrl;

                @Override
                public IMyRunnable setParam(String... param) {
                    detailTaskUrl = param[0];
                    return this;
                }

                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName() + "\t" + detailTaskUrl);
                    //TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                    TaskDO taskDO = new TaskDO();
                    //任务详细url
                    taskDO.setUrl(detailTaskUrl);
                    try {
                        //来源平台
                        taskDO.setPlatform(jobInfo.getPlatform());
                        //读取页面数据
                        Document doc = Jsoup.connect(detailTaskUrl).get();
                        //来源任务id
                        taskDO.setTaskId((doc.selectFirst(".m-detail-item span:nth-child(1) i").text() + "").trim().replace("#", ""));
                        //解析标题
                        String title = (doc.selectFirst(".m-task-main .m-detail-hd h2").text() + "").trim();
                        taskDO.setTitle(title.substring(0, title.lastIndexOf("复制")));
                        //解析价格
                        String priceStr = (doc.selectFirst(".m-detail-hd .fa-cny").text() + "").trim();
                        BigDecimal price = null;
                        try {
                            price = BigDecimal.valueOf(Double.parseDouble(priceStr)).setScale(2, RoundingMode.HALF_UP);
                            taskDO.setPrice(price);
                        } catch (Exception ex) {
                            log.error(ex);
                        }
                        //解析状态
                        taskDO.setStatus((doc.selectFirst(".step-on .step-title").text() + "").trim());
                        //解析任务类型
                        //解析任务详细
                        taskDO.setDetail((doc.selectFirst(".m-task-arc").text() + "").trim());
                        //客户联系方式
                        //任务发布日期
                        taskDO.setPublishDatetime((doc.selectFirst(".m-detail-item span:nth-child(2)").text() + "").trim().replace("发布于", ""));

                        //查询已经存在的任务
                        LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<TaskDO>();
                        wrapper.eq(true, TaskDO::getTaskId, taskDO.getTaskId());
                        TaskDO existsTask = taskService.getOne(wrapper);
                        if (existsTask != null) {
                            //更新
                            jobInfo.setLog(jobInfo.getLog() + ", 任务来源ID:" + taskDO.getTaskId() + ", 已存在, 更新入库");
                            taskDO.setId(existsTask.getId());
                            taskService.updateById(taskDO);
                        } else {
                            //新增
                            jobInfo.setLog(jobInfo.getLog() + ", 任务来源ID:" + taskDO.getTaskId() + ", 新增入库");
                            taskService.save(taskDO);
                        }
                        jobInfo.setCollectionCount(jobInfo.getCollectionCount() + 1);
                        //dataSourceTransactionManager.commit(transactionStatus);
                    } catch (Exception exception) {
                        log.error(exception);
                    }

                }
            };
            scheduExec.schedule(task.setParam(taskList.get(i)), delay, TimeUnit.MILLISECONDS).get();
        }
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