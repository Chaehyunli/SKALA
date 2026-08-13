package com.sk.skala.myapp.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.skala.myapp.aspect.Metrics;
import com.sk.skala.myapp.service.AsyncService;

@RestController
@RequestMapping("/api")
public class AsyncController {

    private final AsyncService asyncService;

    public AsyncController(AsyncService asyncService) {
        this.asyncService = asyncService;
    }

    @GetMapping("/future")
    @Metrics
    public CompletableFuture<String> callAsyncFuture(@RequestParam String message) {
        CompletableFuture<String> future = asyncService.asyncMethodWithReturn(message);
        return future;
    }

    @GetMapping("/void")
    @Metrics
    public String callAsyncVoid(@RequestParam String message) {
        asyncService.asyncMethodWithoutReturn(message);
        return "비동기 작업이 시작되었습니다. (반환값 없음)";
    }
}
