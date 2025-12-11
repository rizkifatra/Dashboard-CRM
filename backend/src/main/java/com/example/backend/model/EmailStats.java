package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for email statistics (incoming/outgoing counts)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailStats {

    private String staffEmail;
    private String staffName;
    private Integer incomingEmailCount;
    private Integer outgoingEmailCount;
    private Integer totalEmailCount;
}
