/*
 *  Copyright 2017, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */

package com.yahoo.bullet.rest.common;

import com.yahoo.bullet.pubsub.PubSubMessage;
import com.yahoo.bullet.pubsub.PubSubResponder;
import com.yahoo.bullet.pubsub.Subscriber;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class Reader {
    private Subscriber subscriber;
    private PubSubResponder responder;
    private Thread reader;
    private int sleepTimeMS;
    /**
     * Create a service with a {@link Subscriber} and a request queue.
     *
     * @param subscriber The Subscriber to read responses from.
     * @param responder The {@link PubSubResponder} to use to respond.
     * @param sleepTimeMS The duration to sleep for if PubSub receive is empty. Helps prevent busy waiting.
     */
    public Reader(Subscriber subscriber, PubSubResponder responder, int sleepTimeMS) {
        Objects.requireNonNull(subscriber);
        Objects.requireNonNull(responder);
        this.subscriber = subscriber;
        this.responder = responder;
        this.sleepTimeMS = sleepTimeMS;
        this.reader = new Thread(this::run);
    }

    /**
     * Starts reading from the pubsub.
     */
    public void start() {
        reader.start();
    }

    /**
     * Interrupt the reader thread and close the {@link Subscriber}.
     */
    public void close() {
        reader.interrupt();
    }

    /**
     * Read responses from the Pub/Sub and update requests.
     */
    public void run() {
        PubSubMessage response;
        log.info("Reader thread started, ID: {}", Thread.currentThread().getId());
        while (!Thread.interrupted()) {
            try {
                response = subscriber.receive();
                if (response == null) {
                    Thread.sleep(sleepTimeMS);
                    continue;
                }
                log.debug("Received message {}", response);
                responder.respond(response.getId(), response);
                subscriber.commit(response.getId());
            } catch (Exception e) {
                // When the reader is closed, this block also catches InterruptedException's from Thread.sleep.
                // If the service is busy reading messages, the while loop will break instead.
                log.error("Closing reader thread with error", e);
                break;
            }
        }
        try {
            subscriber.close();
        } catch (Exception e) {
            log.error("Error closing subscriber", e);
        }
    }
}