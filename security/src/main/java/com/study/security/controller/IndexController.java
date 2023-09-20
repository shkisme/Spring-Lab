package com.study.security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // View를 리턴하는.
public class IndexController {

  @GetMapping({"", "/"})
  public String index() {
    // Mustache 기볼 폴더는 src/main/resources/
    // view resolver 설정 : application.yml에서 설정.
    return "index"; // src/main/resources/templates/index.mustache
  }
}
