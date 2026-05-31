package com.sportvenue.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileControllerSecurityTest {

    @Test
    void uploadStadiumImageRequiresOwnerRole() throws NoSuchMethodException {
        Method method = FileController.class.getMethod(
                "uploadStadiumImage",
                com.sportvenue.security.UserPrincipal.class,
                MultipartFile.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('Owner')", preAuthorize.value());
    }
}
