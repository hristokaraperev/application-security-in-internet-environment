package com.example.wallet.common;

/**
 * Constants for the JWT claims shared between the auth-service (issuer) and the
 * wallet-service (resource server). Keeping them in one place avoids typos that
 * would silently break token validation.
 */
public final class JwtClaims {

    /** Subject = the authenticated username. The wallet derives the account from
     *  this claim only — never from a request parameter (anti-IDOR). */
    public static final String SUBJECT = "sub";

    /** Unique token id, used for the access-token revocation blocklist. */
    public static final String TOKEN_ID = "jti";

    /** Custom claim marking the token type (access vs refresh). */
    public static final String TOKEN_TYPE = "typ";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private JwtClaims() {
    }
}
