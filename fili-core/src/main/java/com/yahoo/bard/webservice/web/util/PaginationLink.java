// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.web.util;

import com.yahoo.bard.webservice.util.pagination.Pagination;
import rx.Observable;

/**
 * Enumerates the names of the page links that may show up in the headers and/or bodies of responses that contain
 * pagination.
 */
public enum PaginationLink {
    FIRST("first", "first") {
       @Override
        public Observable<Integer> getPage(Pagination<?> page) {
           return page.getFirstPage();
       }
    },
    LAST("last", "last") {
       @Override
        public Observable<Integer> getPage(Pagination<?> page) {
           return page.getLastPage();
       }
    },
    NEXT("next", "next") {
       @Override
        public Observable<Integer> getPage(Pagination<?> page) {
           return page.getNextPage();
       }
    },
    PREVIOUS("prev", "previous") {
       @Override
        public Observable<Integer> getPage(Pagination<?> page) {
           return page.getPreviousPage();
       }
    };

    private final String headerName;
    private final String bodyName;

    /**
     * Constructor.
     *
     * @param headerName  Name of the link in a header
     * @param bodyName  Name of the link in the body of a response
     */
    PaginationLink(String headerName, String bodyName) {
        this.headerName = headerName;
        this.bodyName = bodyName;
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getBodyName() {
        return bodyName;
    }

    /**
     * Get the page number if the specified page exists relative to the current page.
     *
     * @param page  Current page to evaluate
     *
     * @return a populated Optional if the page exists based on the current page, empty if not
     */
    public abstract Observable<Integer> getPage(Pagination<?> page);
}
