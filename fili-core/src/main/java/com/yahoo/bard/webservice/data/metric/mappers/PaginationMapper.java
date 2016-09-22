// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.data.metric.mappers;

import com.yahoo.bard.webservice.data.Result;
import com.yahoo.bard.webservice.data.ResultSet;
import com.yahoo.bard.webservice.table.Schema;
import com.yahoo.bard.webservice.util.pagination.Pagination;
import com.yahoo.bard.webservice.web.AbstractResponse;
import com.yahoo.bard.webservice.web.responseprocessors.MappingResponseProcessor;
import com.yahoo.bard.webservice.web.util.PaginationParameters;
import rx.Observable;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Extracts the requested page of data from the Druid results. Behavior is undefined if the page requested is
 * less than 1, or the requested number of results on each page is less than 1.
 */
public class PaginationMapper extends ResultSetMapper {

    private final PaginationParameters paginationParameters;
    private final MappingResponseProcessor responseProcessor;
    private final UriBuilder uriBuilder;
    private final BiFunction<Observable<Result>, PaginationParameters, Pagination<Result>> paginator;

    /**
     * Constructor.
     *  @param paginationParameters  The parameters needed for pagination
     * @param responseProcessor  The API response to which we can add the header links.
     * @param uriBuilder  The builder for creating the pagination links.
     * @param paginator  The function that should paginate the results
     */
    public PaginationMapper(
            PaginationParameters paginationParameters,
            MappingResponseProcessor responseProcessor,
            UriBuilder uriBuilder,
            BiFunction<Observable<Result>, PaginationParameters, Pagination<Result>> paginator
    ) {
        this.paginationParameters = paginationParameters;
        this.responseProcessor = responseProcessor;
        this.uriBuilder = uriBuilder;
        this.paginator = paginator;
    }

    /**
     *  Cuts the result set down to just the page requested.
     *
     * @param resultSet  The result set to be cut down.
     *
     * @return The page of results desired.
     *
     * @throws com.yahoo.bard.webservice.web.PageNotFoundException if the page requested is past the last page of data.
     */
    @Override
    public ResultSet map(ResultSet resultSet) {
        Pagination<Result> pages = paginator.apply(Observable.from(resultSet), paginationParameters);
        AbstractResponse.addLinks(pages, uriBuilder, responseProcessor);
        return new ResultSet(
                pages.getPageOfData()
                        .collect(() -> new ArrayList<Result>(pages.getPerPage()), List::add)
                        .toBlocking() //This is ugly but necessary until we make the mapping chain reactive
                        .single(),
                map(resultSet.getSchema())
        );
    }

    @Override
    protected Result map(Result result, Schema schema) {
        //Not needed, because this mapper overrides map(ResultSet). So it is just a no-op.
        return result;
    }


    @Override
    protected Schema map(Schema schema) {
        //Because this method is not necessary, it just returns the schema unchanged.
        return schema;
    }
}
