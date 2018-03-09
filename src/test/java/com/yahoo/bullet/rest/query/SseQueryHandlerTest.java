/*
 *  Copyright 2018 Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.rest.query;

import com.yahoo.bullet.pubsub.Metadata;
import com.yahoo.bullet.pubsub.PubSubMessage;
import com.yahoo.bullet.rest.service.QueryService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SseQueryHandlerTest extends AbstractTestNGSpringContextTests {
    @Mock
    private QueryService queryService;
    @Mock
    private SseEmitter sseEmitter;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSendOnMessage() throws Exception {
        PubSubMessage message = new PubSubMessage("id", "foo");

        SseQueryHandler sseQueryHandler = new SseQueryHandler("id", sseEmitter, queryService);
        sseQueryHandler.send(message);

        verify(sseEmitter).send(eq("foo"));
        Assert.assertFalse(sseQueryHandler.isComplete());
    }

    @Test
    public void testSendOnException() throws Exception {
        PubSubMessage message = new PubSubMessage("id", "foo");

        doThrow(new IOException()).when(sseEmitter).send(message.getContent());
        SseQueryHandler sseQueryHandler = new SseQueryHandler("id", sseEmitter, queryService);
        sseQueryHandler.send(message);

        verify(queryService).submitSignal("id", Metadata.Signal.KILL);
        Assert.assertTrue(sseQueryHandler.isComplete());
    }

    @Test
    public void testSendAfterComplete() throws Exception {
        SseQueryHandler sseQueryHandler = new SseQueryHandler("id", sseEmitter, queryService);
        sseQueryHandler.complete();
        sseQueryHandler.send(new PubSubMessage("id", "foo"));

        verify(sseEmitter, never()).send(any());
        verify(queryService, never()).submitSignal(any(), any());
        Assert.assertTrue(sseQueryHandler.isComplete());
    }

    @Test
    public void testFailOnCause() throws Exception {
        SseQueryHandler sseQueryHandler = new SseQueryHandler("id", sseEmitter, queryService);
        sseQueryHandler.fail(QueryError.INVALID_QUERY);

        verify(sseEmitter).send(eq(QueryError.INVALID_QUERY.toString()));
        Assert.assertTrue(sseQueryHandler.isComplete());
    }

    @Test
    public void testFailOnException() throws Exception {
        doThrow(new IOException()).when(sseEmitter).send(QueryError.INVALID_QUERY.toString());
        SseQueryHandler sseQueryHandler = new SseQueryHandler("id", sseEmitter, queryService);
        sseQueryHandler.fail(QueryError.INVALID_QUERY);

        verify(queryService).submitSignal("id", Metadata.Signal.KILL);
        Assert.assertTrue(sseQueryHandler.isComplete());
    }

    @Test
    public void testFailAfterComplete() throws Exception {
        SseQueryHandler sseQueryHandler = new SseQueryHandler("id", sseEmitter, queryService);
        sseQueryHandler.complete();
        sseQueryHandler.fail(QueryError.INVALID_QUERY);

        verify(sseEmitter, never()).send(any());
        verify(queryService, never()).submitSignal(any(), any());
        Assert.assertTrue(sseQueryHandler.isComplete());
    }
}
