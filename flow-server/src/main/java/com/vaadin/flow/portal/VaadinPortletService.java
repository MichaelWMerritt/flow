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

package com.vaadin.flow.portal;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import javax.portlet.EventRequest;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.ServletHelper.RequestType;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.theme.AbstractTheme;

public class VaadinPortletService extends VaadinService {
    private final VaadinPortlet portlet;

    public VaadinPortletService(VaadinPortlet portlet,
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        super(deploymentConfiguration);
        this.portlet = portlet;
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        List<RequestHandler> handlers = super.createRequestHandlers();
        handlers.add(new PortletBootstrapHandler());
        handlers.add(new PortletWebComponentProvider());
        handlers.add(new PortletWebComponentBootstrapHandler());
        handlers.add(new PortletUidlRequestHandler());

        // handlers.add(new PortletUIInitHandler());
        // handlers.add(new PortletListenerNotifier());
        // handlers.add(0, new PortletDummyRequestHandler());
        // handlers.add(0, new PortletBootstrapHandler());
        // handlers.add(0, new PortletStateAwareRequestHandler());

        return handlers;
    }

    /**
     * Retrieves a reference to the portlet associated with this service.
     *
     * @return A reference to the VaadinPortlet this service is using
     */
    public VaadinPortlet getPortlet() {
        return portlet;
    }

    private String getParameter(VaadinRequest request, String name,
            String defaultValue) {
        VaadinPortletRequest portletRequest = (VaadinPortletRequest) request;

        String preference = portletRequest.getPortletPreference(name);
        if (preference != null) {
            return preference;
        }

        // String appOrSystemProperty = getAppOrSystemProperty(name, null);
        // if (appOrSystemProperty != null) {
        // return appOrSystemProperty;
        // }

        String portalProperty = portletRequest.getPortalProperty(name);
        if (portalProperty != null) {

            // For backwards compatibility - automatically map old portal
            // default widget set to default widget set
            // if (name.equals(Constants.PORTAL_PARAMETER_VAADIN_WIDGETSET)) {
            // return mapDefaultWidgetset(portalProperty);
            // }

            return portalProperty;
        }

        return defaultValue;
    }

    // private String getAppOrSystemProperty(String name, String defaultValue) {
    // DeploymentConfiguration deploymentConfiguration =
    // getDeploymentConfiguration();
    //
    // return deploymentConfiguration.getApplicationOrSystemProperty(name,
    // defaultValue);
    // }

    // @Override
    // public String getConfiguredTheme(VaadinRequest request) {
    // return getParameter(request, Constants.PORTAL_PARAMETER_VAADIN_THEME,
    // Constants.DEFAULT_THEME_NAME);
    // }

    // @Override
    // public boolean isStandalone(VaadinRequest request) {
    // return false;
    // }

    // @Override
    // public String getStaticFileLocation(VaadinRequest request) {
    // // /html is default for Liferay
    // String staticFileLocation = getParameter(request,
    // Constants.PORTAL_PARAMETER_VAADIN_RESOURCE_PATH, "/html");
    //
    // if (Constants.PORTLET_CONTEXT.equals(staticFileLocation)) {
    // return request.getContextPath();
    // } else {
    // return trimTrailingSlashes(staticFileLocation);
    // }
    // }

    private PortletContext getPortletContext() {
        return getPortlet().getPortletContext();
    }

    @Override
    public String getMimeType(String resourceName) {
        return getPortletContext().getMimeType(resourceName);
    }

    // @Override
    // public File getBaseDirectory() {
    // PortletContext context = getPortletContext();
    // String resultPath = context.getRealPath("/");
    // if (resultPath != null) {
    // return new File(resultPath);
    // } else {
    // try {
    // final URL url = context.getResource("/");
    // return new File(url.getFile());
    // } catch (final Exception e) {
    // // FIXME: Handle exception
    // getLogger().log(Level.INFO,
    // "Cannot access base directory, possible security issue "
    // + "with Application Server or Servlet Container",
    // e);
    // }
    // }
    // return null;
    // }

    @Override
    protected boolean requestCanCreateSession(VaadinRequest request) {
        if (!(request instanceof VaadinPortletRequest)) {
            throw new IllegalArgumentException(
                    "Request is not a VaadinPortletRequest");
        }

        PortletRequest portletRequest = ((VaadinPortletRequest) request)
                .getPortletRequest();
        if (portletRequest instanceof RenderRequest) {
            // In most cases the first request is a render request that
            // renders the HTML fragment. This should create a Vaadin
            // session unless there is already one.
            return true;
        } else if (portletRequest instanceof EventRequest) {
            // A portlet can also be sent an event even though it has not
            // been rendered, e.g. portlet on one page sends an event to a
            // portlet on another page and then moves the user to that page.
            return true;
            // } else if (PortletUIInitHandler.isUIInitRequest(request)) {
            // // In some cases, the RenderRequest seems to be cached, causing
            // the
            // // first request be the one triggered by vaadinBootstrap.js.
            // return true;
        }
        return false;
    }

