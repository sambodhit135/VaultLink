package com.vaultlink.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Full JPA configuration with explicit entity package scanning.
 * Required because VaultlinkApplication lives in com.vaultlink.vaultlink (a sub-package),
 * so Hibernate cannot auto-discover entities without explicit packagesToScan.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.vaultlink.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class JpaConfig {

    @Autowired
    private DataSource dataSource;

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.MySQLDialect}")
    private String dialect;

    @Value("${spring.jpa.show-sql:true}")
    private boolean showSql;

    @Value("${spring.jpa.properties.hibernate.format_sql:true}")
    private String formatSql;

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.vaultlink.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(showSql);
        emf.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.format_sql", formatSql);
        emf.setJpaPropertyMap(props);

        return emf;
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
