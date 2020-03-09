package edu.uci.ics.dtablac.service.idm.core;

import edu.uci.ics.dtablac.service.idm.IDMService;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;
import edu.uci.ics.dtablac.service.idm.security.Crypto;
import edu.uci.ics.dtablac.service.idm.security.Session;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginRecords {

    // Query builders
    public static String queryEmail(String email) {
        String SELECT = "SELECT Count(*) as EmailFound";
        String FROM = " FROM user";
        String WHERE = " WHERE email = \"" + email + "\";";

        return SELECT + FROM + WHERE;
    }

    public static String querySaltAndPassword(String email) {
        String SELECT = "SELECT pword, salt";
        String FROM = " FROM user";
        String WHERE = " WHERE email = \"" + email + "\";";

        return SELECT + FROM + WHERE;
    }

    public static PreparedStatement queryActiveSession(String email) {
        String SELECT = "\nSELECT *\n";
        String FROM = "FROM session\n";
        String WHERE = "WHERE email = ? && status = 1;";

        String query = SELECT + FROM + WHERE;

        PreparedStatement ps = null;
        try {
            ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1, email);
        }
        catch (SQLException e) {
            ServiceLogger.LOGGER.warning("Unable to build query for an active session");
        }
        return ps;
    }

    public static String queryRevokeSession(String sesh_id) {
        String UPDATE = "UPDATE session";
        String SET = " SET status = 4";
        String WHERE = " WHERE session_id = \""+sesh_id+"\";";

        return UPDATE + SET + WHERE;
    }

    public static String queryInsertSession(Session S) {
        String INSERTINTO = "INSERT INTO session(session_id,email,status,time_created,last_used,expr_time)";
        String VALUES = String.format(" VALUES (\"%s\", \"%s\", %d, \"%s\", \"%s\", \"%s\");",
                S.getSessionID().toString(), S.getEmail(), 1, S.getTimeCreated().toString(),
                S.getLastUsed().toString(), S.getExprTime().toString());
        return INSERTINTO + VALUES;
    }

    //// Functions to insert/update session ////

    //   Get sesh ID
    public static String getSeshID(String email) {
        String ID = null;
        try {
            PreparedStatement ps = queryActiveSession(email);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                ID = rs.getString("session_id");
            }
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.info("Query failed. Unable to retrieve session ID.");
            SQLE.printStackTrace();
        }
        return ID;
    }

    //   Main function that inserts/update session on login.
    // ***** MAYBE ALTER "DELETING EXISTING SESSIONS" SECTION
    public static void handleSession(String email) {
        try {
            // Gather active sessions to the email address.
            PreparedStatement psActive = queryActiveSession(email);
            ResultSet rsActive = psActive.executeQuery();

            // Revoke existing and active sessions
            while(rsActive.next()) {
                if (rsActive.getInt("status") == 1) { // Active session revoked.
                    String revokeQuery = queryRevokeSession(rsActive.getString("session_id"));
                    PreparedStatement psRevoke = IDMService.getCon().prepareStatement(revokeQuery);
                    int rsDelete = psRevoke.executeUpdate();
                }
            }
            // Insert new session.
            Session S = Session.createSession(email);
            String insertSessionQuery = queryInsertSession(S);
            PreparedStatement psInsert = IDMService.getCon().prepareStatement(insertSessionQuery);
            int rsInsert = psInsert.executeUpdate();
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed: Unable to update session.");
            SQLE.printStackTrace();
        }

    }
    // Functions to determine validity
    public static boolean emailNotFound(String email) {
        Integer found = 0;
        try {
            String query = queryEmail(email);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);

            ServiceLogger.LOGGER.info("Trying query: " + ps.toString());
            ResultSet rs = ps.executeQuery();
            ServiceLogger.LOGGER.info("Query succeeded.");

            while (rs.next()) {
                found = rs.getInt("EmailFound");
            }
        } catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed: Unable to check if user exists.");
            SQLE.printStackTrace();
        }
        return found == 0;
    }

    public static boolean passwordsUnequal(String email, char[] password) {
        String storedPW = " ";
        String storedSalt = " ";
        try {
            String query = querySaltAndPassword(email);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);

            ServiceLogger.LOGGER.info("Trying query: " + ps.toString());
            ResultSet rs = ps.executeQuery();
            ServiceLogger.LOGGER.info("Query succeeded.");

            while (rs.next()) {
                storedPW = rs.getString("pword");
                storedSalt = rs.getString("salt");
            }
            byte[] salt = Hex.decodeHex(storedSalt);

            // Using stored salt, hash and salt the user-given password from login attempt.
            byte[] hashedGivenPW = Crypto.hashPassword(password, salt,
                    Crypto.ITERATIONS, Crypto.KEY_LENGTH);
            String encodedGivenPW = Hex.encodeHexString(hashedGivenPW);
            // Compared if the given and stored passwords match.
            if (encodedGivenPW.equals(storedPW)) {
                return false; // passwords do match
            }
        } catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed: Unable to check if password matches.");
        } catch (DecoderException DE) {
            ServiceLogger.LOGGER.warning("Decode failed: Unable to decode password in db.");
        }
        return true; // passwords do not match
    }
}