/*
 *  Copyright 2017, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.rest;

import com.yahoo.bullet.common.BulletConfig;
import com.yahoo.bullet.pubsub.PubSub;
import com.yahoo.bullet.pubsub.PubSubException;
import com.yahoo.bullet.pubsub.PubSubResponder;
import com.yahoo.bullet.pubsub.Publisher;
import com.yahoo.bullet.pubsub.Subscriber;
import com.yahoo.bullet.rest.service.HandlerService;
import com.yahoo.bullet.storage.StorageManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableOAuth2Sso
public class APIConfiguration extends WebSecurityConfigurerAdapter {
    private static final String CORS_MAPPING = "/**";
    /**
     * Enables CORS globally.
     *
     * @return A {@link WebMvcConfigurer} instance with default CORS configuration - allow everything.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping(CORS_MAPPING);
            }
        };
    }

    /**
     * Loads custom responders to use for asynchronous queries.
     *
     * @return A {@link List} of {@link PubSubResponder} instances.
     */
    @Bean
    public List<PubSubResponder> configuredResponders() {
        // TODO: Add configs and load configured PubSubResponders
        return Collections.emptyList();
    }

    /**
     * Creates a synchronous {@link PubSubResponder} instance.
     *
     * @param handlerService The {@link HandlerService} to use.
     * @return A responder to use for synchronous queries.
     */
    @Bean
    public PubSubResponder synchronousResponder(HandlerService handlerService) {
        return handlerService;
    }

    /**
     * Creates the {@link PubSubResponder} instances to use for responding to queries.
     *
     * @param synchronousResponder The required responder to use for synchronous queries.
     * @param configuredResponders The String path to the config file.
     * @return A {@link List} of {@link PubSubResponder} instances.
     */
    @Bean
    public List<PubSubResponder> responders(PubSubResponder synchronousResponder, List<PubSubResponder> configuredResponders) {
        List<PubSubResponder> responders = new ArrayList<>(configuredResponders);
        responders.add(synchronousResponder);
        return responders;
    }

    /**
     * Creates a StorageManager instance from a provided config.
     *
     * @param config The String path to the config file.
     * @return An instance of the particular {@link StorageManager} indicated in the config.
     */
    @Bean
    public StorageManager storageManager(@Value("${bullet.storage.config}") String config) {
        return StorageManager.from(new BulletConfig(config));
    }

    /**
     * Creates a PubSub instance from a provided config.
     *
     * @param config The String path to the config file.
     * @return An instance of the particular {@link PubSub} indicated in the config.
     * @throws PubSubException if there were issues creating the PubSub instance.
     */
    @Bean
    public PubSub pubSub(@Value("${bullet.pubsub.config}") String config) throws PubSubException {
        return PubSub.from(new BulletConfig(config));
    }

    /**
     * Creates the specified number of {@link Publisher} instances from the given PubSub.
     *
     * @param pubSub The {@link PubSub} providing the instances.
     * @param publishers The number of publishers to create using {@link PubSub#getPublishers(int)}.
     * @return A {@link List} of {@link Publisher} provided by the {@link PubSub} instance.
     * @throws PubSubException if there were issues creating the instances.
     */
    @Bean
    public List<Publisher> publishers(PubSub pubSub, @Value("${bullet.pubsub.publishers}") int publishers) throws PubSubException {
        return pubSub.getPublishers(publishers);
    }

    /**
     * Creates the specified number of {@link Subscriber} instances from the given PubSub.
     *
     * @param pubSub The {@link PubSub} providing the instances.
     * @param subscribers The number of subscribers to create using {@link PubSub#getSubscribers(int)}.
     * @return A {@link List} of {@link Subscriber} provided by the {@link PubSub} instance.
     * @throws PubSubException if there were issues creating the instances.
     */
    @Bean
    public List<Subscriber> subscribers(PubSub pubSub, @Value("${bullet.pubsub.subscribers}") int subscribers) throws PubSubException {
        return pubSub.getSubscribers(subscribers);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // Disable CSRF
        http.csrf().disable().authorizeRequests().anyRequest().authenticated();
    }
}
