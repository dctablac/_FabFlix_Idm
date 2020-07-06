package edu.uci.ics.dtablac.service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginRequestModel {

    @JsonProperty(value = "email", required = true)
    private String EMAIL;
    @JsonProperty(value = "password", required = true)
    private char[] PASSWORD;

    @JsonCreator
    public LoginRequestModel(@JsonProperty(value = "email", required = true) String newEMAIL,
                             @JsonProperty(value = "password", required = true) char[] newPASSWORD) {
        this.EMAIL = newEMAIL;
        this.PASSWORD = newPASSWORD;
    }

    @JsonProperty(value = "email")
    public String getEMAIL() { return EMAIL; }
    @JsonProperty(value = "password")
    public char[] getPASSWORD() { return PASSWORD; }
}