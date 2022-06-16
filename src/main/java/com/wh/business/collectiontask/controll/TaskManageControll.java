package com.wh.business.collectiontask.controll;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
public class TaskManageControll {

    @PostMapping("taskStatus")
    String taskStatus(String platfrom){
        return  platfrom;
    }
}