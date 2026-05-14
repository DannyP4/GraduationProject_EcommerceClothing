package com.uniform.store.service.impl;

import com.uniform.store.config.VnpayProperties;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.service.VnpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class VnpayServiceImpl implements VnpayService {

    private static final ZoneId VN_ZONE = ZoneId.of("GMT+7");
    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties props;

    @Override
    public String buildPaymentUrl(String orderNumber, BigDecimal vndAmount, String clientIp) {
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", props.getVersion());
        params.put("vnp_Command", props.getCommand());
        params.put("vnp_TmnCode", props.getTmnCode());
        params.put("vnp_Amount", vndAmount.movePointRight(2).toBigInteger().toString());
        params.put("vnp_CurrCode", props.getCurrencyCode());
        params.put("vnp_TxnRef", orderNumber);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderNumber);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", props.getLocale());
        params.put("vnp_ReturnUrl", props.getReturnUrl());
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", now.format(VN_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(props.getExpireMinutes()).format(VN_DATE));

        String signData = buildSignData(params);
        String secureHash = hmacSha512(props.getHashSecret(), signData);

        return props.getPaymentUrl() + "?" + signData + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    public VerifyResult verifyReturn(Map<String, String> rawParams) {
        if (rawParams == null || rawParams.isEmpty()) {
            return new VerifyResult(false, false, null, null, null, null,
                    "Missing parameters", Map.of());
        }
        Map<String, String> filtered = new TreeMap<>();
        String receivedHash = null;
        for (Map.Entry<String, String> e : rawParams.entrySet()) {
            String k = e.getKey();
            if (k == null || !k.startsWith("vnp_")) continue;
            if ("vnp_SecureHash".equals(k)) { receivedHash = e.getValue(); continue; }
            if ("vnp_SecureHashType".equals(k)) continue;
            filtered.put(k, e.getValue() == null ? "" : e.getValue());
        }
        if (receivedHash == null || receivedHash.isBlank()) {
            return new VerifyResult(false, false,
                    filtered.get("vnp_TxnRef"), filtered.get("vnp_ResponseCode"),
                    filtered.get("vnp_TransactionStatus"), filtered.get("vnp_TransactionNo"),
                    "Missing vnp_SecureHash", filtered);
        }
        String signData = buildSignData(filtered);
        String expected = hmacSha512(props.getHashSecret(), signData);
        boolean signatureValid = expected.equalsIgnoreCase(receivedHash);

        String responseCode = filtered.get("vnp_ResponseCode");
        String txnStatus = filtered.get("vnp_TransactionStatus");
        boolean paymentSuccess = signatureValid
                && "00".equals(responseCode)
                && "00".equals(txnStatus);

        return new VerifyResult(
                signatureValid,
                paymentSuccess,
                filtered.get("vnp_TxnRef"),
                responseCode,
                txnStatus,
                filtered.get("vnp_TransactionNo"),
                signatureValid ? (paymentSuccess ? "Payment confirmed" : "Payment failed at gateway")
                        : "Signature mismatch",
                filtered);
    }

    private static String buildSignData(Map<String, String> sortedParams) {
        List<String> pairs = new ArrayList<>(sortedParams.size());
        for (Map.Entry<String, String> e : sortedParams.entrySet()) {
            String v = e.getValue();
            if (v == null || v.isEmpty()) continue;
            pairs.add(urlEncode(e.getKey()) + "=" + urlEncode(v));
        }
        return String.join("&", pairs);
    }

    private static String urlEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.US_ASCII);
    }

    private static String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new BadRequestException("Failed to compute VNPAY signature: " + e.getMessage());
        }
    }
}
