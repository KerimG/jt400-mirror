///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                                 
//                                                                             
// Filename: Statement.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2001 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package java.sql;

/**
 *  The object used for executing a static SQL statement and obtaining the results produced by it. 
 *
 *  Only one ResultSet object per Statement object can be open at any point in time. Therefore, if the reading of
 *  one ResultSet object is interleaved with the reading of another, each must have been generated by different
 *  Statement objects. All statement execute methods implicitly close a statment's current ResultSet object if an
 *  open one exists. 
 *
 *  This class contains the smallest useful set of methods and data from java.sql.Statement
 **/
public interface Statement
{
    /**
     *  Releases this Statement object's database and JDBC resources immediately instead of waiting for this to
     *  happen when it is automatically closed. It is generally good practice to release resources as soon as you
     *  are finished with them to avoid tying up database resources. 
     *
     *  <b>Note:</b> A Statement object is automatically closed when it is garbage collected. When a Statement
     *  object is closed, its current ResultSet object, if one exists, is also closed.
     *
     *  @exception SQLException if a database access error occurs.
     **/
    void close() throws SQLException;

    /**
     *  Executes an SQL statement that may return multiple results. Under some (uncommon) situations a single
     *  SQL statement may return multiple result sets and/or update counts. Normally you can ignore this unless
     *  you are (1) executing a stored procedure that you know may return multiple results or (2) you are
     *  dynamically executing an unknown SQL string. The methods execute, getMoreResults,
     *  getResultSet, and getUpdateCount let you navigate through multiple results. The execute method
     *  executes an SQL statement and indicates the form of the first result. You can then use the methods
     *  getResultSet or getUpdateCount to retrieve the result, and getMoreResults to move to any
     *  subsequent result(s).
     *
     *  @param sql any SQL statement.
     *
     *  @return true if the next result is a ResultSet object; false if it is an update count or there are no more results.
     *
     *  @exception SQLException if a database access error occurs.
     **/
    boolean execute(String sql) throws SQLException;    

    /**
     *  Executes an SQL statement that returns a single ResultSet object.
     *
     *  @param sql typically this is a static SQL SELECT statement.
     *
     *  @return a ResultSet object that contains the data produced by the given query; never null.
     *
     *  @exception SQLException if a database access error occurs.
     **/
    ResultSet executeQuery(String sql) throws SQLException;

    /**
     *  Executes an SQL INSERT, UPDATE or DELETE statement. In addition, SQL statements that return nothing,
     *  such as SQL DDL statements, can be executed.
     *
     *  @param sql an SQL INSERT, UPDATE or DELETE statement or an SQL statement that returns nothing.
     *
     *  @return either the row count for INSERT, UPDATE or DELETE statements, or 0 for SQL statements that return nothing.
     *
     *  @exception SQLException if a database access error occurs.
     **/
    int executeUpdate(String sql) throws SQLException;

    /**
     *  Returns the Connection object that produced this Statement object.
     *
     *  @return the connection that produced this statement.
     *
     *  @exception SQLException if a database access error occurs.
     **/
    Connection getConnection()  throws SQLException;

    /**
     *  Returns the current result as a ResultSet object. This method should be called only once per result.
     *
     *  @return the current result as a ResultSet object; null if the result is an update count or there are no more results.
     *
     *  @exception SQLException if a database access error occurs.
     **/
    ResultSet getResultSet() throws SQLException;

    /**
     *  Retrieves the result set concurrency for ResultSet objects generated by this Statement object.
     *
     *  @return either ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE.
     *
     *  @exception SQLException if a database access error occurs.
     **/
    int getResultSetConcurrency() throws SQLException;

    /**
     *  Retrieves the result set type for ResultSet objects generated by this Statement object.
     *
     *  @return one of ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, or ResultSet.TYPE_SCROLL_SENSITIVE.
     *
     *  @exception SQLException if a database access error occurs.
     **/
    int getResultSetType()  throws SQLException;

    /**
     *  Returns the current result as an update count; if the result is a ResultSet object or there are no more
     *  results, -1 is returned. This method should be called only once per result.
     *
     *  @return the current result as an update count; -1 if the current result is a ResultSet object or there are no more results.
     *
     *  @exception SQLException if a database access error occurs.
     **/
    int getUpdateCount() throws SQLException;
}   
