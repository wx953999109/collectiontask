package com.wh.business.collectiontask.controll;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Component
@Controller
public class PageRouterControll {

    @GetMapping("index")
    String index(Model model){
        return  "index";
    }

    @GetMapping("taskManage")
    String taskManage(Model model){
        return  "taskManage";
    }
}