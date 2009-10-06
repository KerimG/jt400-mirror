///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                              
//                                                                             
// Filename: JDLobLocator.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2001 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



/**
The JDLobLocator class provides access to large objects via a locator.
**/
//
// Implementation note:
//
// 1. The maximum size of a lob in AS/400 is 15 MB, so that the
//    size can be stored in an int and the full contents can be
//    stored in an array.  This assumption makes life a lot easier
//    (and works as long as lobs stay 4 GB or smaller).
//
class JDLobLocator
{
  private static final String copyright = "Copyright (C) 1997-2001 International Business Machines Corporation and others.";




    // Private data.
    private AS400JDBCConnection	    connection_;
    private boolean                 dataCompression_;                   // @B0A
    private int                     id_;
    private int                     handle_;
    private long                    length_             = -1;           // @C1A
    private int                     maxLength_;                         // @A1A
    private int                     columnIndex_;                       // @D1A

    private boolean                 graphic_ = false;                   // @C4A
    private boolean                 firstTime_ = true;                  // @C4a 2


/**
Constructs an JDLobLocator object.  

@param  connection              The connection to the server.
@param  id                      The id.
@param  maxLength               The max length.                            @A1A
**/
    public JDLobLocator (AS400JDBCConnection connection,
                         int id,
                         int maxLength)                                 // @A1A
    {
        connection_      = connection;
        id_              = id;
        handle_          = -1;
        maxLength_       = maxLength;                                   // @A1A
        dataCompression_ = connection_.getDataCompression() == AS400JDBCConnection.DATA_COMPRESSION_OLD_; // @B0A @C2C
    	   columnIndex_     = -1;                                          // @D1A
    }



/**
Returns the locator handle.

@return The locator handle, or -1 if not set.
**/
    public int getHandle ()
    {
        return handle_;
    }



    public long getLength()                                 // @C1A
       throws SQLException                                  // @C1A
    {                                                       // @C1A
        if (length_ == -1)                                  // @C1A
            retrieveData(0,0);                              // @C1A
        return length_;                                     // @C1A
    }                                                       // @C1A
                         
