package com.paymentprocessor.auditservice.crypto;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes a Merkle root over an ordered list of leaf hashes. The daily batch root is a
 * Merkle root of that day's record hashes, which lets a single anchored root attest to
 * every record and supports compact inclusion proofs later if needed.
 *
 * <p>Construction: leaves are the {@code sha256:...} record hashes (hashed again as
 * bytes to normalise length); internal nodes are {@code sha256(left || right)}; an odd
 * node at any level is promoted (duplicated) — a standard, widely understood scheme.
 */
public final class MerkleTree {

    /** Root used for an empty batch (a day with no records). */
    public static final String EMPTY_ROOT =
            Sha256.PREFIX + "0000000000000000000000000000000000000000000000000000000000000000";

    private MerkleTree() {
    }

    public static String computeRoot(List<String> leafHashes) {
        if (leafHashes == null || leafHashes.isEmpty()) {
            return EMPTY_ROOT;
        }

        List<byte[]> level = new ArrayList<>(leafHashes.size());
        for (String leaf : leafHashes) {
            level.add(Sha256.digest(leaf.getBytes(StandardCharsets.UTF_8)));
        }

        while (level.size() > 1) {
            List<byte[]> next = new ArrayList<>((level.size() + 1) / 2);
            for (int i = 0; i < level.size(); i += 2) {
                byte[] left = level.get(i);
                byte[] right = (i + 1 < level.size()) ? level.get(i + 1) : left; // promote odd
                next.add(hashPair(left, right));
            }
            level = next;
        }
        return Sha256.PREFIX + Sha256.toHex(level.get(0));
    }

    private static byte[] hashPair(byte[] left, byte[] right) {
        byte[] combined = new byte[left.length + right.length];
        System.arraycopy(left, 0, combined, 0, left.length);
        System.arraycopy(right, 0, combined, left.length, right.length);
        return Sha256.digest(combined);
    }
}
