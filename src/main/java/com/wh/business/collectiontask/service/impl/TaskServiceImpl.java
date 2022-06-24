package com.wh.business.collectiontask.service.impl;

import com.wh.business.collectiontask.entity.TaskDO;
import com.wh.business.collectiontask.mapper.TaskMapper;
import com.wh.business.collectiontask.service.TaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wh
 * @since 2022-06-24
 */
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, TaskDO> implements TaskService {

}
