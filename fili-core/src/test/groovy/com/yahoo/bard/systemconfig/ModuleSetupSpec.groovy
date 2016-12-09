// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.
package com.yahoo.bard.systemconfig

import com.yahoo.bard.webservice.config.ConfigResourceLoader
import com.yahoo.bard.webservice.config.SystemConfig

import org.apache.commons.configuration.Configuration

import spock.lang.Specification

abstract class ModuleSetupSpec extends Specification {

    public static final String MODULE_NAME_KEY = "moduleName"
    public static final String MODULE_RESOURCE_NAME = "/moduleConfig.properties"

    def "Test that SystemConfig loads for this module"() {
        expect:
        SystemConfig.properties.getOrDefault("foo", "foo") == "foo"
    }

    def "Current module is loaded by ConfigResourceLoader"() {
        setup:
        List<Configuration> configurations = new ConfigResourceLoader().loadConfigurations(MODULE_RESOURCE_NAME)

        expect:
        configurations.stream().map( {it.getString(MODULE_NAME_KEY)}).anyMatch({it.equals(getModuleName())})
    }

    abstract String getModuleName()
}
