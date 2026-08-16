package util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    // So dien thoai VN: bat dau bang 0, 9-10 chu so, hoac dang +84
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(0\\d{9,10}|\\+84\\d{9,10})$");

    private ValidationUtil() {
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return isNotBlank(phone) && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    // CCCD 12 so hoac CMND cu 9 so
    public static boolean isValidCCCD(String cccd) {
        if (!isNotBlank(cccd)) {
            return false;
        }
        String trimmed = cccd.trim();
        return trimmed.matches("\\d{9}") || trimmed.matches("\\d{12}");
    }
}
