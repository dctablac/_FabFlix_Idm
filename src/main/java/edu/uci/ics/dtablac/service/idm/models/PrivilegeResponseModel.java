package edu.uci.ics.dtablac.service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivilegeResponseModel {

    @JsonProperty(value = "resultCode", required = true)
    private Integer RESULTCODE;
    @JsonProperty(value = "message", required = true)
    private String MESSAGE;

    @JsonCreator
    public PrivilegeResponseModel(@JsonProperty(value = "resultCode", required = true) Integer newRESULTCODE,
                                  @JsonProperty(value = "message", required = true) String newMESSAGE) {
        this.RESULTCODE = newRESULTCODE;
        this.MESSAGE = newMESSAGE;
    }

    @JsonProperty(value = "resultCode")
    public Integer getRESULTCODE() { return RESULTCODE; }
    @JsonProperty(value = "message")
    public String getMESSAGE() { return MESSAGE; }
}
