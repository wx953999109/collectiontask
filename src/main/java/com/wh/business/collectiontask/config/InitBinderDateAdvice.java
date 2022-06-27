package com.wh.business.collectiontask.config;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author: wh
 * @date:2022/6/27 0:22
 * @Description:
 */
@ControllerAdvice
public class InitBinderDateAdvice {
    /**
     * 将前台传递过来的日期格式的字符串，自动转化为时间类型
     * 拦截不到@RequestBody注解修饰的参数
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        //java.util.Date类型格式化
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));

        //java.time.LocalDateTime类型格式化
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (!StringUtils.isEmpty(text)) {
                    setValue(LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
            }
        });
    }
}
