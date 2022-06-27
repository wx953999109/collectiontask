package com.wh.business.collectiontask.schedulejob;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.business.collectiontask.controller.TaskManageControll;
import com.wh.business.collectiontask.domain.IMyRunnable;
import com.wh.business.collectiontask.domain.JobInfo;
import com.wh.business.collectiontask.entity.TaskDO;
import com.wh.business.collectiontask.service.TaskService;
import com.wh.business.collectiontask.util.ApplicationContextUtils;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
@Log4j2
public class CollectionJobZBJ implements InterruptableJob {

    final String host = "https://task.zbj.com/";
    JobInfo jobInfo;
    TaskService taskService;
    ScheduledExecutorService scheduExec = Executors.newScheduledThreadPool(1);
    private DataSourceTransactionManager dataSourceTransactionManager;
    private TransactionDefinition transactionDefinition;

    int delay = 5000;

    CollectionJobZBJ() {
        taskService = (TaskService) ApplicationContextUtils.getBean(TaskService.class);
        transactionDefinition = (TransactionDefinition) ApplicationContextUtils.getBean(TransactionDefinition.class);
        dataSourceTransactionManager = (DataSourceTransactionManager) ApplicationContextUtils.getBean(DataSourceTransactionManager.class);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        System.out.println("zbj:" + System.currentTimeMillis());

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
            Document doc = Jsoup.connect(host + "page1.html").get();
            int pageCount = Integer.parseInt(Objects.requireNonNull(doc.selectFirst("[data-linkid=task-list-to-page-last]")).text());
            for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                jobInfo.setLog("加载第" + pageIndex + "页数据");
                //过会再访问
                Thread.sleep(delay);
                List<String> list = ReadTaskList(host + "page" + pageIndex + ".html");
                //读取当前页所有任务详细数据
                ReadDetailRead(pageIndex, pageCount, list);
            }
            jobInfo.setLog("全部读取完毕, 等待重新开始");
        } catch (Exception ex) {
            System.out.println(ex);
            log.error(ex);
        }
    }

    /**
     * 读取列表页中的所有任务链接
     *
     * @param url
     * @throws Exception
     */
    List<String> ReadTaskList(String url) throws Exception {
        List<String> list = new ArrayList<String>();
        Document doc = Jsoup.connect(url).get();

        Elements links = doc.select(".go-to-detail");
        for (Element link : links) {
            String detailTaskUrl = link.attr("href");
            if (!detailTaskUrl.startsWith("http")) {
                detailTaskUrl = "https:" + detailTaskUrl;
            }
            //将任务详细页面的链接存放到集合里面
            list.add(detailTaskUrl);
        }
        return list;
    }

    /**
     * 读取任务详细页面
     *
     * @throws Exception
     */
    private void ReadDetailRead(int pageNumber, int pageCount, List<String> listUrl) throws Exception {
        for (int i = 0; i < listUrl.size(); i++) {
            jobInfo.setLog("加载任务详细, 第" + pageNumber + "/" + pageCount + "页, " + (i + 1) + "/" + listUrl.size() + "个");
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
                        String taskId = (doc.selectFirst(".thrid-module-crumbs-container .color").text() + "").trim();
                        taskDO.setTaskId(taskId.substring(taskId.indexOf("[") + 1, taskId.indexOf("]")));
                        //解析标题
                        taskDO.setTitle((doc.selectFirst(".order-header-title").text() + "").trim());
                        //解析价格
                        String priceStr = (doc.selectFirst(".order-header-price").text() + "").trim().replace("¥", "");
                        BigDecimal price = null;
                        try {
                            price = BigDecimal.valueOf(Double.parseDouble(priceStr)).setScale(2, RoundingMode.HALF_UP);
                            taskDO.setPrice(price);
                        } catch (Exception ex) {
                            System.out.println(ex);
                            log.error(ex);
                        }
                        //解析状态
                        String status = (doc.selectFirst(".third-module-order-detail .order-footer .button-group a").text() + "").trim();
                        if (status.equals("参与类似任务")) {
                            taskDO.setStatus("完成");
                        } else {
                            taskDO.setStatus("进行中");
                        }
                        //解析任务类型
                        Elements modules = doc.select(".thrid-module-crumbs-container a[target]");
                        modules.addAll(doc.select(".order-header-tags span[data-linkid]"));
                        String type = modules.eachText().stream().distinct().collect(Collectors.joining(","));
                        taskDO.setTaskType(type);
                        //解析任务详细
                        Element element = doc.selectFirst(".content-text");
                        if (element != null) {
                            taskDO.setDetail((element.text() + "").trim());
                        }
                        //客户联系方式
                        //任务发布日期 暂时没找到在哪里, 或没有
                        //taskDO.setPublishDatetime((doc.selectFirst(".m-detail-item span:nth-child(2)").text() + "").trim().replace("发布于", ""));

                        //查询已经存在的任务
                        LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<TaskDO>();
                        wrapper.eq(TaskDO::getTaskId, taskDO.getTaskId());
                        wrapper.eq(TaskDO::getPlatform, taskDO.getPlatform());
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
                        System.out.println(exception);
                        log.error(exception);
                    }

                }
            };
            scheduExec.schedule(task.setParam(listUrl.get(i)), delay, TimeUnit.MILLISECONDS).get();
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