package edu.uci.ics.dtablac.service.idm.resources;


// Authenticates a user with existing credentials.
// If the provided password matches the stored password for the provided
//   email, generates a new session to associate with provided email and
//   returns a session_id token. If there are existing and active sessions
//   for the provided email, they should be revoked and replaced with a
//   new session.
// Inserts/updates records in the "session" table.

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uci.ics.dtablac.service.idm.core.LoginRecords;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;
import edu.uci.ics.dtablac.service.idm.models.LoginRequestModel;
import edu.uci.ics.dtablac.service.idm.models.LoginResponseModel;

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

@Path("login")
public class LoginPage {
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response loginUser(@Context HttpHeaders headers, String jsonText) {
        LoginRequestModel requestModel;
        LoginResponseModel responseModel;
        ObjectMapper mapper = new ObjectMapper();
        int resultCode = -1;
        String sesh_id = " ";

        try {
            requestModel = mapper.readValue(jsonText, LoginRequestModel.class);
            ServiceLogger.LOGGER.info("Received [POST] login request.");
            ServiceLogger.LOGGER.info("Request:\n" + jsonText);

            String email = requestModel.getEMAIL();
            char[] password = requestModel.getPASSWORD();

            if (invalidEmailLength(email)) {
                resultCode = -10;
                responseModel = new LoginResponseModel(resultCode,
                        "Email address has invalid length.", null);
                ServiceLogger.LOGGER.warning("Email address has invalid length.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            else if (invalidPasswordLength(password)) {
                resultCode = -12;
                responseModel = new LoginResponseModel(resultCode,
                        "Password has invalid length.", null);
                ServiceLogger.LOGGER.warning("Password has invalid length.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            else if (invalidEmailFormat(email)) {
                resultCode = -11;
                responseModel = new LoginResponseModel(resultCode,
                        "Email address has invalid format.", null);
                ServiceLogger.LOGGER.warning("Email address has invalid format.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            else if (userNotFound(email)) {
                resultCode = 14;
                responseModel = new LoginResponseModel(resultCode,
                        "User not found.", null);
                ServiceLogger.LOGGER.info("User not found.");
                return Response.status(Status.OK).entity(responseModel).build();
            }
            else if (passwordsDoNotMatch(email, password)) {
                resultCode = 11;
                responseModel = new LoginResponseModel(resultCode,
                        "Passwords do not match.", null);
                ServiceLogger.LOGGER.info("Passwords do not match.");
                return Response.status(Status.OK).entity(responseModel).build();
            }
            // Create new session and add to table.
            LoginRecords.handleSession(email);
            // Update session ID in JSON response
            sesh_id = LoginRecords.getSeshID(email);
        }
        catch (IOException e) {
            e.printStackTrace();
            if (e instanceof JsonParseException) {
                resultCode = -3;
                responseModel = new LoginResponseModel(resultCode,
                        "Unable to map JSON to POJO.", null);
                ServiceLogger.LOGGER.warning("JSON Parse exception.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            } else if (e instanceof JsonMappingException) {
                resultCode = -2;
                responseModel = new LoginResponseModel(resultCode,
                        "Unable to map JSON to POJO.", null);
                ServiceLogger.LOGGER.warning("JSON Mapping exception.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
        }
        resultCode = 120;
        responseModel = new LoginResponseModel(resultCode,
                "User logged in successfully.", sesh_id);
        ServiceLogger.LOGGER.info("User logged in successfully.");
        return Response.status(Status.OK).entity(responseModel).build();
    }

    boolean invalidPasswordLength(char[] password) {
        if (password == null) {
            return true;
        }
        if ((password.length < 7) || (password.length > 16)) {
            return true;
        }
        return false;
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

    boolean passwordsDoNotMatch(String email, char[] password) {
        return LoginRecords.passwordsUnequal(email, password);
    }

    boolean userNotFound(String email) {
        return LoginRecords.emailNotFound(email);
    }
}