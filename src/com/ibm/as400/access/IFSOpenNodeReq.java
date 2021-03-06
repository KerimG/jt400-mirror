///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                              
//                                                                             
// Filename: IFSOpenNodeReq.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 2021 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

public class IFSOpenNodeReq extends IFSDataStreamReq {
  private static final String copyright = "Copyright (C) 2021 International Business Machines Corporation and others.";
	private static final int HEADER_LENGTH = 20;
	private static final int TEMPLATE_LENGTH = 14;
	private static final int OBJECT_HANDLE_OFFSET = HEADER_LENGTH + 2;
	private static final int OPEN_FLAG_OFFSET = OBJECT_HANDLE_OFFSET + 4;
	
	IFSOpenNodeReq (int objectHandle, int datastreamLevel) {
		super(HEADER_LENGTH + TEMPLATE_LENGTH);
		setLength(data_.length);
	    setTemplateLen(TEMPLATE_LENGTH);
	    setReqRepID(0x001C);
	    set32bit(objectHandle, OBJECT_HANDLE_OFFSET);
	    //if (datastreamLevel <= 2) {
	    	
	    //} else {
	    long OFLAG = 0x000000000000L;
	    OFLAG = OFLAG | (0x000000000001L | 0x000000010000L);
	    set64bit(OFLAG, OPEN_FLAG_OFFSET);
	    //}
	}
	
	private final static int getTemplateLength(int datastreamLevel) {
	    return (datastreamLevel <= 2 ? 10 : 14);
	}

}
