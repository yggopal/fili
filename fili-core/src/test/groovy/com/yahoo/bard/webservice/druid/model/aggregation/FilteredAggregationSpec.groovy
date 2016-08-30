// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.druid.model.aggregation

import static com.yahoo.bard.webservice.data.time.DefaultTimeGrain.DAY
import static org.joda.time.DateTimeZone.UTC

import com.yahoo.bard.webservice.data.config.metric.MetricInstance
import com.yahoo.bard.webservice.data.config.metric.makers.SketchCountMaker
import com.yahoo.bard.webservice.data.config.names.ApiMetricName
import com.yahoo.bard.webservice.data.dimension.BardDimensionField
import com.yahoo.bard.webservice.data.dimension.DimensionColumn
import com.yahoo.bard.webservice.data.dimension.DimensionDictionary
import com.yahoo.bard.webservice.data.dimension.MapStoreManager
import com.yahoo.bard.webservice.data.dimension.impl.KeyValueStoreDimension
import com.yahoo.bard.webservice.data.dimension.impl.ScanSearchProviderManager
import com.yahoo.bard.webservice.data.filterbuilders.DefaultDruidFilterBuilder
import com.yahoo.bard.webservice.data.filterbuilders.DruidFilterBuilder
import com.yahoo.bard.webservice.data.metric.LogicalMetric
import com.yahoo.bard.webservice.data.metric.MetricColumn
import com.yahoo.bard.webservice.data.metric.MetricDictionary
import com.yahoo.bard.webservice.druid.model.filter.Filter
import com.yahoo.bard.webservice.druid.util.FieldConverterSupplier
import com.yahoo.bard.webservice.table.LogicalTable
import com.yahoo.bard.webservice.table.PhysicalTable
import com.yahoo.bard.webservice.table.TableGroup
import com.yahoo.bard.webservice.web.ApiFilter
import com.yahoo.bard.webservice.web.FilteredSketchMetricsHelper
import com.yahoo.bard.webservice.web.MetricsFilterSetBuilder

import spock.lang.Specification

class FilteredAggregationSpec extends Specification{

    FilteredAggregation filteredAgg
    FilteredAggregation filteredAgg2
    Filter filter1
    Filter filter2
    Aggregation metricAgg
    Aggregation genderDependentMetricAgg
    KeyValueStoreDimension ageDimension
    KeyValueStoreDimension genderDimension
    static MetricsFilterSetBuilder oldBuilder = FieldConverterSupplier.metricsFilterSetBuilder

    def setupSpec() {
        FieldConverterSupplier.metricsFilterSetBuilder = new FilteredSketchMetricsHelper()
    }

    def setup() {
        Set<ApiMetricName> metricNames = ["FOO","FOO_NO_BAR"]

        ageDimension = new KeyValueStoreDimension(
                "age",
                null,
                [BardDimensionField.ID] as LinkedHashSet,
                MapStoreManager.getInstance("age"),
                ScanSearchProviderManager.getInstance("age")
        )

        genderDimension = new KeyValueStoreDimension(
                "gender",
                null,
                [BardDimensionField.ID] as LinkedHashSet,
                MapStoreManager.getInstance("gender"),
                ScanSearchProviderManager.getInstance("gender")
        )


        DimensionDictionary dimensionDictionary = new DimensionDictionary([ageDimension] as Set)

        ageDimension.addDimensionRow(BardDimensionField.makeDimensionRow(ageDimension, "114"))
        ageDimension.addDimensionRow(BardDimensionField.makeDimensionRow(ageDimension, "125"))

        PhysicalTable physicalTable = new PhysicalTable("NETWORK", DAY.buildZonedTimeGrain(UTC), [:])

        physicalTable.addColumn(new DimensionColumn(ageDimension, physicalTable.getPhysicalColumnName(ageDimension.getApiName())))
        physicalTable.addColumn(new DimensionColumn(ageDimension, physicalTable.getPhysicalColumnName(ageDimension.getApiName())))
        metricNames.each {physicalTable.addColumn(new MetricColumn(it))}
        physicalTable.commit()

        TableGroup tableGroup = new TableGroup([physicalTable] as LinkedHashSet, metricNames)
        LogicalTable table = new LogicalTable("NETWORK_DAY", DAY, tableGroup)
        tableGroup.dimensions.each {
            DimensionColumn.addNewDimensionColumn(table, it, physicalTable)
        }

        SketchCountMaker sketchCountMaker = new SketchCountMaker(new MetricDictionary(), 16384)
        MetricInstance fooNoBarSketchPm = new MetricInstance("FOO_NO_BAR",sketchCountMaker,"FOO_NO_BAR_SKETCH")
        LogicalMetric fooNoBarSketch = fooNoBarSketchPm.make()

        metricAgg = fooNoBarSketch.getTemplateDruidQuery().getAggregations().first()
        genderDependentMetricAgg = Mock(Aggregation)
        genderDependentMetricAgg.getDependentDimensions() >> ([genderDimension] as Set)
        genderDependentMetricAgg.withName(_) >> genderDependentMetricAgg
        genderDependentMetricAgg.withFieldName(_) >> genderDependentMetricAgg

        Set<ApiFilter> filterSet = [new ApiFilter("age|id-in[114,125]", table, dimensionDictionary)] as Set

        DruidFilterBuilder filterBuilder = new DefaultDruidFilterBuilder()
        filter1  = filterBuilder.buildFilters([(ageDimension): filterSet])

        filter2 = filterBuilder.buildFilters(
                [(ageDimension): [new ApiFilter("age|id-in[114]", table, dimensionDictionary)] as Set]
        )

        filteredAgg = new FilteredAggregation("FOO_NO_BAR-114_127", metricAgg.getFieldName(), metricAgg, filter1)
        filteredAgg2 = new FilteredAggregation("FOO_NO_BAR-114_127", genderDependentMetricAgg.getFieldName(), genderDependentMetricAgg, filter1)
    }

