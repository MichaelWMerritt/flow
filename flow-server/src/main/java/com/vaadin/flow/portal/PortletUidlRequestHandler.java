package com.vaadin.flow.portal;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.communication.UidlRequestHandler;

public class PortletUidlRequestHandler extends UidlRequestHandler {
    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return "/uidl".equals(request.getPathInfo());
    }
}
