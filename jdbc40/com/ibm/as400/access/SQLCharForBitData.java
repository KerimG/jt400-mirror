///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                                 
//                                                                             
// Filename: SQLCharForBitData.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2006 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

final class SQLCharForBitData
implements SQLData
{
    private static final String copyright = "Copyright (C) 1997-2006 International Business Machines Corporation and others.";

    private SQLConversionSettings   settings_;
    private int                     maxLength_;
    private int                     truncated_;
    private AS400ByteArray          typeConverter_;
    private byte[]                  value_;

    SQLCharForBitData(int maxLength, SQLConversionSettings settings)
    {
        settings_       = settings;
        maxLength_      = maxLength;
        truncated_      = 0;
        typeConverter_  = new AS400ByteArray(maxLength);
        value_          = new byte[maxLength];
    }

    public Object clone()
    {
        return new SQLCharForBitData(maxLength_, settings_);
    }

    //---------------------------------------------------------//
    //                                                         //
    // CONVERSION TO AND FROM RAW BYTES                        //
    //                                                         //
    //---------------------------------------------------------//

    public void convertFromRawBytes(byte[] rawBytes, int offset, ConvTable ccsidConverter) //@P0C
    throws SQLException
    {
        value_ = (byte[])typeConverter_.toObject(rawBytes, offset);
    }

    public void convertToRawBytes(byte[] rawBytes, int offset, ConvTable ccsidConverter) //@P0C
    throws SQLException
    {
        typeConverter_.toBytes(value_, rawBytes, offset);
    }

    //---------------------------------------------------------//
    //                                                         //
    // SET METHODS                                             //
    //                                                         //
    //---------------------------------------------------------//

    public void set(Object object, Calendar calendar, int scale)
    throws SQLException
    {
        if(object instanceof String)
        {
            try
            {
                value_ = BinaryConverter.stringToBytes((String)object);                       // @F1A
            }
            catch(NumberFormatException nfe)
            {
                // the String contains non-hex characters
                JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH, nfe);
            }
        }

        else if(object instanceof byte[])
            value_ = (byte[])object;                                                // @C1C

        else if(object instanceof InputStream)
        {
            //value_ = JDUtilities.streamToBytes((InputStream)object, scale);

            int length = scale; // hack to get the length into the set method
            if(length >= 0)
            {
                InputStream stream = (InputStream)object;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int blockSize = length < AS400JDBCPreparedStatement.LOB_BLOCK_SIZE ? length : AS400JDBCPreparedStatement.LOB_BLOCK_SIZE;
                byte[] byteBuffer = new byte[blockSize];
                try
                {
                    int totalBytesRead = 0;
                    int bytesRead = stream.read(byteBuffer, 0, blockSize);
                    while(bytesRead > -1 && totalBytesRead < length)
                    {
                        baos.write(byteBuffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        int bytesRemaining = length - totalBytesRead;
                        if(bytesRemaining < blockSize)
                        {
                            blockSize = bytesRemaining;
                        }
                        bytesRead = stream.read(byteBuffer, 0, blockSize);
                    }
                }
                catch(IOException ie)
                {
                    JDError.throwSQLException(this, JDError.EXC_INTERNAL, ie);
                }
                value_ = baos.toByteArray();
                if(value_.length < length)
                {
                    // a length longer than the stream was specified
                    JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
                }
            }
            else
            {
                JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
            }
        }

        else if(object instanceof Reader)
        {
            // value_ = BinaryConverter.stringToBytes(JDUtilities.readerToString((Reader)object, scale));

            int length = scale; // hack to get the length into the set method
            if(length >= 0)
            {
                try
                {
                    int blockSize = length < AS400JDBCPreparedStatement.LOB_BLOCK_SIZE ? length : AS400JDBCPreparedStatement.LOB_BLOCK_SIZE;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    HexReaderInputStream stream = new HexReaderInputStream((Reader)object);
                    byte[] byteBuffer = new byte[blockSize];
                    int totalBytesRead = 0;
                    int bytesRead = stream.read(byteBuffer, 0, blockSize);
                    while(bytesRead > -1 && totalBytesRead < length)
                    {
                        baos.write(byteBuffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        int bytesRemaining = length - totalBytesRead;
                        if(bytesRemaining < blockSize)
                        {
                            blockSize = bytesRemaining;
                        }
                        bytesRead = stream.read(byteBuffer, 0, blockSize);
                    }
                    value_ = baos.toByteArray();
                    stream.close(); //@scan1
                    if(value_.length < length)
                    {
                        // a length longer than the stream was specified
                        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
                    }
                }
                catch(ExtendedIOException eie)
                {
                    // the Reader contains non-hex characters
                    JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH, eie);
                }
                catch(IOException ie)
                {
                    JDError.throwSQLException(this, JDError.EXC_INTERNAL, ie);
                }
            }
            else
            {
                JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
            }
        }

        else if(JDUtilities.JDBCLevel_ >= 20 && object instanceof Blob)
            value_ = ((Blob)object).getBytes(1, (int)((Blob)object).length());      // @C1C @E2C Blobs are 1 based.

        else if(JDUtilities.JDBCLevel_ >= 20 && object instanceof Clob)
        {
            try
            {
                value_ = BinaryConverter.stringToBytes(((Clob)object).getSubString(1, (int)((Clob)object).length())); //@F1A
            }
            catch(NumberFormatException nfe)
            {
                // the Clob contains non-hex characters
                JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH, nfe);
            }
        }

        else
            JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);


        // Set to the exact length.
        int valueLength = value_.length;
        if(valueLength < maxLength_)
        {
            byte[] newValue = new byte[maxLength_];
            System.arraycopy(value_, 0, newValue, 0, valueLength);
            value_ = newValue;
            truncated_ = 0;
        }
        else if(valueLength > maxLength_)
        {
            byte[] newValue = new byte[maxLength_];
            System.arraycopy(value_, 0, newValue, 0, maxLength_);
            value_ = newValue;
            truncated_ = valueLength - maxLength_;
        }
        else
            truncated_ = 0;
    }

    //---------------------------------------------------------//
    //                                                         //
    // DESCRIPTION OF SQL TYPE                                 //
    //                                                         //
    //---------------------------------------------------------//

    public int getSQLType()
    {
        return SQLData.CHAR_FOR_BIT_DATA;
    }

    public String getCreateParameters()
    {
        return AS400JDBCDriver.getResource("MAXLENGTH");
    }

    public int getDisplaySize()
    {
        return maxLength_;
    }

    public String getLiteralPrefix()
    {
        return "X\'";
    }

    public String getLiteralSuffix()
    {
        return "\'";
    }

    public String getLocalName()
    {
        // Use "CHAR" not "BINARY".  See ODBC SQLGetTypeInfo().
        return "CHAR() FOR BIT DATA";   //@KKB changed from CHAR
    }

    public int getMaximumPrecision()
    {
        return 32765;
    }

    public int getMaximumScale()
    {
        return 0;
    }

    public int getMinimumScale()
    {
        return 0;
    }

    public int getNativeType()
    {
        return 452;
    }

    public int getPrecision()
    {
        return maxLength_;
    }

    public int getRadix()
    {
        return 0;
    }

    public int getScale()
    {
        return 0;
    }

    public int getType()
    {
        return java.sql.Types.BINARY;
    }

    public String getTypeName()
    {
        return "CHAR() FOR BIT DATA";
    }

    public boolean isSigned()
    {
        return false;
    }

    public boolean isText()
    {
        return true;
    }

    public int getActualSize()
    {
        return value_.length;
    }

    //@F2A JDBC 3.0
    public String getJavaClassName()
    {
        return "[B";
    }

    public int getTruncated()
    {
        return truncated_;
    }

    //---------------------------------------------------------//
    //                                                         //
    // CONVERSIONS TO JAVA TYPES                               //
    //                                                         //
    //---------------------------------------------------------//

    public InputStream getAsciiStream()
    throws SQLException
    {
        truncated_ = 0;
        
        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.

        // changed to return stream containing hex string
        // return new ByteArrayInputStream(toBytes());

        try
        {
            return new ByteArrayInputStream(ConvTable.getTable(819, null).stringToByteArray(BinaryConverter.bytesToString(getBytes())));
        }
        catch(UnsupportedEncodingException e)
        {
            JDError.throwSQLException(this, JDError.EXC_INTERNAL, e);
            return null;
        }
    }

    public BigDecimal getBigDecimal(int scale)
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public InputStream getBinaryStream()
    throws SQLException
    {
        truncated_ = 0;

        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.

        return new ByteArrayInputStream(getBytes());
    }

    public Blob getBlob()
    throws SQLException
    {
        truncated_ = 0;
        
        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.

        return new AS400JDBCBlob(getBytes(), maxLength_);
    }

    public boolean getBoolean()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return false;
    }

    public byte getByte()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public byte[] getBytes()
    {
        // Truncate to the max field size if needed.
        // Do not signal a DataTruncation per the spec. @B1A
        int maxFieldSize = settings_.getMaxFieldSize();
        if((value_.length > maxFieldSize) && (maxFieldSize > 0))
        {
            // @B1D truncated_ = value_.length - maxFieldSize;
            byte[] truncatedValue = new byte[maxFieldSize];
            System.arraycopy(value_, 0, truncatedValue, 0, maxFieldSize);
            return truncatedValue;
        }
        else
        {
            // @B1D truncated_ = 0;
            return value_;
        }
    }

    public Reader getCharacterStream()
    throws SQLException
    {
        truncated_ = 0;

        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.
        //@F1D return new StringReader (new String(toBytes()));
        return new StringReader(BinaryConverter.bytesToString(getBytes())); //@F1A
    }

    public Clob getClob()
    throws SQLException
    {
        truncated_ = 0;

        // This is written in terms of getString(), since it will
        // handle truncating to the max field size if needed.
        //@F1D return new AS400JDBCClob(new String(toBytes()));
        return new AS400JDBCClob(BinaryConverter.bytesToString(getBytes()), maxLength_); //@F1A
    }

    public Date getDate(Calendar calendar)
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public double getDouble()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public float getFloat()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public int getInt()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public long getLong()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public Object getObject()
    throws SQLException
    {
        truncated_ = 0;

        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.
        return getBytes();
    }

    public short getShort()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public String getString()
    throws SQLException
    {
        truncated_ = 0;

        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.
        //@F1D return new String (toBytes());
        return BinaryConverter.bytesToString(getBytes()); //@F1A
    }

    public Time getTime(Calendar calendar)
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public Timestamp getTimestamp(Calendar calendar)
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public InputStream getUnicodeStream()
    throws SQLException
    {
        truncated_ = 0;

        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.

        // changed to return stream containing hex string
        // return new ByteArrayInputStream(toBytes());
        try
        {
            return new ByteArrayInputStream(ConvTable.getTable(13488, null).stringToByteArray(BinaryConverter.bytesToString(getBytes())));
        }
        catch(UnsupportedEncodingException e)
        {
            JDError.throwSQLException(this, JDError.EXC_INTERNAL, e);
            return null;
        }
    }
    
    //@PDA jdbc40
    public Reader getNCharacterStream() throws SQLException
    {
        truncated_ = 0;

        // This is written in terms of getBytes(), since it will
        // handle truncating to the max field size if needed.
        return new StringReader(BinaryConverter.bytesToString(getBytes()));
    }

    //@PDA jdbc40
    public NClob getNClob() throws SQLException
    {        
        truncated_ = 0;

        // This is written in terms of getBytes(), since it will
        // handle truncating to the max field size if needed.
        return new AS400JDBCNClob(BinaryConverter.bytesToString(getBytes()), maxLength_);
    }

    //@PDA jdbc40
    public String getNString() throws SQLException
    {
        truncated_ = 0;

        // This is written in terms of getBytes(), since it will
        // handle truncating to the max field size if needed.
        return BinaryConverter.bytesToString(getBytes()); 
    }

    //@PDA jdbc40
    public RowId getRowId() throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    //@PDA jdbc40
    public SQLXML getSQLXML() throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    // @array
    public Array getArray() throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }
}

