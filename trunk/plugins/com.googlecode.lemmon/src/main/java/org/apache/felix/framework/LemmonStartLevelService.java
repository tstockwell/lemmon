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
package org.apache.felix.framework;

import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.service.startlevel.StartLevel;

/**
 * Custom implementation for Lemmon
 * In order to avoid creating a Thread (which GAE disallows) this 
 * implementation performs all operations synchronously.
 *  
 * @author Ted Stockwell 
 */
public class LemmonStartLevelService implements StartLevel
{
    
    Felix m_felix;

    void stop()
    {
        // do nothing
    }
    
    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#getStartLevel()
    **/
    public int getStartLevel()
    {
        return m_felix.getActiveStartLevel();
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#setStartLevel(int)
    **/
    public void setStartLevel(int startlevel)
    {
            m_felix.checkPermission(
                new AdminPermission(m_felix, AdminPermission.STARTLEVEL));
        
        if (startlevel <= 0)
        {
            throw new IllegalArgumentException(
                "Start level must be greater than zero.");
        }
        
        m_felix.setActiveStartLevel(startlevel);
    }


    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#getBundleStartLevel(org.osgi.framework.Bundle)
    **/
    public int getBundleStartLevel(Bundle bundle)
    {
        return m_felix.getBundleStartLevel(bundle);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#setBundleStartLevel(org.osgi.framework.Bundle, int)
    **/
    public void setBundleStartLevel(Bundle bundle, int startlevel)
    {
        m_felix.checkPermission(
                new AdminPermission(bundle, AdminPermission.STARTLEVEL));
        
        if (bundle.getBundleId() == 0)
        {
            throw new IllegalArgumentException(
                "Cannot change system bundle start level.");
        }
        else if (startlevel <= 0)
        {
            throw new IllegalArgumentException(
                "Start level must be greater than zero.");
        }
        
        m_felix.setBundleStartLevel(bundle, startlevel);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#getInitialBundleStartLevel()
    **/
    public int getInitialBundleStartLevel()
    {
        return m_felix.getInitialBundleStartLevel();
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#setInitialBundleStartLevel(int)
    **/
    public void setInitialBundleStartLevel(int startlevel)
    {
        m_felix.checkPermission(
                new AdminPermission(m_felix, AdminPermission.STARTLEVEL));
        m_felix.setInitialBundleStartLevel(startlevel);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#isBundlePersistentlyStarted(org.osgi.framework.Bundle)
    **/
    public boolean isBundlePersistentlyStarted(Bundle bundle)
    {
        return m_felix.isBundlePersistentlyStarted(bundle);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#isBundleActivationPolicyUsed(org.osgi.framework.Bundle)
    **/
    public boolean isBundleActivationPolicyUsed(Bundle bundle)
    {
        throw new UnsupportedOperationException("This feature has not yet been implemented.");
    }

    void setFramework( Felix felix )
    {
        m_felix= felix;
    }

}