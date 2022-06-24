package com.wh.business.collectiontask.schedulejob;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wh.business.collectiontask.controller.TaskManageControll;
import com.wh.business.collectiontask.domain.IMyRunnable;
import com.wh.business.collectiontask.domain.JobInfo;
import com.wh.business.collectiontask.entity.TaskDO;
import com.wh.business.collectiontask.service.TaskService;
import com.wh.business.collectiontask.util.ApplicationContextUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@DisallowConcurrentExecution
public class CollectionJobA5 implements Job {

    final String A5Host = "https://www.a5.cn/";
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

    int delay = 2000;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        System.out.println("a5:" + System.currentTimeMillis());

        JobDataMap jobDetailMap = context.getJobDetail().getJobDataMap();
        JobDataMap triggerMap = context.getTrigger().getJobDataMap();
        JobDataMap mergeMap = context.getMergedJobDataMap();

        String platform = jobDetailMap.getString("platform");
        jobInfo = TaskManageControll.jobs.get(platform);

        try {
            //读取任务列表页数
            jobInfo.setLog("读取页数");
            Document doc = Jsoup.connect("https://www.a5.cn/tasklist-o-1-page-1.html").get();
            Elements tmpLis = doc.select(".m-page-nums .pagination li");
            int pageCount = Integer.parseInt(tmpLis.get(tmpLis.size() - 2).text());
            for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                jobInfo.setLog("加载第" + pageIndex + "页数据");
                //过会再访问
                Thread.sleep(delay);
                ReadTaskList("https://www.a5.cn/tasklist-o-1-page-" + pageIndex + ".html");
            }

            jobInfo.setLog("读取所有任务详细数据");
            //过会再访问
            Thread.sleep(delay);
            //读取所有任务详细数据
            ReadDetailRead();
        } catch (Exception ex) {
            System.out.println(ex);
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
                        taskDO.setTaskId((doc.select(".m-detail-item span:nth-child(1) i").get(0).text() + "").trim().replace("#", ""));
                        //解析标题
                        String title = (doc.select(".m-task-main .m-detail-hd h2").get(0).text() + "").trim();
                        taskDO.setTitle(title.substring(0, title.lastIndexOf("复制")));
                        //解析价格
                        String priceStr = (doc.select(".m-detail-hd .fa-cny").get(0).text() + "").trim();
                        BigDecimal price = null;
                        try {
                            price = BigDecimal.valueOf(Double.parseDouble(priceStr)).setScale(2, RoundingMode.HALF_UP);
                            taskDO.setPrice(price);
                        } catch (Exception ignore) {
                        }
                        //解析状态
                        taskDO.setStatus((doc.select(".step-on .step-title").get(0).text() + "").trim());
                        //解析任务类型
                        //解析任务详细
                        taskDO.setDetail((doc.select(".m-task-arc").get(0).text() + "").trim());
                        //客户联系方式
                        //任务发布日期
                        taskDO.setPublishDatetime((doc.select(".m-detail-item span:nth-child(2)").get(0).text() + "").trim().replace("发布于", ""));
                    } catch (Exception exception) {
                        taskDO.setStatus("读取异常");
                        taskDO.setDetail(exception.toString());
                        taskService.save(taskDO);
                    }
                    //查询已经存在的任务
                    LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<TaskDO>();
                    wrapper.eq(true, TaskDO::getTaskId, taskDO.getTaskId());
                    TaskDO existsTask = taskService.getOne(wrapper);
                    if (existsTask != null) {
                        //更新
                        taskDO.setId(existsTask.getId());
                        taskService.updateById(taskDO);
                    } else {
                        //新增
                        taskService.save(taskDO);
                    }
                    //dataSourceTransactionManager.commit(transactionStatus);
                }
            };
            scheduExec.schedule(task.setParam(taskList.get(i)), delay, TimeUnit.MILLISECONDS).get();
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

        Elements links = doc.select(".m-tk-list").get(0).select("h3 a");
        if (links.size() == 0) return;
        for (Element link : links) {
            String path = link.attr("href").toString();
            String detailTaskUrl = A5Host + path;
            //将任务详细页面的链接存放到集合里面
            taskList.add(detailTaskUrl);
        }
    }
}