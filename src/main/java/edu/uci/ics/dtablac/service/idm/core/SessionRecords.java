package edu.uci.ics.dtablac.service.idm.core;

import edu.uci.ics.dtablac.service.idm.IDMService;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;
import edu.uci.ics.dtablac.service.idm.security.Session;
import org.glassfish.grizzly.http.util.TimeStamp;

import java.io.Serializable;
import java.sql.*;

import static java.lang.Math.abs;
import static java.sql.Timestamp.valueOf;

public class SessionRecords {

    // Query builders

    public static String queryUserFound(String email) {
        String SELECT = "SELECT COUNT(*) as Ex";
        String FROM = " FROM user";
        String WHERE = " WHERE email = \"" + email + "\";";

        return SELECT + FROM + WHERE;
    }

    public static String querySession(String email, String session_id) {
        String SELECT = "SELECT *";
        String FROM = " FROM session";
        String WHERE = " WHERE session_id = \""+session_id+"\" && email = \""+email+"\";";

        return SELECT + FROM + WHERE;
    }

    public static String queryUpdateStatus(String session_id, Integer status) {
        String UPDATE = "UPDATE session";
        String SET = " SET status = "+status;
        String WHERE = " WHERE session_id = \""+session_id+"\";";

        return UPDATE + SET + WHERE;
    }

    public static String queryUpdateLastUsed(String email, String session_id, String last) {
        String UPDATE = "UPDATE session";
        String SET = " SET last_used = \""+last+"\";";
        String WHERE = " WHERE email = \""+email+"session_id = \""+session_id+"\";";

        return UPDATE + SET + WHERE;
    }

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

    // Query executors

    public static boolean userExists(String email) {
        Integer exists = 0;
        try {
            String query = queryUserFound(email);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                exists = rs.getInt("Ex");
            }
         }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to search if user exists.");
            SQLE.printStackTrace();
        }
        return (exists > 0);
    }

    public static boolean sessionExists(String email, String session_id) {
        String exists = null;
        try {
            String query = querySession(email, session_id);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { // Very first item is null, so no session was found at all.
                return false;
            }
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to search if session exists.");
            SQLE.printStackTrace();
        }
        return true; // Session is found.
    }

    // TODO: Check ** box in be2 again. Will have to return to this.
    public static Integer sessionValidation(String email, String session_id) {
        Session temp = Session.createSession(email);
        int rc = 130;
        try {
            String query = querySession(email, session_id);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            Timestamp lastUsed = valueOf(rs.getString("last_used"));
            Timestamp exprTime = valueOf(rs.getString("expr_time"));
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            Timestamp timeoutTime = new Timestamp(lastUsed.getTime() + temp.SESSION_TIMEOUT);
            if (currentTime.after(timeoutTime)) {
                String updateQuery = queryUpdateStatus(session_id, 4);
                PreparedStatement psUpdate = IDMService.getCon().prepareStatement(updateQuery);
                psUpdate.executeUpdate();
                ServiceLogger.LOGGER.warning("TIMEOUT. SESSION REVOKED. REPEAT LOGIN.");
                rc = 133;
                return rc;
            }
            else if (currentTime.after(exprTime)) {
                String updateQuery = queryUpdateStatus(session_id, 3);
                PreparedStatement psUpdate = IDMService.getCon().prepareStatement(updateQuery);
                psUpdate.executeUpdate();
                ServiceLogger.LOGGER.warning("TIMEOUT. SESSION EXPIRED. REPEAT LOGIN.");
                rc = 131;
                return rc;
            }
            else if (abs(currentTime.getTime() - exprTime.getTime()) < timeoutTime.getTime()) {
                String updateQuery = queryUpdateStatus(session_id, 4);
                PreparedStatement psUpdate = IDMService.getCon().prepareStatement(updateQuery);
                psUpdate.executeUpdate();
                ServiceLogger.LOGGER.warning("CURRENT SESSION REVOKED. NEW SESSION MADE WITHOUT LOGIN.");
                addNewSession(temp.getSessionID().toString(), temp.getEmail(), 1,
                        temp.getTimeCreated(), temp.getLastUsed(), temp.getExprTime());
                rc = 130;
                return rc;
            }
            String updateLastUsedQuery = queryUpdateLastUsed(email, session_id, currentTime.toString());
            PreparedStatement psLast = IDMService.getCon().prepareStatement(updateLastUsedQuery);
            psLast.executeUpdate();
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to search for session.");
            SQLE.printStackTrace();
        }
        return rc;
    }

    // Maybe return resultCode in this function? Depending on status of session.
    public static Integer sessionStatus(String email, String session_id) {
        Integer rc = 134; // Session not found.
        try {
            String query = querySession(email, session_id);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int stat = rs.getInt("status");
                if (stat == 1) {
                    rc = 130; // Session is active.
                }
                else if (stat == 2) {
                    rc = 132; // Session is closed.
                }
                else if (stat == 3) {
                    rc = 131; // Session is expired.
                }
                else if (stat == 4) {
                    rc = 133; // Session is revoked.
                }
            }
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to search for session.");
            SQLE.printStackTrace();
        }
        ServiceLogger.LOGGER.info("RC: "+rc);
        return rc;
    }
}