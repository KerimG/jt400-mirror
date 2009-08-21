///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                                 
//                                                                             
// Filename: SQLSmallint.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2001 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;



class SQLSmallint
implements SQLData
{
  private static final String copyright = "Copyright (C) 1997-2001 International Business Machines Corporation and others.";




    // Private data.
    private int                 truncated_;
    // @D0D private static AS400Bin2    typeConverter_;
    private short               value_;
    private int                 scale_;                             // @A0A
    private BigDecimal          bigDecimalValue_ = null;            // @A0A



    // @D0D static
    // @D0D {
    // @D0D     typeConverter_ = new AS400Bin2 ();
    // @D0D }



    SQLSmallint ()
    {
        this (0);
    }



    SQLSmallint (int scale)                     // @A0A
    {
        truncated_          = 0;
        value_              = 0;
        scale_              = scale;                                      // @A0A
        if (scale_ > 0)                                                   // @C0A
            bigDecimalValue_    = new BigDecimal (Short.toString (value_)); // @A0A
    }



    public Object clone ()
    {
        return new SQLSmallint (scale_);
    }



//---------------------------------------------------------//
//                                                         //
// CONVERSION TO AND FROM RAW BYTES                        //
//                                                         //
//---------------------------------------------------------//



    public void convertFromRawBytes (byte[] rawBytes, int offset, ConvTable ccsidConverter) //@P0C
        throws SQLException
    {
        value_ = BinaryConverter.byteArrayToShort(rawBytes, offset);                             // @D0C

        if (scale_ > 0) {                                                                        // @C0A
            bigDecimalValue_ = (new BigDecimal(Short.toString(value_))).movePointLeft(scale_);   // @A0A
            value_ = (short) bigDecimalValue_.intValue();                                        // @A0A
        }                                                                                        // @C0A
    }



    public void convertToRawBytes (byte[] rawBytes, int offset, ConvTable ccsidConverter) //@P0C
        throws SQLException
    {
        BinaryConverter.shortToByteArray(value_, rawBytes, offset);                              // @D0C
    }



//---------------------------------------------------------//
//                                                         //
// SET METHODS                                             //
//                                                         //
//---------------------------------------------------------//



    public void set (Object object, Calendar calendar, int scale)
        throws SQLException
    {
        truncated_ = 0;                                                     // @D9c

        if (object instanceof String)
        {
            // @D10c new implementation
            // old ...
            //
            // try
            // {
            //     value_ = Short.parseShort ((String) object);
            // }
            // catch (NumberFormatException e)
            // {
            //     JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
            // }                                                                           

            // @P1 First try to convert the string to an int (no extra object creation).  If
            //     that fails try turning it into a double, which will involve an extra object
            //     create but Double will accept bigger numbers and floating point numbers so it 
            //     will catch more truncation cases.  The bottom line is don't create an extra
            //     object in the normal case.  If the user does ps.setString(1, "111222333.444.555")
            //     on an integer field, they can't expect the best performance. 
            //     create.  
            boolean tryAgain = false;                                                    // @P1a

            try
            {
               // @P1d long longValue = (long) Double.parseDouble ((String) object); 
               int  intValue = (int) Integer.parseInt ((String) object);                 // @P1a

               if (( intValue > Short.MAX_VALUE ) || ( intValue < Short.MIN_VALUE ))
               {
                   truncated_ = 6;
               }
               value_ = (short) intValue;                                                // @D9c
            }                                                                           
            catch (NumberFormatException e)
            {          
               tryAgain = true;                                                          // @P1a
               // @P1d JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
            }

            if (tryAgain)                                                                // @P1a
            {                                                                            // @P1a
               try                                                                       // @P1a
               {                                                                         // @P1a
                  double doubleValue = Double.valueOf ((String) object).doubleValue ();  // @P1a
                                                                                         // @P1a
                  if (( doubleValue > Short.MAX_VALUE ) || ( doubleValue < Short.MIN_VALUE )) // @P1a
                  {                                                                      // @P1a
                      truncated_ = 6;                                                    // @P1a
                  }                                                                      // @P1a
                  value_ = (short) doubleValue;                                          // @P1a  
               }                                                                         // @P1a
               catch (NumberFormatException e)                                           // @P1a
               {                                                                         // @P1a
                  JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);            // @P1a
               }                                                                         // @P1a
            }                                                                            // @P1a
        }                                                                                // @P1a

        else if (object instanceof Number)
        {
            // Compute truncation by getting the value as a long
            // and comparing it against MAX_VALUE/MIN_VALUE.  You
            // do this because truncation of the decimal portion of
            // the value is insignificant.  We only care if the
            // whole number portion of the value is too large/small
            // for the column.
            long longValue = ((Number) object).longValue ();                        // @D9c
            if (( longValue > Short.MAX_VALUE ) || ( longValue < Short.MIN_VALUE )) // @D9a
            {
                // Note:  Truncated here is set to 6 bytes.  This is based on
                //        the idea that a long was used and a short was the
                //        column type.  We could check for different types
                //        and provide a more accurate number, but I don't
                //        really know that this field is of any use to people
                //        in this case anyway (for example, you could have a
                //        float (4 bytes) that didn't fit into a bigint (8
                //        bytes) without some data truncation.
                truncated_ = 6;                                                     // @D9c
            }


            // Store the value.
            value_ = (short) longValue;                                             // @D9c
        }

        else if (object instanceof Boolean)
            value_ = (((Boolean) object).booleanValue() == true) ? (short) 1 : (short) 0;

        else
            JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);

        if (scale_ > 0) {                                                                        // @C0A
            bigDecimalValue_ = (new BigDecimal(Short.toString(value_))).movePointLeft(scale_);   // @A0A
            value_ = (short) bigDecimalValue_.intValue();                                        // @A0A
        }                                                                                        // @C0A
    }