    def cleanupSpec() {
        FieldConverterSupplier.metricsFilterSetBuilder = oldBuilder
    }

    def "test the filtered aggregator constructor" (){
        expect:
        filteredAgg.getFieldName() == "FOO_NO_BAR_SKETCH"
        filteredAgg.getName() == "FOO_NO_BAR-114_127"
        filteredAgg.getFilter() == filter1
        filteredAgg.getAggregation() == metricAgg.withName("FOO_NO_BAR-114_127")
    }

    def "test FilteredAggregation withFieldName method" (){
        FilteredAggregation newFilteredAggregation = filteredAgg.withFieldName("NEW_FIELD_NAME");

        expect:
        newFilteredAggregation.getFieldName() == "NEW_FIELD_NAME"
        newFilteredAggregation.getName() == filteredAgg.getName()
        newFilteredAggregation.getFilter() == filteredAgg.getFilter()
        newFilteredAggregation.getAggregation() == metricAgg.withName(filteredAgg.getName()).withFieldName("NEW_FIELD_NAME")
    }

    def "test FilteredAggregation withName method" (){
        FilteredAggregation newFilteredAggregation = filteredAgg.withName("FOO_NO_BAR-US_IN");

        expect:
        newFilteredAggregation.getFieldName() == filteredAgg.getFieldName()
        newFilteredAggregation.getName() == "FOO_NO_BAR-US_IN"
        newFilteredAggregation.getFilter() == filteredAgg.getFilter()
        newFilteredAggregation.getAggregation() == metricAgg.withName("FOO_NO_BAR-US_IN").withFieldName(filteredAgg.getFieldName())
    }

    def "test FilteredAggregation withFilter method" (){
        FilteredAggregation newFilteredAggregation = filteredAgg.withFilter(filter2);

        expect:
        newFilteredAggregation.getFieldName() == filteredAgg.getFieldName()
        newFilteredAggregation.getName() == filteredAgg.getName()
        newFilteredAggregation.getFilter() == filter2
        newFilteredAggregation.getAggregation() == metricAgg.withName(filteredAgg.getName()).withFieldName(filteredAgg.getFieldName())
    }

    def "test FilteredAggregation withAggregation method" (){
        FilteredAggregation newFilteredAggregation = filteredAgg.withAggregation(metricAgg.withName("NEW_AGG").
                withFieldName("NEW_FIELD_NAME"));

        expect:
        newFilteredAggregation.getFieldName() == "NEW_FIELD_NAME"
        newFilteredAggregation.getName() == "NEW_AGG"
        newFilteredAggregation.getFilter() == filteredAgg.getFilter()
        newFilteredAggregation.getAggregation() == metricAgg.withName("NEW_AGG").
                withFieldName("NEW_FIELD_NAME")
    }

    def "Test dependant metric"() {
        expect:
        filteredAgg.dependentDimensions == [ageDimension] as Set
        filteredAgg2.dependentDimensions == [genderDimension, ageDimension] as Set
    }

    def "test serialization" (){
        expect:
        filteredAgg.getAggregation().getFieldName() == "FOO_NO_BAR_SKETCH"
        filteredAgg.getAggregation().getName() == "FOO_NO_BAR-114_127"
    }
}