package com.example.cr;

import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class Nav4thActivity extends AppCompatActivity {

    private LinearLayout reportContainer;
    private ProgressBar progressBar;
    private DatabaseReference officerRef, dataRef;
    private Map<String, List<Officer>> wardToOfficers = new TreeMap<>();
    private Map<Integer, Integer> officerRunningCounts = new HashMap<>();
    private Map<Integer, Integer> officerTotalCounts = new HashMap<>();
    private Map<Integer, Integer> officerDoneCounts = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav4th);

        reportContainer = findViewById(R.id.reportContainer);
        progressBar = findViewById(R.id.progressBar);
        ImageView backBtn = findViewById(R.id.backBtn);
        ImageView downloadPdfBtn = findViewById(R.id.downloadPdfBtn);

        backBtn.setOnClickListener(v -> finish());
        downloadPdfBtn.setOnClickListener(v -> generatePdf());

        officerRef = FirebaseDatabase.getInstance().getReference("officer");
        dataRef = FirebaseDatabase.getInstance().getReference("data");

        fetchData();
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);
        dataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                officerRunningCounts.clear();
                officerTotalCounts.clear();
                officerDoneCounts.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Object slNoObj = snapshot.child("officer_sl_no").getValue();
                    if (slNoObj != null) {
                        try {
                            int slNo = Integer.parseInt(slNoObj.toString());
                            officerTotalCounts.put(slNo, officerTotalCounts.getOrDefault(slNo, 0) + 1);
                            
                            DataSnapshot statusSnap = snapshot.child("status");
                            if (statusSnap.exists()) {
                                Object activeVal = statusSnap.child("active").getValue();
                                if (activeVal != null) {
                                    if (activeVal.toString().equals("1")) {
                                        officerRunningCounts.put(slNo, officerRunningCounts.getOrDefault(slNo, 0) + 1);
                                    } else if (activeVal.toString().equals("0")) {
                                        officerDoneCounts.put(slNo, officerDoneCounts.getOrDefault(slNo, 0) + 1);
                                    }
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
                fetchOfficers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void fetchOfficers() {
        officerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                wardToOfficers.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Officer officer = ds.getValue(Officer.class);
                        if (officer != null) {
                            String ward = officer.getWard_no();
                            if (ward == null || ward.isEmpty()) ward = "Unknown";
                            
                            if (!wardToOfficers.containsKey(ward)) {
                                wardToOfficers.put(ward, new ArrayList<>());
                            }
                            wardToOfficers.get(ward).add(officer);
                        }
                    }
                    updateUI();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void updateUI() {
        reportContainer.removeAllViews();
        for (Map.Entry<String, List<Officer>> entry : wardToOfficers.entrySet()) {
            View wardView = getLayoutInflater().inflate(R.layout.item_ward_report, reportContainer, false);
            TextView wardTitle = wardView.findViewById(R.id.wardTitle);
            LinearLayout officerContainer = wardView.findViewById(R.id.officerContainer);

            wardTitle.setText("ওয়ার্ড নং: " + entry.getKey());

            for (Officer officer : entry.getValue()) {
                View officerRow = getLayoutInflater().inflate(R.layout.item_officer_row, officerContainer, false);
                TextView name = officerRow.findViewById(R.id.officerName);
                TextView mobile = officerRow.findViewById(R.id.officerMobile);
                TextView count = officerRow.findViewById(R.id.runningCount);

                name.setText(officer.getName_rank());
                mobile.setText(officer.getMobile());
                int running = officerRunningCounts.getOrDefault(officer.getSl_no(), 0);
                count.setText(String.valueOf(running));
                
                officerContainer.addView(officerRow);
            }
            reportContainer.addView(wardView);
        }
    }

    private void generatePdf() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1);
        linePaint.setColor(Color.BLACK);

        Typeface tf = ResourcesCompat.getFont(this, R.font.solaimanlipi);
        if (tf != null) paint.setTypeface(tf);

        int y = 30;
        paint.setTextSize(20f);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        canvas.drawText("গাছা থানা সিআর ওয়ারেন্ট রিপোর্ট", 180, y, paint);
        
        y += 15;
        paint.setTextSize(10f);
        paint.setFakeBoldText(false);
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("ডাউনলোড তারিখ: " + date, 40, y, paint);

        y += 10;
        // Table Config
        int startX = 20;
        int endX = 575;
        // Adjusted widths to fit new order
        int[] columnWidths = {25, 200, 45, 50, 55, 55, 55}; // Sl, Officer, OSL, Ward, Total, Running, Others
        String[] headers = {"ক্রঃ", "অফিসার (নাম ও মোবাইল)", "OSL", "ওয়ার্ড", "মোট", "চলমান", "খারিজ"};
        
        // Draw Header Function
        drawTableHeader(canvas, startX, endX, y, columnWidths, headers, paint, linePaint);
        
        y += 25; // headerHeight
        paint.setFakeBoldText(false);
        paint.setTextSize(9f);
        int rowSl = 1;

        int grandTotal = 0;
        int grandRunning = 0;
        int grandOthers = 0;

        for (Map.Entry<String, List<Officer>> entry : wardToOfficers.entrySet()) {
            String ward = entry.getKey();
            for (Officer officer : entry.getValue()) {
                int running = officerRunningCounts.getOrDefault(officer.getSl_no(), 0);
                int total = officerTotalCounts.getOrDefault(officer.getSl_no(), 0);
                int othersVal = total - running;

                grandTotal += total;
                grandRunning += running;
                grandOthers += othersVal;

                int rowHeight = 25;
                if (y + rowHeight > 800) {
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    if (tf != null) paint.setTypeface(tf);
                    y = 50;
                    drawTableHeader(canvas, startX, endX, y, columnWidths, headers, paint, linePaint);
                    y += 25;
                    paint.setFakeBoldText(false);
                    paint.setTextSize(9f);
                }

                canvas.drawRect(startX, y, endX, y + rowHeight, linePaint);
                
                int currentX = startX;
                // Sl
                canvas.drawText(String.valueOf(rowSl++), currentX + 5, y + 17, paint);
                canvas.drawLine(currentX + columnWidths[0], y, currentX + columnWidths[0], y + rowHeight, linePaint);
                currentX += columnWidths[0];
                
                // Officer (Handling Overflow)
                String officerInfo = officer.getName_rank() + " (" + officer.getMobile() + ")";
                float maxWidth = columnWidths[1] - 10;
                if (paint.measureText(officerInfo) > maxWidth) {
                    int charCount = paint.breakText(officerInfo, true, maxWidth - paint.measureText("..."), null);
                    officerInfo = officerInfo.substring(0, charCount) + "...";
                }
                canvas.drawText(officerInfo, currentX + 5, y + 17, paint);
                canvas.drawLine(currentX + columnWidths[1], y, currentX + columnWidths[1], y + rowHeight, linePaint);
                currentX += columnWidths[1];

                // OSL
                canvas.drawText(String.valueOf(officer.getSl_no()), currentX + 5, y + 17, paint);
                canvas.drawLine(currentX + columnWidths[2], y, currentX + columnWidths[2], y + rowHeight, linePaint);
                currentX += columnWidths[2];
                
                // Ward
                canvas.drawText(ward, currentX + 5, y + 17, paint);
                canvas.drawLine(currentX + columnWidths[3], y, currentX + columnWidths[3], y + rowHeight, linePaint);
                currentX += columnWidths[3];
                
                // Total
                canvas.drawText(String.valueOf(total), currentX + 5, y + 17, paint);
                canvas.drawLine(currentX + columnWidths[4], y, currentX + columnWidths[4], y + rowHeight, linePaint);
                currentX += columnWidths[4];

                // Running
                canvas.drawText(String.valueOf(running), currentX + 5, y + 17, paint);
                canvas.drawLine(currentX + columnWidths[5], y, currentX + columnWidths[5], y + rowHeight, linePaint);
                currentX += columnWidths[5];
                
                // Others
                canvas.drawText(String.valueOf(othersVal), currentX + 5, y + 17, paint);
                
                y += rowHeight;
            }
        }

        // Add Summary Row
        int rowHeight = 25;
        if (y + rowHeight > 800) {
            document.finishPage(page);
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            if (tf != null) paint.setTypeface(tf);
            y = 50;
            drawTableHeader(canvas, startX, endX, y, columnWidths, headers, paint, linePaint);
            y += 25;
        }

        paint.setFakeBoldText(true);
        canvas.drawRect(startX, y, endX, y + rowHeight, linePaint);
        int currentX = startX;
        
        // Combine first 4 columns for "Total Sum" label
        int labelWidth = columnWidths[0] + columnWidths[1] + columnWidths[2] + columnWidths[3];
        canvas.drawText("মোট হিসাব (Total Sum):", startX + 50, y + 17, paint);
        canvas.drawLine(startX + labelWidth, y, startX + labelWidth, y + rowHeight, linePaint);
        currentX += labelWidth;

        // Grand Total
        canvas.drawText(String.valueOf(grandTotal), currentX + 5, y + 17, paint);
        canvas.drawLine(currentX + columnWidths[4], y, currentX + columnWidths[4], y + rowHeight, linePaint);
        currentX += columnWidths[4];

        // Grand Running
        canvas.drawText(String.valueOf(grandRunning), currentX + 5, y + 17, paint);
        canvas.drawLine(currentX + columnWidths[5], y, currentX + columnWidths[5], y + rowHeight, linePaint);
        currentX += columnWidths[5];

        // Grand Others
        canvas.drawText(String.valueOf(grandOthers), currentX + 5, y + 17, paint);

        document.finishPage(page);

        String fileName = "Officer_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        OutputStream outputStream = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) outputStream = getContentResolver().openOutputStream(uri);
            } else {
                java.io.File file = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                outputStream = new java.io.FileOutputStream(file);
            }

            if (outputStream != null) {
                document.writeTo(outputStream);
                Toast.makeText(this, "PDF saved in Downloads", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {}
        }
    }

    private void drawTableHeader(Canvas canvas, int startX, int endX, int y, int[] columnWidths, String[] headers, Paint paint, Paint linePaint) {
        paint.setTextSize(10f);
        paint.setFakeBoldText(true);
        int headerHeight = 25;
        canvas.drawRect(startX, y, endX, y + headerHeight, linePaint);
        
        int currentX = startX;
        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], currentX + 5, y + 17, paint);
            if (i < headers.length - 1) {
                canvas.drawLine(currentX + columnWidths[i], y, currentX + columnWidths[i], y + headerHeight, linePaint);
            }
            currentX += columnWidths[i];
        }
    }
}
