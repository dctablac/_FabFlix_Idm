package edu.uci.ics.dtablac.service.idm.util;

import edu.uci.ics.dtablac.service.idm.core.SessionRecords;
import edu.uci.ics.dtablac.service.idm.models.SessionResponseModel;
import edu.uci.ics.dtablac.service.idm.security.Session;

import javax.ws.rs.core.Response;
import java.sql.PreparedStatement;

public class SessionUtility {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Response checkPayload(String email, String session_id) {
        SessionResponseModel responseModel = null;

        if (invalidTokenLength(session_id)) {
            responseModel = new SessionResponseModel(
                    -13, "Token has invalid length.", null);
            return Response.status(Response.Status.BAD_REQUEST).entity(responseModel).build();
        }
        else if (invalidEmailLength(email)) {
            responseModel = new SessionResponseModel(
                    -10, "Email address has invalid length.", null);
            return Response.status(Response.Status.BAD_REQUEST).entity(responseModel).build();
        }
        else if (invalidEmailFormat(email)) {
            responseModel = new SessionResponseModel(
                    -11, "Email address has invalid format.", null);
            return Response.status(Response.Status.BAD_REQUEST).entity(responseModel).build();
        }
        else if (userNotFound(email)) {
            responseModel = new SessionResponseModel(
                    14, "User not found.", null);
            return Response.status(Response.Status.OK).entity(responseModel).build();
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Response verifySession(String email, String session_id) {
        PreparedStatement ps = SessionRecords.buildQuerySession(email, session_id);

        SessionResponseModel responseModel = SessionRecords.sessionStatus(ps);

        Response response = Response.status(Response.Status.OK).header("email", email).header("session_id", session_id).entity(responseModel).build();

        return response;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean invalidEmailFormat(String email) {
        return !email.matches("[a-zA-Z0-9]+@[a-zA-Z0-9]+\\.[a-zA-Z0-9]+");
    }

    public static boolean invalidEmailLength(String email) {
        if (email == null) {
            return true;
        }
        if (email.equals("")) {
            return true;
        }
        if (email.length() > 50) {
            return true;
        }
        return false;
    }

    public static boolean invalidTokenLength(String session_id) {
        return session_id.length() != 128;
    }

    public static boolean userNotFound(String email) {
        return !SessionRecords.userExists(email);
    }
}
