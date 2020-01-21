package edu.uci.ics.dtablac.service.idm.core;

import edu.uci.ics.dtablac.service.idm.IDMService;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;
import edu.uci.ics.dtablac.service.idm.security.Session;
import org.glassfish.grizzly.http.util.TimeStamp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static java.lang.Math.abs;
import static java.sql.Timestamp.valueOf;

public class SessionRecords {

    // Query builders

    public static String queryUserFound(String email) {
        String SELECT = "SELECT Count(*) as Exists";
        String FROM = " FROM user";
        String WHERE = " WHERE email = \""+email+"\"";

        return SELECT + FROM + WHERE;
    }

    public static String querySession(String session_id) {
        String SELECT = "SELECT *";
        String FROM = " FROM session";
        String WHERE = " WHERE session_id = \""+session_id+"\";";

        return SELECT + FROM + WHERE;
    }

    public static String queryUpdateStatus(String session_id, Integer status) {
        String UPDATE = "UPDATE session";
        String SET = " SET status = "+status;
        String WHERE = " WHERE session_id = \""+session_id+"\";";

        return UPDATE + SET + WHERE;
    }

    // Query executors

    public static boolean userExists(String email) {
        Integer exists = 0;
        try {
            String query = queryUserFound(email);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                exists = rs.getInt("Exists");
            }
         }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed. Unable to search if user exists.");
            SQLE.printStackTrace();
        }
        return (exists != 0);
    }

    public static boolean sessionExists(String session_id) {
        String exists = null;
        try {
            String query = querySession(session_id);
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

    // Maybe return resultCode in this function? Depending on status of session.
    public static Integer sessionStatus(String email, String session_id) {
        int rc = 134; // Session not found.
        Session temp = Session.createSession(email); // Do not reference this session's token.
        try {                                        // Used to get EXPR_TIME and TIMEOUTS.
            String query = querySession(session_id);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int stat = rs.getInt("session_id");
                Timestamp lastUsed = valueOf(rs.getString("last_used"));
                Timestamp exprTime = valueOf(rs.getString("expr_time"));
                Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                Timestamp timeoutTime = new Timestamp(lastUsed.getTime()+temp.SESSION_TIMEOUT);

                if (currentTime.after(timeoutTime)) {
                    String updateQuery = queryUpdateStatus(session_id, 4);
                    PreparedStatement psUpdate = IDMService.getCon().prepareStatement(updateQuery);
                    psUpdate.executeUpdate();
                    rc = 133;
                }
                else if (currentTime.after(exprTime)) {
                    String updateQuery = queryUpdateStatus(session_id, 3);
                    PreparedStatement psUpdate = IDMService.getCon().prepareStatement(updateQuery);
                    psUpdate.executeUpdate();
                    rc = 131;
                }
                else if (abs(currentTime.getTime()-exprTime.getTime()) < timeoutTime.getTime()) {
                    String updateQuery = queryUpdateStatus(session_id, 4);
                    PreparedStatement psUpdate = IDMService.getCon().prepareStatement(updateQuery);
                    psUpdate.executeUpdate();
                    rc = 133;
                }
                else if (stat == 1) {
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
        return rc;
    }
}