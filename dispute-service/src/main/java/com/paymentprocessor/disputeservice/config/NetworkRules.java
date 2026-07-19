package com.paymentprocessor.disputeservice.config;

import com.paymentprocessor.disputeservice.domain.enums.DisputeStage;
import com.paymentprocessor.disputeservice.domain.enums.Network;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Central catalogue of card-network business rules: response deadlines,
 * chargeback / representment / arbitration fees and chargeback-rate driven
 * reserve tiers.
 *
 * <p>Fees are expressed in minor currency units (cents). The values are
 * representative mid-points of the ranges documented by each network and are
 * intended to be overridable via configuration in a production deployment.
 */
@Component
public class NetworkRules {

    /** Number of days allowed for the initial representment response. */
    private static final Map<Network, Integer> INITIAL_RESPONSE_DAYS = Map.of(
            Network.VISA, 10,
            Network.MASTERCARD, 7,
            Network.AMEX, 7,
            Network.DISCOVER, 10);

    /** Number of days allowed to respond to a pre-arbitration notice. */
    private static final Map<Network, Integer> PRE_ARBITRATION_DAYS = Map.of(
            Network.VISA, 10,
            Network.MASTERCARD, 7,
            Network.AMEX, 10,
            Network.DISCOVER, 10);

    /** Number of days allowed to file for arbitration. */
    private static final Map<Network, Integer> ARBITRATION_FILING_DAYS = Map.of(
            Network.VISA, 10,
            Network.MASTERCARD, 7,
            Network.AMEX, 10,
            Network.DISCOVER, 10);

    /** Representative chargeback fee in minor units. */
    private static final Map<Network, Long> CHARGEBACK_FEE_MINOR = Map.of(
            Network.VISA, 2000L,
            Network.MASTERCARD, 2250L,
            Network.AMEX, 3750L,
            Network.DISCOVER, 3000L);

    /** Representative representment fee in minor units. */
    private static final Map<Network, Long> REPRESENTMENT_FEE_MINOR = Map.of(
            Network.VISA, 1500L,
            Network.MASTERCARD, 1500L,
            Network.AMEX, 2500L,
            Network.DISCOVER, 2000L);

    /** Arbitration filing fee in minor units. */
    private static final Map<Network, Long> ARBITRATION_FILING_FEE_MINOR = Map.of(
            Network.VISA, 50000L,
            Network.MASTERCARD, 62500L,
            Network.AMEX, 0L,
            Network.DISCOVER, 35000L);

    /**
     * Response window, in days, for the given network at the given stage.
     */
    public int responseDays(Network network, DisputeStage stage) {
        return switch (stage) {
            case RETRIEVAL, CHARGEBACK, REPRESENTMENT ->
                    INITIAL_RESPONSE_DAYS.getOrDefault(network, 7);
            case PRE_ARBITRATION -> PRE_ARBITRATION_DAYS.getOrDefault(network, 7);
            case ARBITRATION -> ARBITRATION_FILING_DAYS.getOrDefault(network, 7);
        };
    }

    public long chargebackFeeMinor(Network network) {
        return CHARGEBACK_FEE_MINOR.getOrDefault(network, 2500L);
    }

    public long representmentFeeMinor(Network network) {
        return REPRESENTMENT_FEE_MINOR.getOrDefault(network, 1500L);
    }

    public long arbitrationFilingFeeMinor(Network network) {
        return ARBITRATION_FILING_FEE_MINOR.getOrDefault(network, 50000L);
    }

    /**
     * Rolling-reserve tier implied by a merchant's monthly chargeback rate
     * (expressed as a percentage, e.g. {@code 0.9} for 0.9%).
     */
    public ReserveTier reserveTier(double chargebackRatePercent) {
        if (chargebackRatePercent < 0.5) {
            return new ReserveTier("STANDARD", 5, 90, false);
        } else if (chargebackRatePercent < 0.9) {
            return new ReserveTier("INCREASED", 10, 120, false);
        } else if (chargebackRatePercent < 1.8) {
            return new ReserveTier("HIGH", 20, 180, true);
        }
        return new ReserveTier("EXCESSIVE", 30, 270, true);
    }

    /**
     * A rolling-reserve configuration.
     *
     * @param label            human-readable tier name
     * @param percentage       reserve percentage held from settlements
     * @param rollingDays      number of days the reserve is held
     * @param networkMonitored whether the merchant falls under a network
     *                         monitoring program
     */
    public record ReserveTier(String label, int percentage, int rollingDays,
                              boolean networkMonitored) {
    }
}
