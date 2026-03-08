package com.example.ocrtestapp;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView tvResult;
    private Uri highResImageUri;

    // 1. The High-Res Camera Launcher
    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess && highResImageUri != null) {
                    imageView.setImageURI(highResImageUri);
                    try {
                        InputImage image = InputImage.fromFilePath(this, highResImageUri);
                        processImageWithMLKit(image);
                    } catch (IOException e) {
                        e.printStackTrace();
                        tvResult.setText("Failed to process high-res image.");
                    }
                }
            }
    );

    // 2. Gallery Launcher
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageView.setImageURI(uri);
                    try {
                        InputImage image = InputImage.fromFilePath(this, uri);
                        processImageWithMLKit(image);
                    } catch (IOException e) {
                        e.printStackTrace();
                        tvResult.setText("Failed to load image from gallery.");
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        tvResult = findViewById(R.id.tvResult);
        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnGallery = findViewById(R.id.btnGallery);

        // FIX: Create a URI and launch the high-res camera
        btnCamera.setOnClickListener(v -> {
            highResImageUri = createImageUri();
            if (highResImageUri != null) {
                takePicture.launch(highResImageUri);
            }
        });

        btnGallery.setOnClickListener(v -> pickImage.launch("image/*"));
    }

    /**
     * Helper method to create a temporary file and return its Content URI
     */
    private Uri createImageUri() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);

            // Use the same authority string you put in the AndroidManifest.xml
            return FileProvider.getUriForFile(this, "com.example.ocrtestapp.provider", image);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 3. The ML Kit & Parsing Logic
    private void processImageWithMLKit(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<Text.Line> allLines = new ArrayList<>();
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        allLines.addAll(block.getLines());
                    }

                    if (allLines.isEmpty()) {
                        tvResult.setText("No text found.");
                        return;
                    }

                    // Sort lines from top to bottom
                    Collections.sort(allLines, (l1, l2) -> {
                        int y1 = l1.getBoundingBox() != null ? l1.getBoundingBox().top : 0;
                        int y2 = l2.getBoundingBox() != null ? l2.getBoundingBox().top : 0;
                        return Integer.compare(y1, y2);
                    });

                    List<List<Text.Line>> rows = new ArrayList<>();
                    for (Text.Line line : allLines) {
                        int topY = line.getBoundingBox() != null ? line.getBoundingBox().top : 0;
                        List<Text.Line> matchedRow = null;

                        for (List<Text.Line> row : rows) {
                            int rowTopY = row.get(0).getBoundingBox() != null ? row.get(0).getBoundingBox().top : 0;
                            if (Math.abs(rowTopY - topY) < 15) {
                                matchedRow = row;
                                break;
                            }
                        }

                        if (matchedRow != null) {
                            matchedRow.add(line);
                        } else {
                            List<Text.Line> newRow = new ArrayList<>();
                            newRow.add(line);
                            rows.add(newRow);
                        }
                    }

                    StringBuilder finalOutput = new StringBuilder();
                    boolean isTable = false;

                    for (List<Text.Line> row : rows) {
                        Collections.sort(row, (l1, l2) -> {
                            int x1 = l1.getBoundingBox() != null ? l1.getBoundingBox().left : 0;
                            int x2 = l2.getBoundingBox() != null ? l2.getBoundingBox().left : 0;
                            return Integer.compare(x1, x2);
                        });

                        if (row.size() > 1) {
                            isTable = true;
                            StringBuilder rowText = new StringBuilder();
                            for (int i = 0; i < row.size(); i++) {
                                rowText.append(row.get(i).getText());
                                if (i < row.size() - 1) rowText.append("  |  ");
                            }
                            finalOutput.append(rowText.toString()).append("\n");
                        } else {
                            finalOutput.append(row.get(0).getText()).append("\n");
                        }
                    }

                    if (isTable) {
                        tvResult.setText("--- DETECTED RECEIPT FORMAT ---\n\n" + finalOutput.toString());
                    } else {
                        tvResult.setText("--- DETECTED PLAIN TEXT ---\n\n" + finalOutput.toString());
                    }
                })
                .addOnFailureListener(e -> tvResult.setText("Error: " + e.getMessage()));
    }
}