package com.wh.business.collectiontask.domain;

/**
 * @author: wh
 * @date:2022/6/24 20:10
 * @Description:
 */
public interface IMyRunnable<T> extends Runnable {
    public IMyRunnable setParam(T... param);
}