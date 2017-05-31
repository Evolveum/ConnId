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
 */
package org.identityconnectors.framework.impl.api;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;

public class ConfigurationPropertiesImpl implements ConfigurationProperties {

    // =======================================================================
    // Fields
    // =======================================================================
    /**
     * Properties, listed in order by their "order" attribute
     */
    LinkedHashMap<String, ConfigurationProperty> properties;

    /**
     * The container. Not serialized in this object. Set when this property is
     * added to parent
     */
    private transient APIConfiguration parent;

    // =======================================================================
    // Internal Methods
    // =======================================================================

    public APIConfiguration getParent() {
        return parent;
    }

    public void setParent(APIConfiguration parent) {
        this.parent = parent;
    }

    private static class PropertyComparator implements Comparator<ConfigurationProperty>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public int compare(final ConfigurationProperty o1, final ConfigurationProperty o2) {
            int or1 = o1.getOrder();
            int or2 = o2.getOrder();
            return or1 < or2 ? -1 : or1 > or2 ? 1 : 0;
        }
    }

    private static final Comparator<ConfigurationProperty> COMPARATOR =
            new PropertyComparator();

    public void setProperties(Collection<ConfigurationProperty> in) {
        List<ConfigurationProperty> properties = new ArrayList<ConfigurationProperty>(in);
        Collections.sort(properties, COMPARATOR);
        LinkedHashMap<String, ConfigurationProperty> temp =
                new LinkedHashMap<String, ConfigurationProperty>();
        for (ConfigurationProperty property : properties) {
            temp.put(property.getName(), property);
            property.setParent(this);
        }
        this.properties = temp;
    }

    public Collection<ConfigurationProperty> getProperties() {
        return properties.values();
    }

    // =======================================================================
    // Interface Methods
    // =======================================================================
    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationProperty getProperty(String name) {
        return properties.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getPropertyNames() {
        List<String> names = new ArrayList<String>(properties.keySet());
        return CollectionUtil.newReadOnlyList(names);
    }

    private static final String MSG = "Property ''{0}'' does not exist.";

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPropertyValue(String name, Object value) {
        ConfigurationProperty property = properties.get(name);
        if (property == null) {
            throw new IllegalArgumentException(MessageFormat.format(MSG, name));
        }
        property.setValue(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConfigurationProperties) {
            ConfigurationProperties other = (ConfigurationProperties) o;
            HashSet<ConfigurationProperty> set1 =
                    new HashSet<ConfigurationProperty>(properties.values());
            HashSet<ConfigurationProperty> set2 =
                    new HashSet<ConfigurationProperty>(other.getProperties());
            return set1.equals(set2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        HashSet<ConfigurationProperty> set1 =
                new HashSet<ConfigurationProperty>(properties.values());
        return set1.hashCode();
    }

	
}
