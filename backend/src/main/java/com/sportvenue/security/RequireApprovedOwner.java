package com.sportvenue.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation dùng để đánh dấu các Method hoặc Controller 
 * chỉ cho phép đối tác (Owner) đã được Admin PHÊ DUYỆT (approvedStatus == APPROVED) truy cập.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireApprovedOwner {
}