    /**
     * Gets the request type for the request.
     *
     * @param request
     *            the request to get a request type for
     * @return the request type
     *
     * @deprecated As of 7.0. Will likely change or be removed in a future
     *             version
     */
    @Deprecated
    protected RequestType getRequestType(VaadinRequest request) {
        RequestType type = (RequestType) request
                .getAttribute(RequestType.class.getName());
        if (type == null) {
            // type = getPortlet().getRequestType((VaadinPortletRequest)
            // request);
            // request.setAttribute(RequestType.class.getName(), type);
        }
        return type;
    }

    /**
     * Gets the currently processed portlet request. The current portlet request
     * is automatically defined when the request is started. The current portlet
     * request can not be used in e.g. background threads because of the way
     * server implementations reuse request instances.
     *
     * @return the current portlet request instance if available, otherwise
     *         <code>null</code>
     *
     */
    public static PortletRequest getCurrentPortletRequest() {
        VaadinPortletRequest currentRequest = getCurrentRequest();
        if (currentRequest != null) {
            return currentRequest.getPortletRequest();
        } else {
            return null;
        }
    }

    /**
     * Gets the currently processed Vaadin portlet request. The current request
     * is automatically defined when the request is started. The current request
     * can not be used in e.g. background threads because of the way server
     * implementations reuse request instances.
     *
     * @return the current Vaadin portlet request instance if available,
     *         otherwise <code>null</code>
     *
     */
    public static VaadinPortletRequest getCurrentRequest() {
        return (VaadinPortletRequest) VaadinService.getCurrentRequest();
    }

    /**
     * Gets the currently processed Vaadin portlet response. The current
     * response is automatically defined when the request is started. The
     * current response can not be used in e.g. background threads because of
     * the way server implementations reuse response instances.
     *
     * @return the current Vaadin portlet response instance if available,
     *         otherwise <code>null</code>
     *
     */
    public static VaadinPortletResponse getCurrentResponse() {
        return (VaadinPortletResponse) VaadinService.getCurrentResponse();
    }

    @Override
    protected VaadinSession createVaadinSession(VaadinRequest request) {
        return new VaadinPortletSession(this);
    }

    @Override
    public String getServiceName() {
        return getPortlet().getPortletName();
    }

    @Override
    protected void handleSessionExpired(VaadinRequest request,
            VaadinResponse response) {
        // TODO Figure out a better way to deal with
        // SessionExpiredExceptions
        LoggerFactory.getLogger(getClass()).debug("A user session has expired");
    }

    private WrappedPortletSession getWrappedPortletSession(
            WrappedSession wrappedSession) {
        return (WrappedPortletSession) wrappedSession;
    }

    @Override
    protected void writeToHttpSession(WrappedSession wrappedSession,
            VaadinSession session) {
        getWrappedPortletSession(wrappedSession).setAttribute(
                getSessionAttributeName(), session,
                PortletSession.APPLICATION_SCOPE);
    }

    @Override
    protected VaadinSession readFromHttpSession(WrappedSession wrappedSession) {
        return (VaadinSession) getWrappedPortletSession(wrappedSession)
                .getAttribute(getSessionAttributeName(),
                        PortletSession.APPLICATION_SCOPE);
    }

    @Override
    protected void removeFromHttpSession(WrappedSession wrappedSession) {
        getWrappedPortletSession(wrappedSession).removeAttribute(
                getSessionAttributeName(), PortletSession.APPLICATION_SCOPE);
    }

    @Override
    protected PwaRegistry getPwaRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getContextRootRelativePath(VaadinRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMainDivId(VaadinSession session, VaadinRequest request) {
        // PortletRequest portletRequest = ((VaadinPortletRequest) request)
        // .getPortletRequest();
        // /*
        // * We need to generate a unique ID because some portals already create
        // a
        // * DIV with the portlet's Window ID as the DOM ID.
        // */
        // return "v-" + portletRequest.getWindowID();
        return null;
    }

    @Override
    public URL getStaticResource(String url) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getResource(String url, WebBrowser browser,
            AbstractTheme theme) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String url, WebBrowser browser,
            AbstractTheme theme) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String resolveResource(String url, WebBrowser browser) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<String> getThemedUrl(String url, WebBrowser browser,
            AbstractTheme theme) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected VaadinContext constructVaadinContext() {
        return new VaadinPortletContext(getPortlet().getPortletContext());
    }

}
