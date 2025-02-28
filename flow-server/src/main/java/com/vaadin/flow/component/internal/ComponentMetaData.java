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
package com.vaadin.flow.component.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.DevModeInitializer.VisitedClasses;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.shared.util.SharedUtil;

/**
 * Immutable meta data related to a component class.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ComponentMetaData {

    /**
     * Dependencies defined for a {@link Component} class.
     * <p>
     * Framework internal class, thus package-private.
     */
    public static class DependencyInfo {
        private final List<HtmlImportDependency> htmlImports = new ArrayList<>();
        private final List<JavaScript> javaScripts = new ArrayList<>();
        private final List<JsModule> jsModules = new ArrayList<>();
        private final List<StyleSheet> styleSheets = new ArrayList<>();

        List<HtmlImportDependency> getHtmlImports() {
            return Collections.unmodifiableList(htmlImports);
        }

        List<JavaScript> getJavaScripts() {
            return Collections.unmodifiableList(javaScripts);
        }

        List<JsModule> getJsModules() {
            return Collections.unmodifiableList(jsModules);
        }

        List<StyleSheet> getStyleSheets() {
            return Collections.unmodifiableList(styleSheets);
        }

    }

    public static class HtmlImportDependency {

        private final Collection<String> uris;

        private final LoadMode loadMode;

        private HtmlImportDependency(Collection<String> uris,
                LoadMode loadMode) {
            this.uris = Collections.unmodifiableCollection(uris);
            this.loadMode = loadMode;
        }

        public Collection<String> getUris() {
            return uris;
        }

        public LoadMode getLoadMode() {
            return loadMode;
        }
    }

    /**
     * Synchronized properties defined for a {@link Component} class.
     * <p>
     * Framework internal class, thus package-private.
     */
    public static class SynchronizedPropertyInfo {
        private final String property;
        private final DisabledUpdateMode mode;
        private final String[] eventNames;

        SynchronizedPropertyInfo(String property, String[] eventNames,
                DisabledUpdateMode mode) {
            this.property = property;
            this.eventNames = eventNames;
            this.mode = mode;
        }

        public String getProperty() {
            return property;
        }

        public Stream<String> getEventNames() {
            return Stream.of(eventNames);
        }

        public DisabledUpdateMode getUpdateMode() {
            return mode;
        }
    }

    private final Collection<SynchronizedPropertyInfo> synchronizedProperties;
    private final ConcurrentHashMap<VaadinService, DependencyInfo> dependencyInfo = new ConcurrentHashMap<>();
    private final Class<? extends Component> componentClass;

    /**
     * Scans the given component class and creates a new instance based on found
     * annotations.
     *
     * @param componentClass
     *            the component class to scan
     */
    public ComponentMetaData(Class<? extends Component> componentClass) {
        this.componentClass = componentClass;
        synchronizedProperties = findSynchronizedProperties(componentClass);
    }

    /**
     * Finds all dependencies (JsModule, HTML, JavaScript, StyleSheet) for the
     * class. Includes dependencies for all classes referred by an {@link Uses}
     * annotation.
     *
     * @return an information object containing all the dependencies
     */
    private static DependencyInfo findDependencies(VaadinService service,
            Class<? extends Component> componentClass) {
        Optional.ofNullable(
                service.getContext().getAttribute(VisitedClasses.class))
                .ifPresent(visitedClasses -> visitedClasses
                        .ensureAllDependenciesVisited(componentClass));

        DependencyInfo dependencyInfo = new DependencyInfo();

        findDependencies(service, componentClass, dependencyInfo,
                new HashSet<>());
        return dependencyInfo;
    }

    private static DependencyInfo findDependencies(VaadinService service,
            Class<? extends Component> componentClass,
            DependencyInfo dependencyInfo,
            Set<Class<? extends Component>> scannedClasses) {
        assert !scannedClasses.contains(componentClass);

        scannedClasses.add(componentClass);

        if (service.getDeploymentConfiguration().isCompatibilityMode()) {
            dependencyInfo.htmlImports
                    .addAll(getHtmlImportDependencies(service, componentClass));

        } else {
            List<JsModule> jsModules = AnnotationReader
                    .getJsModuleAnnotations(componentClass);

            // Ignore @HtmlImport(s) when @JsModule(s) present.
            if (!jsModules.isEmpty()) {
                dependencyInfo.jsModules.addAll(jsModules);
            } else {
                dependencyInfo.jsModules.addAll(
                        getHtmlImportAsJsModuleAnnotations(componentClass));
            }
        }

        dependencyInfo.javaScripts.addAll(
                AnnotationReader.getJavaScriptAnnotations(componentClass));
        dependencyInfo.styleSheets.addAll(
                AnnotationReader.getStyleSheetAnnotations(componentClass));

        List<Uses> usesList = AnnotationReader.getAnnotationsFor(componentClass,
                Uses.class);
        for (Uses uses : usesList) {
            Class<? extends Component> otherClass = uses.value();
            if (!scannedClasses.contains(otherClass)) {
                findDependencies(service, otherClass, dependencyInfo,
                        scannedClasses);
            }
        }
        return dependencyInfo;
    }

    /**
     * Gets the properties that are marked to be synchronized and corresponding
     * events.
     * <p>
     * Framework internal data, thus package-private.
     *
     * @return a collection of information objects about properties to be
     *         synchronized
     */
    public Collection<SynchronizedPropertyInfo> getSynchronizedProperties() {
        return Collections.unmodifiableCollection(synchronizedProperties);
    }

    /**
     * Gets the dependencies, defined using annotations ({@link JsModule},
     * {@link HtmlImport}, {@link JavaScript}, {@link StyleSheet} and
     * {@link Uses}).
     * <p>
     * Framework internal data, thus package-private.
     *
     * @param service
     *            the service to use for resolving dependencies
     *
     * @return the dependencies for the given class
     */
    public DependencyInfo getDependencyInfo(VaadinService service) {
        return dependencyInfo.computeIfAbsent(service, ignore -> {
            service.addServiceDestroyListener(
                    event -> dependencyInfo.remove(service));
            return findDependencies(service, componentClass);
        });
    }

    private static Collection<HtmlImportDependency> getHtmlImportDependencies(
            VaadinService service, Class<? extends Component> componentClass) {
        return AnnotationReader.getHtmlImportAnnotations(componentClass)
                .stream().map(htmlImport -> getHtmlImportDependencies(service,
                        htmlImport))
                .collect(Collectors.toList());
    }

    private static HtmlImportDependency getHtmlImportDependencies(
            VaadinService service, HtmlImport htmlImport) {
        String importPath = SharedUtil.prefixIfRelative(htmlImport.value(),
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);

        DependencyTreeCache<String> cache = service
                .getHtmlImportDependencyCache();

        Set<String> dependencies = cache.getDependencies(importPath);

        return new HtmlImportDependency(dependencies, htmlImport.loadMode());
    }

    /**
     * Scans the class for {@link Synchronize} annotations and gathers the data.
     *
     * @param componentClass
     *            the class to scan
     * @return a set of information objects about properties to be synchronized
     */
    private static Collection<SynchronizedPropertyInfo> findSynchronizedProperties(
            Class<? extends Component> componentClass) {
        Map<String, SynchronizedPropertyInfo> infos = new HashMap<>();
        collectSynchronizedProperties(componentClass, infos);
        return infos.values();
    }

    private static void collectSynchronizedProperties(Class<?> clazz,
            Map<String, SynchronizedPropertyInfo> infos) {
        if (clazz == null || clazz.equals(Object.class)) {
            return;
        }
        doCollectSynchronizedProperties(clazz, infos);

        Class<?> superclass = clazz.getSuperclass();
        collectSynchronizedProperties(superclass, infos);

        Stream.of(clazz.getInterfaces())
                .forEach(iface -> collectSynchronizedProperties(iface, infos));
    }

    private static void doCollectSynchronizedProperties(Class<?> clazz,
            Map<String, SynchronizedPropertyInfo> infos) {
        for (Method method : clazz.getDeclaredMethods()) {
            Synchronize annotation = method.getAnnotation(Synchronize.class);
            if (annotation == null) {
                continue;
            }

            if (!ReflectTools.isGetter(method)) {
                throw new IllegalStateException(method + " is annotated with @"
                        + Synchronize.class.getSimpleName()
                        + " even though it's not a getter.");
            }

            if (infos.containsKey(method.getName())) {
                continue;
            }

            String propertyName;
            if (annotation.property().isEmpty()) {
                propertyName = ReflectTools.getPropertyName(method);
            } else {
                propertyName = annotation.property();
            }

            String[] eventNames = annotation.value();
            infos.put(method.getName(), new SynchronizedPropertyInfo(
                    propertyName, eventNames, annotation.allowUpdates()));
        }
    }

    private static Collection<JsModule> getHtmlImportAsJsModuleAnnotations(
            Class<? extends Component> componentClass) {
        return AnnotationReader.getHtmlImportAnnotations(componentClass)
                .stream()
                .map(ComponentMetaData::getHtmlImportAsJsModuleAnnotation)
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static JsModule getHtmlImportAsJsModuleAnnotation(
            HtmlImport htmlImport) {

        String value = SharedUtil.prefixIfRelative(htmlImport.value(),
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);

        String module = value
                .replaceFirst("^.*bower_components/(vaadin-.*)\\.html",
                        "@vaadin/$1.js")
                .replaceFirst("^.*bower_components/((iron|paper)-.*)\\.html",
                        "@polymer/$1.js");

        return jsModule(module, htmlImport.loadMode());
    }

    private static JsModule jsModule(final String value,
            final LoadMode loadMode) {

        return new JsModule() {

            @Override
            public String value() {
                return value;
            }

            @Override
            public LoadMode loadMode() {
                return loadMode;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return JsModule.class;
            }
        };
    }

}
