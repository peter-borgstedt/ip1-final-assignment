package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import datastructures.Record;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * Basic implementation for connecting to an PostgreSQL database and execute
 * queries and manipulations (DQL and DML).
 * 
 * Using prepared statement as this will be faster for continuous inserts and
 * more important its prevents sql injection.
 *
 * References:
 * https://www.geeksforgeeks.org/sql-ddl-dql-dml-dcl-tcl-commands
 * https://jdbc.postgresql.org/documentation/81/load.html
 * https://dev.mysql.com/doc/connector-j/5.0/en/connector-j-usagenotes-connect-drivermanager.html
 * https://stackoverflow.com/questions/8263371/how-can-prepared-statements-protect-from-sql-injection-attacks/8265319
 * 
 * https://examples.javacodegeeks.com/enterprise-java/tomcat/tomcat-connection-pool-configuration-example/
 * https://www.journaldev.com/2513/tomcat-datasource-jndi-example-java
 * https://www.arquitecturajava.com/tomcat-java-datasource-y-resource/
 * https://www.codejava.net/servers/tomcat/configuring-jndi-datasource-for-database-connection-pooling-in-tomcat
 * https://stackoverflow.com/questions/3485177/how-to-configure-tomcat-to-connect-with-mysql
 * https://tomcat.apache.org/tomcat-7.0-doc/jndi-resources-howto.html
 * 
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class Db {
  private static final Logger log = LogManager.getLogger(Db.class);

  /** A configured db connection pool */
  private static DataSource psqlDataSource = Db.getDataSource("jdbc/db");
  private String url;

  /**
   * Constructor.
   */
  public Db() {
    var host = System.getProperty("DB_HOST");
    var port = System.getProperty("DB_PORT");
    var db = System.getProperty("DB_DB");
    var username = System.getProperty("DB_USER");
    var password = System.getProperty("DB_PASSWORD");
    this.url = String.format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s", host, port, db, username, password);
  }

  /**
   * Get configured data source.
   * @param name Name of data source
   * @return data source
   */
  private static DataSource getDataSource(String name) {
    try {
      return (DataSource)new InitialContext().lookup("java:comp/env/" + name);
    } catch (NamingException e) {
      return null;
    }
  } 

  /**
   * Extracts records from the result using the containing metadata.
   * @param rs The result set with records and metadata
   * @return a list with mapped data representing a database row
   */
  protected static List<Record> extractRecords(ResultSet rs) throws SQLException {
    var records = new ArrayList<Record>();
    while (rs.next()) {
      var md = rs.getMetaData();
      var record = new HashMap<String, Object>();
      for (int i = 0; i < md.getColumnCount(); i++) {
        var column = md.getColumnName(i + 1);
        var value = rs.getObject(i + 1);
        record.put(column, value);
      }
      records.add(new Record(record));
    }
    return records;
  }

  /**
   * Retrieves a connection from the pool.
   */
  private Connection connect() throws ReflectiveOperationException, SQLException {
    log.info(String.format("Connecting to PostgreSQL (%s)", url));

    // This should not happen, but if the pool does not work, which it does not do in Heroku when
    // first attempting to use that service (because it was free), however later changed to AWS EC2
    // and this is not a problem any more (together with a lot of other issue Heroku had -- being super slow),
    // neither is this a problem when running locally, however this is kept for fall back mechanism if something goes wrong
    if (Db.psqlDataSource == null) {
      org.postgresql.Driver.class.getDeclaredConstructor().newInstance();
      return DriverManager.getConnection(url);
    }
    return Db.psqlDataSource.getConnection();
  }

  /**
   * Execute query (DQL)
   * @param query Query to be run
   * @param parameters Parameters used in query
   * @return result from query
   * @throws ReflectiveOperationException
   */
  @SafeVarargs
  public final <T extends Object> List<Record> executeQuery(String query, T... parameters)
  throws SQLException, ReflectiveOperationException {
    log(query, parameters);

    try (
      var connection = this.connect();
      var statement = connection.prepareStatement(query);
    ) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setObject(i + 1, parameters[i]);
      }
      var rs = statement.executeQuery();
      return extractRecords(rs);
    }
  }

  /**
   * Execute update (DDL).
   * 
   * @param query Query to be run
   * @param parameters Parameters used in query
   * @return
   * @throws ReflectiveOperationException
   */
  @SafeVarargs
  public final <T extends Object> int executeUpdate(String query, T... parameters)
  throws SQLException, ReflectiveOperationException {
    log(query, parameters);

    try (
      var connection = this.connect();
      var statement = connection.prepareStatement(query);
    ) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setObject(i + 1, parameters[i]);
      }
      return statement.executeUpdate();
    }
  }

  /**
   * Log query and parameters for debugging.
   * @param query Query to be run
   * @param parameters Parameters used in query
   */
  @SafeVarargs
  private final <T extends Object> void log(String query, T ...parameters) {
    log.debug(String.format("SQL -> STATEMENT: %s", query));
    log.debug(String.format("SQL -> PARAMETERS: %s", Arrays.asList(parameters)
      .stream()
      .map(value -> value == null ? "null" : value.toString())
      .collect(Collectors.joining(", "))));

    // Log out a processed query when parameters and query has been merged,
    // for easier locate errors in faulty queries
    var parameterCount = ((Long)query.chars().filter(c -> (char)c == '?').count()).intValue();
    log.debug(String.format("Parameter count: %s", parameterCount));

    var params = new Object[Math.max(parameterCount, parameters.length)];
    Arrays.fill(params, "null"); // Default to null for parameters missing

    // System.arraycopy(parameters, 0, params, 0, parameters.length);

    for (int i = 0; i < parameters.length; i++) {
      var parameter = parameters[i];
      if (parameter != null) {
        params[i] = parameters[i];
      }
    }

    var queryOutput = query;
    for (var parameter : params) {
      queryOutput = queryOutput.replaceFirst("\\?", parameter.toString());
    }
    log.debug(String.format("SQL -> PROCESSED: %s", queryOutput));
  }
}
