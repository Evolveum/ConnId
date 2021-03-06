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
 * Portions Copyrighted 2010-2014 ForgeRock AS.
 * Portions Copyrighted 2014-2018 Evolveum
 * Portions Copyrighted 2017-2018 ConnId
 */
package org.identityconnectors.framework.impl.api.local.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.api.operations.GetApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Handles both version of update this include simple replace and the advance update.
 */
public class UpdateImpl extends ConnectorAPIOperationRunner implements UpdateApiOp {

    // Special logger with SPI operation log name. Used for logging operation entry/exit
    private static final Log OP_LOG = Log.getLog(UpdateOp.class);

    /**
     * All the operational attributes that can not be added or deleted.
     */
    private static final Set<String> OPERATIONAL_ATTRIBUTE_NAMES = new HashSet<String>();

    static {
        OPERATIONAL_ATTRIBUTE_NAMES.addAll(OperationalAttributes.getOperationalAttributeNames());
        OPERATIONAL_ATTRIBUTE_NAMES.add(Name.NAME);
    }

    private static final String OPERATIONAL_ATTRIBUTE_ERR = "Operational attribute '%s' can not be added or removed.";

    /**
     * Determines which type of update a connector supports and then uses that
     * handler.
     */
    public UpdateImpl(final ConnectorOperationalContext context, final Connector connector) {
        super(context, connector);
    }

    @Override
    public Uid update(
            final ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {

        // validate all the parameters..
        validateInput(objectClass, uid, replaceAttributes, false);
        // cast null as empty
        if (options == null) {
            options = new OperationOptionsBuilder().build();
        }

        final ObjectNormalizerFacade normalizer = getNormalizer(objectClass);
        uid = (Uid) normalizer.normalizeAttribute(uid);
        replaceAttributes = normalizer.normalizeAttributes(replaceAttributes);
        UpdateOp op = (UpdateOp) getConnector();

        logOpEntry("update", objectClass, uid, replaceAttributes, options);

        Uid ret;
        try {
            ret = op.update(objectClass, uid, replaceAttributes, options);
        } catch (RuntimeException e) {
            SpiOperationLoggingUtil.logOpException(OP_LOG, getOperationalContext(), UpdateOp.class, "update", e);
            throw e;
        }

        logOpExit("update", ret);

        return (Uid) normalizer.normalizeAttribute(ret);
    }

    @Override
    public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        // validate all the parameters..
        validateInput(objclass, uid, valuesToAdd, true);
        // cast null as empty
        if (options == null) {
            options = new OperationOptionsBuilder().build();
        }

        final ObjectNormalizerFacade normalizer = getNormalizer(objclass);
        uid = (Uid) normalizer.normalizeAttribute(uid);
        valuesToAdd = normalizer.normalizeAttributes(valuesToAdd);
        UpdateOp op = (UpdateOp) getConnector();
        Uid ret;
        if (op instanceof UpdateAttributeValuesOp) {
            UpdateAttributeValuesOp valueOp = (UpdateAttributeValuesOp) op;
            logOpEntry("addAttributeValues", objclass, uid, valuesToAdd, options);
            try {
                ret = valueOp.addAttributeValues(objclass, uid, valuesToAdd, options);
            } catch (RuntimeException e) {
                logOpException("addAttributeValues", e);
                throw e;
            }
            logOpExit("addAttributeValues", ret);
        } else {
            Set<Attribute> replaceAttributes = fetchAndMerge(objclass, uid, valuesToAdd, true, options);
            logOpEntry("update", objclass, uid, replaceAttributes, options);
            try {
                ret = op.update(objclass, uid, replaceAttributes, options);
            } catch (RuntimeException e) {
                logOpException("update", e);
                throw e;
            }
            logOpExit("update", ret);
        }
        return (Uid) normalizer.normalizeAttribute(ret);
    }

