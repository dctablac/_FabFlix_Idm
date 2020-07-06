package edu.uci.ics.dtablac.service.idm.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uci.ics.dtablac.service.idm.core.PrivilegeRecords;
import edu.uci.ics.dtablac.service.idm.logger.ServiceLogger;
import edu.uci.ics.dtablac.service.idm.models.PrivilegeRequestModel;
import edu.uci.ics.dtablac.service.idm.models.PrivilegeResponseModel;

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

@Path("privilege")
public class PrivilegePage {
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verifyPrivilege(@Context HttpHeaders headers, String jsonText) {
        PrivilegeRequestModel requestModel;
        PrivilegeResponseModel responseModel;
        ObjectMapper mapper = new ObjectMapper();
        Integer resultCode = -1;

        try {
            requestModel = mapper.readValue(jsonText, PrivilegeRequestModel.class);
            ServiceLogger.LOGGER.info("Request:\n" + jsonText);

            String email = requestModel.getEMAIL();
            Integer plevel = requestModel.getPLEVEL();

            if (plevelOutOfRange(plevel)) {
                resultCode = -14;
                responseModel = new PrivilegeResponseModel(resultCode,
                        "Privilege level out of valid range.");
                ServiceLogger.LOGGER.warning("Privilege level out of valid range.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            if (invalidEmailLength(email)) {
                resultCode = -10;
                responseModel = new PrivilegeResponseModel(resultCode,
                        "Email address has invalid length.");
                ServiceLogger.LOGGER.warning("Email address has invalid length.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            if (invalidEmailFormat(email)) {
                resultCode = -11;
                responseModel = new PrivilegeResponseModel(resultCode,
                        "Email address has invalid format.");
                ServiceLogger.LOGGER.warning("Email address has invalid format.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            if (userNotFound(email)) {
                resultCode = 14;
                responseModel = new PrivilegeResponseModel(resultCode,
                        "User not found.");
                ServiceLogger.LOGGER.warning("User not found.");
                return Response.status(Status.OK).entity(responseModel).build();
            }
            if (badPLevel(email, plevel)) {
                resultCode = 141;
                responseModel = new PrivilegeResponseModel(resultCode,
                        "User has insufficient privilege level.");
                ServiceLogger.LOGGER.warning("User has insufficient privilege level.");
                return Response.status(Status.OK).entity(responseModel).build();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            if (e instanceof JsonParseException) {
                resultCode = -3;
                responseModel = new PrivilegeResponseModel(resultCode, "JSON Parse Exception.");
                ServiceLogger.LOGGER.warning("JSON Parse Exception.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
            else if (e instanceof JsonMappingException) {
                resultCode = -2;
                responseModel = new PrivilegeResponseModel(resultCode, "JSON Mapping Exception.");
                ServiceLogger.LOGGER.warning("JSON Mapping Exception.");
                return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
            }
        }
        // User has sufficient privilege level.
        resultCode = 140;
        responseModel = new PrivilegeResponseModel(resultCode,
                "User has sufficient privilege level.");
        ServiceLogger.LOGGER.warning("User has sufficient privilege level.");
        return Response.status(Status.OK).entity(responseModel).build();
    }

    boolean plevelOutOfRange(Integer plevel) {
        return (plevel < 1) || (plevel > 5);
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

    boolean userNotFound(String email) {
        return PrivilegeRecords.emailNotFound(email);
    }

    boolean badPLevel(String email, Integer plevel) {
        return !PrivilegeRecords.sufficientPrivilege(email,plevel);
    }
}
