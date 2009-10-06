///////////////////////////////////////////////////////////////////////////////
//                                                                             
// AS/400 Toolbox for Java - OSS version                                       
//                                                                             
// Filename: AS400Text.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
   The <A HREF="../dtad.htm">AS400Text</A> class provides
   character set conversion between Java String objects and
   AS/400 native code pages.
**/
public class AS400Text implements AS400DataType
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";

    private int length_;
    private int ccsid_ = 65535;
    transient private String encoding_ = null; //@D2A
    private AS400 system_;
    transient Converter table_; 
    transient ConverterImpl tableImpl_;
    private static final String defaultValue = "";


    /**
      Constructs an AS400Text object.
      It uses the most likely CCSID based on the default locale.
      @param  length  The byte length of the AS/400 text.  It must be greater than or equal to zero.
      
      @deprecated Replaced by AS400Text(int, AS400).
      Any AS400Text object that is created without
      specifying an AS400 system object on its constructor may
      not behave as expected in certain environments.
    **/
    public AS400Text(int length)
    {
      if (length < 0)
      {
        throw new ExtendedIllegalArgumentException("length (" + String.valueOf(length) + ")", ExtendedIllegalArgumentException.LENGTH_NOT_VALID);
      }
      length_ = length;
//@D2D      setTable();
    }


    /**
      Constructs an AS400Text object.
      @param  length  The byte length of the AS/400 text.  It must be greater than or equal to zero.
      @param  ccsid  The CCSID of the AS/400 text.  It must refer to a valid and available CCSID.  The value 65535 will cause the data type to use the most likely CCSID based on the default locale.
      
      @deprecated Replaced by AS400Text(int, int, AS400).
      Any AS400Text object that is created without
      specifying an AS400 system object on its constructor may
      not behave as expected in certain environments.
      
    **/
    public AS400Text(int length, int ccsid)
    {
      if (length < 0)
      {
        throw new ExtendedIllegalArgumentException("length (" + String.valueOf(length) + ")", ExtendedIllegalArgumentException.LENGTH_NOT_VALID);
      }
      if (ccsid < 0) //@D2A
      { //@D2A
        throw new ExtendedIllegalArgumentException("ccsid (" + String.valueOf(ccsid) + ")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID); //@D2A
      } //@D2A
      length_ = length;
      ccsid_ = ccsid;      
//@D2D      setTable();
    }

    
    /**
      Constructs AS400Text object.
      @param  length  The byte length of the AS/400 text.  It must be greater than or equal to zero.
      @param  encoding  The name of a character encoding.  It must be a valid and available encoding.
      
      @deprecated Replaced by AS400Text(int, int, AS400).
      Any AS400Text object that is created without
      specifying an AS400 system object on its constructor may
      not behave as expected in certain environments.
      
    **/
    public AS400Text(int length, String encoding)
    {
      if (length < 0)
      {
        throw new ExtendedIllegalArgumentException("length (" + String.valueOf(length) + ")", ExtendedIllegalArgumentException.LENGTH_NOT_VALID);
      }
      if (encoding == null)
      {
        throw new NullPointerException("encoding");
      }
      length_ = length;
      encoding_ = encoding; //@D2A

//@D2M      try
//@D2M      {
//@D2M        table_ = new Converter(encoding); //@B5C
//@D2M        ccsid_ = table_.getCcsid();
//@D2M        tableImpl_ = table_.impl; //@D0A
//@D2M      }
//@D2M      catch (UnsupportedEncodingException e)
//@D2M      {
//@D2M        throw new ExtendedIllegalArgumentException("encoding (" + encoding + ")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
//@D2M      }

    }


    /**
      Constructs an AS400Text object. The ccsid used for conversion will be the ccsid of the <i>system</i> object.
      @param  length  The byte length of the AS/400 text.
                      It must be greater than or equal to zero.
      @param  system  The AS/400 system from which the conversion table may be downloaded.
     */
    public AS400Text(int length, AS400 system)
    {
      this(length, 65535, system); //@D2C - passing a 65535 will cause setTable() to do a system.getCcsid() at conversion time.
    }


    /**
      Constructs an AS400Text object.
      @param  length  The byte length of the AS/400 text.
                      It must be greater than or equal to zero.
      @param  ccsid  The CCSID of the AS/400 text.
                     It must refer to a valid and available CCSID.
                     The value 65535 will cause the data type to use
                     the most likely CCSID based on the default locale.
      @param  system  The AS/400 system from which the conversion table may be downloaded.
     */
    public AS400Text(int length, int ccsid, AS400 system)
    {
      if (length < 0)
      {
        throw new ExtendedIllegalArgumentException("length (" + String.valueOf(length) + ")", ExtendedIllegalArgumentException.LENGTH_NOT_VALID);
      }
      if (ccsid < 0) //@D2A
      { //@D2A
        throw new ExtendedIllegalArgumentException("ccsid (" + String.valueOf(ccsid) + ")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID); //@D2A
      } //@D2A
      if (system == null)
      {
        throw new NullPointerException("system");
      }
      length_ = length;
      ccsid_ = ccsid;
      system_ = system;
//@D2D      setTable();
    }
    
    
    //@B5A - package scope constructor for use on the proxy server
    // Note that this constructor is only used in AS400FileRecordDescriptionImplRemote.
    // It is expected that the client code (AS400FileRecordDescription)
    // will call fillInConverter() on each AS400Text object returned.
    /**
      Constructs an AS400Text object.
      @param  length  The byte length of the AS/400 text.
                      It must be greater than or equal to zero.
      @param  ccsid  The CCSID of the AS/400 text.
                     It must refer to a valid and available CCSID.
                     The value 65535 will cause the data type to use
                     the most likely CCSID based on the default locale.
      @param  system  The AS/400 system from which the conversion table may be downloaded.
     */
    AS400Text(int length, int ccsid, AS400Impl system)
    {
      if (length < 0)
      {
        throw new ExtendedIllegalArgumentException("length (" + String.valueOf(length) + ")", ExtendedIllegalArgumentException.LENGTH_NOT_VALID);
      }
      if (ccsid < 0) //@D2A
      { //@D2A
        throw new ExtendedIllegalArgumentException("ccsid (" + String.valueOf(ccsid) + ")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID); //@D2A
      } //@D2A
      if (system == null)
      {
        throw new NullPointerException("system");
      }
      length_ = length;
      ccsid_ = ccsid;
      // Notice that we have not filled in the Converter object. We can't do that
      // because we don't know if this object will in the end be used on the
      // public side (Converter) or on the server side (ConverterImpl).
      //@D2 - We also can't do that yet since the Converter ctor will connect to the system.
    }

    
    /**
      Creates a new AS400Text object that is identical to the current instance.
      @return  The new object.
     **/
    public Object clone()
    {
      try
      {
        return super.clone();  // Object.clone does not throw exception
      }
      catch (CloneNotSupportedException e)
      {
        Trace.log(Trace.ERROR, "Unexpected cloning error", e);
        throw new InternalErrorException(InternalErrorException.UNKNOWN);
      }
    }

    
    /**
      Returns the byte length of the data type.
      @return  The number of bytes in the AS/400 representation of the data type.
     **/
    public int getByteLength()
    {
      return length_;
    }

    
    /**
      Returns the CCSID of the data type.
      @return  The CCSID.
     **/
    public int getCcsid()
    {
      if (ccsid_ == 65535) setTable(); //@D2A
      return ccsid_; //@D2A
    }

    
    //@B5A
    /**
      Returns the ConverterImpl object so other classes don't
      need to create a new Converter if they already have an
      AS400Text object.
    **/
    Converter getConverter()
    {
      if (table_ == null) setTable(); //@D2A
      return table_;
    }
    
    
    /**
      Returns a Java object representing the default value of the data type.
      @return  The String object representing an empty string ("").
     **/
    public Object getDefaultValue()
    {
      return new String(defaultValue);
    }

    
    /**
      Returns the encoding of the data type.
      @return  The encoding of the data type.
     **/
    public String getEncoding()
    {
      if (encoding_ == null) setTable(); //@D2A
//@D2D      setTable(); // Make sure the table is set
//@D2D      if (tableImpl_ != null) return tableImpl_.getEncoding();
//@D2D      return table_.getEncoding();
      return encoding_; //@D2A
    }

    
    //@B5A
    /**
      This method is used in conjunction with the constructor
      that takes an AS400Impl. It is used to fully instantiate
      the member data of this AS400Text object once it has been
      serialized and received on the client from the proxy server.
      We do it this way because we can't create a normal AS400Text
      object on the proxy server and expect it to be valid on
      the proxy client because its internal Converter object
      would not be proxified correctly.
    **/
    // When an AS400Text object is serialized from the proxy server over to
    // the client, the client code must set the converter using this method.
    void setConverter(AS400 system)
    {
      system_ = system;
      setTable();
    }
      
      
    //@D0A
    // When an AS400Text object is serialized from the client over to
    // the proxy server, the server code must set the converter using
    // this method. Note that we cannot refer directly to the ConverterImplRemote
    // class here, so it is left up to the server code to create that and pass it
    // in to this method.
    void setConverter(ConverterImpl converter)
    {
      tableImpl_ = converter;
      ccsid_ = tableImpl_.getCcsid(); // Just in case this object ever goes back to the client
    }
    
    
    //@D0A
    // private method to initialize the Converter table and its impl
    private void setTable()
    {
      if (table_ == null && tableImpl_ == null)
      {
        if (Trace.isTraceOn() && Trace.isTraceConversionOn()) //@D2A
        {
          Trace.log(Trace.CONVERSION, "AS400Text object initializing with "+encoding_+", "+ccsid_+", "+system_); //@D2A
        }
        if (encoding_ != null) //@D2A
        { //@D2A
          try                                         //@D2M
          {                                           //@D2M
            table_ = new Converter(encoding_); //@B5C //@D2M
            ccsid_ = table_.getCcsid();               //@D2M
            tableImpl_ = table_.impl; //@D0A          //@D2M
          }                                           //@D2M
          catch (UnsupportedEncodingException e)      //@D2M
          {                                           //@D2M
            throw new ExtendedIllegalArgumentException("encoding (" + encoding_ + ")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID); //@D2M
          }                                           //@D2M
        } //@D2A
        else //@D2A
        { //@D2A
          try
          {
            if (system_ == null)
            {
              if (ccsid_ == 65535)
              {
                table_ = new Converter();
                ccsid_ = table_.getCcsid();
              }
              else
              {
                table_ = new Converter(ccsid_);
              }  
              tableImpl_ = table_.impl;
            }
            else
            {
              if (ccsid_ == 65535)
              {
                ccsid_ = system_.getCcsid();
              }        
              table_ = new Converter(ccsid_, system_);
              tableImpl_ = table_.impl;
            }
            encoding_ = (tableImpl_ == null ? table_.getEncoding() : tableImpl_.getEncoding()); //@D2A
          }
          catch(UnsupportedEncodingException e)
          {
            throw new ExtendedIllegalArgumentException("ccsid (" + String.valueOf(ccsid_) + ")", ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
          }
        } //@D2A
        if (Trace.isTraceOn() && Trace.isTraceConversionOn()) //@D2A
        {
          Trace.log(Trace.CONVERSION, "AS400Text object initialized to "+encoding_+", "+ccsid_+", "+system_); //@D2A
        }
      }
    }


    /**
      Converts the specified Java object to AS/400 format.
      @param  javaValue  The object corresponding to the data type.  It must be an instance of String, and the converted text length must be less than or equal to the byte length of this data type.  If the provided string is not long enough to fill the return array, the remaining bytes will be padded with space bytes (EBCDIC 0x40, ASCII 0x20, or Unicode 0x0020).
      @return  The AS/400 representation of the data type.
     **/
    public byte[] toBytes(Object javaValue)
    {
      byte[] as400Value = new byte[length_];
      toBytes(javaValue, as400Value, 0);
      return as400Value;
    }

    
    /**
      Converts the specified Java object into AS/400 format in the specified byte array.
      @param  javaValue  The object corresponding to the data type. It must be an instance of String, and the converted text length must be less than or equal to the byte length of this data type.  If the provided string is not long enough to fill the return array, the remaining bytes will be padded with space bytes (EBCDIC 0x40, ASCII 0x20, or Unicode 0x0020).
      @param  as400Value  The array to receive the data type in AS/400 format.  There must be enough space to hold the AS/400 value.
      @return  The number of bytes in the AS/400 representation of the data type.
     **/
    public int toBytes(Object javaValue, byte[] as400Value)
    {
      return toBytes(javaValue, as400Value, 0);
    }

    
    /**
      Converts the specified Java object into AS/400 format in the specified byte array.
      @param  javaValue  The object corresponding to the data type.  It must be an instance of String, and the converted text length must be less than or equal to the byte length of this data type.  If the provided string is not long enough to fill the return array, the remaining bytes will be padded with space bytes (EBCDIC 0x40, ASCII 0x20, or Unicode 0x0020).
      @param  as400Value  The array to receive the data type in AS/400 format.  There must be enough space to hold the AS/400 value.
      @param  offset  The offset into the byte array for the start of the AS/400 value.  It must be greater than or equal to zero.
      @return  The number of bytes in the AS/400 representation of the data type.
     **/
    public int toBytes(Object javaValue, byte[] as400Value, int offset)
    {
      // Check here to avoid sending bad data to Converter and ConvTable
      if (javaValue == null)
      {
        throw new NullPointerException("javaValue");
      }

      byte[] eValue = null;
      setTable(); // Make sure the table is set
      if (tableImpl_ != null)
      {
        eValue = tableImpl_.stringToByteArray((String)javaValue);
      }
      else
      {
        eValue = table_.stringToByteArray((String)javaValue);  // allow this line to throw ClassCastException
      }
      
      // Check that converted data fits within data type
      if (eValue.length > length_)
      {
        throw new ExtendedIllegalArgumentException("javaValue (" + javaValue.toString() + ")", ExtendedIllegalArgumentException.LENGTH_NOT_VALID);
      }
      System.arraycopy(eValue, 0, as400Value, offset, eValue.length);  // Let this line throw ArrayIndexException

      // pad with spaces
      int i = eValue.length;
      if (i < length_)
      {
        switch (ccsid_)
        {
                // Unicode space 0x0020
                case 13488:
                case 61952:
                {
                    for (; i < length_-1; i+=2)
                    {
                        as400Value[offset+i] = 0x00;
                        as400Value[offset+i+1] = 0x20;
                    }
                    break;
                }

                // Ascii space 0x20
                case 437:
                case 737:
                case 775:
                case 813:
                case 819:
                case 850:
                case 852:
                case 855:
                case 856:
                case 857:
                case 860:
                case 861:
                case 862:
                case 863:
                case 864:
                case 865:
                case 866:
                case 868:
                case 869:
                case 874:
                case 912:
                case 913:
                case 914:
                case 915:
                case 916:
                case 920:
                case 921:
                case 922:
                case 942:
                case 943:
                case 948:
                case 949:
                case 950:
                case 954:
                case 964:
                case 970:
                case 1006:
                case 1046:
                case 1089:
                case 1098:
                case 1124:
                case 1250:
                case 1251:
                case 1252:
                case 1253:
                case 1254:
                case 1255:
                case 1256:
                case 1257:
                case 1258:
                case 1275:
                case 1280:
                case 1281:
                case 1282:
                case 1283:
                case 1350:
                case 1381:
                case 1383:
                case 33722:
                {
                    for (; i < length_; ++i)
                    {
                        as400Value[offset+i] = 0x20;
                    }
                    break;
                }

                // Ebcdic space 0x40
                // case 37:
                // case 273:
                // case 277:
                // case 278:
                // case 280:
                // case 284:
                // case 285:
                // case 297:
                // case 290:
                // case 300:
                // case 420:
                // case 423:
                // case 424:
                // case 500:
                // case 833:
                // case 834:
                // case 835:
                // case 836:
                // case 837:
                // case 838:
                // case 870:
                // case 871:
                // case 875:
                // case 880:
                // case 918:
                // case 930:
                // case 933:
                // case 935:
                // case 937:
                // case 939:
                // case 1025:
                // case 1026:
                // case 1027:
                // case 1097:
                // case 1112:
                // case 1122:
                // case 1123:
                // case 1130:
                // case 1132:
                // case 1388:
                // case 4933:
                // case 5026:
                // case 5035:
                // case 28709:
                default:
                {
                    for (; i < length_; ++i)
                    {
                        as400Value[offset+i] = 0x40;
                    }
                    break;
                }
        }
      }
      return length_;
    }

    
    /**
      Converts the specified AS/400 data type to a Java object.
      @param  as400Value  The array containing the data type in AS/400 format.  The entire data type must be represented.
      @return  The String object corresponding to the data type.
     **/
    public Object toObject(byte[] as400Value)
    {
      // Check here to avoid sending bad data to Converter and ConvTable
      if (as400Value == null)
      {
        throw new NullPointerException("as400Value");
      }
      setTable(); // Make sure the table is set
      if (tableImpl_ != null)
      {
        return tableImpl_.byteArrayToString(as400Value, 0, length_); //@D0A
      }
      return table_.byteArrayToString(as400Value, 0, length_);
    }

    
    /**
      Converts the specified AS/400 data type to a Java object.
      @param  as400Value  The array containing the data type in AS/400 format.  The entire data type must be represented.
      @param  offset  The offset into the byte array for the start of the AS/400 value. It must be greater than or equal to zero.
      @return  The String object corresponding to the data type.
     **/
    public Object toObject(byte[] as400Value, int offset)
    {
      // Check here to avoid sending bad data to Converter and ConvTable
      if (as400Value == null)
      {
        throw new NullPointerException("as400Value");
      }
      setTable(); // Make sure the table is set
      if (tableImpl_ != null)
      {
        return tableImpl_.byteArrayToString(as400Value, offset, length_); //@D0A
      }
      return table_.byteArrayToString(as400Value, offset, length_);
    }
}