    @Override
    public Uid removeAttributeValues(
            ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {

        // validate all the parameters..
        validateInput(objclass, uid, valuesToRemove, true);
        // cast null as empty
        if (options == null) {
            options = new OperationOptionsBuilder().build();
        }

        final ObjectNormalizerFacade normalizer = getNormalizer(objclass);
        uid = (Uid) normalizer.normalizeAttribute(uid);
        valuesToRemove = normalizer.normalizeAttributes(valuesToRemove);
        UpdateOp op = (UpdateOp) getConnector();
        Uid ret;
        if (op instanceof UpdateAttributeValuesOp) {
            UpdateAttributeValuesOp valueOp = (UpdateAttributeValuesOp) op;
            logOpEntry("removeAttributeValues", objclass, uid, valuesToRemove, options);
            try {
                ret = valueOp.removeAttributeValues(objclass, uid, valuesToRemove, options);
            } catch (RuntimeException e) {
                logOpException("removeAttributeValues", e);
                throw e;
            }
            logOpExit("removeAttributeValues", ret);
        } else {
            Set<Attribute> replaceAttributes = fetchAndMerge(objclass, uid, valuesToRemove, false, options);
            logOpEntry("update", objclass, uid, replaceAttributes, options);
            try {
                ret = op.update(objclass, uid, replaceAttributes, options);
            } catch (RuntimeException e) {
                logOpException("update", e);
                throw e;
            }
            logOpExit("update", ret);
        }
        return (Uid) normalizer.normalizeAttribute(ret);
    }

    private Set<Attribute> fetchAndMerge(ObjectClass objclass, Uid uid,
            Set<Attribute> valuesToChange, boolean add, OperationOptions options) {

        // check that this connector supports Search..
        if (!(getConnector() instanceof SearchOp)) {
            throw new UnsupportedOperationException("Connector must support: " + SearchOp.class);
        }

        // add attrs to get to operation options, so that the
        // object we fetch has exactly the set of attributes we require
        // (there may be ones that are not in the default set)
        OperationOptionsBuilder builder = new OperationOptionsBuilder(options);
        Set<String> attrNames = new HashSet<>();
        valuesToChange.forEach((attribute) -> {
            attrNames.add(attribute.getName());
        });
        builder.setAttributesToGet(attrNames);
        options = builder.build();

        // get the connector object from the resource...
        ConnectorObject o = getConnectorObject(objclass, uid, options);
        if (o == null) {
            throw new UnknownUidException(uid, objclass);
        }
        // merge the update data..
        Set<Attribute> mergeAttrs = merge(valuesToChange, o.getAttributes(), add);
        return mergeAttrs;
    }

    /**
     * Merges two connector objects into a single updated object.
     */
    public Set<Attribute> merge(Set<Attribute> updateAttrs, Set<Attribute> baseAttrs, boolean add) {
        // return the merged attributes
        Set<Attribute> ret = new HashSet<>();
        // create map that can be modified to get the subset of changes
        Map<String, Attribute> baseAttrMap = AttributeUtil.toMap(baseAttrs);
        // run through attributes of the current object..
        for (final Attribute updateAttr : updateAttrs) {
            // get the name of the update attributes
            String name = updateAttr.getName();
            // remove each attribute that is an update attribute..
            Attribute baseAttr = baseAttrMap.get(name);
            List<Object> values;
            final Attribute modifiedAttr;
            if (add) {
                if (baseAttr == null) {
                    modifiedAttr = updateAttr;
                } else {
                    // create a new list with the base attribute to add to..
                    values = CollectionUtil.newList(baseAttr.getValue());
                    values.addAll(updateAttr.getValue());
                    modifiedAttr = AttributeBuilder.build(name, values);
                }
            } else {
                if (baseAttr == null) {
                    // nothing to actually do the attribute do not exist
                    continue;
                } else {
                    // create a list with the base attribute to remove from..
                    values = CollectionUtil.newList(baseAttr.getValue());
                    updateAttr.getValue().forEach((val) -> {
                        values.remove(val);
                    });
                    // if the values are empty send a null to the connector..
                    if (values.isEmpty()) {
                        modifiedAttr = AttributeBuilder.build(name);
                    } else {
                        modifiedAttr = AttributeBuilder.build(name, values);
                    }
                }
            }
            ret.add(modifiedAttr);
        }
        return ret;
    }

    /**
     * Get the {@link ConnectorObject} to modify.
     */
    private ConnectorObject getConnectorObject(ObjectClass oclass, Uid uid, OperationOptions options) {
        // attempt to get the connector object..
        GetApiOp get = new GetImpl(new SearchImpl(getOperationalContext(), getConnector()));
        return get.getObject(oclass, uid, options);
    }

    /**
     * Makes things easier if you can trust the input.
     */
    public static void validateInput(final ObjectClass objectClass, final Uid uid,
            final Set<Attribute> replaceAttributes, boolean isDelta) {

        Assertions.nullCheck(uid, "uid");
        Assertions.nullCheck(objectClass, "objectClass");
        if (ObjectClass.ALL.equals(objectClass)) {
            throw new UnsupportedOperationException(
                    "Operation is not allowed on __ALL__ object class");
        }
        Assertions.nullCheck(replaceAttributes, "replaceAttributes");
        // check to make sure there's not a uid..
        if (AttributeUtil.getUidAttribute(replaceAttributes) != null) {
            throw new InvalidAttributeValueException("Parameter 'replaceAttributes' contains a uid.");
        }
        // check for things only valid during ADD/DELETE
        if (isDelta) {
            replaceAttributes.forEach(attr -> {
                Assertions.nullCheck(attr, "replaceAttributes");
                // make sure that none of the values are null..
                if (attr.getValue() == null) {
                    throw new IllegalArgumentException("Can not add or remove a 'null' value.");
                }
                // make sure that if this an delete/add that it doesn't include
                // certain attributes because it doesn't make any sense..
                String name = attr.getName();
                if (OPERATIONAL_ATTRIBUTE_NAMES.contains(name)) {
                    String msg = String.format(OPERATIONAL_ATTRIBUTE_ERR, name);
                    throw new IllegalArgumentException(msg);
                }
            });
        }
    }

    private void logOpEntry(String opName, ObjectClass objectClass, Uid uid, Set<Attribute> attrs,
            OperationOptions options) {
        SpiOperationLoggingUtil.logOpEntry(OP_LOG, getOperationalContext(), UpdateOp.class, opName,
                objectClass, uid, attrs, options);
    }

    private void logOpExit(String opName, Uid uid) {
        SpiOperationLoggingUtil.logOpExit(OP_LOG, getOperationalContext(), UpdateOp.class, opName, uid);
    }

    private void logOpException(String opName, RuntimeException e) {
        SpiOperationLoggingUtil.logOpException(OP_LOG, getOperationalContext(), UpdateOp.class, opName, e);
    }
}
