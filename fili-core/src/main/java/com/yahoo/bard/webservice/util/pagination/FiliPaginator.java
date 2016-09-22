package com.yahoo.bard.webservice.util.pagination;

import com.yahoo.bard.webservice.web.util.PaginationParameters;
import rx.Observable;

import java.util.function.BiFunction;

/**
 * A binary function that takes a stream of query results, and the pagination information, and returns a Pagination
 * object that provides a reactive view (in the sense of ReactiveX) view of the page of data and its associated
 * metadata.
 * <p>
 *  The function takes a stream rather than an Observable
 */
public class FiliPaginator<T> implements BiFunction<Observable<T>, PaginationParameters, Pagination<T>> {

    @Override
    public Pagination<T> apply(Observable<T> results, PaginationParameters paginationParameters) {
        int perPage = paginationParameters.getPerPage();
        int pageRequested = paginationParameters.getPage();
        return new Pagination<>(
                results.skip((pageRequested - 1) * perPage).limit(perPage),
                paginationParameters,
                results.count()
        );
    }
}
