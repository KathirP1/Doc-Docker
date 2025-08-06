package com.pdfreader.pdfreader.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FrontendController {

    // Matches everything except files with a dot (.)
    @GetMapping("/{path:^(?!api|static|assets|index\\.html).*$}")
    public String redirect(@PathVariable String path) {
        return "forward:/index.html";
    }
}
