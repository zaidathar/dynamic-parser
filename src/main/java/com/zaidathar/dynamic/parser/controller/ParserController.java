package com.zaidathar.dynamic.parser.controller;

import com.zaidathar.dynamic.parser.dto.UserData;
import com.zaidathar.dynamic.parser.dto.UserProfile;
import com.zaidathar.dynamic.parser.service.ParserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parse")
public class ParserController {
    private final ParserService service;

    public ParserController(ParserService service) {
        this.service = service;
    }

    @PostMapping
    public Object convert(@RequestBody UserData data) throws Exception {
        return service.convert(data, "UserData->UserProfile", UserProfile.class);
    }
}
