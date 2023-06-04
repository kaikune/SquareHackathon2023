package com.squareHackathon2023;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class WebhookValidator {
    protected static String squareWebhookKey;

    public static boolean validateWebhook(String signatureHeader, String requestBody) {
        squareWebhookKey = Main.mustLoadEnvironmentVariable(System.getenv("WEBHOOK_KEY"));
        try {
            // Convert the signing key to bytes
            byte[] signingKeyBytes = squareWebhookKey.getBytes(StandardCharsets.UTF_8);

            // Create a new HMAC-SHA1 signer
            Mac signer = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(signingKeyBytes, "HmacSHA1");
            signer.init(keySpec);

            // Calculate the HMAC-SHA1 signature of the request body
            byte[] signatureBytes = signer.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));

            // Encode the signature bytes as a base64 string
            String calculatedSignature = Base64.getEncoder().encodeToString(signatureBytes);

            // Compare the calculated signature with the signature from the header
            return calculatedSignature.equals(signatureHeader);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // Handle exception
            e.printStackTrace();
        }

        return false;
    }
}
