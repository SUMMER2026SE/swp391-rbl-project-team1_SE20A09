package com.sportvenue.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Block state between the authenticated user and another user. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockStatusDto {

    private Integer userId;
    private boolean blocked;
    private boolean blockedByThem;
    private boolean mutual;
}
