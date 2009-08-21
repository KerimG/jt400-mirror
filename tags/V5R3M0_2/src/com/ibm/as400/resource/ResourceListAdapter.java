///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: ResourceListAdapter.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.resource;



/**
The ResourceListAdapter class is a default implementation of the
{@link com.ibm.as400.resource.ResourceListListener ResourceListListener}
interface.
**/
public class ResourceListAdapter
implements ResourceListListener
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";




/**
Invoked when the length changes.

@param event    The event.
**/
    public void lengthChanged(ResourceListEvent event) { }



/**
Invoked when the list is closed.

@param event    The event.
**/
    public void listClosed(ResourceListEvent event) { }



/**
Invoked when the list is completely loaded.

@param event    The event.
**/
    public void listCompleted(ResourceListEvent event) { }



/**
Invoked when the list is not completely loaded due to an error.

@param event    The event.
**/
    public void listInError(ResourceListEvent event) { }



/**
Invoked when the list is opened.

@param event    The event.
**/
    public void listOpened(ResourceListEvent event) { }



/**
Invoked when a resource is added to the list.

@param event    The event.
**/
    public void resourceAdded(ResourceListEvent event) { }


}