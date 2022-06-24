package com.wh.business.collectiontask;

/**
 * @author: wh
 * @date:2022/6/24 20:57
 * @Description:
 */

import lombok.extern.log4j.Log4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class TimerManager {
    //

    private final ScheduledExecutorService scheduExec;

    public TimerManager() {

        this.scheduExec = Executors.newScheduledThreadPool(1);
    }

    public ScheduledFuture<String> timerOne() throws Exception {
        return scheduExec.schedule(() -> {

            System.out.println("timerOne invoked .....");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "";
        }, 500, TimeUnit.MILLISECONDS);
    }

    public void timerTwo() throws Exception {
        System.out.println("timerTwo init .....");
        scheduExec.schedule(new Runnable() {
            public void run() {

                System.out.println("timerTwo invoked .....");
            }
        }, 1000, TimeUnit.MILLISECONDS);

    }

    public static void main(String[] args) throws Exception {
        TimerManager test = new TimerManager();
        test.timerOne();
        test.timerTwo();
//        test.timerTwo();
    }
}