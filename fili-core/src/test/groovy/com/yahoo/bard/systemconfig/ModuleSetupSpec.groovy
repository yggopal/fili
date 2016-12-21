// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.
package com.yahoo.bard.systemconfig

import static com.yahoo.bard.webservice.config.ModuleLoader.MODULE_CONFIG_FILE_NAME

import com.yahoo.bard.webservice.config.ConfigResourceLoader
import com.yahoo.bard.webservice.config.SystemConfig

import org.apache.commons.configuration.Configuration

import spock.lang.Specification
import spock.lang.Unroll

/**
 * An abstract base TCK test for modules.
 */
abstract class ModuleSetupSpec extends Specification {

    public static final String MODULE_NAME_KEY = "moduleName"

    @Unroll
    def "SystemConfig loads with #moduleName in classpath"() {
        expect:
        SystemConfig.properties.getOrDefault("Resources don't conflict with system config loading", "foo") == "foo"

        where:
        moduleName << [getModuleName()]
    }

    @Unroll
    def "ConfigResourceLoader discovers #moduleName"() {
        setup:
        List<Configuration> configurations = new ConfigResourceLoader().loadConfigurations(MODULE_CONFIG_FILE_NAME)

        expect:
        configurations.stream().map( {it.getString(MODULE_NAME_KEY)}).anyMatch({it.equals(getModuleName())})

        where:
        moduleName << [getModuleName()]
    }

    /**
     * Implement this test by supplying a subclass that provides the name of the module
     *
     * @return  The name of the module as configured in moduleConfig.properties
     */
    abstract String getModuleName()
}
