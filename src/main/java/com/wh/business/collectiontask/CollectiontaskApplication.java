package com.wh.business.collectiontask;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import lombok.extern.log4j.Log4j2;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.TransactionDefinition;

@SpringBootApplication
@MapperScan(basePackages = "com.wh.business.collectiontask.mapper")
public class CollectiontaskApplication {
    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;
    @Autowired
    private TransactionDefinition transactionDefinition;

    public static void main(String[] args) {
        SpringApplication.run(CollectiontaskApplication.class, args);
    }
}
