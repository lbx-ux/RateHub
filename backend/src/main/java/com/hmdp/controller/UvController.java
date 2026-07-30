package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.dto.UvVo;
import com.hmdp.service.UvService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/uv")
public class UvController {
    private final UvService uvService;

    @GetMapping("/stats")
    public Result<UvVo> stats(){
        return Result.success(uvService.stats());
    }


}
