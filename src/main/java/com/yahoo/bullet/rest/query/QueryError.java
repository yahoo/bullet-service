/*
 *  Copyright 2016, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.rest.query;

import com.yahoo.bullet.common.BulletError;
import com.yahoo.bullet.result.Clip;
import com.yahoo.bullet.result.Meta;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class QueryError {
    public static final QueryError TOO_MANY_QUERIES = new QueryError("Too many concurrent queries in the system", "Please try again later");
    public static final QueryError INVALID_QUERY = new QueryError("Failed to parse query", "Please provide a valid query");
    public static final QueryError MISSING_KEY = new QueryError("Asynchronous queries need a unique key", "Please provide a key");
    public static final QueryError UNSUPPORTED_QUERY =
        new QueryError("This particular REST endpoint does not support windowed queries",
                       "Please provide a valid query without a window, or use the SSE or WS endpoints to submit queries with windows");
    public static final QueryError SERVICE_UNAVAILABLE = new QueryError("Service temporarily unavailable", "Please try again later");

    private List<BulletError> errors;

    /**
     * Constructor that takes an error message and resolution for it.
     *
     * @param error The error message.
     * @param resolution The resolution that can be taken.
     */
    public QueryError(String error, String resolution) {
        this.errors = Collections.singletonList(BulletError.makeError(error, resolution));
    }

    /**
     * Constructor that takes a {@link List} of {@link BulletError}.
     *
     * @param errors The list of errors to use.
     */
    public QueryError(List<BulletError> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return Clip.of(Meta.of(errors)).asJSON();
    }
}
