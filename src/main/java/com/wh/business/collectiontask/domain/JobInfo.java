package com.wh.business.collectiontask.domain;

import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author: wh
 * @date:2022/6/23 18:04
 * @Description:
 */
@Data
public
class JobInfo {
    String platform;
    int collectionCount;
    String startDateTime;
    String stopDateTime;
    boolean running;

    String log;

    public void setRunning(boolean running) {
        this.running = running;

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        if (running) {
            this.startDateTime = formatter.format(date);
        } else {
            this.stopDateTime = formatter.format(date);
        }
    }

    public JobInfo(String platform) {
        this.platform = platform;
    }

}