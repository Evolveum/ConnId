/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */

package org.identityconnectors.contract.test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.contract.data.DataProvider;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Schema;
import org.testng.IObjectFactory;
import org.testng.ITestContext;
import org.testng.annotations.Factory;
import org.testng.internal.ObjectFactoryImpl;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * A ContractITCase is the factory of the OpenICF connector contract tests.
 *
 * @author Laszlo Hordos
 */
@SuppressWarnings("rawtypes")
public class ContractITCase {

    public static final Class[] DEFAULT_TEST_CLASSES = new Class[] { AttributeTests.class,
        AuthenticationApiOpTests.class, ConfigurationTests.class, CreateApiOpTests.class,
        DeleteApiOpTests.class, GetApiOpTests.class, MultiOpTests.class, ResolveUsernameApiOpTests.class,SchemaApiOpTests.class,
        ScriptOnConnectorApiOpTests.class, ScriptOnResourceApiOpTests.class,
        SearchApiOpTests.class, SyncApiOpTests.class, TestApiOpTests.class, UpdateApiOpTests.class,
        ValidateApiOpTests.class };

    /*
     * <pre>
     * <testConfig>default</testConfig>
     * <connectorName>org.forgerock.openicf.connectors.BasicConnector</connectorName>
     * <bundleJar>target/basic-connector-1.1.0.0-SNAPSHOT.jar</bundleJar>
     * <bundleName>org.forgerock.openicf.connectors.basic-connector</bundleName>
     * <bundleVersion>1.1.0.0-SNAPSHOT</bundleVersion>
     * </pre>
     */
    @Factory
    public Object[] createInstances(ITestContext context) {

        Injector injector = getInjector(context);
        List<Object> result = new ArrayList<Object>();
        IObjectFactory objectFactory = null;

        for (Class<?> testClass: getContractTestClasses(context)) {
            Constructor constructor = null;
            try {
                constructor = testClass.getConstructor(String.class);
                Object test = objectFactory.newInstance(constructor, "");
                injector.injectMembers(test);
                result.add(test);
            } catch (NoSuchMethodException e) {
                result.add(injector.getInstance(testClass));
            }
        }
        return result.toArray();
    }

    @SuppressWarnings("rawtypes")
    public List<Class> getContractTestClasses(ITestContext context) {
        String testNames = System.getProperty("testClasses");
        if (StringUtil.isNotBlank(testNames)) {
            String[] clazzNames = testNames.split(",");
            List<Class> testClasses = new ArrayList<Class>(DEFAULT_TEST_CLASSES.length);
            for (String test : clazzNames) {
                for (Class clazz : DEFAULT_TEST_CLASSES) {
                    if (clazz.getSimpleName().equalsIgnoreCase(test)) {
                        testClasses.add(clazz);
                    }
                }
            }
            return testClasses;
        } else {
            return Arrays.asList(DEFAULT_TEST_CLASSES);
        }
    }

    public Injector getInjector(ITestContext context) {
        return Guice.createInjector(new FrameworkModule(ConnectorHelper.createDataProvider()));
    }

    public DataProvider getDataProvider(ITestContext context) {
        return ConnectorHelper.createDataProvider();
    }

    private static class ContractTestFactory {

        private ConnectorFacade connectorFacade = null;

        private Schema schema = null;

        private IObjectFactory objectFactory = new ObjectFactoryImpl();

    }
}
