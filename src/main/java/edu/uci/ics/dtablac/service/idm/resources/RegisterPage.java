package edu.uci.ics.dtablac.service.idm.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;
import edu.uci.ics.dtablac.service.idm.models.RegisterRequestModel;
import edu.uci.ics.dtablac.service.idm.models.RegisterResponseModel;

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
import java.util.Arrays;

import edu.uci.ics.dtablac.service.idm.core.RegisterRecords;

// Allows guest to self-register with web application.
// All user-supplied information must be check for correctness.
// All self-registered users default to plevel "user."
//
// EMAIL: - Must be in form <email>@<domain>.<extension>
//        - Alphanumeric only
// PASSWORD: - Must be at least 7 alphanumeric chars long and no more than 16 chars long
//           - Must contain at least one uppercase alpha, one lowercase alpha, at least one numeric
//           - ** Must be stored as char array, not string **

@Path("register")
public class RegisterPage {
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(@Context HttpHeaders headers, String jsonText) {
        RegisterRequestModel requestModel;
        RegisterResponseModel responseModel;
        ObjectMapper mapper = new ObjectMapper(); // Parse JSON text
        int resultCode;

        try {
            requestModel = mapper.readValue(jsonText, RegisterRequestModel.class);
            ServiceLogger.LOGGER.info("Received [POST] register request.");
            ServiceLogger.LOGGER.info("Request:\n" + jsonText);

            String EMAIL = requestModel.getEMAIL();
            char[] PASSWORD = requestModel.getPASSWORD();


            if (invalidPasswordLength(PASSWORD)) {
                resultCode = -12;
                responseModel = new RegisterResponseModel(resultCode, "Password has invalid length.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            if (invalidEmailLength(EMAIL)) {
                resultCode = -10;
                responseModel = new RegisterResponseModel(resultCode, "Email address had invalid length.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            if (incorrectEmailFormat(EMAIL)) {
                resultCode = -11;
                responseModel = new RegisterResponseModel(resultCode,"Email address has invalid format.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            if (passwordTooShort(PASSWORD) || passwordTooLong(PASSWORD)) {
                resultCode = 12;
                responseModel = new RegisterResponseModel(resultCode,
                                                       "Password does not meet length requirements.");
                return Response.status(Status.OK).entity(responseModel).build();
            }
            if (incorrectPasswordFormat(PASSWORD)) {
                resultCode = 13;
                responseModel = new RegisterResponseModel(resultCode,
                                                       "Password does not meet character requirements.");
                return Response.status(Status.OK).entity(responseModel).build();
            }
            if (RegisterRecords.emailExistsInDB(EMAIL)) {
                resultCode = 16;
                responseModel = new RegisterResponseModel(resultCode, "Email already in use.");
                return Response.status(Status.OK).entity(responseModel).build();
            }
            RegisterRecords.registerUser(EMAIL,PASSWORD);
        }
        catch (IOException e) {
            e.printStackTrace();
            if (e instanceof JsonParseException) {
                resultCode = -3;
                responseModel = new RegisterResponseModel(resultCode, "JSON Parse Exception.");
                ServiceLogger.LOGGER.warning("Unable to map JSON to POJO.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            } else if (e instanceof JsonMappingException) {
                resultCode = -2;
                responseModel = new RegisterResponseModel(resultCode, "JSON Mapping exception.");
                ServiceLogger.LOGGER.warning("Unable to map JSON to POJO.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
        }
        // Email and password are valid and are new to the database.
        responseModel = new RegisterResponseModel(110, "User registered successfully.");
        return Response.status(Status.OK).entity(responseModel).build();
    }

    boolean incorrectEmailFormat(String email) {
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

    boolean passwordTooLong(char[] password) {
        return password.length > 16;
    }

    boolean passwordTooShort(char[] password) {
        return password.length < 7;
    }

    // check if null or empty password
    boolean invalidPasswordLength(char[] password) {
        if (password == null) {
            return true;
        }
        if (password.length == 0) {
            return true;
        }
        return false;
    }

    boolean incorrectPasswordFormat(char[] password) {
        boolean oneUpper = false;
        boolean oneLower = false;
        boolean oneNumeric = false;
        for (char c:password) {
            String Cstr = Character.toString(c);
            if (Cstr.matches("[A-Z]")) {
                oneUpper = true;
            } else if (Cstr.matches("[a-z]")) {
                oneLower = true;
            } else if (Cstr.matches("[0-9]")) {
                oneNumeric = true;
            } else {
                ServiceLogger.LOGGER.warning("Char: " + c + " is not valid in password.");
                return true; // incorrect password format
            }
        }
        return !(oneUpper && oneLower && oneNumeric);
    }
}