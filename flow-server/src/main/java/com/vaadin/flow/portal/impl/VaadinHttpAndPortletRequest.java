package com.vaadin.flow.portal.impl;

import java.util.Enumeration;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.portal.VaadinPortletRequest;
import com.vaadin.flow.portal.VaadinPortletService;

/**
 * Base class for portlet requests that need access to HTTP servlet requests.
 */
public abstract class VaadinHttpAndPortletRequest extends VaadinPortletRequest {

    /**
     * Constructs a new {@link VaadinHttpAndPortletRequest}.
     *
     * @since 7.2
     * @param request
     *            {@link PortletRequest} to be wrapped
     * @param vaadinService
     *            {@link VaadinPortletService} associated with this request
     */
    public VaadinHttpAndPortletRequest(PortletRequest request,
            VaadinPortletService vaadinService) {
        super(request, vaadinService);
    }

    private HttpServletRequest originalRequest;

    /**
     * Returns the original HTTP servlet request for this portlet request.
     *
     * @since 7.2
     * @param request
     *            {@link PortletRequest} used to
     * @return the original HTTP servlet request
     */
    protected abstract HttpServletRequest getServletRequest(
            PortletRequest request);

    private HttpServletRequest getOriginalRequest() {
        if (originalRequest == null) {
            PortletRequest request = getRequest();
            originalRequest = getServletRequest(request);
        }

        return originalRequest;
    }

    @Override
    public String getParameter(String name) {
        String parameter = super.getParameter(name);
        if (parameter == null && getOriginalRequest() != null) {
            parameter = getOriginalRequest().getParameter(name);
        }
        return parameter;
    }

    @Override
    public String getRemoteAddr() {
        if (getOriginalRequest() != null) {
            return getOriginalRequest().getRemoteAddr();
        } else {
            return super.getRemoteAddr();
        }

    }

    @Override
    public String getRemoteHost() {
        if (getOriginalRequest() != null) {
            return getOriginalRequest().getRemoteHost();
        } else {
            return super.getRemoteHost();
        }
    }

    @Override
    public int getRemotePort() {
        if (getOriginalRequest() != null) {
            return getOriginalRequest().getRemotePort();
        } else {
            return super.getRemotePort();
        }
    }

    @Override
    public String getHeader(String name) {
        String header = super.getHeader(name);
        if (header == null && getOriginalRequest() != null) {
            header = getOriginalRequest().getHeader(name);
        }
        return header;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Enumeration<String> headerNames = super.getHeaderNames();
        if (headerNames == null && getOriginalRequest() != null) {
            headerNames = getOriginalRequest().getHeaderNames();
        }
        return headerNames;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        Enumeration<String> headers = super.getHeaders(name);
        if (headers == null && getOriginalRequest() != null) {
            headers = getOriginalRequest().getHeaders(name);
        }
        return headers;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameterMap = super.getParameterMap();
        if (parameterMap == null && getOriginalRequest() != null) {
            parameterMap = getOriginalRequest().getParameterMap();
        }
        return parameterMap;
    }
}
