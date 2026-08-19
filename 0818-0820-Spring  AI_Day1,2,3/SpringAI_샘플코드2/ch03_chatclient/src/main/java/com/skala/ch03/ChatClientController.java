package com.skala.ch03;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ch03")
public class ChatClientController {

    private final HelloAiService service;

    public ChatClientController(HelloAiService service) {
        this.service = service;
    }

    /** curl 'localhost:8080/ch03/ask?q=Spring AI가 뭔가요' */
    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam String q) {
        return Map.of("answer", service.ask(q));
    }

    @GetMapping("/ask-as")
    public Map<String, String> askAs(@RequestParam String role, @RequestParam String q) {
        return Map.of("answer", service.askAs(role, q));
    }

    @GetMapping("/translate")
    public Map<String, String> translate(@RequestParam String text,
                                         @RequestParam(defaultValue = "영어") String lang) {
        return Map.of("translated", service.translate(text, lang));
    }
}
