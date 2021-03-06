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
package org.identityconnectors.framework.impl.serializer.xml;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.identityconnectors.common.XmlUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.impl.serializer.ObjectDecoder;
import org.identityconnectors.framework.impl.serializer.ObjectSerializationHandler;
import org.identityconnectors.framework.impl.serializer.ObjectSerializerRegistry;
import org.identityconnectors.framework.impl.serializer.ObjectTypeMapper;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class XmlObjectDecoder implements ObjectDecoder {

    private final Element node;

    private final Class<?> expectedClass;

    public XmlObjectDecoder(Element node, Class<?> expectedClass) {
        this.node = node;
        this.expectedClass = expectedClass;
    }

    public Object readObject() {
        return readObjectInternal();
    }

    @Override
    public boolean readBooleanContents() {
        return decodeBoolean(readStringContentsInternal());
    }

    @Override
    public boolean readBooleanField(String fieldName, boolean dflt) {
        return decodeBoolean(readStringAttributeInternal(fieldName, XmlObjectEncoder
                .encodeBoolean(dflt)));
    }

    @Override
    public byte readByteContents() {
        return decodeByte(readStringContentsInternal());
    }

    @Override
    public byte[] readByteArrayContents() {
        return decodeByteArray(readStringContentsInternal());
    }

    @Override
    public Class<?> readClassContents() {
        return decodeClass(readStringContentsInternal());
    }

    @Override
    public Class<?> readClassField(String name, Class<?> dflt) {
        String val = readStringAttributeInternal(name, null);
        if (val == null) {
            return dflt;
        } else {
            return decodeClass(val);
        }
    }

    @Override
    public double readDoubleContents() {
        return decodeDouble(readStringContentsInternal());
    }

    @Override
    public double readDoubleField(String fieldName, double dflt) {
        return decodeDouble(readStringAttributeInternal(fieldName, XmlObjectEncoder.encodeDouble(dflt)));
    }

    @Override
    public float readFloatContents() {
        return decodeFloat(readStringContentsInternal());
    }

    @Override
    public float readFloatField(String fieldName, float dflt) {
        return decodeFloat(readStringAttributeInternal(fieldName, XmlObjectEncoder.encodeFloat(dflt)));
    }

    @Override
    public int readIntContents() {
        return decodeInt(readStringContentsInternal());
    }

    @Override
    public int readIntField(String fieldName, int dflt) {
        return decodeInt(readStringAttributeInternal(fieldName, XmlObjectEncoder.encodeInt(dflt)));
    }

    @Override
    public long readLongContents() {
        return decodeLong(readStringContentsInternal());
    }

    @Override
    public long readLongField(String fieldName, long dflt) {
        return decodeLong(readStringAttributeInternal(fieldName, XmlObjectEncoder.encodeLong(dflt)));
    }

    @Override
    public int getNumSubObjects() {
        int count = 0;
        for (Element subElement = XmlUtil.getFirstChildElement(node); subElement != null; subElement =
                XmlUtil.getNextElement(subElement)) {
            count++;
        }
        return count;
    }

    @Override
    public Object readObjectContents(int index) {

        Element subElement = XmlUtil.getFirstChildElement(node);
        for (int i = 0; i < index; i++) {
            subElement = XmlUtil.getNextElement(subElement);
        }

        if (subElement == null) {
            throw new ConnectorException("Missing subelement number: " + index);
        }

        return new XmlObjectDecoder(subElement, null).readObject();
    }

    @Override
    public Object readObjectField(String fieldName, Class<?> expected, Object dflt) {
        Element child = XmlUtil.findImmediateChildElement(node, fieldName);
        if (child == null) {
            return dflt;
        }
        if (expected != null) {
            return new XmlObjectDecoder(child, expected).readObject();
        }
        Element subElement = XmlUtil.getFirstChildElement(child);
        if (subElement == null) {
            return dflt;
        }
        // if they specify null, don't apply defaults
        return new XmlObjectDecoder(subElement, null).readObject();
    }

    @Override
    public String readStringContents() {
        String rv = readStringContentsInternal();
        return rv == null ? "" : rv;
    }

    @Override
    public String readStringField(String fieldName, String dflt) {
        return readStringAttributeInternal(fieldName, dflt);
    }

    private String readStringContentsInternal() {
        String xml = XmlUtil.getContent(node);
        return xml;
    }

    private String readStringAttributeInternal(String name, String dflt) {
        Attr attr = node.getAttributeNode(name);
        if (attr == null) {
            return dflt;
        }
        return attr.getValue();
    }

    private boolean decodeBoolean(String v) {
        return Boolean.parseBoolean(v);
    }

    private byte decodeByte(String v) {
        return Byte.decode(v);
    }

    private byte[] decodeByteArray(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    private Class<?> decodeClass(String type) {
        if (type.endsWith("[]")) {
            String componentName = type.substring(0, type.length() - "[]".length());
            Class<?> componentClass = decodeClass(componentName);
            Class<?> arrayClass = Array.newInstance(componentClass, 0).getClass();
            return arrayClass;
        } else {
            ObjectTypeMapper mapper = ObjectSerializerRegistry.getMapperBySerialType(type);
            if (mapper == null) {
                throw new ConnectorException("No deserializer for type: " + type);
            }
            Class<?> clazz = mapper.getHandledObjectType();
            return clazz;
        }
    }

    private double decodeDouble(String val) {
        return Double.parseDouble(val);
    }

    private float decodeFloat(String val) {
        return Float.parseFloat(val);
    }

    private int decodeInt(String val) {
        return Integer.parseInt(val);
    }

    private long decodeLong(String val) {
        return Long.parseLong(val);
    }

    private Object readObjectInternal() {
        if (expectedClass != null) {
            ObjectSerializationHandler handler =
                    ObjectSerializerRegistry.getHandlerByObjectType(expectedClass);
            if (handler == null) {
                if (expectedClass.isArray()) {
                    List<Object> temp = new ArrayList<>();
                    for (Element child = XmlUtil.getFirstChildElement(node); child != null; child =
                            XmlUtil.getNextElement(child)) {
                        XmlObjectDecoder sub = new XmlObjectDecoder(child, null);
                        Object obj = sub.readObject();
                        temp.add(obj);
                    }
                    int length = temp.size();
                    Object array = Array.newInstance(expectedClass.getComponentType(), length);
                    for (int i = 0; i < length; i++) {
                        Object element = temp.get(i);
                        Array.set(array, i, element);
                    }
                    return array;
                } else {
                    throw new ConnectorException("No deserializer for type: " + expectedClass);
                }
            } else {
                return handler.deserialize(this);
            }
        } else if (node.getTagName().equals("null")) {
            return null;
        } else if (node.getTagName().equals("Array")) {
            String componentType = XmlUtil.getAttribute(node, "componentType");
            if (componentType == null) {
                componentType = "Object";
            }
            Class<?> componentClass = decodeClass(componentType);
            List<Object> temp = new ArrayList<>();
            for (Element child = XmlUtil.getFirstChildElement(node); child != null; child =
                    XmlUtil.getNextElement(child)) {
                XmlObjectDecoder sub = new XmlObjectDecoder(child, null);
                Object obj = sub.readObject();
                temp.add(obj);
            }
            int length = temp.size();
            Object array = Array.newInstance(componentClass, length);
            for (int i = 0; i < length; i++) {
                Object element = temp.get(i);
                Array.set(array, i, element);
            }
            return array;
        } else {
            Class<?> clazz = decodeClass(node.getTagName());
            ObjectSerializationHandler handler =
                    ObjectSerializerRegistry.getHandlerByObjectType(clazz);
            if (handler == null) {
                throw new ConnectorException("No deserializer for type: " + clazz);
            } else {
                return handler.deserialize(this);
            }
        }
    }
}
