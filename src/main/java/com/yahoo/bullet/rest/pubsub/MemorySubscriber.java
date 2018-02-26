/*
 *  Copyright 2017, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.rest.pubsub;
import com.yahoo.bullet.BulletConfig;
import com.yahoo.bullet.pubsub.BufferingSubscriber;
import com.yahoo.bullet.pubsub.PubSubException;
import com.yahoo.bullet.pubsub.PubSubMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MemorySubscriber extends BufferingSubscriber {
    MemoryPubSubConfig config;
    List<String> URIs;
    HttpClient client;

    public MemorySubscriber(BulletConfig config, int maxUncommittedMessages) {
        super(maxUncommittedMessages);
        this.config = new MemoryPubSubConfig(config);

        this.URIs = getURIs();
        this.client = HttpClients.createDefault();
    }

    @Override
    public List<PubSubMessage> getMessages() throws PubSubException {
        List<PubSubMessage> messages = new ArrayList<>();
        for (String uri : URIs) {
            try {
                //HttpPost post = getPost(uri);
                HttpGet get = new HttpGet(uri);
                //HttpResponse response = client.execute(post);
                HttpResponse response = client.execute(get);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == HttpStatus.SC_NO_CONTENT) {
                    // SC_NO_CONTENT status (204) indicates there are no new messages
                    continue;
                }
                if (statusCode != HttpStatus.SC_OK) {
                    // Can't throw error here because often times the first few calls return error codes until the service comes up
                    log.error("Http call failed with status code {} and response {}.", statusCode, response);
                    continue;
                }
                messages.add(PubSubMessage.fromJSON(readResponseContent(response)));
            } catch (Exception e){
                // Can't throw error here because often times the first few calls return error codes until the service comes up
                log.error("Http post failed with error: " + e);
                //throw new PubSubException("Http post failed with error: " + e);
            }
        }
        return messages;
    }

    private String readResponseContent(HttpResponse response) throws UnsupportedOperationException, IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuffer result = new StringBuffer();
        String line = null;
        while ((line = rd.readLine()) != null) {
            result.append(line);
            result.append('\n');
        }
        return result.toString();
    }

    /**
     * Each subclass should implement this function to return the appropriate list of complete URIs from which to read.
     * The backend can read from all in-memory pubsub hosts, the WS should just read from the single in-memory pubsub
     * instance that is running on that host.
     * @return The list of URIs from which to read.
     */
    protected abstract List<String> getURIs();

    @Override
    public void close() {
        // Probably do nothing?
    }
}