///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: JobLogBeanInfo.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.beans.VetoableChangeListener;
import java.util.ResourceBundle;



/**
The JobLogBeanInfo class represents the bean information
for the JobLog class.
**/
public class JobLogBeanInfo
extends SimpleBeanInfo
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";


    // Private data.
    private static final Class                  beanClass_              = JobLog.class;

    private static BeanInfo[]                   additionalBeanInfo_;
    private static BeanDescriptor               beanDescriptor_;
    private static EventSetDescriptor[]         eventSetDescriptors_;
    private static Image                        icon16_;
    private static Image                        icon32_;
    private static PropertyDescriptor[]         propertyDescriptors_;
    private static ResourceBundleLoader         resourceBundle_;



/**
Static initializer.
**/
    static
    {
        try
        {
            // Set up the additional bean info.
            additionalBeanInfo_ = null;

            // Set up the bean descriptor.
            beanDescriptor_ = new BeanDescriptor(beanClass_);

            // Set up the event set descriptors.
            EventSetDescriptor propertyChange = new EventSetDescriptor(beanClass_, "propertyChange", PropertyChangeListener.class, "propertyChange");
            propertyChange.setDisplayName(resourceBundle_.getText("EVT_NAME_PROPERTY_CHANGE"));
            propertyChange.setShortDescription(resourceBundle_.getText("EVT_DESC_PROPERTY_CHANGE"));

            EventSetDescriptor vetoableChange = new EventSetDescriptor(beanClass_, "vetoableChange", VetoableChangeListener.class, "vetoableChange");
            vetoableChange.setDisplayName(resourceBundle_.getText("EVT_NAME_PROPERTY_VETO"));
            vetoableChange.setShortDescription(resourceBundle_.getText("EVT_DESC_PROPERTY_VETO"));

            eventSetDescriptors_ = new EventSetDescriptor[] { propertyChange, vetoableChange };

            // Set up the property descriptors.
            PropertyDescriptor system = new PropertyDescriptor("system", beanClass_);
            system.setBound(true);
            system.setConstrained(true);
            system.setDisplayName(resourceBundle_.getText("PROP_NAME_SYSTEM"));
            system.setShortDescription(resourceBundle_.getText("PROP_DESC_SYSTEM"));

            PropertyDescriptor name = new PropertyDescriptor("name", beanClass_);
            name.setBound(true);
            name.setConstrained(true);
            name.setDisplayName(resourceBundle_.getText("PROP_NAME_NAME"));
            name.setShortDescription(resourceBundle_.getText("PROP_DESC_NAME"));

            PropertyDescriptor number = new PropertyDescriptor("number", beanClass_);
            number.setBound(true);
            number.setConstrained(true);
            number.setDisplayName(resourceBundle_.getText("PROP_NAME_JOB_NUMBER"));
            number.setShortDescription(resourceBundle_.getText("PROP_DESC_JOB_NUMBER"));

            PropertyDescriptor user = new PropertyDescriptor("user", beanClass_);
            user.setBound(true);
            user.setConstrained(true);
            user.setDisplayName(resourceBundle_.getText("PROP_NAME_JOB_USER"));
            user.setShortDescription(resourceBundle_.getText("PROP_DESC_JOB_USER"));

            propertyDescriptors_ = new PropertyDescriptor[] { system, name, number, user };

        }
        catch (Exception e)
        {
            if (Trace.isTraceOn())
                Trace.log(Trace.ERROR, "Error while loading bean info", e);
            throw new Error(e.toString());
        }
    }



/**
Returns the additional bean information.

@return The additional bean information.
**/
    public BeanInfo[] getAdditionalBeanInfo()
    {
        return additionalBeanInfo_;
    }


/**
Returns the bean descriptor.

@return The bean descriptor.
**/
    public BeanDescriptor getBeanDescriptor()
    {
        return beanDescriptor_;
    }



/**
Returns the default event index.

@return The default event index.
**/
    public int getDefaultEventIndex()
    {
        return 0;
    }



/**
Returns the event set descriptors.

@return The event set descriptors.
**/
    public EventSetDescriptor[] getEventSetDescriptors()
    {
       return eventSetDescriptors_;
    }



/**
Returns the property descriptors.

@return The property descriptors.
**/
    public PropertyDescriptor[] getPropertyDescriptors()
    {
       return propertyDescriptors_;
    }



/**
Returns the icon.

@param icon The icon kind.  Possible values are:
                <ul>
                <li>BeanInfo.ICON_MONO_16x16
                <li>BeanInfo.ICON_MONO_32x32
                <li>BeanInfo.ICON_COLOR_16x16
                <li>BeanInfo.ICON_COLOR_32x32
                </ul>
@return         The icon.
**/
    public Image getIcon(int icon)
    {
        switch(icon)
        {
            case BeanInfo.ICON_MONO_16x16:
            case BeanInfo.ICON_COLOR_16x16:
                if (icon16_ == null)
                    icon16_ = loadImage("JobLog16.gif");
                return icon16_;

            case BeanInfo.ICON_MONO_32x32:
            case BeanInfo.ICON_COLOR_32x32:
                if (icon32_ == null)
                    icon32_ = loadImage("JobLog32.gif");
                return icon32_;

            default:
            throw new ExtendedIllegalArgumentException("icon", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
        }
    }

}

