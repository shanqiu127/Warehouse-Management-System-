package org.example.back.controller;

import org.example.back.common.result.Result;
import org.example.back.service.HomeService;
import org.example.back.vo.HomeSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    private HomeService homeService;

    @GetMapping("/summary")
    public Result<HomeSummaryVO> summary() {
        return Result.success(homeService.summary());
    }
}
