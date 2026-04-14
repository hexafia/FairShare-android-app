package com.example.fairshare;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrHelper {

    public interface OcrCallback {
        void onSuccess(String amount);
        void onError(Exception e);
    }

    public static void extractAmountFromReceipt(Context context, Uri imageUri, OcrCallback callback) {
        try {
            InputImage image = InputImage.fromFilePath(context, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            String amount = findHighestCurrencyAmount(text.getText());
                            callback.onSuccess(amount);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("OCR", "Text recognition failed", e);
                            callback.onError(e);
                        }
                    });
        } catch (IOException e) {
            callback.onError(e);
        }
    }

    private static String findHighestCurrencyAmount(String rawText) {
        // Regex to find numbers that look like currency (e.g., 10.50, 1,000.00, 45.00)
        Pattern pattern = Pattern.compile("(\\d{1,3}(,\\d{3})*|\\d+)\\.\\d{2}");
        Matcher matcher = pattern.matcher(rawText);

        double maxAmount = 0.0;
        String maxAmountStr = "";

        while (matcher.find()) {
            String match = matcher.group();
            try {
                // Remove commas for parsing
                double value = Double.parseDouble(match.replace(",", ""));
                if (value > maxAmount) {
                    maxAmount = value;
                    maxAmountStr = match.replace(",", ""); // Return clean float string
                }
            } catch (NumberFormatException ignored) {
                // Ignore parsing errors for individual matches
            }
        }

        return maxAmountStr;
    }
}
