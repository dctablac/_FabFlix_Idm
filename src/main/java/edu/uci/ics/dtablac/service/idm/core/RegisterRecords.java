package edu.uci.ics.dtablac.service.idm.core;

import edu.uci.ics.dtablac.service.idm.IDMService;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;
import edu.uci.ics.dtablac.service.idm.security.Crypto;
import org.apache.commons.codec.binary.Hex;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterRecords {

    // Query builders
    public static String buildEmailValidationQuery(String email) {
        String SELECT = "SELECT COUNT(*) AS Total";
        String FROM   = " FROM user";
        String WHERE  = " WHERE 1=1 && email = \""+email+"\";";

        return SELECT + FROM + WHERE;
    }

    public static String buildRegisterQuery(String email, Integer status, Integer plevel,
                                            String salt, String pword) {
        String INSERTINTO = "INSERT INTO user (email, status, plevel, salt, pword)";
        String VALUES = String.format(" VALUES (\"%s\", %d, %d, \"%s\", \"%s\");", email,status,plevel,salt,pword);

        return INSERTINTO + VALUES;
    }

    // Encode password and salt
    public static String encodePassword(char[] password, byte[] salt) {
        byte[] hashedPassword = Crypto.hashPassword(password, salt, Crypto.ITERATIONS, Crypto.KEY_LENGTH);
        // ^ apply salt to password
        String encodedPassword = Hex.encodeHexString(hashedPassword); // salted and hashed password is encoded
        return encodedPassword;
    }

    public static String encodeSalt(byte[] salt) {
        String encodedSalt = Hex.encodeHexString(salt);
        return encodedSalt;
    }

    // Check if email is in database already.

    public static boolean emailExistsInDB(String email) {
        Integer count = 0;
        try {
            String query = buildEmailValidationQuery(email);
            ServiceLogger.LOGGER.info("Query: "+query);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);

            ServiceLogger.LOGGER.info("Trying query: " + ps.toString());
            ResultSet rs = ps.executeQuery();
            ServiceLogger.LOGGER.info("Query succeeded.");

            while (rs.next()) {
                count = rs.getInt("Total");
            }
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed: Unable to check if email exists.");
            SQLE.printStackTrace();
        }
        return count >= 1;
    }

    // Add new user to the database.

    public static void registerUser(String email, char[] pword) {
        byte[] salt = Crypto.genSalt();
        String encodedPassword = encodePassword(pword, salt);
        String encodedSalt = encodeSalt(salt);

        try {
            String query = buildRegisterQuery(email, 1, 5, encodedSalt, encodedPassword);
            ServiceLogger.LOGGER.info("Query: "+query);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);

            // Add new user to database
            ServiceLogger.LOGGER.info("Trying query: " + ps.toString());
            ps.executeUpdate();
            ServiceLogger.LOGGER.info("Query succeeded.");
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed: Unable to add user to database.");
            SQLE.printStackTrace();
        }
    }
}