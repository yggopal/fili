// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.util.pagination;

import com.yahoo.bard.webservice.web.PageNotFoundException;
import com.yahoo.bard.webservice.web.util.PaginationParameters;
import rx.Observable;

import java.io.Serializable;


/**
 * A bean containing an observable stream of the requested page of data, and that provides observable streams of
 * metadata around the paginated data.
 *
 * @param <T> type of data being paginated
 */
public class Pagination<T> implements Serializable {
    long serialVersionUID = 1;

    private static final int FIRST_PAGE = 1;

    // Collection to be paginated
    private final Observable<T> page;
    private final PaginationParameters paginationParameters;
    private final Observable<Integer> numResults;

    /**
     * Constructor.
     *
     * @param page  The page of data requested.
     * @param paginationParameters  The page and rows per page requested by the user
     * @param numResults  The total number of results in the response
     *
     * @throws PageNotFoundException if pageToFetch is greater than the number of pages.
     */
    public Pagination(
            Observable<T> page,
            PaginationParameters paginationParameters,
            Observable<Integer> numResults
    ) throws PageNotFoundException {
        this.paginationParameters = paginationParameters;
        this.numResults = numResults;
        this.page = page;
    }

    /**
     * Returns the page number requested by the query.
     *
     * @return  The page number requested
     */
    public int getRequestedPageNumber() {
        return paginationParameters.getPage();
    }

    /**
     * Returns the number of rows per page.
     *
     * @return The number of rows per page
     */
    public int getPerPage() {
        return paginationParameters.getPerPage();
    }

    /**
     * Returns an observable containing the number of the first page if the requested page is not the first.
     *
     * @return An Observable containing the first page, or an empty Observable if the requested page is the first
     */
    public Observable<Integer> getFirstPage() {
        return getRequestedPageNumber() == FIRST_PAGE ? Observable.empty() : Observable.just(FIRST_PAGE);
    }

    /**
     * Returns an Observable that generates the number of the last page, if the requested page is not the last.
     *
     * @return An Observable containing the number of the last page, or an empty Observable if the requested page
     * is the last page
     */
    public Observable<Integer> getLastPage() {
        int perPage = getPerPage();
        return numResults
                .map(numRows -> numRows > perPage ? (numRows - 1) / perPage + 1 : 1)
                .filter(lastPage -> lastPage != getRequestedPageNumber());
    }

    /**
     * Returns an Observable that generates the number of the next page, if the requested page is not the last page.
     *
     * @return An Observable containing the number of the next page, or an empty Observable if the requested page is
     * the last page
     */
    public Observable<Integer> getNextPage() {
        return getLastPage().map(ignored -> getRequestedPageNumber() + 1);

    }

    /**
     * Returns an observable containing the number of the previous page, if the requested page is not the first page.
     *
     * @return An Observable containing the number of the previous page, or an empty Observable if the requested page
     * is the first
     */
    public Observable<Integer> getPreviousPage() {
        return getFirstPage().map(ignored -> getRequestedPageNumber() - 1);

    }

    /**
     * Returns the Observable that generates the page of data requested by the user
     *
     * @return The stream of data requested by the user
     */
    public Observable<T> getPageOfData() {
        return page;
    }

    /**
     * Returns an Observable that generates the total number of results of the request.
     *
     * @return An Observable that generates the total number of results of the request
     */
    public Observable<Integer> getNumResults() {
        return numResults;
    }
}
