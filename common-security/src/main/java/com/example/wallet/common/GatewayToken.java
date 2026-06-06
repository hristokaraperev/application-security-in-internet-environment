package com.example.wallet.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Service-to-service authentication token used by the gateway to prove to the
 * downstream services that a request really passed through it.
 *
 * <p>The gateway computes {@code HMAC-SHA256(secret, timestamp)} and sends both
 * the timestamp and the signature as headers. Downstream services recompute the
 * signature with the same shared secret and reject the request if it does not
 * match — or if the timestamp is too old (replay protection).</p>
 *
 * <p>This is what stops an attacker from calling the wallet-service directly and
 * bypassing the gateway's rate limiting and routing.</p>
 */
public final class GatewayToken {

    public static final String HEADER_TIMESTAMP = "X-Gateway-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Gateway-Auth";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private GatewayToken() {
    }

    /** Signs the current timestamp, returning {@code [timestamp, signature]}. */
    public static String[] sign(String sharedSecret) {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        return new String[]{timestamp, sign(sharedSecret, timestamp)};
    }

    public static String sign(String sharedSecret, String timestamp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(timestamp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute gateway HMAC", e);
        }
    }

    /**
     * Verifies a presented signature against the shared secret and checks that
     * the timestamp is within {@code maxSkewSeconds} of now. Uses a
     * constant-time comparison to avoid timing side-channels.
     */
    public static boolean verify(String sharedSecret, String timestamp, String signature, long maxSkewSeconds) {
        if (timestamp == null || signature == null) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        long age = Math.abs(Instant.now().getEpochSecond() - ts);
        if (age > maxSkewSeconds) {
            return false;
        }
        String expected = sign(sharedSecret, timestamp);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
