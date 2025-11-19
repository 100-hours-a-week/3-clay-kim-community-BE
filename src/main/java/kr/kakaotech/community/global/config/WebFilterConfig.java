package kr.kakaotech.community.global.config;

import jakarta.servlet.Filter;
import kr.kakaotech.community.auth.AuthFilter;
import kr.kakaotech.community.global.filter.CorsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebFilterConfig {

    private final AuthFilter authFilter;
    private final CorsFilter corsFilter;

    public WebFilterConfig(AuthFilter authFilter, CorsFilter corsFilter) {
        this.authFilter = authFilter;
        this.corsFilter = corsFilter;
    }

    @Bean
    public FilterRegistrationBean<Filter> corsFilterRegistration() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(corsFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(0);  // AuthFilter보다 먼저 실행

        return filterRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean<Filter> jwtAuthFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(authFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(1);

        return filterRegistrationBean;
    }
}
