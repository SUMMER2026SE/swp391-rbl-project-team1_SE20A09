package com.sportvenue.util;

import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumImage;
import java.util.Comparator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StadiumUtils {

    public static String resolveImageUrl(Stadium stadium) {
        if (stadium.getImages() == null || stadium.getImages().isEmpty()) {
            return "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800";
        }
        return stadium.getImages().stream()
                .sorted(Comparator.comparing(StadiumImage::getUploadedAt))
                .map(StadiumImage::getImageUrl)
                .findFirst()
                .orElse("https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800");
    }

    /** Resolve the address inherited by a Facility/Court from its Complex. */
    public static String resolveAddress(Stadium stadium) {
        if (stadium == null) {
            return null;
        }
        if (stadium.getAddress() != null && !stadium.getAddress().isBlank()) {
            return stadium.getAddress();
        }
        if (stadium.getComplex() != null) {
            return stadium.getComplex().getAddress();
        }
        if (stadium.getParentStadium() != null
                && stadium.getParentStadium().getComplex() != null) {
            return stadium.getParentStadium().getComplex().getAddress();
        }
        return null;
    }

    /** Resolve the name of the Complex a Facility/Court belongs to — null if standalone/legacy data. */
    public static String resolveComplexName(Stadium stadium) {
        if (stadium == null) {
            return null;
        }
        if (stadium.getComplex() != null) {
            return stadium.getComplex().getName();
        }
        if (stadium.getParentStadium() != null
                && stadium.getParentStadium().getComplex() != null) {
            return stadium.getParentStadium().getComplex().getName();
        }
        return null;
    }

    public static String toSportLabel(String sportName) {
        return switch (sportName) {
            case "Football" -> "Bóng đá";
            case "Badminton" -> "Cầu lông";
            case "Basketball" -> "Bóng rổ";
            case "Tennis" -> "Quần vợt";
            case "Pickleball" -> "Pickleball";
            default -> sportName;
        };
    }
}
