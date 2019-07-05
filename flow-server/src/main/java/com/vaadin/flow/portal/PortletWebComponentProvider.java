package com.vaadin.flow.portal;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.communication.WebComponentProvider;

public class PortletWebComponentProvider extends WebComponentProvider {
    @Override
    protected String generateNPMResponse(VaadinRequest request,
            String tagName) {
        String webcomponentBootstrapUrl = VaadinPortlet.getCurrent()
                .getWebComponentBootstrapHandlerURL();
        return "var bootstrapAddress='" + webcomponentBootstrapUrl + "';\n"
                + bootstrapNpm();

    }
}
