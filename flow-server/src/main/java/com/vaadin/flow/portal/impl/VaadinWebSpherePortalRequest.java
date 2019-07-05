package com.vaadin.flow.portal.impl;

import java.lang.reflect.Method;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.portal.VaadinPortletService;

/**
 * Portlet request for WebSphere Portal.
 */
public class VaadinWebSpherePortalRequest extends VaadinHttpAndPortletRequest {

    public VaadinWebSpherePortalRequest(PortletRequest request,
            VaadinPortletService vaadinService) {
        super(request, vaadinService);
    }

    @Override
    protected HttpServletRequest getServletRequest(PortletRequest request) {
        try {
            Class<?> portletUtils = Class.forName(
                    "com.ibm.ws.portletcontainer.portlet.PortletUtils");
            Method getHttpServletRequest = portletUtils
                    .getMethod("getHttpServletRequest", PortletRequest.class);

            return (HttpServletRequest) getHttpServletRequest.invoke(null,
                    request);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "WebSphere Portal request not detected.");
        }
    }
}
