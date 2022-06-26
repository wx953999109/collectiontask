package com.wh.business.collectiontask.controller;


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
}
