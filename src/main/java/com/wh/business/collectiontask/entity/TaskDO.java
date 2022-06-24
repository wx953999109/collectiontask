package com.wh.business.collectiontask.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author wh
 * @since 2022-06-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("task")
public class TaskDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 来源任务id
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 标题
     */
    private String title;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 状态
     */
    private String status;

    /**
     * 类型
     */
    private String taskType;

    /**
     * 发布日期
     */
    private String publishDatetime;

    /**
     * 详细需求
     */
    private String detail;

    /**
     * 我的标记
     */
    private Integer flag;

    /**
     * 客户联系方式
     */
    private String customerContact;

    /**
     * 来源平台
     */
    private String platform;

    /**
     * 任务详细url
     */
    private String url;

    /**
     * 任务入库日期
     */
    private Date createDatetime;

    /**
     * 数据更新日期
     */
    private Date updateDatetime;

    /**
     * 是否标记为删除
     */
    private Integer del;


}
