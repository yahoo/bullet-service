/*
 *  Copyright 2017 Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.rest.controller;

import com.yahoo.bullet.pubsub.PubSubMessage;
import com.yahoo.bullet.rest.query.HTTPQueryHandler;
import com.yahoo.bullet.rest.query.QueryError;
import com.yahoo.bullet.rest.query.SseQueryHandler;
import com.yahoo.bullet.rest.service.QueryService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class QueryControllerTest extends AbstractTestNGSpringContextTests {
    @Autowired @InjectMocks
    private QueryController controller;
    @Mock
    private QueryService service;
    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void testSend() throws Exception {
        String query = "foo";
        CompletableFuture<String> response = controller.submitQuery(query);

        ArgumentCaptor<HTTPQueryHandler> argument = ArgumentCaptor.forClass(HTTPQueryHandler.class);
        verify(service).submit(anyString(), eq(query), argument.capture());
        argument.getValue().send(new PubSubMessage("", "bar"));
        Assert.assertEquals(response.get(), "bar");
    }

    @Test
    public void testIllegalQuery() throws Exception {
        CompletableFuture<String> response = controller.submitQuery(null);

        Assert.assertEquals(response.get(), QueryError.INVALID_QUERY.toString());
    }

    @Test
    public void testStreamingQuery() throws Exception {
        String query = "foo";

        MvcResult result = mockMvc.perform(
                post("/streaming")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(query)
        ).andReturn();

        ArgumentCaptor<SseQueryHandler> argument = ArgumentCaptor.forClass(SseQueryHandler.class);
        verify(service).submit(anyString(), eq(query), argument.capture());
        argument.getValue().send(new PubSubMessage("", "bar"));
        Assert.assertEquals(result.getResponse().getContentAsString(), "data:bar\n\n");

        argument.getValue().send(new PubSubMessage("", "baz"));
        Assert.assertEquals(result.getResponse().getContentAsString(), "data:bar\n\ndata:baz\n\n");
    }
}
