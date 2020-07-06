package edu.uci.ics.dtablac.service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResponseModel {
    @JsonProperty(value = "resultCode", required = true)
    private int RESULTCODE;
    @JsonProperty(value = "message", required = true)
    private String MESSAGE;

    @JsonCreator
    public RegisterResponseModel(@JsonProperty(value = "resultCode", required = true) int RESULTCODE,
                                 @JsonProperty(value = "message", required = true) String MESSAGE) {
        this.RESULTCODE = RESULTCODE;
        this.MESSAGE = MESSAGE;
    }

    @JsonProperty(value = "resultCode")
    public int getRESULTCODE() { return RESULTCODE; }

    @JsonProperty(value = "message")
    public String getMESSAGE() { return MESSAGE; }
}
