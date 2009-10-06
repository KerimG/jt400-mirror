///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: AS400PortMapDS.java
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

// A class representing a "port map" request data stream.  This is a special request class that does not derive from the DataStream class.  The DataStream class defines data streams that originate from or are destined to a server job.
class AS400PortMapDS
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";

    byte[] data_;

    // Create request, data is name of server in ASCII.
    AS400PortMapDS(String server)
    {
        // Cheat conversion from Unicode to ASCII by casting away the high byte.
        // Server names use an acceptable restricted character set.
        char[] uniChars = server.toCharArray();
        data_ = new byte[uniChars.length];
        for (int i = 0; i < uniChars.length; ++i)
        {
            data_[i] = (byte)uniChars[i];
        }
    }

    // Send request to the port mapper.
    void write(OutputStream out) throws IOException
    {
        Trace.log(Trace.DIAGNOSTIC, "Sending port mapper request...");
        synchronized(out)
        {
            out.write(data_);
            out.flush();
        }
        if (Trace.isTraceOn()) Trace.log(Trace.DATASTREAM, "Data stream sent...", data_);
    }
}