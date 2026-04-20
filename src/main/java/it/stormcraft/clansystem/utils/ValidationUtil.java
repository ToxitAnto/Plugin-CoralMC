package it.stormcraft.clansystem.utils;

import it.stormcraft.clansystem.config.PluginConfig;

import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private ValidationUtil() {}

    public enum NameResult { OK, TOO_SHORT, TOO_LONG, INVALID }

    public static NameResult validateName(String name, PluginConfig cfg) {
        if (name == null || !VALID_PATTERN.matcher(name).matches()) return NameResult.INVALID;
        if (name.length() < cfg.getNameMinLength())  return NameResult.TOO_SHORT;
        if (name.length() > cfg.getNameMaxLength())  return NameResult.TOO_LONG;
        return NameResult.OK;
    }

    public static NameResult validateTag(String tag, PluginConfig cfg) {
        if (tag == null || !VALID_PATTERN.matcher(tag).matches()) return NameResult.INVALID;
        if (tag.length() < cfg.getTagMinLength())  return NameResult.TOO_SHORT;
        if (tag.length() > cfg.getTagMaxLength())  return NameResult.TOO_LONG;
        return NameResult.OK;
    }

    public static boolean isValidInput(String input) {
        return input != null && VALID_PATTERN.matcher(input).matches();
    }
}
