package edu.uci.ics.dtablac.service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterRequestModel {

    @JsonProperty(value = "email", required = true)
    private String EMAIL;

    @JsonProperty(value = "password", required = true)
    private char[] PASSWORD;

    @JsonCreator
    public RegisterRequestModel(@JsonProperty(value = "email", required = true) String EMAIL,
                                @JsonProperty(value = "password", required = true) char[] PASSWORD) {
        this.EMAIL = EMAIL;
        this.PASSWORD = PASSWORD;
    }

    @JsonProperty(value = "email")
    public String getEMAIL() { return this.EMAIL; }

    @JsonProperty(value = "password")
    public char[] getPASSWORD() { return PASSWORD; }
}