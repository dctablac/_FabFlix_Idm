package edu.uci.ics.dtablac.service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrivilegeRequestModel {
    
    @JsonProperty(value = "email", required = true)
    private String EMAIL;
    @JsonProperty(value = "plevel", required = true)
    private Integer PLEVEL;

    @JsonCreator
    public PrivilegeRequestModel(@JsonProperty(value = "email", required = true) String newEMAIL,
                                 @JsonProperty(value = "plevel", required = true) Integer newPLEVEL) {
        this.EMAIL = newEMAIL;
        this.PLEVEL = newPLEVEL;
    }

    @JsonProperty(value = "email")
    public String getEMAIL() { return EMAIL; }
    @JsonProperty(value = "plevel")
    public Integer getPLEVEL() { return PLEVEL; }
}
