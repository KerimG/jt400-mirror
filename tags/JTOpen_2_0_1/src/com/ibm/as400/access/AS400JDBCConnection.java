///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (AS/400 Toolbox for Java - OSS version)                              
//                                                                             
// Filename: AS400JDBCConnection.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2000 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.BitSet;                    // @DAA
import java.util.Enumeration;               // @DAA
import java.util.Map;
import java.util.Properties;
import java.util.Vector;



/**
<p>The AS400JDBCConnection class provides a JDBC connection
to a specific DB2 for OS/400 database.  Use
DriverManager.getConnection() to create new AS400JDBCConnection
objects.

<p>There are many optional properties that can be specified
when the connection is created.  Properties can be specified either
as part of the URL or in a java.util.Properties object.  See
<a href="../../../../JDBCProperties.html"> JDBC properties</a> for a complete
list of properties supported by the AS400JDBCDriver.

<p>Note that a connection may contain at most 9999 open
statements.
**/
//
// Implementation notes:
//
// 1.  Each connection and statement has an "id" associated with
//     it.  All ids are unique within a connection, and this
//     uniqueness is maintained by the id table for each
//     connection.
//
//     The id is used as a convention for assigning each
//     connection and statement its own ORS (Operation Result
//     Set) on the server as well as assigning each statement
//     its own RPB (Request Parameter Block).
//
//     Every communication to the server requires a connection
//     and an id within that connection.
//
// 2.  It is a requirement that no finalize() methods need to
//     receive a reply from the server.  Because of the way the
//     AS400Server class is implemented, certain scenarios where
//     this is the case will result in deadlock.  The AS400Server
//     class provides sendAndDiscardReply() specifically to avoid
//     this problem.
//
//     Within the JDBC driver, finalize() usually calls one or more
//     close() methods.  Therefore, this requirement is also
//     imposed on close() methods.
//
// 3.  All requests for the connection and the related objects in
//     its context should be sent via a variation of one of the
//     sendXXX() methods.  This makes debugging cleaner.
//
public class AS400JDBCConnection
implements Connection
{
  private static final String copyright = "Copyright (C) 1997-2000 International Business Machines Corporation and others.";



    // This is a compile time flag for doing simple
    // communications traces.
    //
    // The choices are:
    //   0 = No communication trace (for production code).
    //   1 = Only request and reply ids.
    //   2 = Request and reply ids and contents.
    //
    // Note that the LL (length) and parameter count for
    // requests will not be accurate, since they have not yet
    // been set at the time when the request is dumped.
    //
    private static final int            DEBUG_COMM_TRACE_       = 0;



    // This is a compile time flag for temporarily disabling
    // request chaining.  This can be useful when a request
    // is failing, but all we see is an error class == 7,
    // return code == -1000.  This means a chain request
    // failed.
    //
    // The choices are:
    //   true  = Enable request chaining (for production code).
    //   false = Disable request chaining.
    //
    // @E5D private static final boolean        DEBUG_REQUEST_CHAINING_ = true;



    // This is a compile time flag for forcing the use of
    // extended datastream formats.  This can be useful when
    // testing extended formats, but the server is not reporting
    // the correct VRM.
    //
    // The choices are:
    //   true  = Force extended datastream formats.
    //   false = Decide based on server VRM (for production code).
    //
    // @E9D private static final boolean        FORCE_EXTENDED_FORMATS_ = false;



            static final int            BIGINT_SUPPORTED_       = 0x00040500; // @D0A
    private static final String         CLIENT_FUNCTIONAL_LEVEL_= "1         "; // @EDA
    private static final int            DRDA_SCROLLABLE_CUTOFF_ = 129;        // @B1A
    private static final int            DRDA_SCROLLABLE_MAX_    = 255;        // @DAA
    private static final int            INITIAL_STATEMENT_TABLE_SIZE_ = 256;    // @DAA
            static final int            LOB_SUPPORTED_          = 0x00040400; // @D6A
            static final int            UNICODE_CCSID_          = 13488;      // @E3C

            // The max number of open statements per connection.  If this          @DAA
            // changes, then change the relevant sentence in the javadoc, too.     @DAA
            static final int            MAX_STATEMENTS_         = 9999;         // @DAC

            static final int            DATA_COMPRESSION_NONE_  = 0;            // @ECA
            static final int            DATA_COMPRESSION_OLD_   = 1;            // @ECA
            static final int            DATA_COMPRESSION_RLE_   = 0x3832;       // @ECA @EIC @EJC


    // Private data.
    private AS400ImplRemote             as400_;
    private AS400                       as400PublicClassObj_; // Prevents garbage collection.
    private BitSet                      assigned_;                      // @DAC
    private boolean                     cancelling_;                    // @E8A
    private Object                      cancelLock_ = new Object();     // @E8A
    private String                      catalog_;
    private boolean                     closing_;            // @D4A
    private ConverterImplRemote         converter_;
    private int                         dataCompression_            = -1;               // @ECA
    private JDDataSourceURL                 dataSourceUrl_;
    private boolean                     drda_;                          // @B1A
    private String                      defaultSchema_;
     private boolean                     extendedFormats_;
    // @E2D private ConverterImplRemote          graphicConverter_;
    // @E2D private boolean                     graphicConverterLoaded_;
    private Vector                      heldRequests_;                                  // @E5A
    private Object                      heldRequestsLock_           = new Object();     // @E5A
    private int                                 id_;
    private AS400JDBCDatabaseMetaData   metaData_;
     private JDPackageManager            packageManager_;
     private JDProperties                properties_;
     private boolean                     readOnly_;
     private BitSet                      requestPending_;                // @DAC
     private AS400Server                    server_;
    private int                         serverFunctionalLevel_;         // @E7A
    private String                      serverJobIdentifier_ = null;    // @E8A
    private SQLWarning                      sqlWarning_;
    private Vector                       statements_;                    // @DAC
     private JDTransactionManager           transactionManager_;
    private ConverterImplRemote         unicodeConverter_;              // @E3A
    private int                         vrm_;                           // @D0A



/**
Static initializer.  Initializes the reply data streams
that we expect to receive.
**/
    static
    {
        // The database server will only return 1 type of reply.
         AS400Server.addReplyStream (new DBReplyRequestedDS (),
             AS400.DATABASE);
    }



// The default constructor reserved for use within the package.
    AS400JDBCConnection ()  //@A3A
    {}



// @A3D  Deleted constructor:
//    AS400JDBCConnection (JDDataSourceURL dataSourceUrl, JDProperties properties)
//        throws SQLException



// @E8A
/**
Cancels a statement within this connection.

@param id   The id.

@exception              SQLException    If the statement cannot be executed.
**/
    void cancel(int id)
        throws SQLException
    {
        // Lock out all other operations for this connection.
        synchronized(cancelLock_) {
            cancelling_ = true;

            try {
                // If the server job identifier was returned, and the server is at a
                // functional level 5 or greater, then use the job identifier to issue
                // the cancel from another connection.  Otherwise, do nothing.
                if ((serverJobIdentifier_ != null) && (serverFunctionalLevel_ >= 5)){

                    if (JDTrace.isTraceOn())
                        JDTrace.logInformation (this, "Cancelling statement " + id);

                    // Create another connection to issue the cancel.
                    AS400JDBCConnection cancelConnection = new AS400JDBCConnection();
                    AS400 system = new AS400(as400PublicClassObj_);
                    cancelConnection.setSystem(system);
                    cancelConnection.setProperties(dataSourceUrl_, properties_, system);

                   // Send the cancel request.
              try {
                   DBSQLRequestDS request = new DBSQLRequestDS(DBSQLRequestDS.FUNCTIONID_CANCEL, id_,
                       DBBaseRequestDS.ORS_BITMAP_RETURN_DATA, 0);
                        request.setJobIdentifier(serverJobIdentifier_, converter_);
                        DBReplyRequestedDS reply = cancelConnection.sendAndReceive (request);

                        int errorClass = reply.getErrorClass();
                   int returnCode = reply.getReturnCode();
                   if (errorClass != 0)
                        JDError.throwSQLException(this, id_, errorClass, returnCode);
                   }
                   catch (DBDataStreamException e) {
                   JDError.throwSQLException (JDError.EXC_INTERNAL, e);
                  }

                    cancelConnection.close();
                }
                else {
                    if (JDTrace.isTraceOn())
                        JDTrace.logInformation (this, "Cancel of statement " + id + " requested, but is not supported by server");
                }
            }
            finally {
                // Let others back in.
                    cancelling_ = false;
                    cancelLock_.notifyAll();
            }
        }
    }



/**
Checks that the specified SQL statement can be executed.
This decision is based on the access specified by the caller
and the read only mode.

@param   sqlStatement   The SQL statement.

@exception              SQLException    If the statement cannot be executed.
**/
     void checkAccess (JDSQLStatement sqlStatement)
         throws SQLException
     {
         String access = properties_.getString (JDProperties.ACCESS);

         // If we only have read only access, then anything other
         // than a SELECT can not be executed.
         if ((access.equalsIgnoreCase (JDProperties.ACCESS_READ_ONLY))
             && (! sqlStatement.isSelect ()))
               JDError.throwSQLException (JDError.EXC_ACCESS_MISMATCH);

        // If we have read call access, then anything other than
        // a SELECT or CALL can not be executed.
          if (((readOnly_)
              || ((access.equalsIgnoreCase (JDProperties.ACCESS_READ_CALL))))
              && (! sqlStatement.isSelect())
              && (! sqlStatement.isProcedureCall()))
               JDError.throwSQLException (JDError.EXC_ACCESS_MISMATCH);
     }



// @E8A
/**
Checks to see if we are cancelling a statement.  If so, wait until the
cancel is done.  If not, go ahead.
**/
    private void checkCancel()
    {
        synchronized(cancelLock_) {
            if (cancelling_) {
                try {
                    cancelLock_.wait();
                }
                catch(InterruptedException e) {
                    // Ignore.
                }
            }
        }
    }



/**
Checks that the connection is open.  Public methods
that require an open connection should call this first.

@exception  SQLException    If the connection is not open.
**/
    void checkOpen ()
        throws SQLException
    {
        if (server_ == null)
            JDError.throwSQLException (JDError.EXC_CONNECTION_NONE);
    }



/**
Clears all warnings that have been reported for the connection.
After this call, getWarnings() returns null until a new warning
is reported for the connection.

@exception SQLException If an error occurs.
**/
    public void clearWarnings ()
        throws SQLException
    {
          sqlWarning_ = null;
    }



/**
Releases the connection's resources immediately instead of waiting
for them to be automatically released.  This rolls back any active
transactions, closes all statements that are running in the context
of the connection, and disconnects from the server.

@exception SQLException If an error occurs.
**/
//
// Implementation notes:
//
// 1. We do not have to worry about thread synchronization here,
//    since the AS400Server object handles it.
//
// 2. It is a requirement to not get replies during a finalize()
//    method.  Since finalize() calls this method, this requirement
//    applies here, too.
//
    public void close ()
        throws SQLException
    {
        // @D4A
        // Avoid recursion.  When we close associated statements, they try
        // to close this connection.
        if (closing_) return;
        closing_ = true;

        // If this is already closed, then just do nothing.
        //
        // The spec does not define what happens when a connection
        // is closed multiple times.  The official word from the Sun
        // JDBC team is that "the driver's behavior in this case
        // is implementation defined.   Applications that do this are
        // non-portable."
        if (isClosed ())
            return;

        // partial close (moved rollback and closing of all the statements).     @E1
        pseudoClose();

          // Disconnect from the server.
          if (server_ != null) {

            // @B3  It turns out that we were closing the connection,
            // @B3  then the AS400Server object was in its disconnectServer()
            // @B3  method.  Since the AS400Server object needs to do other
            // @B3  cleanup, we still need to call it.

               // @B3D try {
               // @B3D   DBSQLEndCommDS request = new DBSQLEndCommDS (
               // @B3D       DBSQLEndCommDS.FUNCTIONID_END_COMMUNICATION,
               // @B3D       id_, 0, 0);
               // @B3D   send (request);
               // @B3D }
               // @B3D catch (Exception e) {
               // @B3D   JDError.throwSQLException (JDError.EXC_INTERNAL, e);
               // @B3D }

               as400_.disconnectServer (server_);
          server_ = null;
          }

        if (JDTrace.isTraceOn())
            JDTrace.logClose (this);
    }



// @E4C
/**
Commits all changes made since the previous commit or
rollback and releases any database locks currently held by
the connection.  This has no effect when the connection
is in auto-commit mode.

<p>This method can not be called when the connection is part
of a distributed transaction.  See <a href="AS400JDBCXAResource.html">
AS400JDBCXAResource</a> for more information.

@exception SQLException     If the connection is not open
                            or an error occurs.
**/
    public void commit ()
      throws SQLException
    {
        checkOpen ();

        if (!transactionManager_.isLocalTransaction())                      // @E4A
            JDError.throwSQLException (JDError.EXC_TXN_STATE_INVALID);      // @E4A

        // Note:  Intuitively, it seems like if we are in
        //        auto-commit mode, that we should not need to
        //        do anything for an explicit commit.  However,
        //        somewhere along the line, the server gets
        //        confused, so we go ahead an send the commit
        //        anyway.

        transactionManager_.commit ();

        if (transactionManager_.getHoldIndicator() == JDTransactionManager.CURSOR_HOLD_FALSE)   // @B4A
            markCursorsClosed();                                                                // @B4A

        if (JDTrace.isTraceOn())
            JDTrace.logInformation (this, "Transaction commit");
    }



/**
Corrects the result set type based on the result set concurrency
and posts a warning.

@param resultSetType            The result set type.
@param resultSetConcurrency     The result set concurrency.
@return                         The correct result set type.
**/
    private int correctResultSetType (int resultSetType,
                                      int resultSetConcurrency)
    throws SQLException // @EGA
    {
        int newResultSetType = (resultSetConcurrency == ResultSet.CONCUR_UPDATABLE)
            ? ResultSet.TYPE_SCROLL_SENSITIVE : ResultSet.TYPE_SCROLL_INSENSITIVE;
        postWarning (JDError.getSQLWarning (JDError.WARN_OPTION_VALUE_CHANGED));
        return newResultSetType;
    }



/**
Creates a Statement object for executing SQL statements without
parameters.  If the same SQL statement is executed many times, it
is more efficient to use prepareStatement().

<p>Result sets created using the statement will be type
ResultSet.TYPE_FORWARD_ONLY and concurrency
ResultSet.CONCUR_READ_ONLY.

@return     The statement object.

@exception  SQLException    If the connection is not open,
                        the maximum number of statements
                        for this connection has been reached, or an
                            error occurs.
**/
    public Statement createStatement ()
        throws SQLException
    {
        return createStatement (ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY);
    }



// JDBC 2.0
/**
Creates a Statement object for executing SQL statements without
parameters.  If the same SQL statement is executed many times, it
is more efficient to use prepareStatement().

@param resultSetType            The result set type.  Valid values are:
                                <ul>
                                  <li>ResultSet.TYPE_FORWARD_ONLY
                                  <li>ResultSet.TYPE_SCROLL_INSENSITIVE
                                  <li>ResultSet.TYPE_SCROLL_SENSITIVE
                                </ul>
@param resultSetConcurrency     The result set concurrency.  Valid values are:
                                <ul>
                                  <li>ResultSet.CONCUR_READ_ONLY
                                  <li>ResultSet.CONCUR_UPDATABLE
                                </ul>
@return                         The statement object.

@exception      SQLException    If the connection is not open,
                           the maximum number of statements
                            for this connection has been reached, the
                                result type or currency is not supported,
                                or an error occurs.
**/
    public Statement createStatement (int resultSetType,
                                           int resultSetConcurrency)
      throws SQLException
    {
        // Validation.
        checkOpen ();
        if (! metaData_.supportsResultSetConcurrency (resultSetType, resultSetConcurrency))
            resultSetType = correctResultSetType (resultSetType, resultSetConcurrency);

        // Create the statement.
        int statementId = getUnusedId (resultSetType); // @B1C
          AS400JDBCStatement statement = new AS400JDBCStatement (this,
              statementId, transactionManager_, packageManager_,
              properties_.getString (JDProperties.BLOCK_CRITERIA),
              properties_.getInt (JDProperties.BLOCK_SIZE),
              properties_.getBoolean (JDProperties.PREFETCH),
              properties_.getString (JDProperties.PACKAGE_CRITERIA), // @A2A
              resultSetType, resultSetConcurrency);
        statements_.addElement(statement);          // @DAC
        return statement;
    }



/**
Outputs debug information for a request.  This should only be used
for debugging the JDBC driver and is not intended for production code.

@param   request     The request.
**/
    private void debug (DBBaseRequestDS request)
    {
        if (DEBUG_COMM_TRACE_ >= 1)
            System.out.println ("Server request: "
                + Integer.toString (request.getServerID(), 16).toUpperCase()
                + ":" + Integer.toString (request.getReqRepID(), 16).toUpperCase()
                + ".");
        if (DEBUG_COMM_TRACE_ >= 2)
            request.dump (System.out);
    }



/**
Outputs debug information for a reply.  This should only be used
for debugging the JDBC driver and is not intended for production code.

@param   reply     The reply.
**/
    private void debug (DBReplyRequestedDS reply)
    {
        if (DEBUG_COMM_TRACE_ >= 1)
            System.out.println ("Server reply:   "
                + Integer.toString (reply.getServerID(), 16).toUpperCase()
                + ":" + Integer.toString (reply.getReturnDataFunctionId(), 16).toUpperCase()
                + ".");
        if (DEBUG_COMM_TRACE_ >= 2)
            reply.dump (System.out);

          int errorClass = ((DBReplyRequestedDS) reply).getErrorClass();
          int returnCode = ((DBReplyRequestedDS) reply).getReturnCode();

        if (DEBUG_COMM_TRACE_ >= 1)
             if ((errorClass != 0) || (returnCode != 0))
                System.out.println ("Server error = " + errorClass + ":"
                    + returnCode + ".");
    }



/**
Closes the connection if not explicitly closed by the caller.

@exception   Throwable      If an error occurs.
**/
    protected void finalize ()
        throws Throwable
    {
        if (! isClosed ())
            close ();

        super.finalize ();
    }



/**
Returns the AS400 object for this connection.

@return     The AS400 object.
**/
    AS400Impl getAS400 ()
    throws SQLException // @EGA
    {
        return as400_;
    }



/**
Returns the auto-commit state.

@return     true if the connection is in auto-commit mode;
            false otherwise.

@exception  SQLException    If the connection is not open.
**/
    public boolean getAutoCommit ()
        throws SQLException
    {
        checkOpen ();
          return transactionManager_.getAutoCommit ();
    }



/**
Returns the catalog name.

@return     The catalog name.

@exception  SQLException    If the connection is not open.
**/
    public String getCatalog ()
        throws SQLException
    {
        checkOpen ();
          return catalog_;
    }



/**
Returns the converter for this connection.

@return     The converter.
**/
    ConverterImplRemote getConverter ()
    throws SQLException // @EGA
    {
        return converter_;
    }



/**
Returns the converter for the specified CCSID, unless
it is 0 or -1 (i.e. probably set for a non-text field), in
which case it returns the converter for this connection.
This is useful for code that handles all types of fields
in a generic manner.

@param      ccsid       The CCSID.
@return     The converter.

@exception  SQLException    If the CCSID is not valid.
**/
    ConverterImplRemote getConverter (int ccsid)
        throws SQLException
    {
        try {
            switch(ccsid) {                                                                 // @E3A
            case -1:                                                                        // @E3A
            case 0:                                                                         // @E3A
            case 1:                                                                         // @E3A
                return converter_;                                                          // @E3A
            case UNICODE_CCSID_:                                                            // @E3A
                if (unicodeConverter_ == null)                                              // @E3A
                    unicodeConverter_ = ConverterImplRemote.getConverter(13488, as400_);    // @E3A
                return unicodeConverter_;                                                   // @E3A
            default:                                                                        // @E3A
                return ConverterImplRemote.getConverter (ccsid, as400_);                    // @E3C
            }                                                                               // @E3A
        }
        catch (UnsupportedEncodingException e) {
            JDError.throwSQLException (JDError.EXC_INTERNAL, e);
            return null;
        }
    }



// @ECA
/**
Returns the style of data compression.

@return The style of data compression.  Possible values are DATA_COMPRESSION_NONE_,
        DATA_COMPRESSION_OLD_, and DATA_COMPRESSION_RLE_.
**/
    int getDataCompression()                                                                // @ECA
    {                                                                                       // @ECA
        return dataCompression_;                                                            // @ECA
    }                                                                                       // @ECA



/**
Returns the default schema.

@return     The default schema, or QGPL if none was
            specified.
**/
    String getDefaultSchema ()
    throws SQLException // @EGA
    {
        return ((defaultSchema_ == null) ? "QGPL" : defaultSchema_);
    }



// @E2D /**
// @E2D Returns the graphic converter for this connection.
// @E2D
// @E2D @return     The graphic converter.
// @E2D
// @E2D @exception  SQLException    If no graphic converter was loaded.
// @E2D **/
// @E2D //
// @E2D // Implementation note:
// @E2D //
// @E2D // * Graphic data is pure double-byte, so we will need a
// @E2D //   different converter for that.  If there is no associated
// @E2D //   double-byte CCSID, or the converter can not be loaded,
// @E2D //   then we should throw an exception.  We wait to load this,
// @E2D //   since the majority of callers do not need this converter.
// @E2D //
// @E2D     ConverterImplRemote getGraphicConverter ()
// @E2D         throws SQLException
// @E2D     {
// @E2D         // If the graphic converter has not yet been loaded,
// @E2D         // then do so.
// @E2D         if (graphicConverterLoaded_ == false) {
// @E2D             int serverGraphicCCSID = ExecutionEnvironment.getAssociatedDbcsCcsid (converter_.getCcsid ());
// @E2D             if (serverGraphicCCSID != -1) {
// @E2D                 try {
// @E2D                       graphicConverter_ = ConverterImplRemote.getConverter (serverGraphicCCSID, as400_);
// @E2D                  }
// @E2D                  catch (UnsupportedEncodingException e) {
// @E2D                      graphicConverter_ = null;
// @E2D                  }
// @E2D             }
// @E2D
// @E2D             if (JDTrace.isTraceOn ()) {
// @E2D                 if (graphicConverter_ != null)
// @E2D                     JDTrace.logInformation (this, "Server graphic CCSID = " + serverGraphicCCSID);
// @E2D                 else
// @E2D                     JDTrace.logInformation (this, "No graphic CCSID was loaded");
// @E2D             }
// @E2D         }
// @E2D
// @E2D         // Return the graphic converter, or throw an exception.
// @E2D         if (graphicConverter_ == null)
// @E2D             JDError.throwSQLException (JDError.EXC_CCSID_INVALID);
// @E2D         return graphicConverter_;
// @E2D     }



/**
Returns the DatabaseMetaData object that describes the
connection's tables, supported SQL grammar, stored procedures,
capabilities and more.

@return     The metadata object.

@exception  SQLException    If an error occurs.
**/
    public DatabaseMetaData getMetaData ()
          throws SQLException
    {
        // We allow a user to get this object even if the
        // connection is closed.

          return metaData_;
    }



/**
Returns the connection properties.

@return    The connection properties.
**/
    JDProperties getProperties ()
    throws SQLException // @EGA
    {
        return properties_;
    }



// @E8A
/**
Returns the job identifier of the host server job corresponding to this connection.
Every JDBC connection is associated with a host server job on the AS/400.  The
format is:
<ul>
  <li>10 character job name
  <li>10 character user name
  <li>6 character job number
</ul>

<p>Note: Since this method is not defined in the JDBC Connection interface,
you typically need to cast a Connection object to AS400JDBCConnection in order
to call this method:
<blockquote><pre>
String serverJobIdentifier = ((AS400JDBCConnection)connection).getServerJobIdentifier();
</pre></blockquote>

@return The server job identifier, or null if not known.
**/
    public String getServerJobIdentifier()                              // @E8A
    {                                                                   // @E8A
        return serverJobIdentifier_;                                    // @E8A
    }                                                                   // @E8A




    int getServerFunctionalLevel()                                      // @EEA
    {                                                                   // @EEA
        return serverFunctionalLevel_;                                  // @EEA
    }                                                                   // @EEA


// @EHA
/**
Returns the system object which is managing the connection to the AS/400.

<p>Note: Since this method is not defined in the JDBC Connection interface,
you typically need to cast a Connection object to AS400JDBCConnection in order
to call this method:
<blockquote><pre>
AS400 system = ((AS400JDBCConnection)connection).getSystem();
</pre></blockquote>

@return The system.
**/
    public AS400 getSystem()                                            // @EHA
    {                                                                   // @EHA
        return as400PublicClassObj_;                                    // @EHA
    }                                                                   // @EHA





/**
Returns the transaction isolation level.

@return     The transaction isolation level.  Possible
            values are:
            <ul>
            <li>TRANSACTION_NONE
            <li>TRANSACTION_READ_UNCOMMITTED
            <li>TRANSACTION_READ_COMMITTED
               <li>TRANSACTION_REPEATABLE_READ
            </ul>

@exception  SQLException    If the connection is not open.
**/
    public int getTransactionIsolation ()
          throws SQLException
    {
        checkOpen ();
          return transactionManager_.getIsolation ();
    }



    JDTransactionManager getTransactionManager()                                // @E4A
    {                                                                           // @E4A
        return transactionManager_;                                             // @E4A
    }                                                                           // @E4A



// JDBC 2.0
/**
Returns the type map.

<p>This driver does not support the type map.

@return     The type map.

@exception  SQLException    This exception is always thrown.
**/
    public Map getTypeMap ()
        throws SQLException
    {
        JDError.throwSQLException (JDError.EXC_FUNCTION_NOT_SUPPORTED);
        return null;
    }



// @B1C
/**
Returns the next unused id.

@param      resultSetType       The result set type.  This is
                                relevant only when the connection
                                is being used for DRDA.
@return                         The next unused id.
**/
//
// Implementation note:  This method needs to be synchronized
// so that the same id does not get assigned twice.
//
    private synchronized int getUnusedId (int resultSetType)
        throws SQLException
    {
        // Note: We will always assume id 0 is being used,
        // since that represents the connection itself.

        // If this connection is being used for DRDA, then we
        // must use statement ids of 1-128 for non-scrollable
        // cursors and 129-255 for scrollable cursors.
        if (drda_) {
            if (resultSetType == ResultSet.TYPE_FORWARD_ONLY) {
                for (int i = 1; i < DRDA_SCROLLABLE_CUTOFF_; ++i) {
                    if (assigned_.get(i) == false) {                                    // @DAC
                        assigned_.set(i);                                               // @DAC
                        return i;
                    }
                }
            }
            else {
                for (int i = DRDA_SCROLLABLE_CUTOFF_; i < DRDA_SCROLLABLE_MAX_; ++i) {  // @DAC
                    if (assigned_.get(i) == false) {                                    // @DAC
                        assigned_.set(i);                                               // @DAC
                        return i;
                    }
                }
            }
        }

        // If this connection is NOT being used for DRDA, then
        // we can use any statement id.
        else {
            for (int i = 1; i < MAX_STATEMENTS_; ++i) {
                if (assigned_.get(i) == false) {                                    // @DAC
                    assigned_.set(i);                                               // @DAC
                    return i;
                }
            }
        }

          // All ids are being used.
          JDError.throwSQLException (JDError.EXC_MAX_STATEMENTS_EXCEEDED);
          return -1;
    }



/**
Returns the URL for the connection's database.

@return      The URL for the database.
**/
     String getURL ()
    throws SQLException // @EGA
     {
          return dataSourceUrl_.toString ();
     }



/**
Returns the user name as currently signed on to the server.

@return      The user name.
**/
     String getUserName ()
    throws SQLException // @EGA
     {
         return as400_.getUserId ();
     }



    int getVRM()                                            // @D0A
    throws SQLException // @EGA
    {                                                       // @D0A
        return vrm_;                                        // @D0A
    }                                                       // @D0A



/**
Returns the first warning reported for the connection.
Subsequent warnings may be chained to this warning.

@return     The first warning or null if no warnings
            have been reported.

@exception  SQLException    If an error occurs.
**/
    public SQLWarning getWarnings ()
          throws SQLException
    {
          return sqlWarning_;
    }



/**
Indicates if the specified cursor name is already used
in the connection.

@return     true if the cursor name is already used;
            false otherwise.
**/
    boolean isCursorNameUsed (String cursorName)
    throws SQLException // @EGA
    {
        Enumeration enum = statements_.elements();                                                          // @DAA
        while(enum.hasMoreElements()) {                                                                     // @DAC
            if (((AS400JDBCStatement)enum.nextElement()).getCursorName().equalsIgnoreCase(cursorName))      // @DAC
                return true;
        }
          return false;
    }



/**
Indicates if the connection is closed.

@return     true if the connection is closed; false
            otherwise.

@exception  SQLException    If an error occurs.
**/
    public boolean isClosed ()
          throws SQLException
    {
          if (server_ == null)                        // @EFC
            return true;                            // @EFA
        if (!server_.isConnected()) {               // @EFA
            server_ = null;                         // @EFA
            return true;                            // @EFA
        }                                           // @EFA
        else                                        // @EFA
            return false;                           // @EFA
    }



/**
Indicates if the connection is in read-only mode.

@return     true if the connection is in read-only mode;
            false otherwise.

@exception  SQLException    If the connection is not open.
**/
    public boolean isReadOnly ()
          throws SQLException
    {
        checkOpen ();
          return ((readOnly_)
                  || (properties_.getString (JDProperties.ACCESS).equalsIgnoreCase (JDProperties.ACCESS_READ_ONLY))
                  || (properties_.getString (JDProperties.ACCESS).equalsIgnoreCase (JDProperties.ACCESS_READ_CALL)));
    }



// @B4A
/**
Marks all of the cursors as closed.
**/
    private void markCursorsClosed()
    {
        if (JDTrace.isTraceOn())
            JDTrace.logInformation (this, "Cursors were not held.  Marking all closed");

        Enumeration enum = statements_.elements();                              // @DAA
        while(enum.hasMoreElements())                                           // @DAC
            ((AS400JDBCStatement)enum.nextElement()).markCursorClosed();        // @DAC
    }



/**
Returns the native form of an SQL statement without
executing it. The JDBC driver converts all SQL statements
from the JDBC SQL grammar into the native DB2 for OS/400
SQL grammar prior to executing them.

@param  sql     The SQL statement in terms of the JDBC SQL grammar.
@return         The translated SQL statement in the native
                DB2 for OS/400 SQL grammar.

@exception      SQLException    If the SQL statement has a syntax error.
**/
    public String nativeSQL (String sql)
      throws SQLException
    {
          JDSQLStatement sqlStatement = new JDSQLStatement (sql,
              properties_.getString (JDProperties.DECIMAL_SEPARATOR), true,
              properties_.getString (JDProperties.PACKAGE_CRITERIA) );           // @A2A
          return sqlStatement.toString ();
    }



/**
Notifies the connection that a statement in its context has
been closed.

@param   statement   The statement.
@param   id          The statement's id.
**/
    void notifyClose (AS400JDBCStatement statement, int id)
    throws SQLException // @EGA
    {
        statements_.removeElement(statement);           // @DAC
        assigned_.clear(id);                            // @DAC
    }


// @A3D - Moved this logic up into AS400JDBCDriver:
//    private void open ()
//        throws SQLException



/**
Posts a warning for the connection.

@param   sqlWarning  The warning.
**/
     void postWarning (SQLWarning sqlWarning)
    throws SQLException // @EGA
    {
          if (sqlWarning_ == null)
               sqlWarning_ = sqlWarning;
          else
               sqlWarning_.setNextWarning (sqlWarning);
    }



/**
Precompiles an SQL stored procedure call with optional input
and output parameters and stores it in a CallableStatement
object.  This object can be used to efficiently call the SQL
stored procedure multiple times.

<p>Result sets created using the statement will be type
ResultSet.TYPE_FORWARD_ONLY and concurrency
ResultSet.CONCUR_READ_ONLY.

@param  sql     The SQL stored procedure call.
@return         The callable statement object.

@exception      SQLException    If the connection is not open,
                           the maximum number of statements
                           for this connection has been reached,  or an
                                error occurs.
**/
    public CallableStatement prepareCall (String sql)
      throws SQLException
    {
          return prepareCall (sql, ResultSet.TYPE_FORWARD_ONLY,
              ResultSet.CONCUR_READ_ONLY);
    }



// JDBC 2.0
/**
Precompiles an SQL stored procedure call with optional input
and output parameters and stores it in a CallableStatement
object.  This object can be used to efficiently call the SQL
stored procedure multiple times.

@param sql                      The SQL statement.
@param resultSetType            The result set type.  Valid values are:
                                <ul>
                                  <li>ResultSet.TYPE_FORWARD_ONLY
                                  <li>ResultSet.TYPE_SCROLL_INSENSITIVE
                                  <li>ResultSet.TYPE_SCROLL_SENSITIVE
                                </ul>
@param resultSetConcurrency     The result set concurrency.  Valid values are:
                                <ul>
                                  <li>ResultSet.CONCUR_READ_ONLY
                                  <li>ResultSet.CONCUR_UPDATABLE
                                </ul>
@return                         The prepared statement object.

@exception      SQLException    If the connection is not open,
                           the maximum number of statements
                            for this connection has been reached, the
                                result type or currency is not valid,
                                or an error occurs.
**/
    public CallableStatement prepareCall (String sql,
                                          int resultSetType,
                                               int resultSetConcurrency)
        throws SQLException
    {
        // Validation.
        checkOpen ();
        if (! metaData_.supportsResultSetConcurrency (resultSetType, resultSetConcurrency))
            resultSetType = correctResultSetType (resultSetType, resultSetConcurrency);

        // Create the statement.
          JDSQLStatement sqlStatement = new JDSQLStatement (sql,
              properties_.getString (JDProperties.DECIMAL_SEPARATOR), true,
              properties_.getString (JDProperties.PACKAGE_CRITERIA) );           // @A2A
          int statementId = getUnusedId (resultSetType); // @B1C
          AS400JDBCCallableStatement statement = new AS400JDBCCallableStatement (this,
            statementId, transactionManager_, packageManager_,
            properties_.getString (JDProperties.BLOCK_CRITERIA),
              properties_.getInt (JDProperties.BLOCK_SIZE),
              sqlStatement,
              properties_.getString (JDProperties.PACKAGE_CRITERIA),
              resultSetType, resultSetConcurrency);
        statements_.addElement(statement);                  // @DAC
          return statement;
    }



/**
Precompiles an SQL statement with optional input parameters
and stores it in a PreparedStatement object.  This object can
be used to efficiently execute this SQL statement
multiple times.

<p>Result sets created using the statement will be type
ResultSet.TYPE_FORWARD_ONLY and concurrency
ResultSet.CONCUR_READ_ONLY.

@param  sql     The SQL statement.
@return         The prepared statement object.

@exception      SQLException    If the connection is not open,
                           the maximum number of statements
                           for this connection has been reached,  or an
                                error occurs.
**/
    public PreparedStatement prepareStatement (String sql)
      throws SQLException
    {
          return prepareStatement (sql, ResultSet.TYPE_FORWARD_ONLY,
              ResultSet.CONCUR_READ_ONLY);
    }



// JDBC 2.0
/**
Precompiles an SQL statement with optional input parameters
and stores it in a PreparedStatement object.  This object can
be used to efficiently execute this SQL statement
multiple times.

@param sql                      The SQL statement.
@param resultSetType            The result set type.  Valid values are:
                                <ul>
                                  <li>ResultSet.TYPE_FORWARD_ONLY
                                  <li>ResultSet.TYPE_SCROLL_INSENSITIVE
                                  <li>ResultSet.TYPE_SCROLL_SENSITIVE
                                </ul>
@param resultSetConcurrency     The result set concurrency.  Valid values are:
                                <ul>
                                  <li>ResultSet.CONCUR_READ_ONLY
                                  <li>ResultSet.CONCUR_UPDATABLE
                                </ul>
@return                         The prepared statement object.

@exception      SQLException    If the connection is not open,
                           the maximum number of statements
                            for this connection has been reached, the
                                result type or currency is not valid,
                                or an error occurs.
**/
    public PreparedStatement prepareStatement (String sql,
                                               int resultSetType,
                                                    int resultSetConcurrency)
      throws SQLException
    {
        // Validation.
        checkOpen ();
        if (! metaData_.supportsResultSetConcurrency (resultSetType, resultSetConcurrency))
            resultSetType = correctResultSetType (resultSetType, resultSetConcurrency);

        // Create the statement.
          JDSQLStatement sqlStatement = new JDSQLStatement (sql,
              properties_.getString (JDProperties.DECIMAL_SEPARATOR), true,
              properties_.getString (JDProperties.PACKAGE_CRITERIA) );           // @A2A
          int statementId = getUnusedId (resultSetType); // @B1C
          AS400JDBCPreparedStatement statement = new AS400JDBCPreparedStatement (this,
              statementId, transactionManager_, packageManager_,
              properties_.getString (JDProperties.BLOCK_CRITERIA),
              properties_.getInt (JDProperties.BLOCK_SIZE),
              properties_.getBoolean (JDProperties.PREFETCH),
              sqlStatement, false,
              properties_.getString (JDProperties.PACKAGE_CRITERIA),
              resultSetType, resultSetConcurrency);
          statements_.addElement(statement);                      // @DAC
          return statement;
    }



/**
Partial closing of the connection.
@exception SQLException If a database error occurs.
**/
void pseudoClose() throws SQLException                      // @E1
{
   // Rollback before closing.
   if ((transactionManager_.isLocalTransaction()) && (transactionManager_.isLocalActive()))  // @E4A
       rollback ();

   // Close all statements that are running in the context of this connection.
   // Make a clone of the vector, since it will be modified as each statement          @DAA
   // closes itself                                                                 // @DAA
   Vector statements = (Vector)statements_.clone();                                 // @DAA
   Enumeration enum = statements.elements();                                        // @DAA
   while(enum.hasMoreElements()) {                                                  // @DAC
       AS400JDBCStatement statement = (AS400JDBCStatement)enum.nextElement();       // @DAA
       if (! statement.isClosed())                                                  // @DAC
           statement.close();                                                       // @DAC
   }
}



// @E4C
/**
Drops all changes made since the previous commit or
rollback and releases any database locks currently held by
the connection.  This has no effect when the connection
is in auto-commit mode.

<p>This method can not be called when the connection is part
of a distributed transaction.  See <a href="AS400JDBCXAResource.html">
AS400JDBCXAResource</a> for more information.

@exception SQLException     If the connection is not open
                            or an error occurs.
**/
    public void rollback ()
      throws SQLException
    {
        checkOpen ();

        if (!transactionManager_.isLocalTransaction())                      // @E4A
            JDError.throwSQLException (JDError.EXC_TXN_STATE_INVALID);      // @E4A

        if (! transactionManager_.getAutoCommit ()) {
            transactionManager_.rollback ();

            if (transactionManager_.getHoldIndicator() == JDTransactionManager.CURSOR_HOLD_FALSE)   // @B4A
                markCursorsClosed();                                                                // @B4A

            if (JDTrace.isTraceOn())
                JDTrace.logInformation (this, "Transaction rollback");
        }
    }



/**
Sends a request data stream to the server using the
connection's id and does not expect a reply.

@param   request     The request.

@exception           SQLException   If an error occurs.
**/
//
// See implementation notes for sendAndReceive().
//
    void send (DBBaseRequestDS request)
          throws SQLException
     {
         send (request, id_, true);
     }



/**
Sends a request data stream to the server and does not
expect a reply.

@param   request     The request.
@param   id          The id.

@exception           SQLException   If an error occurs.
**/
//
// See implementation notes for sendAndReceive().
//
    void send (DBBaseRequestDS request, int id)
          throws SQLException
     {
         send (request, id, true);
     }



/**
Sends a request data stream to the server and does not
expect a reply.

@param   request        The request.
@param   id             The id.
@param   leavePending   Indicates if the request should
                        be left pending.  This indicates
                        whether or not to base the next
                        request on this one.

@exception              SQLException   If an error occurs.
**/
//
// See implementation notes for sendAndReceive().
//
    void send (DBBaseRequestDS request, int id, boolean leavePending)
          throws SQLException
     {
        checkCancel();                                                                      // @E8A

         try {
            // Since we are just calling send() (instead of sendAndReceive()),              // @EAA
            // make sure we are not asking the server for a reply.  Otherwise,              // @EAA
            // the reply will come back and it will get held in the AS400                   // @EAA
            // read daemon indefinitely - - a memory leak.                                  // @EAA
            if (JDTrace.isTraceOn()) {                                                      // @EAA
                if (request.getOperationResultBitmap() != 0)                                // @EAA
                    JDTrace.logInformation (this, "Reply requested but not collected:" + request.getReqRepID()); // @EAA
            }                                                                               // @EAA

             request.setBasedOnORSHandle (0);                 // @DAC @EKC
          DBReplyRequestedDS reply = null;

            if (dataCompression_ == DATA_COMPRESSION_RLE_) {                                // @ECA
                request.addOperationResultBitmap(DBBaseRequestDS.ORS_BITMAP_REQUEST_RLE_COMPRESSION); // @ECA
                request.addOperationResultBitmap(DBBaseRequestDS.ORS_BITMAP_REPLY_RLE_COMPRESSION); // @ECA
                request.compress();                                                         // @ECA
            }                                                                               // @ECA

            DataStream actualRequest;                                                       // @E5A
            synchronized(heldRequestsLock_) {                                               // @E5A
                if (heldRequests_ != null)                                                  // @E5A
                    actualRequest = new DBConcatenatedRequestDS(heldRequests_, request);    // @E5A
                else                                                                        // @E5A
                    actualRequest = request;                                                // @E5A
                heldRequests_ = null;                                                       // @E5A
            }                                                                               // @E5A

            // @E5D if (DEBUG_REQUEST_CHAINING_ == true) {
                   server_.send(actualRequest);                                                // @E5C
                if (leavePending)                                                           // @DAA
                    requestPending_.set(id);                                                // @DAC
                else                                                                        // @DAA
                    requestPending_.clear(id);                                              // @DAA

            // @E5D }
            // @E5D else {
            // @E5D     request.addOperationResultBitmap (DBBaseRequestDS.ORS_BITMAP_RETURN_DATA);
            // @E5D     reply = (DBReplyRequestedDS) server_.sendAndReceive (request);
            // @E5D     requestPending_[id] = false;
            // @E5D }

            if (DEBUG_COMM_TRACE_ > 0) {
                debug (request);
                // @E5D if (DEBUG_REQUEST_CHAINING_ == false)
                // @E5D     debug (reply);
            }
        }
       catch (ConnectionDroppedException e) {                               // @C1A
           server_ = null;                                                  // @D8
           JDError.throwSQLException (JDError.EXC_CONNECTION_NONE, e);      // @C1A
       }                                                                    // @C1A
          catch (Exception e) {
               JDError.throwSQLException (JDError.EXC_INTERNAL, e);
          }
     }



// @EBD /**
// @EBD Sends a request data stream to the server and discards
// @EBD the reply.
// @EBD
// @EBD @param   request        The request.
// @EBD @param   id             The id.
// @EBD @param   leavePending   Indicates if the request should
// @EBD                         be left pending.  This indicates
// @EBD                         whether or not to base the next
// @EBD                         request on this one.
// @EBD
// @EBD @exception              SQLException   If an error occurs.
// @EBD **/
// @EBD //
// @EBD // See implementation notes for sendAndReceive().
// @EBD //
// @EBD     void sendAndDiscardReply (DBBaseRequestDS request, int id)
// @EBD        throws SQLException
// @EBD   {
// @EBD         checkCancel();                                                                      // @E8A
// @EBD
// @EBD       try {
// @EBD           request.setBasedOnORSHandle (0); 		//@EKC
// @EBD
// @EBD             DataStream actualRequest;                                                       // @E5A
// @EBD             synchronized(heldRequestsLock_) {                                               // @E5A
// @EBD                 if (heldRequests_ != null)                                                  // @E5A
// @EBD                     actualRequest = new DBConcatenatedRequestDS(heldRequests_, request);    // @E5A
// @EBD                 else                                                                        // @E5A
// @EBD                     actualRequest = request;                                                // @E5A
// @EBD                 heldRequests_ = null;                                                       // @E5A
// @EBD             }                                                                               // @E5A
// @EBD
// @EBD                 server_.sendAndDiscardReply(actualRequest);                                     // @E5C
// @EBD             requestPending_[id] = false;
// @EBD
// @EBD             if (DEBUG_COMM_TRACE_ > 0)
// @EBD                 debug (request);
// @EBD         }
// @EBD         catch (ConnectionDroppedException e) {                               // @C1A
// @EBD             server_ = null;                                                  // @D8
// @EBD             JDError.throwSQLException (JDError.EXC_CONNECTION_NONE, e);      // @C1A
// @EBD         }                                                                    // @C1A
// @EBD        catch (Exception e) {
// @EBD             JDError.throwSQLException (JDError.EXC_INTERNAL, e);
// @EBD        }
// @EBD   }



// @E5A
/**
Holds a request until the next explicit request.  It will
be concatenated at the beginning of the next request.

@param   request     The request.
@param   id          The id.

@exception           SQLException   If an error occurs.
**/
//
// See implementation notes for sendAndReceive().
//
    void sendAndHold(DBBaseRequestDS request, int id)
          throws SQLException
     {
        checkCancel();                                                                      // @E8A

        try {
            // Since we are just calling send() (instead of sendAndReceive()),              // @EAA
            // make sure we are not asking the server for a reply.  Otherwise,              // @EAA
            // the reply will come back and it will get held in the AS400                   // @EAA
            // read daemon indefinitely - - a memory leak.                                  // @EAA
            if (JDTrace.isTraceOn()) {                                                      // @EAA
                if (request.getOperationResultBitmap() != 0)                                // @EAA
                    JDTrace.logInformation (this, "Reply requested but not collected:" + request.getReqRepID()); // @EAA
            }                                                                               // @EAA

            request.setBasedOnORSHandle(0);                  // @DAC @EKC

            if (dataCompression_ == DATA_COMPRESSION_RLE_) {                                // @ECA
                request.addOperationResultBitmap(DBBaseRequestDS.ORS_BITMAP_REQUEST_RLE_COMPRESSION); // @ECA
                request.addOperationResultBitmap(DBBaseRequestDS.ORS_BITMAP_REPLY_RLE_COMPRESSION); // @ECA
                request.compress();                                                         // @ECA
            }                                                                               // @ECA

            synchronized(heldRequestsLock_) {
                if (heldRequests_ == null)
                    heldRequests_ = new Vector();
                heldRequests_.addElement(request);
            }

            requestPending_.set(id);                                                        // @DAC

            if (DEBUG_COMM_TRACE_ > 0) {
                debug (request);
                System.out.println("This request was HELD.");
            }
        }
          catch (Exception e) {
               JDError.throwSQLException (JDError.EXC_INTERNAL, e);
          }
     }



/**
Sends a request data stream to the server using the
connection's id and returns the corresponding reply from
the server.

@param   request     The request.
@return              The reply.

@exception           SQLException   If an error occurs.
**/
//
// See implementation notes for sendAndReceive().
//
     DBReplyRequestedDS sendAndReceive (DBBaseRequestDS request)
          throws SQLException
     {
         return sendAndReceive (request, id_);
     }



/**
Sends a request data stream to the server and returns the
corresponding reply from the server.

@param   request     The request.
@param   id          The id.
@return              The reply.

@exception           SQLException   If an error occurs.
**/
//
// Implementation notes:
//
// 1. We do not have to worry about thread synchronization
//    here, since the AS400Server object handles it.
//
// 2. The based on id is used to chain requests for the
//    same ORS without needing to get a reply for each one.
//    If a request fails, then all subsequent requests will
//    too, and the results from the original failure will
//    ultimately be returned.
//
//    Initially, the based on id is set to 0.  After a
//    request is sent the based on id is set to the statement's
//    id, so that subsequent requests will base on this id.
//    Finally, when a reply is retrieved, the based on id
//    is reset to 0.
//
//    The status of the based on id depends on whether a
//    request is pending, which is maintained in the id table.
//
     DBReplyRequestedDS sendAndReceive (DBBaseRequestDS request, int id)
          throws SQLException
     {
        checkCancel();                                                                      // @E8A

          DBReplyRequestedDS reply = null;

          try {
             request.setBasedOnORSHandle (0);                 // @DAC @EKC

            if (dataCompression_ == DATA_COMPRESSION_RLE_) {                                // @ECA
                request.addOperationResultBitmap(DBBaseRequestDS.ORS_BITMAP_REQUEST_RLE_COMPRESSION); // @ECA
                request.addOperationResultBitmap(DBBaseRequestDS.ORS_BITMAP_REPLY_RLE_COMPRESSION); // @ECA
                request.compress();                                                         // @ECA
            }                                                                               // @ECA

            DataStream actualRequest;                                                       // @E5A
            synchronized(heldRequestsLock_) {                                               // @E5A
                if (heldRequests_ != null)                                                  // @E5A
                    actualRequest = new DBConcatenatedRequestDS(heldRequests_, request);    // @E5A
                else                                                                        // @E5A
                    actualRequest = request;                                                // @E5A
                heldRequests_ = null;                                                       // @E5A
            }                                                                               // @E5A

               reply = (DBReplyRequestedDS)server_.sendAndReceive(actualRequest);              // @E5C
            reply.parse(dataCompression_);                                                  // @E5A
            requestPending_.clear(id);                                                      // @DAC

            if (DEBUG_COMM_TRACE_ > 0) {
                debug (request);
                debug (reply);
            }
          }
      catch (ConnectionDroppedException e) {                               // @C1A
          server_ = null;                                                  // @D8
          JDError.throwSQLException (JDError.EXC_CONNECTION_NONE, e);      // @C1A
      }                                                                    // @C1A
          catch (Exception e) {
               JDError.throwSQLException (JDError.EXC_INTERNAL, e);
          }

          return (DBReplyRequestedDS) reply;
     }



// @E4C
/**
Sets the auto-commit mode.   If the connection is in auto-commit
mode, then all of its SQL statements are executed and committed
as individual transactions.  Otherwise, its SQL statements are
grouped into transactions that are terminated by either a commit
or rollback.

<p>By default, the connection is in auto-commit mode.  The
commit occurs when the statement execution completes or the
next statement execute occurs, whichever comes first.  In the
case of statements returning a result set, the statement
execution completes when the last row of the result set has
been retrieved or the result set has been closed.  In advanced
cases, a single statement may return multiple results as well
as output parameter values.  Here the commit occurs when all results
and output parameter values have been retrieved.

<p>The auto-commit mode is always false when the connection is part
of a distributed transaction.  See <a href="AS400JDBCXAResource.html">
AS400JDBCXAResource</a> for more information.

@param  autoCommit  true to turn on auto-commit mode, false to
                    turn it off.

@exception          SQLException    If the connection is not open
                                    or an error occurs.
**/
    public void setAutoCommit (boolean autoCommit)
        throws SQLException
    {
        checkOpen ();

        transactionManager_.setAutoCommit (autoCommit);

        if (JDTrace.isTraceOn())
            JDTrace.logProperty (this, "Auto commit", transactionManager_.getAutoCommit ());
    }



/**
This method is not supported.

@exception          SQLException    If the connection is not open.
**/
    public void setCatalog (String catalog)
          throws SQLException
    {
        checkOpen ();

        // No-op.
    }



// @B1A
/**
Sets whether the connection is being used for DRDA.

@param  drda        true if the connection is being used for DRDA,
                    false otherwise.
**/
    void setDRDA (boolean drda)
    throws SQLException // @EGA
    {
        drda_ = drda;

        if (JDTrace.isTraceOn())
            JDTrace.logProperty (this, "DRDA", drda_);
    }



    //@D4A
    void setProperties (JDDataSourceURL dataSourceUrl, JDProperties properties,
                        AS400 as400)
      throws SQLException
    {
      try { as400.connectService (AS400.DATABASE); }
      catch (AS400SecurityException e) {                            //@D5C
        JDError.throwSQLException (JDError.EXC_CONNECTION_REJECTED, e);
      }
      catch (IOException e) {                                       //@D5C
        JDError.throwSQLException (JDError.EXC_CONNECTION_UNABLE, e);
      }

      setProperties (dataSourceUrl, properties, as400.getImpl());
    }



    //@A3A - This logic formerly resided in the ctor.
    void setProperties (JDDataSourceURL dataSourceUrl, JDProperties properties,
                        AS400Impl as400)
      throws SQLException
    {
      // Initialization.
      as400_                  = (AS400ImplRemote) as400;           //@A3A
      assigned_               = new BitSet(INITIAL_STATEMENT_TABLE_SIZE_);          // @DAC
      dataSourceUrl_          = dataSourceUrl;
      extendedFormats_        = false;
      properties_             = properties;
      requestPending_         = new BitSet(INITIAL_STATEMENT_TABLE_SIZE_);         // @DAC
      statements_             = new Vector(INITIAL_STATEMENT_TABLE_SIZE_);         // @DAC

      // Issue any warnings.
      if (dataSourceUrl_.isExtraPathSpecified ())
        postWarning (JDError.getSQLWarning (JDError.WARN_URL_EXTRA_IGNORED));
      if (dataSourceUrl_.isPortSpecified ())
        postWarning (JDError.getSQLWarning (JDError.WARN_URL_EXTRA_IGNORED));
      if (properties.isExtraPropertySpecified ())
        postWarning (JDError.getSQLWarning (JDError.WARN_PROPERTY_EXTRA_IGNORED));

      // Initialize the library list.
      String urlSchema = dataSourceUrl_.getSchema ();
      if (urlSchema == null)
        JDError.throwSQLException (JDError.WARN_URL_SCHEMA_INVALID);
      JDLibraryList libraryList = new JDLibraryList (
                                           properties_.getString (JDProperties.LIBRARIES), urlSchema,
                                           properties_.getString (JDProperties.NAMING)); // @B2C
      defaultSchema_ = libraryList.getDefaultSchema ();

      // The connection gets an id automatically, but never
      // creates an RPB on the server.  There should never be a need
      // to create an RPB on the server for a connection, but an
      // id is needed for retrieving Operational Result Sets (ORS)
      // for errors, etc.

      // Initialize a transaction manager for this connection.
      transactionManager_ = new JDTransactionManager (this, id_,
                                             properties_.getString (JDProperties.TRANSACTION_ISOLATION));

      transactionManager_.setHoldIndicator(properties_.getString(JDProperties.CURSOR_HOLD));       // @D9

      // Initialize the read-only mode to true if the access
      // property says read only.
      readOnly_ = (properties_.equals (JDProperties.ACCESS,
                                       JDProperties.ACCESS_READ_ONLY));

      //@A3D
      // Initialize the conversation.
      //open ();

      //@A3A
      // Connect.
      try {
        server_ = as400_.getConnection (AS400.DATABASE, false);
      }
      catch (AS400SecurityException e) {
        JDError.throwSQLException (JDError.EXC_CONNECTION_REJECTED, e);
      }
      catch (IOException e) {
        JDError.throwSQLException (JDError.EXC_CONNECTION_UNABLE, e);
      }

      // Initialize the catalog name at this point to be the system
      // name.  After we exchange attributes, we can change it to
      // the actual name.
      catalog_ = dataSourceUrl.getServerName();                              // @D7A
      if (catalog_.length() == 0)                                            // @D7A
        catalog_ = as400_.getSystemName ().toUpperCase ();                   // @A3A

      setServerAttributes ();
      libraryList.addOnServer (this, id_);

      // @E7D // Initialize a transaction manager for this connection.  Turn on                                @E7A
      // @E7D // new auto-commit support when the server functional level is                                   @E7A
      // @E7D // greater than or equal to 3.                                                                   @E7A
      // @E7D boolean newAutoCommitSupport = (serverFunctionalLevel_ >= 3);                                 // @E7A
      // @E7D transactionManager_.setNewAutoCommitSupport(newAutoCommitSupport);                            // @E7A

      // We keep a metadata object around for quick access.
      // The metadata object should share the id of the
      // connection, since it operates on a connection-wide
      // scope.
      metaData_ = new AS400JDBCDatabaseMetaData (this, id_);

      // The conversation was initialized to a certain
      // transaction isolation.  It is now time to turn on auto-
      // commit by default.
      transactionManager_.setAutoCommit (true);

      // Initialize the package manager.
      packageManager_ = new JDPackageManager (this, id_, properties_,
                                              transactionManager_.getCommitMode ());

      // Trace messages.
      if (JDTrace.isTraceOn()) {
        JDTrace.logOpen (this);
        JDTrace.logProperty (this, "Auto commit", transactionManager_.getAutoCommit ());
        JDTrace.logProperty (this, "Read only", readOnly_);
        JDTrace.logProperty (this, "Transaction isolation", transactionManager_.getIsolation ());
        if (packageManager_.isEnabled ())
          JDTrace.logInformation (this, "SQL package = "
                                  + packageManager_.getLibraryName() + "/"
                                  + packageManager_.getName ());
      }
    }



/**
Sets the read-only mode.  This will provide read-only
access to the database.  Read-only mode can be useful by
enabling certain database optimizations. If the caller
specified "read only" or "read call" for the "access" property,
then the read-only mode cannot be set to false.  The read-only
mode cannot be changed while in the middle of a transaction.

<p>This method can not be called when the connection is part
of a distributed transaction.  See <a href="AS400JDBCXAResource.html">
AS400JDBCXAResource</a> for more information.

@exception          SQLException    If the connection is not open,
                                    a transaction is active, or the
                                    "access" property is set to "read
                                    only".
**/
    public void setReadOnly (boolean readOnly)
          throws SQLException
    {
        checkOpen ();

          if (transactionManager_.isLocalActive () || transactionManager_.isGlobalActive()) // @E4C
               JDError.throwSQLException (JDError.EXC_TXN_STATE_INVALID);      // @E4C

        if ((readOnly == false)
            && ((properties_.getString (JDProperties.ACCESS).equalsIgnoreCase (JDProperties.ACCESS_READ_ONLY))
                || (properties_.getString (JDProperties.ACCESS).equalsIgnoreCase (JDProperties.ACCESS_READ_CALL))))
            JDError.throwSQLException (JDError.EXC_ACCESS_MISMATCH);

          readOnly_ = readOnly;

        if (JDTrace.isTraceOn())
            JDTrace.logProperty (this, "Read only", readOnly_);
    }



/**
Sets the server attributes.

@param      libraryList     The library list.

@exception  SQLException    If an error occurs.
**/
     private void setServerAttributes ()
          throws SQLException
     {
          try {
            vrm_ = as400_.getVRM();                                     // @D0A @ECM

               DBSQLAttributesDS request = new DBSQLAttributesDS (
                   DBSQLAttributesDS.FUNCTIONID_SET_ATTRIBUTES,
                   id_, DBBaseRequestDS.ORS_BITMAP_RETURN_DATA
                   + DBBaseRequestDS.ORS_BITMAP_SERVER_ATTRIBUTES, 0, 0);

            // We need to set a temporary CCSID just for this
            // request, since we use this request to get the
            // actual CCSID.
            ConverterImplRemote tempConverter =
                  ConverterImplRemote.getConverter (as400_.getCcsid(), as400_);

               // @E2D // Do not set the client CCSID.  We do not want
               // @E2D // the server to convert data, since we are going
               // @E2D // to do all conersion on the client.  By not telling
               // @E2D // the server our CCSID, then we achieve this.
               // @E2D //
               // @E2D // Note that the database server documentation
               // @E2D // states that when we do this, the CCSID values
               // @E2D // in data formats may be incorrect and that we
               // @E2D // should always use the server job's CCSID.

            // Set the client CCSID to Unicode.                             // @E2A
            request.setClientCCSID(13488);                                  // @E2A

            // This language feature code is used to tell the
            // server what language to send error messages in.
            // If that language is not installed on the server,
            // we get messages back in the default language that
            // was installed on the server.
            //
            String nlv = ExecutionEnvironment.getNlv ();
               request.setLanguageFeatureCode(nlv);                            // @EDC

               if (JDTrace.isTraceOn ())
                JDTrace.logInformation (this, "Setting server NLV = " + nlv);

               // Client functional level.
               request.setClientFunctionalLevel(CLIENT_FUNCTIONAL_LEVEL_);    // @EDC

               if (JDTrace.isTraceOn ())                                                                   // @EDC
                JDTrace.logInformation (this, "Client functional level = " + CLIENT_FUNCTIONAL_LEVEL_); // @EDC

            // Sort sequence.
            if (! properties_.equals (JDProperties.SORT, JDProperties.SORT_JOB)) {
                JDSortSequence sortSequence = new JDSortSequence (
                    properties_.getString (JDProperties.SORT),
                    properties_.getString (JDProperties.SORT_LANGUAGE),
                    properties_.getString (JDProperties.SORT_TABLE),
                    properties_.getString (JDProperties.SORT_WEIGHT));
                request.setNLSSortSequence (sortSequence.getType (),
                    sortSequence.getTableFile (),
                    sortSequence.getTableLibrary (),
                    sortSequence.getLanguageId (),
                    tempConverter);
            }

              request.setTranslateIndicator (0xF0);                       // @E2C
               request.setDRDAPackageSize (1);

            // Server attributes based on property values.
            // These all match the index within the property's
            // choices.
            int dateFormat = properties_.getIndex (JDProperties.DATE_FORMAT);
            if (dateFormat != -1)
                request.setDateFormatParserOption (dateFormat);

            int dateSeparator = properties_.getIndex (JDProperties.DATE_SEPARATOR);
            if (dateSeparator != -1)
                request.setDateSeparatorParserOption (dateSeparator);

            int timeFormat = properties_.getIndex (JDProperties.TIME_FORMAT);
            if (timeFormat != -1)
                request.setTimeFormatParserOption (timeFormat);

            int timeSeparator = properties_.getIndex (JDProperties.TIME_SEPARATOR);
            if (timeSeparator != -1)
                request.setTimeSeparatorParserOption (timeSeparator);

            int decimalSeparator = properties_.getIndex (JDProperties.DECIMAL_SEPARATOR);
            if (decimalSeparator != -1)
                request.setDecimalSeparatorParserOption (decimalSeparator);

            request.setNamingConventionParserOption (properties_.getIndex (JDProperties.NAMING));

               // Do not set the ignore decimal data error parser option.

               request.setCommitmentControlLevelParserOption (
                   transactionManager_.getCommitMode ());

            // If the system supports RLE data compression, then use it.               @ECA
            // Otherwise, use the old-style data compression.                          @ECA
            if (properties_.getBoolean(JDProperties.DATA_COMPRESSION)) {            // @ECA
                if (vrm_ >= AS400.generateVRM(5, 1, 0)) {                           // @ECA
                    dataCompression_ = DATA_COMPRESSION_RLE_;                       // @ECA
                    request.setDataCompressionOption(0);                            // @ECA
                    if (JDTrace.isTraceOn ())                                       // @ECA
                        JDTrace.logInformation (this, "Data compression = RLE");    // @ECA
                }                                                                   // @ECA
                else {                                                              // @ECA
                    dataCompression_ = DATA_COMPRESSION_OLD_;                       // @ECA
                    request.setDataCompressionOption(1);                            // @D3A @ECC
                    if (JDTrace.isTraceOn ())                                       // @ECA
                        JDTrace.logInformation (this, "Data compression = old");    // @ECA
                }                                                                   // @ECA
            }                                                                       // @ECA
            else {                                                                  // @ECA
                dataCompression_ = DATA_COMPRESSION_NONE_;                          // @ECA
                request.setDataCompressionOption(0);                                // @ECA
                if (JDTrace.isTraceOn ())                                           // @ECA
                    JDTrace.logInformation (this, "Data compression = none");       // @ECA
            }                                                                       // @ECA

            // Default schema.
               if (defaultSchema_ != null)
                    request.setDefaultSQLLibraryName (defaultSchema_, tempConverter);

            // There is no need to tell the server what our code
            // page is, nor is there any reason to get a translation
            // table back from the server at this point.  This
            // will be handled later by the Converter class.

            // I haven't found a good reason to set the ambiguous select
            // option.  ODBC sets it only when block criteria is "unless
            // FOR UPDATE OF", but it causes some problems for JDBC.
            // The difference is that ODBC has the luxury of setting cursor
            // concurrency.

               request.setPackageAddStatementAllowed (properties_.getBoolean (JDProperties.PACKAGE_ADD) ? 1 : 0);

            // If the server is at V4R4 or later, then set some more attributes.
            if (vrm_ >= AS400.generateVRM (4, 4, 0)) {                  // @D0C @E9C
                // @E9D || (FORCE_EXTENDED_FORMATS_)) {

                request.setUseExtendedFormatsIndicator (0xF1);

                // Although we publish a max lob threshold of 16777216,                   @E6A
                // the server can only handle 15728640.  We do it this                    @E6A
                // way to match ODBC.                                                     @E6A
                int lobThreshold = properties_.getInt (JDProperties.LOB_THRESHOLD);    // @E6A
                if (lobThreshold <= 0)                                                 // @E6A
                    request.setLOBFieldThreshold(0);                                   // @E6A
                else if (lobThreshold >= 15728640)                                     // @E6A
                    request.setLOBFieldThreshold(15728640);                            // @E6A
                else                                                                   // @E6A
                    request.setLOBFieldThreshold(lobThreshold);                        // @E6C

                    extendedFormats_ = true;
            }

            if (JDTrace.isTraceOn ()) {
                if (extendedFormats_)
                    JDTrace.logInformation (this, "Using extended datastreams");
                else
                    JDTrace.logInformation (this, "Using original datastreams");
            }

            // Send the request and process the reply.
               DBReplyRequestedDS reply = sendAndReceive (request);

               int errorClass = reply.getErrorClass();
               int returnCode = reply.getReturnCode();

               // Sort sequence attribute cannot be set.
               if ((errorClass == 7)
                    && ((returnCode == 301) || (returnCode == 303)))
                    postWarning (JDError.getSQLWarning (this, id_, errorClass, returnCode));

            // Language feature code id was not changed.   This is caused
            // when the secondary language can not be added to the library
            // list, and shows up as a PWS0003.
            else if ((errorClass == 7) && (returnCode == 304))
                postWarning (JDError.getSQLWarning (this, id_, errorClass, returnCode));

               // Other server errors.
               else if (errorClass != 0)
                    JDError.throwSQLException (this, id_, errorClass, returnCode);

               // Process the returned server attributes.
               DBReplyServerAttributes serverAttributes = reply.getServerAttributes ();

               // The CCSID that comes back is a mixed CCSID (i.e. mixed
               // SBCS and DBCS).  This will be the CCSID that all
               // non-graphic data will be returned as for this
               // connection, so we own the converter here.
            int serverCCSID = serverAttributes.getServerCCSID();
               converter_ = ConverterImplRemote.getConverter (serverCCSID, as400_);

            // Get the server functional level.  It comes back as in the                           @E7A
            // format VxRxMx9999.                                                                  @E7A
            String serverFunctionalLevelAsString = serverAttributes.getServerFunctionalLevel(converter_); // @E7A
            try {                                                                               // @E7A
                serverFunctionalLevel_ = Integer.parseInt(serverFunctionalLevelAsString.substring(6)); // @E7A
            }                                                                                   // @E7A
            catch(NumberFormatException e) {                                                    // @E7A
                serverFunctionalLevel_ = 0;                                                     // @E7A
            }                                                                                   // @E7A

            // Get the job number, but only if .                                                   @E8A
            if (serverFunctionalLevel_ >= 5)                                                    // @E8A
                serverJobIdentifier_ = serverAttributes.getServerJobIdentifier(converter_);     // @E8A

        if (JDTrace.isTraceOn ()) {                                         // @C2C
            int v = (vrm_ & 0xffff0000) >>> 16;                             // @D1A
            int r = (vrm_ & 0x0000ff00) >>>  8;                             // @D1A
            int m = (vrm_ & 0x000000ff);                                    // @D1A
            JDTrace.logInformation (this, "JDBC driver major version = "    // @C2A
                                    + AS400JDBCDriver.MAJOR_VERSION_);      // @C2A
            JDTrace.logInformation (this, "AS/400 VRM = V" + v              // @C2A
                                    + "R" + r + "M" + m);                   // @C2A
            JDTrace.logInformation (this, "Server CCSID = " + serverCCSID);
            JDTrace.logInformation(this, "Server functional level = "       // @E7A
                                   + serverFunctionalLevelAsString          // @E7A
                                   + " (" + serverFunctionalLevel_ + ")");  // @E7A

            StringBuffer buffer = new StringBuffer();                                           // @E8A
            if (serverJobIdentifier_ == null)                                                   // @E8A
                buffer.append("Not available");                                                 // @E8A
            else {                                                                              // @E8A
                buffer.append(serverJobIdentifier_.substring(20, 26).trim());  // job number    // @E8A
                buffer.append('/');                                                             // @E8A
                buffer.append(serverJobIdentifier_.substring(10, 20).trim());  // user name     // @E8A
                buffer.append('/');                                                             // @E8A
                buffer.append(serverJobIdentifier_.substring(0, 10).trim());   // job name      // @E8A
            }                                                                                   // @E8A
            JDTrace.logInformation(this, "Server job identifier = " + buffer);                  // @E8A
        }                                                                   // @C2A

            // @E2D // Wait to load graphic converter until it is needed.
               // @E2D graphicConverter_ = null;
            // @E2D graphicConverterLoaded_ = false;

            // Get the catalog name from the RDB entry.  If no RDB entry is
            // set on the system, then use the system name from the AS400 object
            // (which originally came from the URL).
               String rdbEntry = serverAttributes.getRelationalDBName (converter_).trim();
               if ((rdbEntry.length() > 0) && (! rdbEntry.equalsIgnoreCase ("*N")))
                   catalog_ = rdbEntry;

            // In the cases where defaults come from the server
            // job, get the defaults for properties that were not set.
            if (decimalSeparator == -1) {
                switch (serverAttributes.getDecimalSeparatorPO ()) {
                case 0:
                    properties_.setString (JDProperties.DECIMAL_SEPARATOR, JDProperties.DECIMAL_SEPARATOR_PERIOD);
                    break;
                case 1:
                    properties_.setString (JDProperties.DECIMAL_SEPARATOR, JDProperties.DECIMAL_SEPARATOR_COMMA);
                    break;
                }
            }

            if (dateFormat == -1) {
                switch (serverAttributes.getDateFormatPO ()) {
                case 0:
                    properties_.setString (JDProperties.DATE_FORMAT, JDProperties.DATE_FORMAT_JULIAN);
                    break;
                case 1:
                    properties_.setString (JDProperties.DATE_FORMAT, JDProperties.DATE_FORMAT_MDY);
                    break;
                case 2:
                    properties_.setString (JDProperties.DATE_FORMAT, JDProperties.DATE_FORMAT_DMY);
                    break;
                case 3:
                    properties_.setString (JDProperties.DATE_FORMAT, JDProperties.DATE_FORMAT_YMD);
                    break;
                case 4:
                    properties_.setString (JDProperties.DATE_FORMAT, JDProperties.DATE_FORMAT_USA);
                    break;
                case 5:
                    properties_.setString (JDProperties.DATE_FORMAT, JDProperties.DATE_FORMAT_ISO);
                    break;
                case 6:
                    properties_.setString (JDProperties.DATE_FORMAT, JDProperties.DATE_FORMAT_EUR);
                    break;
                case 7:
                    properties_.setString (JDProperties.DATE_FORMAT, JDProperties.DATE_FORMAT_JIS);
                    break;
                }
            }

            if (dateSeparator == -1) {
                switch (serverAttributes.getDateSeparatorPO ()) {
                case 0:
                    properties_.setString (JDProperties.DATE_SEPARATOR, JDProperties.DATE_SEPARATOR_SLASH);
                    break;
                case 1:
                    properties_.setString (JDProperties.DATE_SEPARATOR, JDProperties.DATE_SEPARATOR_DASH);
                    break;
                case 2:
                    properties_.setString (JDProperties.DATE_SEPARATOR, JDProperties.DATE_SEPARATOR_PERIOD);
                    break;
                case 3:
                    properties_.setString (JDProperties.DATE_SEPARATOR, JDProperties.DATE_SEPARATOR_COMMA);
                    break;
                case 4:
                    properties_.setString (JDProperties.DATE_SEPARATOR, JDProperties.DATE_SEPARATOR_SPACE);
                    break;
                }
            }

            if (timeFormat == -1) {
                switch (serverAttributes.getTimeFormatPO ()) {
                case 0:
                    properties_.setString (JDProperties.TIME_FORMAT, JDProperties.TIME_FORMAT_HMS);
                    break;
                case 1:
                    properties_.setString (JDProperties.TIME_FORMAT, JDProperties.TIME_FORMAT_USA);
                    break;
                case 2:
                    properties_.setString (JDProperties.TIME_FORMAT, JDProperties.TIME_FORMAT_ISO);
                    break;
                case 3:
                    properties_.setString (JDProperties.TIME_FORMAT, JDProperties.TIME_FORMAT_EUR);
                    break;
                case 4:
                    properties_.setString (JDProperties.TIME_FORMAT, JDProperties.TIME_FORMAT_JIS);
                    break;
                }
            }

            if (timeSeparator == -1) {
                switch (serverAttributes.getTimeSeparatorPO ()) {
                case 0:
                    properties_.setString (JDProperties.TIME_SEPARATOR, JDProperties.TIME_SEPARATOR_COLON);
                    break;
                case 1:
                    properties_.setString (JDProperties.TIME_SEPARATOR, JDProperties.TIME_SEPARATOR_PERIOD);
                    break;
                case 2:
                    properties_.setString (JDProperties.TIME_SEPARATOR, JDProperties.TIME_SEPARATOR_COMMA);
                    break;
                case 3:
                    properties_.setString (JDProperties.TIME_SEPARATOR, JDProperties.TIME_SEPARATOR_SPACE);
                    break;
                }
            }
          }
          catch (DBDataStreamException e) {
               JDError.throwSQLException (JDError.EXC_INTERNAL, e);
          }
        catch (IOException e) {
               JDError.throwSQLException (JDError.EXC_INTERNAL, e);
          }
     }



    //@A3A
    void setSystem (AS400 as400)
    throws SQLException // @EGA
    {
      as400PublicClassObj_    = as400;
    }



// @D2C
/**
Sets the transaction isolation level.  The transaction
isolation level cannot be changed while in the middle of
a transaction.

<p>JDBC and DB2/400 use different terminology for transaction
isolation levels.  The following table provides a terminology
mapping:

<p><table border>
<tr><th>AS/400 isolation level</th><th>JDBC transaction isolation level</th></tr>
<tr><td>*CHG</td> <td>TRANSACTION_READ_UNCOMMITTED</td></tr>
<tr><td>*CS</td>  <td>TRANSACTION_READ_COMMITTED</td></tr>
<tr><td>*ALL</td> <td>TRANSACTION_READ_REPEATABLE_READ</td></tr>
<tr><td>*RR</td>  <td>TRANSACTION_SERIALIZABLE</td></tr>
</table>

@param      level   The transaction isolation level.  Possible
                    values are:
                    <ul>
                    <li>TRANSACTION_READ_UNCOMMITTED
                    <li>TRANSACTION_READ_COMMITTED
                       <li>TRANSACTION_REPEATABLE_READ
                    <li>TRANSACTION_SERIALIZABLE
                    </ul>

@exception      SQLException    If the connection is not open,
                                the input level is not valid
                                or unsupported, or a transaction
                                is active.
**/
    public void setTransactionIsolation (int level)
      throws SQLException
    {
        checkOpen ();

          transactionManager_.setIsolation (level);

        if (JDTrace.isTraceOn())
            JDTrace.logProperty (this, "Transaction isolation", transactionManager_.getIsolation ());
    }



// JDBC 2.0
/**
Sets the type map to be used for distinct and structured
types.

<p>Note: Distinct types are supported by DB2 for OS/400, but
are not externalized by the AS/400 Toolbox for Java JDBC driver.
In other words, distinct types behave as if they are the underlying
type.  Structured types are not supported by DB2 for OS/400.
Consequently, this driver does not support the type map.

@param typeMap  The type map.

@exception  SQLException    This exception is always thrown.
**/
    public void setTypeMap (Map typeMap)
      throws SQLException
    {
        JDError.throwSQLException (JDError.EXC_FUNCTION_NOT_SUPPORTED);
    }



/**
Returns the connection's catalog name.  This is the
name of the server.

@return     The catalog name.
**/
    public String toString ()
    {
        return catalog_;
    }



/**
Indicates if the connection is using extended formats.

@return     true if the connection is using extended formats, false
            otherwise.
**/
    boolean useExtendedFormats ()
    throws SQLException // @EGA
    {
        return extendedFormats_;
    }


}