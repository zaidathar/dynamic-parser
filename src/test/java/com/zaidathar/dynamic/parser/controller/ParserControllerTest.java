package com.zaidathar.dynamic.parser.controller;

import com.zaidathar.dynamic.parser.service.ParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParserController.class)
@DisplayName("ParserController Tests")
public class ParserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParserService parserService;

    private static final String PARSER_ENDPOINT = "/api/parse";

    private static final String SAMPLE_PARSER_PAYLOAD = """
        {
           "name": "John",
           "age": 32,
           "profile": "https://someurl.com/123.jpeg",
           "contact": "090910"
        }
    """;


    @Test
    void testParserEndpoint_HappyFlow() throws Exception {
        mockMvc.perform(post(PARSER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE_PARSER_PAYLOAD))
                .andExpect(status().isOk());

        verify(parserService, only()).convert(any(),any(),any());
    }

}
