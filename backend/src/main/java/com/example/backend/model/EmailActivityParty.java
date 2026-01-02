package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing an Email Activity Party from Dynamics 365
 * Each party represents a participant in an email (To, From, Cc, Bcc)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailActivityParty {

    @JsonAlias("activitypartyid")
    private String activityPartyId;

    @JsonAlias("_activityid_value")
    private String activityId;

    @JsonAlias("_partyid_value")
    private String partyId;

    /**
     * Participation Type Mask:
     * 1 = Organizer
     * 2 = To Recipient
     * 3 = Sender (From)
     * 4 = Cc Recipient
     * 5 = Bcc Recipient
     * 6 = Required Attendee
     * 7 = Optional Attendee
     * 8 = Resources
     */
    @JsonAlias("participationtypemask")
    private Integer participationTypeMask;

    @JsonAlias("participationtypemask@OData.Community.Display.V1.FormattedValue")
    private String participationTypeMaskFormatted;

    @JsonAlias("addressused")
    private String addressUsed;

    @JsonAlias("addressusedemailcolumnnumber")
    private Integer addressUsedEmailColumnNumber;

    @JsonAlias("ispartydeleted")
    private Boolean isPartyDeleted;

    @JsonAlias("donotemail")
    private Boolean doNotEmail;

    @JsonAlias("donotfax")
    private Boolean doNotFax;

    @JsonAlias("donotphone")
    private Boolean doNotPhone;

    @JsonAlias("donotpostalmail")
    private Boolean doNotPostalMail;
}
