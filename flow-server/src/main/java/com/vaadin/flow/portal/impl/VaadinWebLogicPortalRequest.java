package com.vaadin.flow.portal.impl;

import java.lang.reflect.Method;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.portal.VaadinPortletService;

/**
 * Portlet request for WebSphere Portal.
 */
public class VaadinWebLogicPortalRequest extends VaadinHttpAndPortletRequest {
    private static boolean warningLogged = false;

    private static Method servletRequestMethod = null;

    public VaadinWebLogicPortalRequest(PortletRequest request,
            VaadinPortletService vaadinService) {
        super(request, vaadinService);
    }

    @Override
    protected HttpServletRequest getServletRequest(PortletRequest request) {
        try {
            if (servletRequestMethod == null) {
                Class<?> portletRequestClass = Class.forName(
                        "com.bea.portlet.container.PortletRequestImpl");
                servletRequestMethod = portletRequestClass
                        .getDeclaredMethod("getInternalRequest");
                servletRequestMethod.setAccessible(true);
            }

            return (HttpServletRequest) servletRequestMethod.invoke(request);
        } catch (Exception e) {
            if (!warningLogged) {
                warningLogged = true;
                LoggerFactory.getLogger(VaadinWebLogicPortalRequest.class).warn(
                        "Could not determine underlying servlet request for WebLogic Portal portlet request",
                        e);
            }
            return null;
        }
    }
}
