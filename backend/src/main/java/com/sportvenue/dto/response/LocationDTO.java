package com.sportvenue.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationDTO {
    private String displayName;
    private Double latitude;
    private Double longitude;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String source;
}
