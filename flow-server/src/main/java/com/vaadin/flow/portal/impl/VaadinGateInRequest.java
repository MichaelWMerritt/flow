package com.vaadin.flow.portal.impl;

import java.lang.reflect.Method;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.vaadin.flow.portal.VaadinPortletService;

/**
 * Portlet request for GateIn.
 */
public class VaadinGateInRequest extends VaadinHttpAndPortletRequest {
    public VaadinGateInRequest(PortletRequest request,
            VaadinPortletService vaadinService) {
        super(request, vaadinService);
    }

    @Override
    protected HttpServletRequest getServletRequest(PortletRequest request) {
        try {
            Method getRealReq = request.getClass().getMethod("getRealRequest");
            HttpServletRequestWrapper origRequest = (HttpServletRequestWrapper) getRealReq
                    .invoke(request);
            return origRequest;
        } catch (Exception e) {
            throw new IllegalStateException("GateIn request not detected", e);
        }
    }
}
