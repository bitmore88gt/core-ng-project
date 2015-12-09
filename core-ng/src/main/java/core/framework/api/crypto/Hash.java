package core.framework.api.crypto;

import core.framework.api.util.Strings;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author neo
 */
public final class Hash {
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String md5Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(Strings.bytes(text));
            return hex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    private static String hex(byte[] bytes) {
        int length = bytes.length;
        char[] chars = new char[length << 1];
        int index = 0;
        for (byte b : bytes) {  // two characters form the hex value.
            chars[index++] = HEX_DIGITS[(0xF0 & b) >>> 4];
            chars[index++] = HEX_DIGITS[0x0F & b];
        }
        return new String(chars);
    }
}
