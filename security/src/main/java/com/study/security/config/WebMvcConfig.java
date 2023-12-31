package com.study.security.config;

import org.springframework.boot.web.servlet.view.MustacheViewResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // IoC로 등록하기 위해.
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void configureViewResolvers(ViewResolverRegistry registry) {
    MustacheViewResolver resolver = new MustacheViewResolver();
    resolver.setCharset("UTF-8");
    resolver.setContentType("text/html; charset=UTF-8");
    resolver.setPrefix("classpath:/templates/");
    resolver.setSuffix(".html"); // .html 파일도 머스테치가 인식을 하게 됨.

    registry.viewResolver(resolver); // 뷰 리졸버 등록
  }
}
