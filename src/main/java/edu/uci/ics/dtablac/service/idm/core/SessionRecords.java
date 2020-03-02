package edu.uci.ics.dtablac.service.idm.core;

import edu.uci.ics.dtablac.service.idm.IDMService;
import edu.uci.ics.dtablac.service.idm.configs.ServiceConfigs;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;
import edu.uci.ics.dtablac.service.idm.models.SessionResponseModel;
import edu.uci.ics.dtablac.service.idm.security.Session;
import org.glassfish.grizzly.http.util.TimeStamp;
import org.yaml.snakeyaml.events.Event;

import java.io.Serializable;
import java.sql.*;

import static java.lang.Math.abs;
import static java.sql.Timestamp.valueOf;

public class SessionRecords {

    // Query builders ////////////////////////////////////////////////////////////////////////////////////////////////

    public static PreparedStatement buildQueryUserFound(String email) {
        String SELECT = "\nSELECT *\n";
        String FROM = "FROM user\n";
        String WHERE = "WHERE email = ?;";

        String query = SELECT + FROM + WHERE;

        PreparedStatement ps = null;
        try {
            ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1, email);
        }
        catch (SQLException e) {
            e.printStackTrace();
            ServiceLogger.LOGGER.warning("Unable to build query to find a user.");
        }

