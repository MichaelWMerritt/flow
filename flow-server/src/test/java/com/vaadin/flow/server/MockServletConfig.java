/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MockServletConfig implements ServletConfig {

    private ServletContext context = new MockServletContext();
    private static final Properties DEFAULT_PROPERTIES = new Properties();
    {
        DEFAULT_PROPERTIES.setProperty(
                Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.FALSE.toString());
    }

    private final Properties initParameters;

    public MockServletConfig() {
        this(DEFAULT_PROPERTIES);
    }

    public MockServletConfig(Properties initParameters) {
        this.initParameters = initParameters;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletConfig#getServletName()
     */
    @Override
    public String getServletName() {
        return "Mock Servlet";
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletConfig#getServletContext()
     */
    @Override
    public ServletContext getServletContext() {
        return context;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
     */
    @Override
    public String getInitParameter(String name) {
        return initParameters.getProperty(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletConfig#getInitParameterNames()
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getInitParameterNames() {
        return initParameters.propertyNames();
    }

}
