package com.flashsale.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.master")
    public DataSourceProperties masterDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.slave")
    public DataSourceProperties slaveDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource masterDataSource() {
        return masterDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public DataSource slaveDataSource() {
        return slaveDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public DataSource routingDataSource(@Qualifier("masterDataSource") DataSource masterDataSource,
                                        @Qualifier("slaveDataSource") DataSource slaveDataSource) {
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(DataSourceType.MASTER, masterDataSource);
        dataSourceMap.put(DataSourceType.SLAVE, slaveDataSource);

        AbstractRoutingDataSource routingDataSource = new DynamicRoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource routingDataSource) {
        return new DataSourceTransactionManager(routingDataSource);
    }
}
