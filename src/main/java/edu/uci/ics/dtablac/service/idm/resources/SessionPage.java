package edu.uci.ics.dtablac.service.idm.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uci.ics.dtablac.service.idm.core.SessionRecords;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;
import edu.uci.ics.dtablac.service.idm.models.SessionRequestModel;
import edu.uci.ics.dtablac.service.idm.models.SessionResponseModel;
import edu.uci.ics.dtablac.service.idm.security.Session;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

@Path("session")
public class SessionPage {
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sessionHandler(@Context HttpHeaders headers, String jsonText) {
        SessionRequestModel requestModel;
        SessionResponseModel responseModel;
        ObjectMapper mapper = new ObjectMapper();
        int resultCode = -1;
        String sesh_id = " ";

        try {
            requestModel = mapper.readValue(jsonText, SessionRequestModel.class);
            ServiceLogger.LOGGER.info("Received [POST] login request.");
            ServiceLogger.LOGGER.info("Request:\n" + jsonText);

            String email = requestModel.getEMAIL();
            String session_id = requestModel.getSESSION_ID();
            sesh_id = session_id;

            if (invalidEmailLength(email)) {
                resultCode= -10;
                responseModel = new SessionResponseModel(resultCode,
                        "Email address has invalid length.", null);
                ServiceLogger.LOGGER.info("Email address has invalid length.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            else if (invalidEmailFormat(email)) {
                resultCode = -11;
                responseModel = new SessionResponseModel(resultCode,
                        "Email address has invalid format.", null);
                ServiceLogger.LOGGER.info("Email address has invalid format.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            else if (invalidTokenLength(session_id)) {
                resultCode = -13;
                responseModel = new SessionResponseModel(resultCode,
                        "Token has invalid length.", null);
                ServiceLogger.LOGGER.info("Token has invalid length.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            else if (userNotFound(email)) {
                resultCode = 14;
                responseModel = new SessionResponseModel(resultCode,
                        "User not found.", null);
                ServiceLogger.LOGGER.info("User not found.");
                return Response.status(Status.OK).entity(responseModel).build();
            }
            resultCode = getSessionStatus(email, session_id);
            if (resultCode == 130) {
                resultCode = SessionRecords.sessionValidation(email, session_id);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            if (e instanceof JsonParseException) {
                resultCode = -3;
                responseModel = new SessionResponseModel(resultCode, "JSON Parse Exception.", null);
                ServiceLogger.LOGGER.warning("JSON Parse Exception.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            else if (e instanceof JsonMappingException) {
                resultCode = -2;
                responseModel = new SessionResponseModel(resultCode, "JSON Mapping Exception.", null);
                ServiceLogger.LOGGER.warning("JSON Mapping Exception.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
        }
        if (resultCode == 131) {
            responseModel = new SessionResponseModel(resultCode, "Session is expired.", null);
            return Response.status(Status.OK).entity(responseModel).build();
        }
        else if (resultCode == 132) {
            responseModel = new SessionResponseModel(resultCode, "Session is closed.", null);
            return Response.status(Status.OK).entity(responseModel).build();
        }
        else if (resultCode == 133) {
            responseModel = new SessionResponseModel(resultCode, "Session is revoked.", null);
            return Response.status(Status.OK).entity(responseModel).build();
        }

        responseModel = new SessionResponseModel(resultCode, "Session is active.", sesh_id);
        return Response.status(Status.OK).entity(responseModel).build();
    }

    boolean invalidEmailFormat(String email) {
        return !email.matches("[a-zA-Z0-9]+@[a-zA-Z0-9]+\\.[a-zA-Z0-9]+");
    }

    boolean invalidEmailLength(String email) {
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

    boolean invalidTokenLength(String session_id) {
        return session_id.length() != 128;
    }

    boolean userNotFound(String email) {
        return !SessionRecords.userExists(email);
    }

    Integer getSessionStatus(String email, String session_id) {
        return SessionRecords.sessionStatus(email, session_id);
    }
}