//---------------------------------------------------------//
//                                                         //
// DESCRIPTION OF SQL TYPE                                 //
//                                                         //
//---------------------------------------------------------//



    public String getCreateParameters ()
    {
        return null;
    }


    public int getDisplaySize ()
    {
        return 6;
    }

    //@F1A JDBC 3.0
    public String getJavaClassName()
    {
        return "java.lang.Short";
    }

    public String getLiteralPrefix ()
    {
        return null;
    }


    public String getLiteralSuffix ()
    {
        return null;
    }


    public String getLocalName ()
    {
        return "SMALLINT";
    }


    public int getMaximumPrecision ()
    {
        return 5;
    }


    public int getMaximumScale ()
    {
        return 0;
    }


    public int getMinimumScale ()
    {
        return 0;
    }


    public int getNativeType ()
    {
        return 500;
    }


    public int getPrecision ()
    {
        return 5;
    }


    public int getRadix ()
    {
        return 10;
    }


    public int getScale ()
    {
        return scale_;
    }


     public int getType ()
     {
          return java.sql.Types.SMALLINT;
     }



     public String getTypeName ()
     {
          return "SMALLINT";
     }



// @E1D    public boolean isGraphic ()
// @E1D    {
// @E1D        return false;
// @E1D    }



    public boolean isSigned ()
    {
        return true;
    }



    public boolean isText ()
    {
        return false;
    }



//---------------------------------------------------------//
//                                                         //
// CONVERSIONS TO JAVA TYPES                               //
//                                                         //
//---------------------------------------------------------//



    public int getActualSize ()
    {
        return 2; // @D0C
    }



    public int getTruncated ()
    {
        return truncated_;
    }



     public InputStream toAsciiStream ()
         throws SQLException
     {
          JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



     public BigDecimal toBigDecimal (int scale)
         throws SQLException
     {
        if (scale_ > 0) {                                                   // @C0A
             if (scale >= 0)
                return bigDecimalValue_.setScale(scale);                    // @A0A
            else
                return bigDecimalValue_;
        }                                                                   // @C0A
        else {                                                              // @C0A
            if (scale <= 0)                                                 // @C0A
                return BigDecimal.valueOf ((long) value_);                  // @C0A
            else                                                            // @C0A
                return BigDecimal.valueOf ((long) value_).setScale (scale); // @C0A
        }                                                                   // @C0A
     }



     public InputStream toBinaryStream ()
         throws SQLException
     {
          JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



     public Blob toBlob ()
         throws SQLException
     {
          JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



     public boolean toBoolean ()
         throws SQLException
     {
         return (value_ != 0);
     }



     public byte toByte ()
         throws SQLException
     {
         return (byte) value_;
     }



     public byte[] toBytes ()
         throws SQLException
     {
         JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



     public Reader toCharacterStream ()
         throws SQLException
     {
          JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



     public Clob toClob ()
         throws SQLException
     {
          JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



     public Date toDate (Calendar calendar)
         throws SQLException
     {
         JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



     public double toDouble ()
         throws SQLException
     {
        if (scale_ > 0)                                 // @C0A
            return bigDecimalValue_.doubleValue();      // @A0A
        else                                            // @C0A
            return (double) value_;                     // @A0D @C0A
     }



     public float toFloat ()
         throws SQLException
     {
        if (scale_ > 0)                                 // @C0A
            return bigDecimalValue_.floatValue();       // @A0A
        else                                            // @C0A
            return (float) value_;                      // @A0D @C0A
     }



     public int toInt ()
         throws SQLException
     {
         return (int) value_;
     }



     public long toLong ()
         throws SQLException
     {
         return value_;
     }



     public Object toObject ()
     {
         return new Integer (value_);              // @D2c -- used to be new Short (value_)
     }



     public short toShort ()
         throws SQLException
     {
         return value_;
     }



     public String toString ()
     {
        if (scale_ > 0)                                 // @C0A
            return bigDecimalValue_.toString();         // @A0A
        else                                            // @C0A
            return Short.toString (value_);             // @A0D @C0A
     }



     public Time toTime (Calendar calendar)
         throws SQLException
     {
         JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



     public Timestamp toTimestamp (Calendar calendar)
         throws SQLException
     {
         JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



     public InputStream  toUnicodeStream ()
         throws SQLException
     {
         JDError.throwSQLException (JDError.EXC_DATA_TYPE_MISMATCH);
          return null;
     }



}
