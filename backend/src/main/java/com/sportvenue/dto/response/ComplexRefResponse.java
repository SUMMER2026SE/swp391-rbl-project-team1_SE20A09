package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexRefResponse {
    private Integer stadiumId;
    private Integer complexId;
}
