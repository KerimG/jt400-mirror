///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: ConvTable1046.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

class ConvTable1046 extends ConvTableBidiMap
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";

  private static final String toUnicode_ = 
    "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000B\f\r\u000E\u000F" +
    "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F" +
    "\u0020\u0021\"\u0023\u0024\u0025\u0026\'\u0028\u0029\u002A\u002B\u002C\u002D\u002E\u002F" +
    "\u0030\u0031\u0032\u0033\u0034\u0035\u0036\u0037\u0038\u0039\u003A\u003B\u003C\u003D\u003E\u003F" +
    "\u0040\u0041\u0042\u0043\u0044\u0045\u0046\u0047\u0048\u0049\u004A\u004B\u004C\u004D\u004E\u004F" +
    "\u0050\u0051\u0052\u0053\u0054\u0055\u0056\u0057\u0058\u0059\u005A\u005B\\\u005D\u005E\u005F" +
    "\u0060\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F" +
    "\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u007B\u007C\u007D\u007E\u007F" +
    "\uFE88\u00D7\u00F7\uFEB1\uFEB5\uFEB9\uFEBD\uFE71\u0088\u25A0\u2502\u2500\u2510\u250C\u2514\u2518" +
    "\uFE79\uFE7B\uFE7D\uFE7F\uFE77\uFE8A\uFEF0\uFEF3\uFEF2\uFECE\uFECF\uFED0\uFEF6\uFEF8\uFEFA\uFEFC" +
    "\u00A0\uFE82\uFE84\uFE88\u00A4\uFE8E\uFE8B\uFE91\uFE97\uFE9B\uFE9F\uFEA3\u060C\u00AD\uFEA7\uFEB3" +
    "\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669\uFEB7\u061B\uFEBB\uFEBF\uFECA\u061F" +
    "\uFECB\u0621\u0622\u0623\u0624\u0625\u0626\u0627\u0628\u0629\u062A\u062B\u062C\u062D\u062E\u062F" +
    "\u0630\u0631\u0632\u0633\u0634\u0635\u0636\u0637\u0638\u0639\u063A\uFECC\uFE82\uFE84\uFE8E\uFED3" +
    "\u0640\u0641\u0642\u0643\u0644\u0645\u0646\u0647\u0648\u0649\u064A\u064B\u064C\u064D\u064E\u064F" +
    "\u0650\u0651\u0652\uFED7\uFEDB\uFEDF\u200B\uFEF5\uFEF7\uFEF9\uFEFB\uFEE3\uFEE7\uFEEC\uFEE9\u001A";


  private static final String fromUnicode_ = 
    "\u0001\u0203\u0405\u0607\u0809\u0A0B\u0C0D\u0E0F\u1011\u1213\u1415\u1617\u1819\u1A1B\u1C1D\u1E1F" +
    "\u2021\u2223\u2425\u2627\u2829\u2A2B\u2C2D\u2E2F\u3031\u3233\u3435\u3637\u3839\u3A3B\u3C3D\u3E3F" +
    "\u4041\u4243\u4445\u4647\u4849\u4A4B\u4C4D\u4E4F\u5051\u5253\u5455\u5657\u5859\u5A5B\u5C5D\u5E5F" +
    "\u6061\u6263\u6465\u6667\u6869\u6A6B\u6C6D\u6E6F\u7071\u7273\u7475\u7677\u7879\u7A7B\u7C7D\u7E7F" +
    "\uFFFF\u0004\u1A1A\u881A\uFFFF\u000B\u1A1A\uA01A\u1A1A\uA41A\u1A1A\u1A1A\u1A1A\u1AAD\uFFFF\u0014" +
    "\u1A1A\u1A81\uFFFF\u000F\u1A1A\u1A82\uFFFF\u0004\u1A1A\uFFFF\u0280\u7F7F\uFFFF\u0006\u1A1A\uAC1A" +
    "\uFFFF\u0006\u1A1A\u1ABB\u1A1A\u1ABF\u1AC1\uC2C3\uC4C5\uC6C7\uC8C9\uCACB\uCCCD\uCECF\uD0D1\uD2D3" +
    "\uD4D5\uD6D7\uD8D9\uDA1A\u1A1A\u1A1A\uE0E1\uE2E3\uE4E5\uE6E7\uE8E9\uEAEB\uECED\uEEEF\uF0F1\uF21A" +
    "\uFFFF\u0006\u1A1A\uB0B1\uB2B3\uB4B5\uB6B7\uB8B9\u252C\u2E2A\uFFFF\u0049\u1A1A\uFFFF\u0C80\u7F7F" +
    "\uFFFF\u0005\u1A1A\u1AF6\uFFFF\u007A\u1A1A\uFFFF\u0200\u7F7F\u8B1A\u8A1A\uFFFF\u0004\u1A1A\u8D1A" +
    "\u1A1A\u8C1A\u1A1A\u8E1A\u1A1A\u8F1A\uFFFF\u0043\u1A1A\u891A\uFFFF\u002F\u1A1A\uFFFF\u6C00\u7F7F" +
    "\uFFFF\u0038\u1A1A\uEB87\uEC1A\uED1A\uEE94\uEF90\uF091\uF192\uF293\uC1C2\uDCC3\uDDC4\uC4C5\u80C6" +
    "\u95A6\uA6C7\uDEC8\uC8A7\uA7C9\uC9CA\uCAA8\uA8CB\uCBA9\uA9CC\uCCAA\uAACD\uCDAB\uABCE\uCEAE\uAECF" +
    "\uCFD0\uD0D1\uD1D2\uD2D3\uD3AF\uAFD4\uD4BA\uBAD5\uD5BC\uBCD6\uD6BD\uBDD7\uD7D7\uD7D8\uD8D8\uD8D9" +
    "\uBEC0\uDBDA\u999A\u9BE1\uE1DF\uDFE2\uE2F3\uF3E3\uE3F4\uF4E4\uE4F5\uF5E5\uE5FB\uFBE6\uE6FC\uFCFE" +
    "\uFEE7\uFDE8\uE8E9\u96EA\u9897\u97F7\u9CF8\u9DF9\u9EFA\u9F1A\u1A1A\u1A21\u2223\u2425\u2627\u2829" +
    "\u2A2B\u2C2D\u2E2F\u3031\u3233\u3435\u3637\u3839\u3A3B\u3C3D\u3E3F\u4041\u4243\u4445\u4647\u4849" +
    "\u4A4B\u4C4D\u4E4F\u5051\u5253\u5455\u5657\u5859\u5A5B\u5C5D\u5E5F\u6061\u6263\u6465\u6667\u6869" +
    "\u6A6B\u6C6D\u6E6F\u7071\u7273\u7475\u7677\u7879\u7A7B\u7C7D\u7E1A\uFFFF\u0044\u1A1A\u8A1A\u1A1A" +
    "\u1A89\uFFFF\t\u1A1A";


  ConvTable1046()
  {
    super(1046, toUnicode_.toCharArray(), fromUnicode_.toCharArray());
  }
}
