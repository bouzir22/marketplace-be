package com._ach.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

@GetMapping("hello")
    public String hello() {
    return "Hello World";
}

}
