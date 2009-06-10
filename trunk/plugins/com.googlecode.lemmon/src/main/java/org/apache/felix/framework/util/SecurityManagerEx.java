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
 * Modified for Lemmon.
 * Always returns an empty class context since GAE does not allow applications 
 * to create a SecurityManager.
 * This limits Felix class handling in certain corner cases. 
 */

/**
 * <p>
 * Simple utility class used to provide public access to the protected
 * <tt>getClassContext()</tt> method of <tt>SecurityManager</tt>
 * </p>
**/
public class SecurityManagerEx 
{
    public Class[] getClassContext()
    {
        return new Class[0];
//        try
//        {
//            SecurityManager securityManager= System.getSecurityManager();
//            if (securityManager == null)
//                securityManager= new SecurityManager();
//            Method getClassContext= SecurityManager.class.getMethod( "getClassContext", null );
//            getClassContext.setAccessible( true );
//            return ( Class[] ) getClassContext.invoke( securityManager, null );
//        }
//        catch ( Throwable e )
//        {
//            e.printStackTrace();
//            throw new RuntimeException("Error generating class context", e);
//        }
   }
}