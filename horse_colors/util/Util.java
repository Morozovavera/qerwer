package sekelsta.horse_colors.util;

import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.util.text.TextComponentTranslation;

import sekelsta.horse_colors.HorseColors;

public class Util {
    public static boolean horseCanMate(AbstractHorse horse) {
        // This is the same as calling other.canMate() but doesn't require
        // reflection
        return !horse.isBeingRidden() && !horse.isRiding() && horse.isTame() && !horse.isChild() && horse.getHealth() >= horse.getMaxHealth() && horse.isInLove();
    }

    public static String translate(String in) {
        return new TextComponentTranslation(HorseColors.MODID + "." + in).getFormattedText();
    }

    public static char toBase64(int v) {
        // Doesn't matter since this will be overwritten
        char c = '\0';
        // A-Z
        if (v < 26) {
            c = (char)(v + (int)'A');
        }
        // a-z
        else if (v < 52) {
            c = (char)(v - 26 + (int)'a');
        }
        // 0-9
        else if (v < 62) {
            c = (char)(v - 36 + (int)'0');
        }
        else if (v == 62) {
            c = '+';
        }
        else if (v == 63) {
            c = '/';
        }
        else {
            throw new IllegalArgumentException(
                    "Must be at most 63. Found: " + v + "\n");
        }
        return c;
    }

    public static int fromBase64(char c) {
        // Doesn't matter since this will be overwritten
        int v = 0;
        // A-Z
        if (c >= 'A' && c <= 'Z') {
            v = (int)c - (int)'A';
        }
        // a-z
        else if (c >= 'a' && c <= 'z') {
            v = 26 + (int)c - (int)'a';
        }
        // 0-9
        else if (c >= '0' && c <= '9') {
            v = 52 + (int)c - (int)'0';
        }
        else if (c == '+') {
            v = 62;
        }
        else if (c == '/') {
            v = 63;
        }
        else {
            throw new IllegalArgumentException(
                    "Must be a valid base 64 character. Found: " + c + "\n");
        }
        return v;
    }
}
