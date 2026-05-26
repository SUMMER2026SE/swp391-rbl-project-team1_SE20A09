package com.sportvenue.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ImageContentValidatorTest {

    @Test
    void detectFormat_recognizesJpegPngAndGif() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        byte[] png = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
        };
        byte[] gif = "GIF89a".getBytes();

        assertEquals(ImageContentValidator.DetectedFormat.JPEG, ImageContentValidator.detectFormat(jpeg));
        assertEquals(ImageContentValidator.DetectedFormat.PNG, ImageContentValidator.detectFormat(png));
        assertEquals(ImageContentValidator.DetectedFormat.GIF, ImageContentValidator.detectFormat(gif));
    }
}
