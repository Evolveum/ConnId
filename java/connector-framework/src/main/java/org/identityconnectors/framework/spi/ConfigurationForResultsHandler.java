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
package org.identityconnectors.framework.spi;


public interface ConfigurationForResultsHandler extends Configuration {
	
	public boolean isEnableAttributesToGetSearchResultsHandler();

    public void setEnableAttributesToGetSearchResultsHandler(
            boolean enableAttributesToGetSearchResultsHandler);

    public boolean isEnableCaseInsensitiveFilter();

    public void setEnableCaseInsensitiveFilter(boolean enableCaseInsensitiveFilter);

    public boolean isFilteredResultsHandlerInValidationMode();

    public void setFilteredResultsHandlerInValidationMode(boolean filteredResultsHandlerInValidationMode);

    public boolean isEnableFilteredResultsHandler();

    public void setEnableFilteredResultsHandler(boolean enableFilteredResultsHandler);

    public boolean isEnableNormalizingResultsHandler();

    public void setEnableNormalizingResultsHandler(boolean enableNormalizingResultsHandler);
	

}