        return ps;
    }

    public static PreparedStatement buildQuerySession(String email, String session_id) {
        String SELECT = "SELECT *\n";
        String FROM = "FROM session\n";
        String WHERE = "WHERE session_id = ? && email = ?;";

        String query = SELECT + FROM + WHERE;
        
        PreparedStatement ps = null;
        try {
            ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1, session_id);
            ps.setString(2, email);
        }
        catch (SQLException e) {
            e.printStackTrace();
            ServiceLogger.LOGGER.warning("Unable to build query to get the session");
        }
        return ps;
    }

    public static PreparedStatement buildQueryUpdateStatus(String session_id, Integer status) {
        String UPDATE = "\nUPDATE session\n";
        String SET = "SET status = ?\n";
        String WHERE = " WHERE session_id = ?;";

        String query = UPDATE + SET + WHERE;

        PreparedStatement ps = null;
        try {
            ps = IDMService.getCon().prepareStatement(query);
            ps.setInt(1, status);
            ps.setString(2, session_id);
        }
        catch (SQLException e) {
            e.printStackTrace();
            ServiceLogger.LOGGER.warning("Unable to build query that updates the session's status.");
        }
        return ps;
    }

    public static PreparedStatement buildQueryUpdateLastUsed(String email, String session_id, String last) {
        //String UPDATE = "UPDATE session";
        //String SET = " SET last_used = \""+last+"\"";
        //String WHERE = " WHERE email = \""+email+"session_id = \""+session_id+"\";";
        String UPDATE = "\nUPDATE session\n";
        String SET = "SET last_used = ?\n";
        String WHERE = "WHERE email = ? && session_id = ?;";

        String query = UPDATE + SET + WHERE;

        PreparedStatement ps = null;
        try {
            ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1, last);
            ps.setString(2, email);
            ps.setString(3, session_id);
        }
        catch (SQLException e) {
            e.printStackTrace();
            ServiceLogger.LOGGER.warning("Unable to build query to update session's last_used field.");
        }
        return ps;
    }

    // Query executors ////////////////////////////////////////////////////////////////////////////////////////////////

    public static void addNewSession(String email, String session_id,
                                            Integer status, Timestamp time_created,
                                            Timestamp last_used, Timestamp expr_time) {
        try {
            String INSERTINTO = "INSERT INTO session(session_id, email, status, time_created, last_used, expr_time)";
            String VALUES = " VALUES (?,?,?,?,?,?)";

            String query = INSERTINTO + VALUES;

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1, email);
            ps.setString(2, session_id);
            ps.setInt(3, status);
            ps.setTimestamp(4, time_created);
            ps.setTimestamp(5, last_used);
            ps.setTimestamp(6, expr_time);

            ServiceLogger.LOGGER.info("Trying query: "+ps.toString());
            ps.executeUpdate();
            ServiceLogger.LOGGER.info("Query succeeded.");
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to create a new session.");
            SQLE.printStackTrace();
        }
    }

    public static boolean userExists(String email) {
        try {
            PreparedStatement ps = buildQueryUserFound(email);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return false; // user DNE
            }
         }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to search if user exists.");
            SQLE.printStackTrace();
        }
        return true; // user exists
    }

    public static boolean sessionExists(String email, String session_id) {
        try {
            PreparedStatement ps = buildQuerySession(email, session_id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { // Very first item is null, so session DNE
                return false;
            }
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to search if session exists.");
            SQLE.printStackTrace();
        }
        return true; // session exists
    }

    // TODO: Check ** box in be2 again. Will have to return to this.
    public static String sessionValidation(String email, String session_id) {
        // Get config timeout and expiration
        long timeout = IDMService.getServiceConfigs().getTimeout();
        long expiration = IDMService.getServiceConfigs().getExpiration();

        Session temp = Session.createSession(email);
        int rc = 130;
        try {
            PreparedStatement ps = buildQuerySession(email, session_id);
            ResultSet rs = ps.executeQuery();
            rs.next();

            int status = rs.getInt("status");

            Timestamp lastUsed = valueOf(rs.getString("last_used"));
            Timestamp exprTime = valueOf(rs.getString("expr_time"));
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            Timestamp timeoutTime = new Timestamp(lastUsed.getTime() + timeout);

            PreparedStatement psLast = buildQueryUpdateLastUsed(email, session_id, currentTime.toString());
            psLast.executeUpdate();

            if (((currentTime.getTime() - exprTime.getTime()) > timeout) && (status != 2 && status != 3)) {
                PreparedStatement psUpdate = buildQueryUpdateStatus(session_id, 4);
                psUpdate.executeUpdate();
                ServiceLogger.LOGGER.warning("TIMEOUT. SESSION REVOKED. REPEAT LOGIN.");
                return session_id;
            }
            else if ((currentTime.after(exprTime)) && (status != 2 && status != 3)) {
                PreparedStatement psUpdate = buildQueryUpdateStatus(session_id, 3);
                psUpdate.executeUpdate();
                ServiceLogger.LOGGER.warning("TIMEOUT. SESSION EXPIRED. REPEAT LOGIN.");
                return session_id;
            }
            else if (((currentTime.getTime() - exprTime.getTime()) < timeout) && (status != 2 && status != 3)) {
                PreparedStatement psUpdate = buildQueryUpdateStatus(session_id, 4);
                psUpdate.executeUpdate();
                ServiceLogger.LOGGER.warning("CURRENT SESSION REVOKED. NEW SESSION MADE WITHOUT LOGIN.");
                addNewSession(temp.getSessionID().toString(), temp.getEmail(), 1,
                        temp.getTimeCreated(), temp.getLastUsed(), temp.getExprTime());
                return temp.getSessionID().toString();
            }
            
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to search for session.");
            SQLE.printStackTrace();
        }
        return session_id;
    }

    // Maybe return resultCode in this function? Depending on status of session.
    public static SessionResponseModel sessionStatus(PreparedStatement ps) {
        Integer rc = 134; // Session not found.
        String message = "Session not found.";
        String session_id = null;
        try {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int stat = rs.getInt("status");
                if (stat == 1) {
                    rc = 130; // Session is active.
                    message = "Session is active.";
                    session_id = rs.getString("session_id");
                }
                else if (stat == 2) {
                    rc = 132; // Session is closed.
                    message = "Session is closed.";
                }
                else if (stat == 3) {
                    rc = 131; // Session is expired.
                    message = "Session is expired.";
                }
                else if (stat == 4) {
                    rc = 133; // Session is revoked.
                    message = "Session is revoked.";
                }
            }
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to search for session.");
            SQLE.printStackTrace();
        }
        return new SessionResponseModel(rc, message, session_id);
    }
}