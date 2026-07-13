package com.osinergmin.firma.desktop.integration.invoke;

/** Coordenadas aproximadas para {@code positioningMode} sobre pagina A4 (595 x 842 pt). */
public final class SigningPositionPresets {

    private static final float PAGE_WIDTH = 595f;
    private static final float PAGE_HEIGHT = 842f;
    private static final int MARGIN = 10;

    private SigningPositionPresets() {}

    public static int[] resolve(String mode) {
        if (mode == null || mode.isBlank()) {
            return null;
        }
        return switch (mode.trim().toUpperCase()) {
            case "SD" -> coords(rightX(), topY());
            case "SI" -> coords(MARGIN, topY());
            case "SM" -> coords(centerX(), topY());
            case "MD" -> coords(rightX(), centerY());
            case "MI" -> coords(MARGIN, centerY());
            case "MM" -> coords(centerX(), centerY());
            case "ID" -> coords(rightX(), bottomY());
            case "II" -> coords(MARGIN, bottomY());
            case "IM" -> coords(centerX(), bottomY());
            default -> null;
        };
    }

    private static int[] coords(int x, int y) {
        return new int[] {x, y};
    }

    private static int topY() {
        return MARGIN;
    }

    private static int centerY() {
        return Math.round(PAGE_HEIGHT / 2f);
    }

    private static int bottomY() {
        return Math.round(PAGE_HEIGHT - 120);
    }

    private static int centerX() {
        return Math.round(PAGE_WIDTH / 2f) - 80;
    }

    private static int rightX() {
        return Math.round(PAGE_WIDTH - 180);
    }
}
