package com.sportvenue.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationHiddenStateId implements Serializable {
    private Long conversationId;
    private Integer userId;
}
