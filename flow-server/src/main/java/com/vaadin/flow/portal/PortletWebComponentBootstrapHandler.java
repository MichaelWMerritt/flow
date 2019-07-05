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

    @Override
    protected String modifyPath(String path, String basePath) {
        // FIXME This is a hack to make a single portlet work
        path = path.replaceFirst("^.VAADIN/", "./VAADIN/");
        if (path.startsWith("./VAADIN/")) {
            return "/" + VaadinPortlet.getCurrent().getName() + "/" + path;
        }
        return super.modifyPath(path, basePath);
    }
}
