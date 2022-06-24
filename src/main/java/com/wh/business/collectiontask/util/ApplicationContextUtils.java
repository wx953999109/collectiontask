package com.wh.business.collectiontask.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author: wh
 * @date:2022/6/24 19:14
 * @Description:
 */
@Component
public class ApplicationContextUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    // 根据name获取相应的bean
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    public static Object getBean(Class c) {
        return applicationContext.getBean(c);
    }
}
