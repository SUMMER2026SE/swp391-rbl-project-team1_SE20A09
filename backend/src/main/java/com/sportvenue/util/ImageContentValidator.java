package com.sportvenue.util;

import com.sportvenue.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Xác thực nội dung file ảnh bằng magic bytes (không chỉ dựa vào extension / Content-Type).
 */
public final class ImageContentValidator {

    private static final int HEADER_READ_SIZE = 16;

    private ImageContentValidator() {
    }

    public enum DetectedFormat {
        JPEG, PNG, GIF, WEBP, BMP, HEIC, UNKNOWN
    }

    public static DetectedFormat detectFormat(byte[] header) {
        if (header == null || header.length < 3) {
            return DetectedFormat.UNKNOWN;
        }
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return DetectedFormat.JPEG;
        }
        if (header.length >= 8
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
            return DetectedFormat.PNG;
        }
        if (header.length >= 6) {
            String gifSig = new String(header, 0, 6);
            if ("GIF87a".equals(gifSig) || "GIF89a".equals(gifSig)) {
                return DetectedFormat.GIF;
            }
        }
        if (header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50) {
            return DetectedFormat.WEBP;
        }
        if (header.length >= 2 && header[0] == 0x42 && header[1] == 0x4D) {
            return DetectedFormat.BMP;
        }
        if (header.length >= 12 && header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
            String brand = new String(header, 8, 4);
            if (Set.of("heic", "heix", "hevc", "hevx", "mif1", "msf1").contains(brand)) {
                return DetectedFormat.HEIC;
            }
        }
        return DetectedFormat.UNKNOWN;
    }

    public static void validateImageContent(InputStream inputStream, String extension) {
        if (!inputStream.markSupported()) {
            throw new BadRequestException("Không thể xác thực nội dung file ảnh.");
        }
        byte[] header;
        try {
            inputStream.mark(HEADER_READ_SIZE);
            header = inputStream.readNBytes(HEADER_READ_SIZE);
            inputStream.reset();
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc file ảnh.");
        }

        DetectedFormat format = detectFormat(header);
        if (format == DetectedFormat.UNKNOWN) {
            throw new BadRequestException(
                    "File không phải ảnh hợp lệ. Chỉ chấp nhận JPG, PNG, WEBP, GIF hoặc BMP.");
        }

        String ext = extension != null ? extension.toLowerCase(Locale.ROOT) : "";
        if (!ext.isBlank() && !extensionMatchesFormat(ext, format)) {
            throw new BadRequestException(
                    "Đuôi file không khớp với nội dung ảnh. Vui lòng chọn file ảnh đúng định dạng.");
        }
    }

    private static boolean extensionMatchesFormat(String extension, DetectedFormat format) {
        return switch (format) {
            case JPEG -> Set.of(".jpg", ".jpeg").contains(extension);
            case PNG -> ".png".equals(extension);
            case GIF -> ".gif".equals(extension);
            case WEBP -> ".webp".equals(extension);
            case BMP -> ".bmp".equals(extension);
            case HEIC -> Set.of(".heic", ".heif").contains(extension);
            default -> false;
        };
    }
}
