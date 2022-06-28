package com.wh.business.collectiontask.schedulejob;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.business.collectiontask.controller.TaskManageControll;
import com.wh.business.collectiontask.domain.IMyRunnable;
import com.wh.business.collectiontask.domain.JobInfo;
import com.wh.business.collectiontask.entity.TaskBlackNameListDO;
import com.wh.business.collectiontask.entity.TaskDO;
import com.wh.business.collectiontask.service.TaskBlackNameListService;
import com.wh.business.collectiontask.service.TaskService;
import com.wh.business.collectiontask.util.ApplicationContextUtils;
import lombok.extern.log4j.Log4j2;
import org.jsoup.HttpStatusException;
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
public class CollectionJobYPWK implements InterruptableJob {

    final String host = "https://task.epwk.com/";
    JobInfo jobInfo;
    TaskService taskService;
    TaskBlackNameListService taskBlackNameListService;
    ScheduledExecutorService scheduExec = Executors.newScheduledThreadPool(1);
    private DataSourceTransactionManager dataSourceTransactionManager;
    private TransactionDefinition transactionDefinition;

    int delay = 3000;

    CollectionJobYPWK() {
        taskService = (TaskService) ApplicationContextUtils.getBean(TaskService.class);
        taskBlackNameListService = (TaskBlackNameListService) ApplicationContextUtils.getBean(TaskBlackNameListService.class);
        transactionDefinition = (TransactionDefinition) ApplicationContextUtils.getBean(TransactionDefinition.class);
        dataSourceTransactionManager = (DataSourceTransactionManager) ApplicationContextUtils.getBean(DataSourceTransactionManager.class);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        System.out.println("ypwk:" + System.currentTimeMillis());

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
            int pageCount = Integer.parseInt(Objects.requireNonNull(doc.selectFirst(".el-pager li:last-of-type")).text());
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

        Elements links = doc.select(".content-lists .itemblock .topH .title a");
        for (Element link : links) {
            String detailTaskUrl = link.attr("href");
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
            LambdaQueryWrapper<TaskBlackNameListDO> lqw = new LambdaQueryWrapper<TaskBlackNameListDO>();
            lqw.eq(TaskBlackNameListDO::getUrl, listUrl.get(i));
            if (taskBlackNameListService.count(lqw) > 0) {
                jobInfo.setLog(jobInfo.getLog() + ", 在黑名单, 已跳过");
                continue;
            }
            IMyRunnable<String> task = new IMyRunnable<String>() {
                String detailTaskUrl;

                @Override
                public IMyRunnable setParam(String... param) {
                    detailTaskUrl = param[0];
                    return this;
                }

                @Override
                public void run() {
                    log.info(Thread.currentThread().getName() + "\t" + detailTaskUrl);
                    //TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                    TaskDO taskDO = new TaskDO();
                    //任务详细url
                    taskDO.setUrl(detailTaskUrl);
                    try {
                        //来源平台
                        taskDO.setPlatform(jobInfo.getPlatform());
                        //读取页面数据
                        Document doc = Jsoup.connect(detailTaskUrl).get();

                        if (doc.select(".tasktodetail .clearfix .f_l .clearfix b").text().contains("该任务为直接雇佣")) {
                            TaskBlackNameListDO blackName = new TaskBlackNameListDO();
                            blackName.setUrl(detailTaskUrl);
                            //上面已经判断过, 这里不用判断是否存在了, 应该是不存在的
//                            if (taskBlackNameListService.count(lqw) == 0) {
//                                taskBlackNameListService.saveOrUpdate(blackName);
//                            }
                            taskBlackNameListService.saveOrUpdate(blackName);
                            return;
                        }

                        //来源任务id
                        String taskId = detailTaskUrl.replace(host, "").replace("/", "");
                        taskDO.setTaskId(taskId);
                        //解析标题
                        taskDO.setTitle((doc.selectFirst(".taskend .font30 a").text() + "").trim());
                        //解析价格
                        String priceStr = (doc.selectFirst(".task_user_info .nummoney span").text() + "").trim()
                                .replace("￥", "").trim();
                        if (priceStr.contains("-")) {
                            priceStr = priceStr.substring(priceStr.indexOf("-") + 1);
                        }
                        if (priceStr.contains("万")) {
                            priceStr = priceStr.replace("万", "0000");
                        }
                        BigDecimal price = null;
                        try {
                            price = BigDecimal.valueOf(Double.parseDouble(priceStr)).setScale(2, RoundingMode.HALF_UP);
                            taskDO.setPrice(price);
                        } catch (Exception ex) {
                            System.out.println(ex);
                            log.error(ex);
                        }
                        //解析状态
                        String status = (doc.selectFirst(".task-progress-item .crent b").text() + "").trim();
                        taskDO.setStatus(status);
                        //解析任务类型
                        String type = doc.select(".crumbs .f_l span").text();
                        type = type.replace("当前位置： 首页 > 所有任务 > ", "").replace(">", ",").replace(" ", "");
                        type = type.substring(0, type.lastIndexOf(","));
                        taskDO.setTaskType(type);
                        //解析任务详细
                        Element element = doc.selectFirst(".task-info-content");
                        if (element != null) {
                            taskDO.setDetail((element.text() + "").trim());
                        }
                        //客户联系方式
                        //任务发布日期
                        taskDO.setPublishDatetime((doc.selectFirst(".task-progress-item .step_on font").text() + "").trim().replace("发布于", ""));

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
                    } catch (HttpStatusException igion) {

                    } catch (Exception exception) {
                        log.error(detailTaskUrl);
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