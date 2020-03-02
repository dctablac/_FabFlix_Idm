package edu.uci.ics.dtablac.service.idm.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uci.ics.dtablac.service.idm.core.SessionRecords;
import edu.uci.ics.dtablac.service.idm.models.SessionRequestModel;
import edu.uci.ics.dtablac.service.idm.models.SessionResponseModel;
import edu.uci.ics.dtablac.service.idm.util.SessionUtility;

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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response session(String jsonText) {
        // Declare mappers and models
        Response response;
        ObjectMapper mapper  = new ObjectMapper();
        SessionRequestModel requestModel;
        SessionResponseModel responseModel = null;

        try {
            // Define payload
            requestModel = mapper.readValue(jsonText, SessionRequestModel.class);
            String email = requestModel.getEMAIL();
            String session_id = requestModel.getSESSION_ID();

            // Checks on email and session_id
            response = SessionUtility.checkPayload(email, session_id);
            if (response != null) {
                return response;
            }

            // Verify the session's details
            response = SessionUtility.verifySession(email, session_id);

            // Update the session if needed. (if not found, skip)
            if (SessionRecords.sessionExists(email, session_id)) {
                SessionRecords.sessionValidation(email, session_id);
            }
        }
        catch (IOException e) {
            if (e instanceof JsonParseException) {
                responseModel = new SessionResponseModel(-3, "JSON Parse Exception.", null);
            }
            else if (e instanceof JsonMappingException) {
                responseModel = new SessionResponseModel(-2, "JSON Mapping Exception.", null);
            }
            return Response.status(Status.BAD_REQUEST).entity(responseModel).build();
        }
        return response;
    }
}
