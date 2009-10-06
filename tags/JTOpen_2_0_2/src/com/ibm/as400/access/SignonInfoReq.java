///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: SignonInfoReq.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.IOException;
import java.io.OutputStream;

class SignonInfoReq extends ClientAccessDataStream
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";

    SignonInfoReq(byte[] userIDbytes, byte[] encryptedPassword, int serverLevel)
    {
        super(new byte[(serverLevel == 0) ? 51 : (encryptedPassword.length == 8) ? 61 : 73]);

        setLength(data_.length);
        // setHeaderID(0x0000);
        setServerID(0xE009);
        // setCSInstance(0x00000000);
        // setCorrelation(0x00000000);
        setTemplateLen(0x0001);
        setReqRepID(0x7004);

        // Password's always encrypted.
        data_[20] = (encryptedPassword.length == 8) ? (byte)0x01 : (byte)0x03;

        // Set user ID info.
        //   LL
        set32bit(16, 21);
        //   CP
        set16bit(0x1104, 25);
        //   EBCDIC user ID.
        System.arraycopy(userIDbytes, 0, data_, 27, 10);

        // Set password info.
        //   LL
        set32bit(6 + encryptedPassword.length, 37);
        //   CP
        set16bit(0x1105, 41);
        //   Password data.
        System.arraycopy(encryptedPassword, 0, data_, 43, encryptedPassword.length);

        if (serverLevel != 0)
        {
            // Client CCSID.
            //   LL
            set32bit(10, 43 + encryptedPassword.length);
            //   CP
            set16bit(0x1113, 47 + encryptedPassword.length);
            //   CCSID
            set32bit(13488, 49 + encryptedPassword.length);    // Client CCSID.
        }
    }

    void write(OutputStream out) throws IOException
    {
        Trace.log(Trace.DIAGNOSTIC, "Sending retrieve signon information request...");
        super.write(out);
    }
}