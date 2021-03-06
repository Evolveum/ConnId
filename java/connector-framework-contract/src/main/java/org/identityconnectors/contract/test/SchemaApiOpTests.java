/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2018 ConnId
 */
package org.identityconnectors.contract.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.contract.exceptions.ObjectNotFoundException;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.SchemaApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.api.operations.TestApiOp;
import org.identityconnectors.framework.api.operations.ValidateApiOp;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.jupiter.api.Test;

/**
 * Contract test of {@link SchemaApiOp} operation.
 *
 * @author Zdenek Louzensky
 */
public class SchemaApiOpTests extends ContractTestBase {

    public static final String TEST_NAME = "Schema";

    /*
     * Properties prefixes:
     * it's added .testsuite.${type.name} after the prefix
     */
    private static final String SUPPORTED_OBJECT_CLASSES_PROPERTY_PREFIX = "oclasses";

    private static final String SUPPORTED_OPERATIONS_PROPERTY_PREFIX = "operations";

    private static final String STRICT_CHECK_PROPERTY_PREFIX = "strictCheck";

    /*
     * AttributeInfo field names used in property configuration:
     */
    private static final String ATTRIBUTE_FIELD_RETURNED_BY_DEFAULT = "returnedByDefault";

    private static final String ATTRIBUTE_FIELD_MULTI_VALUE = "multiValue";

    private static final String ATTRIBUTE_FIELD_REQUIRED = "required";

    private static final String ATTRIBUTE_FIELD_CREATEABLE = "createable";

    private static final String ATTRIBUTE_FIELD_UPDATEABLE = "updateable";

    private static final String ATTRIBUTE_FILED_READABLE = "readable";

    private static final String ATTRIBUTE_FIELD_TYPE = "type";

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<? extends APIOperation>> getAPIOperations() {
        Set<Class<? extends APIOperation>> s = new HashSet<>();
        // list of required operations by this test:
        s.add(SchemaApiOp.class);
        return s;
    }

    /**
     * Tests that the schema doesn't contain {@link Uid}
     */
    @Test
    public void testUidNotPresent() {
        final Schema schema = getConnectorFacade().schema();
        Set<ObjectClassInfo> ocInfos = schema.getObjectClassInfo();
        ocInfos.stream().map((ocInfo) -> ocInfo.getAttributeInfo()).
                forEachOrdered(attInfos -> {
                    attInfos.forEach((attInfo) -> {
                        //ensure there is not Uid present
                        assertTrue(!attInfo.is(Uid.NAME), "Uid can't be present in connector Schema!");
                    });
                });
    }

    /**
     * Tests that every object class contains {@link Name} among its attributes.
     */
    @Test
    public void testNamePresent() {
        final Schema schema = getConnectorFacade().schema();
        Set<ObjectClassInfo> ocInfos = schema.getObjectClassInfo();
        ocInfos.forEach((ocInfo) -> {
            Set<AttributeInfo> attInfos = ocInfo.getAttributeInfo();
            // ensure there is NAME present
            boolean found = attInfos.stream().anyMatch(attInfo -> attInfo.is(Name.NAME));
            final String msg = "Name is not present among attributes of object class '%s'.";
            assertTrue(found, String.format(msg, ocInfo.getType()));
        });
    }

    /**
     * List of all operations which must be supported by all object classes when
     * supported at all.
     */
    private static final List<Class<? extends APIOperation>> OP_SUPPORTED_BY_ALL_OCLASSES =
            new LinkedList<Class<? extends APIOperation>>();

    static {
        OP_SUPPORTED_BY_ALL_OCLASSES.add(ScriptOnConnectorApiOp.class);
        OP_SUPPORTED_BY_ALL_OCLASSES.add(ScriptOnResourceApiOp.class);
        OP_SUPPORTED_BY_ALL_OCLASSES.add(TestApiOp.class);
        OP_SUPPORTED_BY_ALL_OCLASSES.add(ValidateApiOp.class);
    }