    // @C4a new method.  Character based objects (Clobs, for example),
    //      need to know how many characters are in the lob, not
    //      how many bytes. Bytes = characters unless the lob
    //      is a graphic clob. 
    public long getLengthInCharacters()                     
           throws SQLException                                  
    {                                                      
        if (length_ == -1)                                 
            retrieveData(0,0);                             
            
        if (graphic_)                                  
           return length_ / 2;
        else
           return length_;                                 
    }                                                      



    
    public int getMaxLength ()                              // @A1A
    {                                                       // @A1A
        if (graphic_)                           // @C4A
        {                                       // @C4A
            if (firstTime_)                     // @C4a 2
            {                                   // @C4a 2
               maxLength_ = maxLength_ * 2;     // @C4a 2
               firstTime_ = false;              // @C4a 2
            }                                   // @C4a 2
        }                                       // @C4a 2
    
        return maxLength_;                                  // @A1A
    }                                                       // @A1A


/**
Returns the position at which a pattern is found,
in the lob.

@param  patternClause       The pattern clause for the SQL SELECT statement.
                            There must be a parameter marker in this clause.
@param  patternParameter    The pattern parameter to set in the pattern
                            clause.
@param  start               The position within the lob to begin searching.
@return                     The position at which the pattern is found, 
                            or -1 if the pattern is not found.

@exception SQLException     If the pattern is null, the position is not valid,
                            or an error occurs.
**/
/* @A1D - We will not support this for now.  

    public long position (String patternClause, Object patternParameter, long start)
        throws SQLException
    {
        // We need to force the call to LOCATE() into
        // a dynamic SQL SELECT statement so that we can
        // get the results easily.  The QSYS.SYSPROCS is
        // just a dummy table.
        String select = "SELECT LOCATE(" + patternClause + ", ?, ?) FROM QSYS2.SYSPROCS";

        PreparedStatement ps = connection_.prepareStatement (select);
        ps.setObject (1, patternParameter);
        ps.setObject (2, this);
        ps.setLong (3, start);
        
        ResultSet rs = ps.executeQuery ();
        rs.next ();
        long position = rs.getInt (1);
        rs.close ();

        ps.close ();
        return position;
    }
*/    



/**
Retrieves part of the contents of the lob.

@param  start       The position within the lob.
@param  length      The length to return.
@return             The contents.

@exception  SQLException    If the position is not valid,
                            if the length is not valid,
                            or an error occurs.
**/
    public DBLobData retrieveData (int start, int length)           // @B0C
        throws SQLException
    {
        // Validate the parameters.                                 // @A1A
        if ((start < 0) || (start >= maxLength_)                    // @A1A
            || (length < 0))                                        // @A1A
            JDError.throwSQLException (JDError.EXC_ATTRIBUTE_VALUE_INVALID); // @A1A

        // @C1D // The database flags an error if you pass in a 0              @A1A
        // @C1D // length, so do nothing in that case.                         @A1A
        // @C1D if (length == 0) {                                          // @A1A
        // @C1D     DBLobData lobData = new DBLobData (0, 0, false);        // @A1A @B0C @B1C
        // @C1D     lobData.overlay (new byte[0], 0);                       // @A1A @B0C
        // @C1D     return lobData;                                         // @A1A @B0C
        // @C1D }                                                           // @A1A

        // The input parameter (length) is the number of bytes to read.  If
        // the bloc is a graphic clob (DBClob), we must tell the server the
        // number of characters to read beginning with a certain character, 
        // not the number of bytes starting at a certain byte.  Graphic
        // clobs guarentee two bytes per character so take the length and
        // starting offset and divide them by 2.
        int bytesToRead = length;                 // @C4a 2
        int startingOffset = start;               // @C4a 2
        if (graphic_)                             // @C4a 2
        {                                         // @C4a 2
           startingOffset = startingOffset / 2;   // @C4a 2
           bytesToRead = bytesToRead / 2;         // @C4a 2
        }   

        try {
    	  	DBSQLRequestDS request = new DBSQLRequestDS (
    		    DBSQLRequestDS.FUNCTIONID_RETRIEVE_LOB_DATA,
	    	    id_, DBBaseRequestDS.ORS_BITMAP_RETURN_DATA
	    	    + DBBaseRequestDS.ORS_BITMAP_RESULT_DATA, 0);
	    	request.setLOBLocatorHandle (handle_);
	    	request.setRequestedSize (bytesToRead);                   // @C4c 2
	    	request.setStartOffset (startingOffset);                  // @C4c 2
	    	request.setCompressionIndicator (dataCompression_ ? 0xF1 : 0xF0);   // @B0C
            request.setReturnCurrentLengthIndicator(0xF1);                      // @C1A
          request.setColumnIndex(columnIndex_);   //@C3A
             
            if (JDTrace.isTraceOn ())
                JDTrace.logInformation (connection_, "Retrieving lob data");

            DBReplyRequestedDS reply = connection_.sendAndReceive (request, id_);
           	int errorClass = reply.getErrorClass();
	        int returnCode = reply.getReturnCode();

            if (errorClass != 0)
	    	    JDError.throwSQLException (connection_, id_, errorClass, returnCode);

            length_ = reply.getCurrentLOBLength();                              // @C1A

            //@C4D return reply.getLOBData ();
            //@C4 Adjust lengths if necessary.  The server returns the number
            //    of characters but these routine work in bytes.  If the
            //    clob is a dbclob then there are two bytes per characters
            //    so multiple the lengths.
            if (graphic_)                           // @C4A
               length_ = length_ * 2;               // @C4A
               
            DBLobData lobData = reply.getLOBData(); // @C4A

            if (graphic_)                           // @C4A
            {                                       // @C4A
                lobData.adjustForGraphic();         // @C4A
                if (firstTime_)                     // @C4a 2
                {                                   // @C4a 2
                   maxLength_ = maxLength_ * 2;     // @C4a 2
                   firstTime_ = false;              // @C4a 2
                }                                   // @C4a 2
            }                                       // @C4a 2

            return lobData;                         // @C4A
	    }
        catch (DBDataStreamException e) {
    	  	JDError.throwSQLException (JDError.EXC_INTERNAL, e);
            return null;
   	    }
    }


//@D1A
/**
Sets the column index.

@param handle The column index.
**/
    public void setColumnIndex (int columnIndex)
    {
        columnIndex_ = columnIndex;
    }



/**
Sets the locator handle.

@param handle The locator handle.
**/
    public void setHandle (int handle)
    {
        handle_ = handle;
        length_ = -1;                                           // @C1A
    }



/**
Writes part of the contents of the lob.

@param  start       The position within the lob.
@param  length      The length to write.
@param  data        The contents.

@exception  SQLException    If the position is not valid,
                            if the length is not valid,
                            or an error occurs.
**/
    public void writeData (int start, int length, byte[] data)
        throws SQLException
    {
        // The database flags an error if you pass in a 0              @A1A
        // length, so do nothing in that case.                         @A1A
        if (length == 0)                                            // @A1A
            return;                                                 // @A1A

        try {

    	  	DBSQLRequestDS request = new DBSQLRequestDS (
    		    DBSQLRequestDS.FUNCTIONID_WRITE_LOB_DATA,
	    	    id_, DBBaseRequestDS.ORS_BITMAP_RETURN_DATA
	    	    + DBBaseRequestDS.ORS_BITMAP_RESULT_DATA, 0);
       	    request.setLOBLocatorHandle (handle_);
            request.setRequestedSize (length);
	    	request.setStartOffset (start);
	    	request.setCompressionIndicator (0xF0); // No compression for now.
            request.setLOBData (data);

            if (JDTrace.isTraceOn ())
                JDTrace.logInformation (connection_, "Writing lob data");

            DBReplyRequestedDS reply = connection_.sendAndReceive (request, id_);
           	int errorClass = reply.getErrorClass();
	        int returnCode = reply.getReturnCode();

            if (errorClass != 0)
	    	    JDError.throwSQLException (connection_, id_, errorClass, returnCode);
	    }
        catch (DBDataStreamException e) {
    	  	JDError.throwSQLException (JDError.EXC_INTERNAL, e);
   	    }
    }


    void setGraphic(boolean graphic)  // @C4A
    {
        graphic_ = graphic;
    }
    
    boolean isGraphic()  // @C4a
    {
        return graphic_;
    }

}