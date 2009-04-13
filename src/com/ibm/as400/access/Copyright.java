///////////////////////////////////////////////////////////////////////////////
//
// JTOpen (IBM Toolbox for Java - OSS version)
//
// Filename:  Copyright.java
//
// The source code contained herein is licensed under the IBM Public License
// Version 1.0, which has been approved by the Open Source Initiative.
// Copyright (C) 1997-2005 International Business Machines Corporation and
// others.  All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

/**
 The Copyright interface is used to hold the copyright string and the version information for the IBM Toolbox for Java.
 **/
public interface Copyright
{
    /** @deprecated This field is reserved for use within the Toolbox product. **/
    public static final String copyright = "Copyright (C) 1997-2008 International Business Machines Corporation and others.";
    public static final String version   = "Open Source Software, JTOpen 6.5, codebase 5761-JC1 V6R1M0.7";

    // Constants for reference by AS400JDBCDriver.
    static final int    MAJOR_VERSION = 8;  // ex: "8" indicates V6R1
    static final int    MINOR_VERSION = 7;  // ex: "7" indicates PTF #7
    static final String DRIVER_LEVEL  = "06010007"; //(ex: 05040102->V5R4M1.2) (needed for hidden clientInfo) (each # is 2 digits in length)
}
