package edu.uci.ics.dtablac.service.idm.core;

import edu.uci.ics.dtablac.service.idm.IDMService;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrivilegeRecords {

    // Query builders

    public static String queryUser(String email) {
        String SELECT = "SELECT Count(*) as EmailFound";
        String FROM = " FROM user";
        String WHERE = " WHERE email = \"" + email + "\";";

        return SELECT + FROM + WHERE;
    }

    public static String queryPLevel(String email) {
        String SELECT = "SELECT plevel";
        String FROM = " FROM user";
        String WHERE = " WHERE email = \"" + email + "\";";

        return SELECT + FROM + WHERE;
    }

    // Query executors

    public static boolean emailNotFound(String email) {
        Integer found = 0;
        try {
            String query = queryUser(email);
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

    // Returns true if stored plevel is <= required plevel.
    public static boolean sufficientPrivilege(String email, Integer plevel) {
        Integer p = 6;
        try {
            String query = queryPLevel(email);
            PreparedStatement ps = IDMService.getCon().prepareStatement(query);

            ServiceLogger.LOGGER.info("Trying query: " + ps.toString());
            ResultSet rs = ps.executeQuery();
            ServiceLogger.LOGGER.info("Query succeeded.");

            while (rs.next()) {
                p = rs.getInt("plevel");
            }
        }
        catch (SQLException SQLE) {
            ServiceLogger.LOGGER.warning("Query failed: Unable to check user's privilege level.");
            SQLE.printStackTrace();
        }
        return p <= plevel;
    }
}