    /**
     * Test ensures that following operations are supported by all object
     * classes when supported at all: ScriptOnConnectorApiOp, ScriptOnResourceApiOp,
     * TestApiOp, ValidateApiOp.
     */
    @Test
    public void testOpSupportedByAllOClasses() {
        final Schema schema = getConnectorFacade().schema();
        Set<ObjectClassInfo> ocInfos = schema.getObjectClassInfo();
        OP_SUPPORTED_BY_ALL_OCLASSES.forEach((apiOp) -> {
            Set<ObjectClassInfo> suppOClasses = schema.getSupportedObjectClassesByOperation(apiOp);
            if (!suppOClasses.isEmpty()) {
                // operation is supported for at least one object class
                // then it must be supported for all object classes
                final String MSG =
                        "Operation %s must be in the schema supported by all object classes which supports connector.";
                assertTrue(CollectionUtil.equals(suppOClasses, ocInfos), String.format(MSG, apiOp));
            }
        });
    }

    /**
     * Tests that returned schema by connector is the same as expected schema to
     * be returned.
     */
    @Test
    public void testSchemaExpected() {
        final Schema schema = getConnectorFacade().schema();
        String msg = null;

        Boolean strictCheck = getStrictCheckProperty();

        // list of expected object classes
        @SuppressWarnings("unchecked")
        List<String> expOClasses = (List<String>) getTestPropertyOrFail(List.class.getName(),
                SUPPORTED_OBJECT_CLASSES_PROPERTY_PREFIX, true);

        List<String> testedOClasses = new ArrayList<>();

        // iterate over object classes and check that were expected and check
        // their attributes
        for (ObjectClassInfo ocInfo : schema.getObjectClassInfo()) {
            boolean expected = expOClasses.contains(ocInfo.getType());
            if (strictCheck) {
                msg = "Schema returned object class %s that is not expected to be suported.";
                assertTrue(expected, String.format(msg, ocInfo.getType()));
            } else if (!expected) {
                // this object class was not expected, and we are not checking strictly,
                // so skip this object class
                continue;
            }

            testedOClasses.add(ocInfo.getType());

            // list of expected attributes for the object class
            @SuppressWarnings("unchecked")
            List<String> expAttrs = (List<String>) getTestPropertyOrFail(List.class.getName(),
                    "attributes." + ocInfo.getType() + "."
                    + SUPPORTED_OBJECT_CLASSES_PROPERTY_PREFIX, strictCheck);

            // check object class attributes
            for (AttributeInfo attr : ocInfo.getAttributeInfo()) {
                if (strictCheck) {
                    msg = "Object class %s contains unexpected attribute: %s.";
                    assertTrue(expAttrs.contains(attr.getName()), String.format(
                            msg, ocInfo.getType(), attr.getName()));
                }

                // expected attribute values
                @SuppressWarnings("unchecked")
                Map<String, Object> expAttrValues = (Map<String, Object>) getTestPropertyOrFail(
                        Map.class.getName(), attr.getName() + ".attribute." + ocInfo.getType()
                        + "." + SUPPORTED_OBJECT_CLASSES_PROPERTY_PREFIX, strictCheck);

                // check attribute's values in case the test is strict or property is provided
                if (strictCheck || expAttrValues != null) {
                    // check all attribute's fields
                    checkAttributeValues(ocInfo, attr, expAttrValues);
                }
            }

            // check that all expected attributes are in schema
            for (String expAttr : expAttrs) {
                msg = "Schema doesn't contain expected attribute '%s' in object class '%s'.";
                assertNotNull(AttributeInfoUtil.find(expAttr, ocInfo.getAttributeInfo()),
                        String.format(msg, expAttr, ocInfo.getType()));
            }

        }

        Set<String> notFoundOClasses = new HashSet<>(expOClasses);
        notFoundOClasses.removeAll(testedOClasses);
        if (!notFoundOClasses.isEmpty()) {
            msg = "Schema did not contain expected object class %s.";
            fail(String.format(msg, notFoundOClasses.iterator().next()));
        }

        // expected object classes supported by operations
        @SuppressWarnings("unchecked")
        Map<String, List<String>> expOperations = (Map<String, List<String>>) getTestPropertyOrFail(
                Map.class.getName(), SUPPORTED_OPERATIONS_PROPERTY_PREFIX, true);
        Map<Class<? extends APIOperation>, Set<ObjectClassInfo>> supportedOperations = schema
                .getSupportedObjectClassesByOperation();

        List<String> testedOps = new ArrayList<>();

        // iterate over operations
        for (Class<? extends APIOperation> operation : supportedOperations.keySet()) {
            boolean expectedOp = expOperations.containsKey(operation.getSimpleName());
            if (strictCheck) {
                msg = "Schema returned unexpected operation: %s.";
                assertTrue(expectedOp, String.format(msg, operation.getSimpleName()));
            } else if (!expectedOp) {
                // this operation was not expected, and we are not checking strictly,
                // so skip this operation
                continue;
            }

            testedOps.add(operation.getSimpleName());

            // expected object classes supported by the operation
            List<String> expOClassesForOp = expOperations.get(operation.getSimpleName());
            assertNotNull(expOClassesForOp);

            List<String> testedOClassesForOp = new ArrayList<>();

            for (ObjectClassInfo ocInfo : supportedOperations.get(operation)) {
                boolean expectedOClassForOp = expOClassesForOp.contains(ocInfo.getType());
                if (strictCheck) {
                    msg = "Operation %s supports unexpected object class: %s.";
                    assertTrue(expectedOClassForOp, String.format(msg, operation.getSimpleName(), ocInfo.getType()));
                } else if (!expectedOClassForOp) {
                    // this object class was not expected for this operation, and we are not checking strictly,
                    // so skip this object class
                    continue;
                }

                testedOClassesForOp.add(ocInfo.getType());
            }

            Set<String> notFoundOClassesForOp = new HashSet<>(expOClassesForOp);
            notFoundOClassesForOp.removeAll(testedOClassesForOp);
            if (!notFoundOClassesForOp.isEmpty()) {
                msg = "Operation %s is not supported by object class %s.";
                fail(String.format(msg, operation.getSimpleName(), notFoundOClassesForOp.iterator().next()));
            }
        }

        Set<String> notFoundOps = new HashSet<>(expOperations.keySet());
        notFoundOps.removeAll(testedOps);
        if (!notFoundOps.isEmpty()) {
            msg = "Schema did not contain expected operation %s.";
            fail(String.format(msg, notFoundOps.iterator().next()));
        }

    }

