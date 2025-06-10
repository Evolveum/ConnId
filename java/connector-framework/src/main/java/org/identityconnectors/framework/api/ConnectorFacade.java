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
 * Portions Copyrighted 2010-2013 ForgeRock AS.
 * Portions Copyrighted 2018-2022 Evolveum
 */
package org.identityconnectors.framework.api;

import java.util.Set;

import org.identityconnectors.framework.api.operations.*;

/**
 * Main interface through which an application invokes Connector operations.
 * Represents at the API level a specific instance of a Connector that has been
 * configured in a specific way.
 *
 * @see ConnectorFacadeFactory
 *
 * @author Will Droste
 * @since 1.0
 */
public interface ConnectorFacade extends CreateApiOp, DeleteApiOp, SearchApiOp, UpdateApiOp, UpdateDeltaApiOp,
        SchemaApiOp, AuthenticationApiOp, ResolveUsernameApiOp, GetApiOp, ValidateApiOp, TestApiOp,
        ScriptOnConnectorApiOp, ScriptOnResourceApiOp, SyncApiOp, DiscoverConfigurationApiOp, PartialSchemaApiOp {

    /**
     * Gets the unique generated identifier of this ConnectorFacade.
     *
     * It's not guarantied that the equivalent configuration will generate the
     * same configuration key. Always use the generated value and maintain it in
     * the external application.
     *
     * @return identifier of this ConnectorFacade instance.
     * @since 1.4
     */
    public String getConnectorFacadeKey();

    /**
     * Get the set of operations that this {@link ConnectorFacade} will support.
     */
    Set<Class<? extends APIOperation>> getSupportedOperations();

    /**
     * Get an instance of an operation that this facade supports.
     */
    APIOperation getOperation(Class<? extends APIOperation> clazz);
    
    /**
     * Dispose of any resources associated with this facade (except for facade classes).
     * This will dispose of any connector instances in the connector pool. The purpose
     * of this method is to reduce resource waste for connectors that are no longer used.
     * But it can also be used to implement "logout and login" functionality for connectors,
     * e.g. in cases when server-side configuration has changed and the operator needs to
     * force closing and re-opening of all connector connections.
     * 
     * @since 1.5.0.0
     */
    void dispose();

}
