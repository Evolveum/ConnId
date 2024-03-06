/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Evolveum. All rights reserved.
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
package org.identityconnectors.framework.common.objects;

import java.util.Objects;

/**
 * Reference to a connector object. It may contain the identifier/identifiers only (e.g. Uid, Name, or other attributes),
 * or it may contain the whole object, fetched partially or fully.
 */
public class ConnectorObjectReference {

    /** Must be either {@link ConnectorObject} or {@link ConnectorObjectIdentification}. */
    private final BaseConnectorObject referencedObject;

    public ConnectorObjectReference(BaseConnectorObject referencedObject) {
        if (!(referencedObject instanceof ConnectorObject)
                && !(referencedObject instanceof ConnectorObjectIdentification)) {
            throw new IllegalArgumentException("Referenced object must be either ConnectorObject or ConnectorObjectIdentification");
        }
        this.referencedObject = referencedObject;
    }

    /** True if the object is present. False if only the identifiers are. */
    public boolean hasObject() {
        return referencedObject instanceof ConnectorObject;
    }

    /** Returns {@link ConnectorObject} or {@link ConnectorObjectIdentification}, whatever is present. */
    public BaseConnectorObject getReferencedValue() {
        return Objects.requireNonNull(referencedObject);
    }

    /** Returns {@link ConnectorObject} or fails if there's none. */
    public ConnectorObject getReferencedObject() {
        if (referencedObject instanceof ConnectorObject) {
            return (ConnectorObject) referencedObject;
        } else {
            throw new IllegalStateException("Referenced object is not available");
        }
    }

    public ConnectorObjectIdentification getReferencedObjectIdentification() {
        if (referencedObject instanceof ConnectorObjectIdentification) {
            return (ConnectorObjectIdentification) referencedObject;
        } else {
            return ((ConnectorObject) referencedObject).getIdentification();
        }
    }
}
