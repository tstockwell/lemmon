/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.framework.util;

/**
 * Modified for Lemmon
 * Added SYSTEMBUNDLE_START_SERVICE
 * Added SYSTEMBUNDLE_BUNDLE_CACHE
 * Added SYSTEMBUNDLE_FIBER_FACTORY
 * 
 * @author ted stockwell
 */
public interface FelixConstants extends org.osgi.framework.Constants
{
    public static final String SYSTEM_BUNDLE_SYMBOLICNAME = "org.apache.felix.framework";
    // Framework constants and values.
    public static final String FRAMEWORK_VERSION_VALUE = "1.3";
    public static final String FRAMEWORK_VENDOR_VALUE = "Apache Software Foundation";

    // Framework constants and values.
    public static final String FELIX_VERSION_PROPERTY = "felix.version";

    // Miscellaneous manifest constants.
    public static final String DIRECTIVE_SEPARATOR = ":=";
    public static final String ATTRIBUTE_SEPARATOR = "=";
    public static final String CLASS_PATH_SEPARATOR = ",";
    public static final String CLASS_PATH_DOT = ".";
    public static final String PACKAGE_SEPARATOR = ";";
    public static final String VERSION_SEGMENT_SEPARATOR = ".";
    public static final int VERSION_SEGMENT_COUNT = 3;
    public static final String BUNDLE_NATIVECODE_OPTIONAL = "*";

    // Miscellaneous OSGi constants.
    public static final String BUNDLE_URL_PROTOCOL = "bundle";

    // Miscellaneous framework configuration property names.
    public static final String LOG_LEVEL_PROP = "felix.log.level";
    public static final String LOG_LOGGER_PROP = "felix.log.logger";
    public static final String SYSTEMBUNDLE_ACTIVATORS_PROP
        = "felix.systembundle.activators";
    public static final String SYSTEMBUNDLE_BUNDLE_CACHE
    	= "felix.systembundle.bundle.cache";
    public static final String SYSTEMBUNDLE_START_SERVICE
        = "felix.systembundle.start.service";
    public static final String SYSTEMBUNDLE_FIBER_FACTORY
        = "felix.systembundle.fiber.factory";
    public static final String BUNDLE_STARTLEVEL_PROP
        = "felix.startlevel.bundle";
    public static final String SERVICE_URLHANDLERS_PROP = "felix.service.urlhandlers";

    // THESE CAN BE DELETED ONCE WE HAVE COMPLETE FRAGMENT SUPPORT.
    public static final String FRAGMENT_VALIDATION_PROP = "felix.fragment.validation";
    public static final String FRAGMENT_VALIDATION_EXCEPTION_VALUE = "exception";
    public static final String FRAGMENT_VALIDATION_WARNING_VALUE = "warning";

    // Start level-related constants.
    public static final int FRAMEWORK_INACTIVE_STARTLEVEL = 0;
    public static final int FRAMEWORK_DEFAULT_STARTLEVEL = 1;
    public static final int SYSTEMBUNDLE_DEFAULT_STARTLEVEL = 0;
    public static final int BUNDLE_DEFAULT_STARTLEVEL = 1;

    // Miscellaneous properties values.
    public static final String FAKE_URL_PROTOCOL_VALUE = "location:";
    public static final String FELIX_EXTENSION_ACTIVATOR = "Felix-Activator";
}
