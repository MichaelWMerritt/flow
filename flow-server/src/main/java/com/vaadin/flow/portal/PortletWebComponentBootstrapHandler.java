package com.vaadin.flow.portal;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.communication.WebComponentBootstrapHandler;

public class PortletWebComponentBootstrapHandler
        extends WebComponentBootstrapHandler {

    @Override
    protected String getServiceUrl(VaadinRequest request) {
        return VaadinPortlet.getCurrent()
                .getWebComponentUIDLRequestHandlerURL();
    }

}
