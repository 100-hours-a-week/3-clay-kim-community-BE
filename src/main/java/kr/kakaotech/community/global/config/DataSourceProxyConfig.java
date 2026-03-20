package kr.kakaotech.community.global.config;

import kr.kakaotech.community.global.monitoring.QueryCountListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceProxyConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && !(bean instanceof net.ttddyy.dsproxy.support.ProxyDataSource)) {
            return ProxyDataSourceBuilder.create(dataSource)
                    .name("queryCountProxy")
                    .listener(new QueryCountListener())
                    .build();
        }
        return bean;
    }
}
