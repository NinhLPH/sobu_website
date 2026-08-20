package com.vn.sodu.location;

/**
 * Versioned address write-contract rule for local mode.
 *
 * <p>The canonical contract is {@value #CANONICAL_VERSION}: province/district/
 * ward administrative codes plus a human-readable snapshot, both owned by Sobu.
 * The legacy Nhanh contract is {@value #LEGACY_NHANH_VERSION}. Before any
 * adapter is released, writes are gated here:</p>
 *
 * <ul>
 *   <li>absent version or canonical version -&gt; accepted, stores
 *       {@value #CANONICAL_VERSION};</li>
 *   <li>legacy Nhanh version -&gt; rejected with a migration error (no
 *       translation exists yet, so old-format IDs are never persisted as the
 *       authoritative address);</li>
 *   <li>unknown version -&gt; rejected with a migration error.</li>
 * </ul>
 *
 * <p>The adapter must never translate a local address back into Nhanh IDs.</p>
 */
public final class AddressContract {

    /** Canonical Sobu administrative address contract (local province/ward dataset). */
    public static final String CANONICAL_VERSION = "v2";

    /** Legacy Nhanh location contract, retired for new writes in local mode. */
    public static final String LEGACY_NHANH_VERSION = "v1";

    private AddressContract() {
    }

    /**
     * Resolves the version to persist on a new write. Rejects the legacy Nhanh
     * contract and any unknown version with a migration error; a request with
     * no explicit version is treated as the canonical local contract.
     */
    public static String resolveVersionForWrite(String requestedVersion) {
        if (requestedVersion == null || requestedVersion.isBlank()) {
            return CANONICAL_VERSION;
        }
        String version = requestedVersion.trim();
        if (CANONICAL_VERSION.equals(version)) {
            return CANONICAL_VERSION;
        }
        if (LEGACY_NHANH_VERSION.equals(version)) {
            throw new AddressMigrationRequiredException(
                    "The Nhanh location contract v1 is retired in local mode. "
                            + "Resubmit the address with the local v2 province/district/ward codes, or migrate the record first.");
        }
        throw new AddressMigrationRequiredException("Unsupported location contract version: " + version);
    }
}
