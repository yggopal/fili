// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.util;

import com.yahoo.bard.webservice.web.PageNotFoundException;
import com.yahoo.bard.webservice.web.util.PaginationParameters;
import org.apache.commons.lang3.tuple.ImmutablePair;
import rx.Observable;

import java.util.Collections;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * Pagination class that contains all matching results of a query.
 * An instance of AllPagesPagination is responsible for performing pagination, expects
 * the full data set, and perform the trimming itself.
 * This is different from SinglePagePagination which expects an already paginated result set.
 *
 * @param <T> collection type
 */
public class Pagination<T> {

    private static final int FIRST_PAGE = 1;

    // Collection to be paginated
    private final Observable<T> page;
    private final PaginationParameters paginationParameters;
    private final Observable<Integer> numResults;

    /**
     * Constructor.
     *
     * @param pageOfData  The page of data requested.
     *
     * @throws PageNotFoundException if pageToFetch is greater than the number of pages.
     */
    public Pagination(Observable<T> page, PaginationParameters paginationParameters, Observable<Integer> numResults)
            throws PageNotFoundException {
        this.paginationParameters = paginationParameters;
        this.numResults = numResults;
        this.page = page;
        this.lastPage = (collectionSize > countPerPage) ? (collectionSize - 1) / countPerPage + 1 : 1;

        if (this.pageToFetch > this.lastPage || this.pageToFetch < FIRST_PAGE) {
            throw new PageNotFoundException(this.pageToFetch, this.countPerPage, lastPage);
        }

        this.pageOfData = Collections.unmodifiableList(
                entireCollection
                        .stream()
                        .skip((pageToFetch - 1) * countPerPage)
                        .limit(countPerPage)
                        .collect(Collectors.toList())
        );
    }

    public int getRequestedPageNumber() {
        return paginationParameters.getPage();
    }

    public int getPerPage() {
        return paginationParameters.getPerPage();
    }

    public Observable<Integer> getFirstPage() {
        return Observable.just(getRequestedPageNumber()).filter(pageNumber -> pageNumber != FIRST_PAGE);
    }

    public Observable<Integer> getLastPage() {
        int perPage = getPerPage();
        return numResults
                .map(numRows -> numRows > perPage ? (numRows - 1) / perPage + 1 : 1)
                .filter(lastPage -> lastPage != getRequestedPageNumber());
    }

    public Observable<Integer> getNextPage() {
        return getLastPage().filter(lastPage -> getRequestedPageNumber() < lastPage);
    }

    public Observable<Integer> getPreviousPage() {
        return Observable.just(getRequestedPageNumber())
                .filter(pageNumber -> pageNumber > FIRST_PAGE)
                .map(pageNumber -> pageNumber - 1);
    }

    public Observable<T> getPageOfData() {
        return page;
    }

    public Observable<Integer> getNumResults() {
        return numResults;
    }
}
