///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: FileListElement.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.util.html;

import java.io.File;
import java.util.Date;
import java.text.Collator;                                 // @A2A
import java.text.SimpleDateFormat;
import java.beans.PropertyVetoException;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

import javax.servlet.http.HttpServletRequest;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSJavaFile;
import com.ibm.as400.access.Trace;
import com.ibm.as400.access.ExtendedIllegalStateException;

import com.ibm.as400.util.servlet.ListMetaData;
import com.ibm.as400.util.servlet.ListRowData;
import com.ibm.as400.util.servlet.RowMetaDataType;
import com.ibm.as400.util.servlet.HTMLTableConverter;
import com.ibm.as400.util.servlet.RowDataException;

/**
*  The FileListElement class represents the contents of an Integrated File System directory.
*
*  <P>This example creates an FileListElement object:
*  
*  <P>
*  <PRE>
*  // Create a HTMLTree object.
*  HTMLTree tree = new HTMLTree(req);
*  <p>
*  // Create a URLParser object.
*  URLParser urlParser = new URLParser(httpServletRequest.getRequestURI());
*  <p>
*  // Create an AS400 object.
*  AS400 system = new AS400(mySystem, myUserId, myPassword);
*  <p>
*  // Create an IFS object.
*  IFSJavaFile root = new IFSJavaFile(system, "/QIBM");
*  <p>
*  // Create a DirFilter object and get the directories.
*  DirFilter filter = new DirFilter();
*  File[] dirList = root.listFiles(filter);
*  <p>
*  
*  for (int i=0; i < dirList.length; i++)
*  {  <p>
*     // Create a FileTreeElement.
*     FileTreeElement element = new FileTreeElement(dirList[i]);
*     <p>
*     // Set the Icon URL.
*     ServletHyperlink sl = new ServletHyperlink(urlParser.getURI());
*     sl.setHttpServletResponse(resp);
*     element.setIconUrl(sl);
*     <p>
*     // Set the text url so it calls another
*     // servlet to display the contents of the FileTreeElement.
*     ServletHyperlink tl = new ServletHyperlink("/servlet/myListServlet");        
*     tl.setTarget("listFrame");
*     <p>
*     // Set the TextUrl for the FileTreeElement.
*     element.setTextUrl(tl);
*     <p>
*     // Add the FileTreeElement to the tree.
*     tree.addElement(element);
*  }
*
*  <p>
*  // When the user clicks on text url in the HTMLTree it should call another
*  // servlet to display the contents.  It is here that the FileListElement
*  // will be created.
*  AS400 sys = new AS400(mySystem, myUserId, myPassword);
*  <p>
*  // The FileTreeElment will properly create the text url and pass the
*  // file and path information through the httpservletrequest.  If the
*  // list is meant to display the contents of the local file system,
*  // then only pass the HttpServletRequest on the constructor.
*  FileListElement fileList = new FileListElement(sys, httpservletrequest);
*  <p>
*  // Output the content of the FileListElement.
*  out.println(fileList.list());
*  </PRE>
*
*  Once the contents are listed and a user traverses the HTML tree down to /QIBM/ProdData/Http/Public/  and clicks on the
*  jt400 directory link, the FileListElement will look something like the following:
*  <P>
*
*  <table cellpadding="7">
*  <tr>
*  <th>Name</th>
*  <th>Size</th>
*  <th>Type</th>
*  <th>Modified</th>
*  </tr>
*  <tr>
*  <td><a href="/servlet/myListServlet/QIBM/ProdData/HTTP/Public">../ (Parent Directory)</a></td>
*  <td align="right"></td>
*  <td></td>
*  <td></td>
*  </tr>
*  <tr>
*  <td><a href="/servlet/myListServlet/QIBM/ProdData/HTTP/Public/jt400/com">com</a></td>
*  <td align="right"></td>
*  <td>Directory</td>
*  <td>06/09/2000 11:00:46 AM</td>
*  </tr>
*  <tr>
*  <td><a href="/servlet/myListServlet/QIBM/ProdData/HTTP/Public/jt400/lib">lib</a></td>
*  <td align="right"></td>
*  <td>Directory</td>
*  <td>09/11/2000 10:32:24 AM</td>
*  </tr>
*  <tr>
*  <td><a href="/servlet/myListServlet/QIBM/ProdData/HTTP/Public/jt400/MRI2924">MRI2924</a></td>
*  <td align="right"></td>
*  <td>Directory</td>
*  <td>06/09/2000 11:03:12 PM</td>
*  </tr>
*  <tr>
*  <td><a href="/servlet/myListServlet/QIBM/ProdData/HTTP/Public/jt400/SSL128">SSL128</a></td>
*  <td align="right"></td>
*  <td>Directory</td>
*  <td>09/22/2000 10:46:29 AM</td>
*  </tr>
*  <tr>
*  <td><a href="/servlet/myListServlet/QIBM/ProdData/HTTP/Public/jt400/SSL56">SSL56</a></td>
*  <td align="right"></td>
*  <td>Directory</td>
*  <td>09/22/2000 10:43:52 PM</td>
*  </tr>
*  <tr>
*  <td><a href="/servlet/myListServlet/QIBM/ProdData/HTTP/Public/jt400/utilities">utilities</a></td>
*  <td align="right"></td>
*  <td>Directory</td>
*  <td>06/09/2000 11:01:58 AM</td>
*  </tr>
*  <tr>
*  <td>ACCESS.LST</td>
*  <td align="right">15950</td>
*  <td>File</td>
*  <td>06/29/2000 06:26:25 PM</td>
*  </tr>
*  <tr>
*  <td>ACCESS.LVL</td>
*  <td align="right">23</td>
*  <td>File</td>
*  <td>06/29/2000 06:26:09 PM</td>
*  </tr>
*  <tr>
*  <td>CKSETUP.INI</td>
*  <td align="right">88</td>
*  <td>File</td>
*  <td>06/29/2000 06:26:21 PM</td>
*  </tr>
*  <tr>
*  <td>GTXSETUP.ini</td>
*  <td align="right">102</td>
*  <td>File</td>
*  <td>05/16/2000 05:51:46 PM</td>
*  </tr>
*  <tr>
*  <td>JT400.PKG</td>
*  <td align="right">19</td>
*  <td>File</td>
*  <td>09/08/1999 04:25:51 PM</td>
*  </tr>
*  <tr>
*  <td>OPNAV.LST</td>
*  <td align="right">16827</td>
*  <td>File</td>
*  <td>09/08/1999 04:26:08 PM</td>
*  </tr>
*  <tr>
*  <td>OPNAV.LVL</td>
*  <td align="right">19</td>
*  <td>File</td>
*  <td>05/16/2000 05:51:31 AM</td>
*  </tr>
*  <tr>
*  <td>OPV4R5M0.LST</td>
*  <td align="right">24121</td>
*  <td>File</td>
*  <td>09/08/1999 04:26:14 PM</td>
*  </tr>
*  <tr>
*  <td>OPV4R5M01.LST</td>
*  <td align="right">104</td>
*  <td>File</td>
*  <td>05/16/2000 05:51:46 AM</td>
*  </tr>
*  <tr>
*  <td>PROXY.LST</td>
*  <td align="right">4636</td>
*  <td>File</td>
*  <td>09/08/1999 04:26:00 AM</td>
*  </tr>
*  <tr>
*  <td>PROXY.LVL</td>
*  <td align="right">29</td>
*  <td>File</td>
*  <td>06/29/2000 06:26:26 AM</td>
*  </tr>
*  <tr>
*  <td>PXV4R5M0.LST</td>
*  <td align="right">7101</td>
*  <td>File</td>
*  <td>09/08/1999 04:25:58 PM</td>
*  </tr>
*  <tr>
*  <td>PXV4R5M01.LST</td>
*  <td align="right">38</td>
*  <td>File</td>
*  <td>06/29/2000 06:27:46 PM</td>
*  </tr>
*  <tr>
*  <td>QSF631215769JC10004R05M00505000000005</td>
*  <td align="right">4518</td>
*  <td>File</td>
*  <td>05/10/2000 10:35:10 AM</td>
*  </tr>
*  <tr>
*  <td>QSF631215769JC10004R05M00505000000006</td>
*  <td align="right">20</td>
*  <td>File</td>
*  <td>05/10/2000 10:35:00 AM</td>
*  </tr>
*  <tr>
*  <td>QSF631215769JC10004R05M00505000000007</td>
*  <td align="right">15</td>
*  <td>File</td>
*  <td>05/10/2000 10:35:19 AM</td>
*  </tr>
*  <tr>
*  <td>QSF631215769JC10004R05M00505000000008</td>
*  <td align="right">15950</td>
*  <td>File</td>
*  <td>05/10/2000 10:34:57 AM</td>
*  </tr>
*  <tr>
*  <td>QSF631215769JC10004R05M00505000000009</td>
*  <td align="right">38</td>
*  <td>File</td>
*  <td>05/10/2000 10:35:13 AM</td>
*  </tr>
*  <tr>
*  <td>QSF631215769JC10004R05M00505000000010</td>
*  <td align="right">38586</td>
*  <td>File</td>
*  <td>05/10/2000 10:35:06 AM</td>
*  </tr>
*  <tr>
*  <td>QSF631215769JC10004R05M00505000000011</td>
*  <td align="right">33</td>
*  <td>File</td>
*  <td>05/10/2000 10:35:09 AM</td>
*  </tr>
*  <tr>
*  <td>QSF631215769JC10004R05M00505000000013</td>
*  <td align="right">88</td>
*  <td>File</td>
*  <td>05/10/2000 10:35:03 AM</td>
*  </tr>
*  <tr>
*  <td>READMEGT.TXT</td>
*  <td align="right">3480</td>
*  <td>File</td>
*  <td>05/16/2000 05:52:27 AM</td>
*  </tr>
*  <tr>
*  <td>READMESP.TXT</td>
*  <td align="right">5161</td>
*  <td>File</td>
*  <td>06/29/2000 06:26:14 PM</td>
*  </tr>
*  <tr>
*  <td>V4R5M0.LST</td>
*  <td align="right">38586</td>
*  <td>File</td>
*  <td>06/29/2000 06:26:19 PM</td>
*  </tr>
*  <tr>
*  <td>V4R5M01.LST</td>
*  <td align="right">33</td>
*  <td>File</td>
*  <td>06/29/2000 06:26:33 PM</td>
*  </tr>
*  <tr>
*  <td>V4R5M02.LST</td>
*  <td align="right">33</td>
*  <td>File</td>
*  <td>06/29/2000 06:26:26 PM</td>
*  </tr>
*  </table>
*  <P>
*  FileListElement objects generate the following events:
*  <ul>
*    <li>PropertyChangeEvent
*  </ul>
*
*  @see com.ibm.as400.util.html.DirFilter
**/
public class FileListElement implements java.io.Serializable
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";

    private AS400     system_;
    private HTMLTable table_;
    private HttpServletRequest request_;
    private SimpleDateFormat   formatter_ = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");  // @B2C
    private FileListRenderer   renderer_;                   // @A4A
    private StringBuffer       sharePath_;                           // @B1A
    private StringBuffer       shareName_;                           // @B1A

    private boolean   sort_   = true;                       // @A2A                   
    private Collator  collator_;                            // @A2A

    // Handles loading the appropriate resource bundle
    private static ResourceBundleLoader_h loader_;         // @A5A

    // The FileListElement default column headers.
    private static String name = loader_.getText("PROP_FLE_NAME_NAME");                    // @A5A
    private static String size = loader_.getText("PROP_FLE_NAME_SIZE");                    // @A5A
    private static String type = loader_.getText("PROP_FLE_NAME_TYPE");                    // @A5A
    private static String modified = loader_.getText("PROP_FLE_NAME_MODIFIED");            // @A5A



    transient private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);


    /**
     *  Constructs a default FileListElement object.
     **/
    public FileListElement()
    {
    }


    // @A7A
    /**
     *  Constructs an FileListElement for the local file system            
     *  using the pathInfo from the specified <i>request</i>.  
     *
     *  Internally a java.io.File object will be used to retrieve 
     *  the contents of the file system.                                    
     *  
     *  @param request The Http servlet request.
     **/
    public FileListElement(HttpServletRequest request)
    {
        setHttpServletRequest(request); 
        setRenderer(new FileListRenderer(request));                              
    }


    /**
     *  Constructs an FileListElement for an AS/400 file system
     *  using the pathInfo from the specified <i>request</i>, and 
     *  the designated <i>system</i>.
     *
     *  Internally a com.ibm.as400.access.IFSJavaFile object will be 
     *  used to retrieve the contents of the file system.  
     *
     *  @param system  The AS/400 system.
     *  @param request The Http servlet request. 
     **/
    public FileListElement(AS400 system, HttpServletRequest request)
    {
        setSystem(system);
        setHttpServletRequest(request); 
        setRenderer(new FileListRenderer(request));                              // @A4A
    }


    /**
     *  Constructs an FileListElement with the specified <i>system</i>, <i>request</i>, and <i>table</i>.
     *
     *  Internally a com.ibm.as400.access.IFSJavaFile object will be 
     *  used to retrieve the contents of the file system.  
     *
     *  @param system  The AS/400 system.
     *  @param request The Http servlet request.
     *  @param table   The HTML table.
     **/
    public FileListElement(AS400 system, HttpServletRequest request, HTMLTable table)
    {
        setSystem(system);
        setHttpServletRequest(request);
        setTable(table);
        setRenderer(new FileListRenderer(request));                              // @A4A
    }


    /**
     *  Constructs a FileListElement with the specified <i>system</i>, <i>requst</i>, NetServer <i>sharePath</i>, and
     *  NetServer <i>shareName</i>.
     *
     *  Internally a com.ibm.as400.access.IFSJavaFile object will be 
     *  used to retrieve the contents of the file system at the network share point.  
     *
     *  @param system    The iSeries system.
     *  @param request   The Http servlet request.
     *  @param shareName The NetServer share name.
     *  @param sharePath The NetServer share path.
     *
     **/
    public FileListElement(AS400 system, HttpServletRequest request, String shareName, String sharePath) // @B1A
    {                                                                                                    // @B1A
        setSystem(system);                                                                               // @B1A
        setHttpServletRequest(request);                                                                  // @B1A
        setRenderer(new FileListRenderer(request, shareName, sharePath));                                // @B1A
        setShareName(shareName);                                                                         // @B1A
        setSharePath(sharePath);                                                                         // @B1A
    }


    /**
     *  Adds a PropertyChangeListener.  The specified 
     *  PropertyChangeListener's <b>propertyChange</b> 
     *  method is called each time the value of any
     *  bound property is changed.
     *
     *  @see #removePropertyChangeListener
     *  @param listener The PropertyChangeListener.
    **/
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");
        changes_.addPropertyChangeListener(listener);
    }


    /**
     *  Returns the Http servlet request.
     *
     *  @return The request.
     **/
    public HttpServletRequest getHttpServletRequest()
    {
        return request_;
    }


    /**
     *  Return the file list renderer.
     *
     *  @return The renderer.
     **/
    public FileListRenderer getRenderer()
    {
        return renderer_;
    }


    /**
     *  Return the NetServer share point.
     *
     *  @return The NetServer share path.
     **/
    public String getSharePath()                    // @B1A
    {                                               // @B1A
        return sharePath_.toString();               // @B1A
    }                                               // @B1A

    /**
     *  Return the name of the NetServer share.
     *
     *  @return The name of the NetServer share.
     **/
    public String getShareName()                    // @B1A
    {                                               // @B1A
        return shareName_.toString();               // @B1A
    }                                               // @B1A


    /**
     *  Returns the AS/400 system.
     *
     *  @return The system.
     *
     **/
    public AS400 getSystem()
    {
        return system_;
    }


    /**
     *  Returns the HTMLTable.
     *
     *  @return The table.
     **/
    public HTMLTable getTable()
    {
        return table_;
    }


    /**
     *  Returns a string containing the list of files and directories
     *  in the path defined in the HttpServletRequest.  
     *
     *  If the <i>system</i> has not been set, a java.io.File object 
     *  will be created with the pathInfo from the HttpServletRequest
     *  to retrieve the list of file and directories from the
     *  local file system.
     *
     *  @return The list.
     **/
    public String list() 
    {   
        if (request_ == null)
            throw new ExtendedIllegalStateException("request", ExtendedIllegalStateException.PROPERTY_NOT_SET);

        // @A7D - If a system_ object is not provided then a java.io.File object will be created with the
        //        path info from the request.
        //
        //if (system_ == null)
        //    throw new ExtendedIllegalStateException("system", ExtendedIllegalStateException.PROPERTY_NOT_SET);

        if (Trace.isTraceOn())
            Trace.log(Trace.INFORMATION, "Creating FileListElement list...");

        StringBuffer buffer = new StringBuffer();

        String path = request_.getPathInfo();                                         // @A3C

        if (path == null)
            path = "/";

        if (sharePath_ != null)                                                                            // @B1A
        {                                                                                                  // @B1A
            try                                                                                            // @B1A
            {                                                                                              // @B1A
                path = sharePath_.append(path.substring(path.indexOf('/', 1), path.length())).toString();  // @B1A
            }                                                                                              // @B1A
            catch(StringIndexOutOfBoundsException e)                                                       // @B1A
            {                                                                                              // @B1A
                path = sharePath_.insert(0, "/").toString();                                               // @B1A
            }                                                                                              // @B1A
        }  

        if (Trace.isTraceOn())                                                                             // @B1A
            Trace.log(Trace.INFORMATION, "FileList path: " + path);                                        // @B1A

        try
        {
            File file;                                                                // @A7A

            // @A7A
            // If a system_ object is not provided then a java.io.File object will be created with the
            // path info from the request.

            if (system_ != null)                                                      // @A7A
                file = new IFSJavaFile(system_, path.replace('\\','/'));
            else                                                                      // @A7A
                file = new File(path);                                                // @A7A

            if (Trace.isTraceOn())                                                    // @A6A
                Trace.log(Trace.INFORMATION, "   FileListElement path: " + path);     // @A6A

            // Create a table converter object.
            HTMLTableConverter conv = new HTMLTableConverter();

            // Set the default table properties if the user has not set the table.
            if (table_ == null)
            {
                table_ = new HTMLTable();
                table_.setCellPadding(7);

                // Set the converter meta data property.
                conv.setUseMetaData(true);
            }
            else
            {
                // If the table has been set and the headers are empty, use
                // the default headers from the meta data.
                if (table_.getHeader() == null)
                    conv.setUseMetaData(true);
            }



            // Set the converter table property.
            conv.setTable(table_);

            ListMetaData metaData = new ListMetaData(4);

            metaData.setColumnName(0, "Name");
            metaData.setColumnLabel(0, name);
            metaData.setColumnType(0, RowMetaDataType.STRING_DATA_TYPE);

            metaData.setColumnName(1, "Size");                                      // @A3C
            metaData.setColumnLabel(1, size);                                       // @A3C @A5C
            metaData.setColumnType(1, RowMetaDataType.INTEGER_DATA_TYPE);           // @A3C

            metaData.setColumnName(2, "Type");
            metaData.setColumnLabel(2, type);                                       // @A5C
            metaData.setColumnType(2, RowMetaDataType.STRING_DATA_TYPE);      

            metaData.setColumnName(3, "Modified");                                  // @A3C
            metaData.setColumnLabel(3, modified);                                   // @A3C @A5C
            metaData.setColumnType(3, RowMetaDataType.STRING_DATA_TYPE);            // @A3C

            ListRowData rowData = new ListRowData();
            rowData.setMetaData(metaData);      

            // Get the string to display from the renderer.  This allows          // @A4A
            // the servlet more flexibility as to which files to display          // @A4A
            // and how to display them.                                           // @A4A
            String parentName = renderer_.getParentName(file);                    // @A4A

            if (parentName != null)                                               // @A4A
            {
                Object[] row = new Object[4];

                row[0] = parentName.replace('\\','/');                             // @A4C
                row[1] = "";
                row[2] = "";
                row[3] = "";

                rowData.addRow(row);
            }

            DirFilter dirFilter = new DirFilter();

            // Get the list of files that satisfy the directory filter.
            String[] dlist = file.list(dirFilter);                                //$A1C

            if (dlist != null)                                                    // @A6A
            {
                if (sort_)                                                        // @A2A
                    dlist = sort(dlist);                                          // @A2A

                File[] dirList = new File[dlist.length];                          //$A1A

                // $A1D
                // We don't want to require webservers to use JDK1.2 because
                // most webserver JVM's are slower to upgrade to the latest JDK level.
                // The most efficient way to create these file objects is to use
                // the listFiles(filter) method in JDK1.2 which would be done
                // like the following, instead of using the list(filter) method
                // and then converting the returned string arrary into the appropriate
                // File array.
                // File[] dirList = file.listFiles(dirFilter);

                for (int j=0; j<dlist.length; ++j)                                    //$A1A
                {
                    //$A1A
                    if (file instanceof IFSJavaFile)                                  //$A1A
                        dirList[j] = new IFSJavaFile((IFSJavaFile)file, dlist[j]);    //$A1A
                    else                                                              //$A1A
                        dirList[j] = new File(file, dlist[j]);                        //$A1A @A7C
                }  
                //$A1A
                for (int i=0; i<dirList.length; i++)
                {
                    // Get the string to display from the renderer.  This allows          // @A4A
                    // the servlet more flexibility as to which files to display          // @A4A
                    // and how to display them.                                           // @A4A
                    String dirName = renderer_.getDirectoryName(dirList[i]);              // @A4A

                    if (dirName != null)                                                  // @A4A
                    {
                        Object[] row = new Object[4];                  

                        Date d = new Date(dirList[i].lastModified());                      // @A4C

                        row[0] = dirName.replace('\\','/');                                // @A4C
                        row[1] = "";                                                       // @A3C
                        row[2] = "Directory";
                        row[3] = formatter_.format(d);                                     // @A3C

                        rowData.addRow(row);
                    }
                }
            }

            HTMLFileFilter fileFilter = new HTMLFileFilter();

            // Get the list of files that satisfy the file filter.
            String[] flist = file.list(fileFilter);                                     //$A1C

            if (flist != null)                                                          // @A6A
            {
                if (sort_)                                                              // @A2A
                    flist = sort(flist);                                                // @A2A

                File[] fileList = new File[flist.length];                               //$A1A

                // $A1D
                // We don't want to require webservers to use JDK1.2 because
                // most webserver JVM's are slower to upgrade to the latest JDK level.
                // The most efficient way to create these file objects is to use
                // the listFiles(filter) method in JDK1.2 which would be done
                // like the following, instead of using the list(filter) method
                // and then converting the returned string arrary into the appropriate
                // File array.
                // File[] fileList = file.listFiles(fileFilter);

                for (int j=0; j<flist.length; ++j)                                      //$A1A
                {
                    //$A1A
                    if (file instanceof IFSJavaFile)                                    //$A1A
                        fileList[j] = new IFSJavaFile((IFSJavaFile)file, flist[j]);     //$A1A
                    else                                                                //$A1A
                        fileList[j] = new File(file, flist[j]);                         //$A1A @A7C
                }                                                                       //$A1A

                for (int i=0; i<fileList.length; i++)
                {
                    // Get the string to display from the renderer.  This allows          // @A4A
                    // the servlet more flexibility as to which files to display          // @A4A
                    // and how to display them.                                           // @A4A
                    String fileName = renderer_.getFileName(fileList[i]);                 // @A4A

                    if (fileName != null)                                                 // @A4A
                    {
                        Object[] row = new Object[4];          

                        Date d = new Date(fileList[i].lastModified());                     

                        row[0] = fileName.replace('\\','/');                               // @A4C
                        row[1] = new Long(fileList[i].length());                             
                        row[2] = "File";                                                   // @A3C
                        row[3] = formatter_.format(d);                                       

                        rowData.addRow(row);          
                    }
                }
            }

            if (rowData.length() > 0)                                                   // @A6C
            {
                if (Trace.isTraceOn())
                    Trace.log(Trace.INFORMATION, "Generating FileListElement table...");

                String[] Table = conv.convert(rowData);
                buffer.append(Table[0]);
            }
        }
        catch (PropertyVetoException e)
        { /* Ignore */
        }
        catch (RowDataException rde)
        {
            if (Trace.isTraceOn())
                Trace.log(Trace.ERROR, rde);
        }

        return buffer.toString();
    }


    /**
     *  Deserializes and initializes transient data.
     **/
    private void readObject(java.io.ObjectInputStream in)          
    throws java.io.IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        changes_ = new PropertyChangeSupport(this);
    }


    /**
    *  Removes the PropertyChangeListener from the internal list.
    *  If the PropertyChangeListener is not on the list, nothing is done.
    *  
    *  @see #addPropertyChangeListener
    *  @param listener The PropertyChangeListener.
   **/
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");
        changes_.removePropertyChangeListener(listener);
    }


    /**
     *  Sets the Http servlet request for the element.
     *
     *  @param request The Http servlet request. 
     **/
    public void setHttpServletRequest(HttpServletRequest request)
    {
        if (request == null)
            throw new NullPointerException("request");

        HttpServletRequest old = request_;

        request_ = request;

        changes_.firePropertyChange("request", old, request_);
    }


    /**
     *  Set the renderer for the FileListElement.  The default
     *  is FileListRenderer.
     *
     *  @param renderer The file list renderer.
     **/
    public void setRenderer(FileListRenderer renderer)
    {
        if (renderer == null)
            throw new NullPointerException("renderer");

        FileListRenderer old = renderer_;

        renderer_ = renderer;

        changes_.firePropertyChange("renderer", old, renderer_);
    }


    /**
     *  Set the NetServer share path.  
     *
     *  @param sharePath The NetServer share path.
     **/
    public void setSharePath(String sharePath)                                      // @B1A
    {                                                                               // @B1A
        if (sharePath == null)                                                      // @B1A
            throw new NullPointerException("sharePath");                            // @B1A
                                                                                    // @B1A
        StringBuffer old = sharePath_;                                              // @B1A
                                                                                    // @B1A
        sharePath_ = new StringBuffer(sharePath);                                   // @B1A
                                                                                    // @B1A
        changes_.firePropertyChange("sharePath", 
                                    old==null ? null : old.toString(), sharePath_.toString());       // @B1A
    }


    /**
     *  Set the name of the NetServer share.
     *
     *  @param shareName The NetServer share name.
     **/
    public void setShareName(String shareName)                                      // @B1A
    {                                                                               // @B1A
        if (shareName == null)                                                      // @B1A
            throw new NullPointerException("shareName");                            // @B1A
                                                                                    // @B1A
        StringBuffer old = shareName_;                                              // @B1A
                                                                                    // @B1A
        shareName_ = new StringBuffer(shareName);                                   // @B1A
                                                                                    // @B1A
        changes_.firePropertyChange("shareName", 
                                    old==null ? null : old.toString(), shareName_.toString());       // @B1A
    }


    /**
     *  Set the AS/400 system.
     *
     *  @param The system.
     **/
    public void setSystem(AS400 system)
    {
        if (system == null)
            throw new NullPointerException("system");

        AS400 old = system_;

        system_ = system;

        changes_.firePropertyChange("system", old, system_);
    }


    /**
     *  Set the HTMLTable to use when displaying the file list.
     *  This will replace the default HTMLTable used.
     *
     *  @param table The HTML table.
     **/
    public void setTable(HTMLTable table)
    {
        if (table == null)
            throw new NullPointerException("table");

        HTMLTable old = table;

        table_ = table;

        changes_.firePropertyChange("table", old, table_);
    }


    /**
     *  Sorts the list elements.
     *
     *  @param sort true if the elements are sorted; false otherwise.
     *              The default is true.
     **/
    public void sort(boolean sort)                         // @A2A
    {
        sort_ = sort;
    }


    /**
     *  Sorts an array of string objects.
     *
     *  @param  objects The objects.
     *
     *  @return The sorted string array.
     **/
    private String[] sort (String[] list)                  // @A2A
    {
        String[] objectArray = list;

        HTMLTree.sort2(objectArray);

        for (int i = 0; i < objectArray.length; ++i)
            list[i] = objectArray[i];

        return list;
    }
}
