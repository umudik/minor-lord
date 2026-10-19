package io.github.umudik.minorlord.roles;

/** Counts are supplied from the approved per-species scope; no animal species list is invented here. */
public final class ButcherPolicy {
    private ButcherPolicy() {}

    public static boolean mayCull(int adultCount, boolean candidateAdult, boolean sheep, boolean sheared) {
        if (adultCount < 0) throw new IllegalArgumentException("Negative adult count");
        return adultCount > 8 && candidateAdult && (!sheep || sheared);
    }
}
