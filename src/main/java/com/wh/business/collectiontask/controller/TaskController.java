package com.wh.business.collectiontask.controller;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.business.collectiontask.domain.R;
import com.wh.business.collectiontask.entity.TaskDO;
import com.wh.business.collectiontask.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author wh
 * @since 2022-06-24
 */
@RestController
@RequestMapping("task")
public class TaskController {
    @Autowired
    TaskService taskService;

    @PostMapping("page")
    public R Page(Page<TaskDO> p) {
        Page<TaskDO> page = taskService.page(p);
        return R.success(page);
    }

    @PostMapping("saveTask")
    public R saveTask(TaskDO task) {
        LambdaUpdateWrapper<TaskDO> luw = new LambdaUpdateWrapper<TaskDO>();
        luw.eq(TaskDO::getId, task.getId());
        luw.set(TaskDO::getFlag, task.getFlag());
        luw.set(TaskDO::getRemark, task.getRemark());
        luw.set(TaskDO::getCustomerContact, task.getCustomerContact());
        if (taskService.update(luw)) {
            return R.success();
        } else {
            return R.error();
        }
    }
}