    /**
     * Checks that attribute values are the same as expectedValues.
     */
    private void checkAttributeValues(ObjectClassInfo ocInfo, AttributeInfo attribute,
            Map<String, Object> expectedValues) {
        // check that all attributes are provided
        String msg = "Missing property definition for field '%s' of attribute '" + attribute.getName()
                + "' in object class " + ocInfo.getType();
        assertNotNull(expectedValues.get(ATTRIBUTE_FIELD_TYPE), String.format(msg, ATTRIBUTE_FIELD_TYPE));
        assertNotNull(expectedValues.get(ATTRIBUTE_FILED_READABLE), String.format(msg, ATTRIBUTE_FILED_READABLE));
        assertNotNull(expectedValues.get(ATTRIBUTE_FIELD_CREATEABLE), String.format(msg, ATTRIBUTE_FIELD_CREATEABLE));
        assertNotNull(expectedValues.get(ATTRIBUTE_FIELD_UPDATEABLE), String.format(msg, ATTRIBUTE_FIELD_UPDATEABLE));
        assertNotNull(expectedValues.get(ATTRIBUTE_FIELD_REQUIRED), String.format(msg, ATTRIBUTE_FIELD_REQUIRED));
        assertNotNull(expectedValues.get(ATTRIBUTE_FIELD_MULTI_VALUE), String.format(msg, ATTRIBUTE_FIELD_MULTI_VALUE));
        assertNotNull(expectedValues.get(ATTRIBUTE_FIELD_RETURNED_BY_DEFAULT), String.format(msg,
                ATTRIBUTE_FIELD_RETURNED_BY_DEFAULT));

        msg = "Object class '" + ocInfo.getType() + "', attribute '" + attribute.getName()
                + "': field '%s' expected value is '%s', but returned '%s'.";
        assertEquals(attribute.getType(), expectedValues.get(ATTRIBUTE_FIELD_TYPE), String.format(msg,
                ATTRIBUTE_FIELD_TYPE, expectedValues
                        .get(ATTRIBUTE_FIELD_TYPE), attribute.getType().getName()));
        assertEquals(attribute.isReadable(), expectedValues.get(ATTRIBUTE_FILED_READABLE), String.format(msg,
                ATTRIBUTE_FILED_READABLE, expectedValues
                        .get(ATTRIBUTE_FILED_READABLE), attribute.isReadable()));
        assertEquals(attribute.isCreateable(), expectedValues.get(ATTRIBUTE_FIELD_CREATEABLE), String.format(msg,
                ATTRIBUTE_FIELD_CREATEABLE, expectedValues
                        .get(ATTRIBUTE_FIELD_CREATEABLE), attribute.isCreateable()));
        assertEquals(attribute.isUpdateable(), expectedValues.get(ATTRIBUTE_FIELD_UPDATEABLE), String.format(msg,
                ATTRIBUTE_FIELD_UPDATEABLE, expectedValues
                        .get(ATTRIBUTE_FIELD_UPDATEABLE), attribute.isUpdateable()));
        assertEquals(attribute.isRequired(), expectedValues.get(ATTRIBUTE_FIELD_REQUIRED), String.format(msg,
                ATTRIBUTE_FIELD_REQUIRED, expectedValues
                        .get(ATTRIBUTE_FIELD_REQUIRED), attribute.isRequired()));
        assertEquals(attribute.isMultiValued(), expectedValues.get(ATTRIBUTE_FIELD_MULTI_VALUE), String.format(msg,
                ATTRIBUTE_FIELD_MULTI_VALUE, expectedValues
                        .get(ATTRIBUTE_FIELD_MULTI_VALUE), attribute.isMultiValued()));
        assertEquals(attribute.isReturnedByDefault(), expectedValues.get(ATTRIBUTE_FIELD_RETURNED_BY_DEFAULT), String.
                format(msg, ATTRIBUTE_FIELD_RETURNED_BY_DEFAULT,
                        expectedValues.get(ATTRIBUTE_FIELD_RETURNED_BY_DEFAULT), attribute.isReturnedByDefault()));
    }

    /**
     * Returns strictCheck property value.
     * When property is not defined true is assumed.
     */
    private Boolean getStrictCheckProperty() {
        Boolean strict = true;
        try {
            strict = (Boolean) getDataProvider().getTestSuiteAttribute(STRICT_CHECK_PROPERTY_PREFIX, TEST_NAME);
        } catch (ObjectNotFoundException ex) {
            // ok - property not defined
        }

        return strict;
    }

    /**
     * Returns property value or fails test if property is not defined.
     */
    private Object getTestPropertyOrFail(String typeName, String propName, boolean failOnError) {
        Object propValue = null;

        try {
            propValue = getDataProvider().getTestSuiteAttribute(propName, TEST_NAME);
        } catch (ObjectNotFoundException ex) {
            if (failOnError) {
                fail("Property definition not found: " + ex.getMessage());
            }
        }
        if (failOnError) {
            assertNotNull(propValue);
        }

        return propValue;
    }
}
