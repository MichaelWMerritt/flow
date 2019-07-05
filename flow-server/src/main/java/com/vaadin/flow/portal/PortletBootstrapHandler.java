package com.vaadin.flow.portal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceURL;

import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.WebComponentProvider;

public class PortletBootstrapHandler extends SynchronizedRequestHandler {

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return ((VaadinPortletRequest) request)
                .getPortletRequest() instanceof RenderRequest;
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        VaadinPortlet portlet = VaadinPortlet.getCurrent();
        String tag;
        try {
            tag = portlet.getMainComponentTag();
        } catch (PortletException e) {
            throw new IOException(e);
        }
        PrintWriter writer = response.getWriter();

        PortletResponse resp = ((VaadinPortletResponse) response)
                .getPortletResponse();

        PortletContext portletContext = portlet.getPortletContext();
        String scriptUrl = (String) portletContext
                .getAttribute(WebComponentProvider.class.getName());
        if (scriptUrl == null) {
            RenderResponse renderResponse = (RenderResponse) resp;
            ResourceURL url = renderResponse.createResourceURL();
            url.setResourceID("/web-component/" + tag + ".js");
            scriptUrl = url.toString();
            portlet.setWebComponentProviderURL(url.toString());

            url = renderResponse.createResourceURL();
            url.setResourceID("/web-component/web-component-ui.js");
            portlet.setWebComponentBootstrapHandlerURL(url.toString());

            url = renderResponse.createResourceURL();
            url.setResourceID("/uidl");
            portlet.setWebComponentUIDLRequestHandlerURL(url.toString());
        }
        writer.write("<script src='" + scriptUrl + "'></script>");
        writer.write("<" + tag + "></" + tag + ">");
        return true;
    }

}
