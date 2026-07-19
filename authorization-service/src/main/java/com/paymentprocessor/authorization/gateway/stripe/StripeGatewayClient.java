package com.paymentprocessor.authorization.gateway.stripe;

import com.paymentprocessor.authorization.common.MoneyUtil;
import com.paymentprocessor.authorization.config.GatewayProperties;
import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.enums.AvsResult;
import com.paymentprocessor.authorization.domain.enums.CardNetwork;
import com.paymentprocessor.authorization.domain.enums.CvvResult;
import com.paymentprocessor.authorization.gateway.GatewayClient;
import com.paymentprocessor.authorization.gateway.GatewayException;
import com.paymentprocessor.authorization.gateway.model.GatewayAuthorizeCommand;
import com.paymentprocessor.authorization.gateway.model.GatewayCaptureCommand;
import com.paymentprocessor.authorization.gateway.model.GatewayResult;
import com.paymentprocessor.authorization.gateway.model.GatewayReversalCommand;
import com.stripe.Stripe;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.CardException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * {@link GatewayClient} backed by Stripe. Authorizations are modelled as manual-capture
 * PaymentIntents:
 * <ul>
 *   <li>authorize &rarr; create a confirmed PaymentIntent with {@code capture_method=manual}
 *       (status {@code requires_capture} means the hold is placed);</li>
 *   <li>capture &rarr; capture the PaymentIntent (optionally partial);</li>
 *   <li>reverse/void &rarr; cancel the PaymentIntent.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StripeGatewayClient implements GatewayClient {

    private final GatewayProperties properties;

    @PostConstruct
    void init() {
        GatewayProperties.Stripe cfg = properties.getStripe();
        if (cfg.getApiKey() != null && !cfg.getApiKey().isBlank()) {
            Stripe.apiKey = cfg.getApiKey();
        } else {
            log.warn("Stripe API key is not configured; gateway calls will fail until STRIPE_API_KEY is set");
        }
        Stripe.setConnectTimeout(cfg.getConnectTimeoutMs());
        Stripe.setReadTimeout(cfg.getReadTimeoutMs());
        Stripe.setMaxNetworkRetries(cfg.getMaxNetworkRetries());
    }

    @Override
    public String provider() {
        return "stripe";
    }

    @Override
    public GatewayResult authorize(GatewayAuthorizeCommand command) {
        long minor = MoneyUtil.toMinorUnits(command.getAmount(), command.getCurrency());
        PaymentIntentCreateParams.CaptureMethod captureMethod = command.isCaptureImmediately()
                ? PaymentIntentCreateParams.CaptureMethod.AUTOMATIC
                : PaymentIntentCreateParams.CaptureMethod.MANUAL;

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(minor)
                .setCurrency(command.getCurrency().toLowerCase())
                .setCaptureMethod(captureMethod)
                .setConfirm(true)
                .setPaymentMethod(command.getPaymentMethodToken())
                .addPaymentMethodType("card")
                .putMetadata("payment_reference", nullSafe(command.getPaymentReference()))
                .putMetadata("merchant_id", nullSafe(command.getMerchantId()))
                .addExpand("latest_charge");

        if (command.getCustomerReference() != null) {
            builder.setCustomer(command.getCustomerReference());
        }
        if (command.getStatementDescriptor() != null) {
            builder.setStatementDescriptorSuffix(command.getStatementDescriptor());
        }
        command.getMetadata().forEach(builder::putMetadata);

        RequestOptions options = requestOptions(command.getIdempotencyKey());
        try {
            PaymentIntent intent = PaymentIntent.create(builder.build(), options);
            return toResult(intent);
        } catch (CardException declined) {
            return declinedResult(declined);
        } catch (StripeException ex) {
            throw translate("authorize", ex);
        }
    }

    @Override
    public GatewayResult capture(GatewayCaptureCommand command) {
        RequestOptions options = requestOptions(command.getIdempotencyKey());
        try {
            PaymentIntent intent = PaymentIntent.retrieve(command.getGatewayAuthorizationId());
            PaymentIntentCaptureParams.Builder builder = PaymentIntentCaptureParams.builder()
                    .addExpand("latest_charge");
            if (command.getAmount() != null) {
                builder.setAmountToCapture(MoneyUtil.toMinorUnits(command.getAmount(), command.getCurrency()));
            }
            PaymentIntent captured = intent.capture(builder.build(), options);
            GatewayResult base = toResult(captured);
            boolean partial = base.getCapturedAmount() != null
                    && base.getApprovedAmount() != null
                    && base.getCapturedAmount().compareTo(base.getApprovedAmount()) < 0;
            AuthorizationStatus status = partial ? AuthorizationStatus.PARTIALLY_CAPTURED
                    : AuthorizationStatus.CAPTURED;
            return base.toBuilder().status(status).build();
        } catch (StripeException ex) {
            throw translate("capture", ex);
        }
    }

    @Override
    public GatewayResult reverse(GatewayReversalCommand command) {
        RequestOptions options = requestOptions(command.getIdempotencyKey());
        try {
            PaymentIntent intent = PaymentIntent.retrieve(command.getGatewayAuthorizationId());
            PaymentIntentCancelParams.Builder builder = PaymentIntentCancelParams.builder();
            if (command.getReason() != null) {
                builder.setCancellationReason(
                        PaymentIntentCancelParams.CancellationReason.ABANDONED);
            }
            PaymentIntent canceled = intent.cancel(builder.build(), options);
            return toResult(canceled).toBuilder()
                    .status(AuthorizationStatus.REVERSED)
                    .build();
        } catch (StripeException ex) {
            throw translate("reverse", ex);
        }
    }

    @Override
    public GatewayResult retrieve(String gatewayAuthorizationId) {
        try {
            PaymentIntentRetrieveParams params = PaymentIntentRetrieveParams.builder()
                    .addExpand("latest_charge")
                    .build();
            PaymentIntent intent = PaymentIntent.retrieve(gatewayAuthorizationId, params, null);
            return toResult(intent);
        } catch (StripeException ex) {
            throw translate("retrieve", ex);
        }
    }

    // ------------------------------------------------------------------ mapping

    private GatewayResult toResult(PaymentIntent intent) {
        String currency = intent.getCurrency() != null ? intent.getCurrency().toUpperCase() : "USD";
        BigDecimal approved = MoneyUtil.fromMinorUnits(
                intent.getAmountCapturable() != null && intent.getAmountCapturable() > 0
                        ? intent.getAmountCapturable()
                        : (intent.getAmount() != null ? intent.getAmount() : 0L),
                currency);
        BigDecimal captured = MoneyUtil.fromMinorUnits(
                intent.getAmountReceived() != null ? intent.getAmountReceived() : 0L, currency);

        GatewayResult.GatewayResultBuilder result = GatewayResult.builder()
                .status(StripeStatusMapper.fromPaymentIntentStatus(intent.getStatus()))
                .gatewayAuthorizationId(intent.getId())
                .approvedAmount(approved)
                .capturedAmount(captured)
                .responseCode(intent.getStatus())
                .rawResponse(intent.toJson());

        if ("requires_action".equals(intent.getStatus())) {
            result.requiresAuthentication(true);
            if (intent.getNextAction() != null && intent.getNextAction().getRedirectToUrl() != null) {
                result.authenticationUrl(intent.getNextAction().getRedirectToUrl().getUrl());
            }
        }

        enrichFromCharge(intent, result);
        return result.build();
    }

    private void enrichFromCharge(PaymentIntent intent, GatewayResult.GatewayResultBuilder result) {
        Charge charge;
        try {
            charge = intent.getLatestChargeObject();
        } catch (Exception e) {
            charge = null;
        }
        if (charge == null) {
            return;
        }
        // Stripe exposes the scheme reference via the charge id / network transaction id rather than
        // a classic issuer authorization code.
        result.networkReferenceId(charge.getId());
        if (charge.getOutcome() != null) {
            result.responseMessage(charge.getOutcome().getSellerMessage());
            if (charge.getOutcome().getReason() != null) {
                result.responseCode(charge.getOutcome().getReason());
            }
        }
        Charge.PaymentMethodDetails details = charge.getPaymentMethodDetails();
        if (details != null && details.getCard() != null) {
            Charge.PaymentMethodDetails.Card card = details.getCard();
            result.cardNetwork(mapNetwork(card.getBrand()));
            result.cardLast4(card.getLast4());
            if (card.getChecks() != null) {
                Charge.PaymentMethodDetails.Card.Checks checks = card.getChecks();
                result.avsResult(mapAvs(checks.getAddressLine1Check(), checks.getAddressPostalCodeCheck()));
                result.cvvResult(mapCvv(checks.getCvcCheck()));
            }
        }
    }

    private GatewayResult declinedResult(CardException ex) {
        String code = ex.getDeclineCode() != null ? ex.getDeclineCode() : ex.getCode();
        log.info("Stripe declined authorization: code={}, message={}", code, ex.getMessage());
        String gatewayId = null;
        String raw = null;
        if (ex.getStripeError() != null) {
            raw = ex.getStripeError().toJson();
            if (ex.getStripeError().getPaymentIntent() != null) {
                gatewayId = ex.getStripeError().getPaymentIntent().getId();
            }
        }
        return GatewayResult.builder()
                .status(AuthorizationStatus.DECLINED)
                .gatewayAuthorizationId(gatewayId)
                .responseCode(code)
                .responseMessage(ex.getMessage())
                .approvedAmount(BigDecimal.ZERO)
                .capturedAmount(BigDecimal.ZERO)
                .rawResponse(raw)
                .build();
    }

    private GatewayException translate(String operation, StripeException ex) {
        boolean retryable = ex instanceof ApiConnectionException || ex instanceof RateLimitException;
        log.error("Stripe {} failed (retryable={}): {}", operation, retryable, ex.getMessage());
        return new GatewayException("Gateway " + operation + " failed: " + ex.getMessage(), retryable, ex);
    }

    private RequestOptions requestOptions(String idempotencyKey) {
        RequestOptions.RequestOptionsBuilder builder = RequestOptions.builder();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.setIdempotencyKey(idempotencyKey);
        }
        return builder.build();
    }

    private static CardNetwork mapNetwork(String brand) {
        if (brand == null) {
            return CardNetwork.UNKNOWN;
        }
        return switch (brand.toLowerCase()) {
            case "visa" -> CardNetwork.VISA;
            case "mastercard" -> CardNetwork.MASTERCARD;
            case "amex", "american_express" -> CardNetwork.AMEX;
            case "discover" -> CardNetwork.DISCOVER;
            case "jcb" -> CardNetwork.JCB;
            case "diners", "diners_club" -> CardNetwork.DINERS;
            case "unionpay" -> CardNetwork.UNIONPAY;
            default -> CardNetwork.UNKNOWN;
        };
    }

    private static AvsResult mapAvs(String line1, String postal) {
        boolean line1Pass = "pass".equalsIgnoreCase(line1);
        boolean postalPass = "pass".equalsIgnoreCase(postal);
        boolean anyChecked = line1 != null || postal != null;
        if (line1Pass && postalPass) {
            return AvsResult.FULL_MATCH;
        }
        if (line1Pass || postalPass) {
            return AvsResult.PARTIAL_MATCH;
        }
        if ("fail".equalsIgnoreCase(line1) || "fail".equalsIgnoreCase(postal)) {
            return AvsResult.NO_MATCH;
        }
        return anyChecked ? AvsResult.UNAVAILABLE : AvsResult.NOT_CHECKED;
    }

    private static CvvResult mapCvv(String cvcCheck) {
        if (cvcCheck == null) {
            return CvvResult.NOT_PROVIDED;
        }
        return switch (cvcCheck.toLowerCase()) {
            case "pass" -> CvvResult.MATCH;
            case "fail" -> CvvResult.NO_MATCH;
            case "unavailable" -> CvvResult.UNAVAILABLE;
            default -> CvvResult.NOT_PROVIDED;
        };
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
