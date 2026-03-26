package com.example.cr;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class AccusedDetailActivity extends AppCompatActivity {

    private DatabaseReference officerRef, dataRef;
    private Button btnRunning, btnTamil, btnRecall, btnOther, btnReport, btnCertificate;
    private Accused currentAccused;
    private String officerNameRank = "", officerBP = "", officerStation = "গাছা থানা, জি.এম.পি,গাজীপুর";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accused_detail);

        ImageView backBtn = findViewById(R.id.backBtn);
        ImageView editBtn = findViewById(R.id.editBtn);
        TextView name = findViewById(R.id.detailName);
        TextView guardian = findViewById(R.id.detailGuardian);
        TextView caseNo = findViewById(R.id.detailCaseNo);
        TextView section = findViewById(R.id.detailSection);
        TextView process = findViewById(R.id.detailProcess);
        TextView address = findViewById(R.id.detailAddress);
        TextView wardPsDist = findViewById(R.id.detailWardPsDist);
        TextView court = findViewById(R.id.detailCourt);
        TextView courtDist = findViewById(R.id.detailCourtDist);
        TextView officerSl = findViewById(R.id.detailOfficerSl);

        btnRunning = findViewById(R.id.btnRunning);
        btnTamil = findViewById(R.id.btnTamil);
        btnRecall = findViewById(R.id.btnRecall);
        btnOther = findViewById(R.id.btnOther);
        btnReport = findViewById(R.id.btnReport);
        btnCertificate = findViewById(R.id.btnCertificate);

        backBtn.setOnClickListener(v -> finish());

        currentAccused = (Accused) getIntent().getSerializableExtra("accused");

        if (currentAccused != null && currentAccused.getKey() != null) {
            dataRef = FirebaseDatabase.getInstance().getReference("data").child(currentAccused.getKey()).child("status");
            
            name.setText(currentAccused.getName());
            guardian.setText(currentAccused.getGuardian());
            caseNo.setText(currentAccused.getCase_number());
            section.setText(currentAccused.getSection());
            address.setText(currentAccused.getAddress());
            wardPsDist.setText(String.format("ওয়ার্ডঃ %s, থানাঃ %s, জেলাঃ %s", currentAccused.getWard(), currentAccused.getPs(), currentAccused.getDist()));
            court.setText(currentAccused.getCourt_name());
            courtDist.setText(currentAccused.getCourt_district());
            
            // Display all process numbers from the 'procces' map
            Map<String, String> processMap = currentAccused.getProcces();
            if (processMap != null && !processMap.isEmpty()) {
                TreeMap<String, String> sortedMap = new TreeMap<>((a, b) -> b.compareTo(a));
                sortedMap.putAll(processMap);
                
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                process.setText(sb.toString().trim());
            } else {
                process.setText("কোন তথ্য নেই");
            }

            officerSl.setText("অফিসার এস.এল নং: " + currentAccused.getOfficer_sl_no());

            fetchOfficerDetails(currentAccused.getOfficer_sl_no(), officerSl);
            listenToStatusChanges();

            btnRunning.setOnClickListener(v -> updateStatus(1, 1));
            btnTamil.setOnClickListener(v -> updateStatus(0, 2));
            btnRecall.setOnClickListener(v -> updateStatus(0, 3));
            btnOther.setOnClickListener(v -> updateStatus(0, 4));
            
            btnReport.setOnClickListener(v -> exportToDocx());
            btnCertificate.setOnClickListener(v -> exportCertificateToDocx());

            editBtn.setOnClickListener(v -> {
                Intent intent = new Intent(AccusedDetailActivity.this, AddAccusedActivity.class);
                intent.putExtra("accused", currentAccused);
                startActivity(intent);
            });

            if (currentAccused.getStep() >= 2) {
                name.setTextColor(ContextCompat.getColor(this, R.color.emerald_400));
            } else {
                name.setTextColor(ContextCompat.getColor(this, R.color.red_500));
            }
        }
    }

    private void fetchOfficerDetails(int slNo, TextView tv) {
        officerRef = FirebaseDatabase.getInstance().getReference("officer");
        officerRef.orderByChild("sl_no").equalTo(slNo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                officerNameRank = child.child("name_rank").getValue(String.class);
                                officerBP = child.child("mobile").getValue(String.class);
                                if (officerNameRank != null) tv.setText(officerNameRank);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenToStatusChanges() {
        dataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object activeObj = snapshot.child("active").getValue();
                    Object stepObj = snapshot.child("step").getValue();
                    if (activeObj != null && stepObj != null) {
                        try {
                            int active = Integer.parseInt(activeObj.toString());
                            int step = Integer.parseInt(stepObj.toString());
                            updateButtonUI(active, step);
                        } catch (NumberFormatException e) {}
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateButtonUI(int active, int step) {
        int defaultColor = ContextCompat.getColor(this, R.color.slate_800);
        btnRunning.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btnTamil.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btnRecall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btnOther.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));

        if (active == 1 && step == 1) {
            btnRunning.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_600)));
        } else if (active == 0) {
            int emerald = ContextCompat.getColor(this, R.color.emerald_400);
            if (step == 2) btnTamil.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emerald));
            else if (step == 3) btnRecall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emerald));
            else if (step == 4) btnOther.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emerald));
        }
    }

    private void updateStatus(int active, int step) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("active", active);
        updates.put("step", step);
        dataRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportToDocx() {
        if (currentAccused == null) return;

        XWPFDocument document = new XWPFDocument();

        // Header
        XWPFParagraph headerPara = document.createParagraph();
        headerPara.setAlignment(ParagraphAlignment.LEFT);
        headerPara.setSpacingAfter(0);
        XWPFRun headerRun = headerPara.createRun();
        headerRun.setText("বরাবর,");
        headerRun.addBreak();
        headerRun.addTab();
        headerRun.setText(currentAccused.getCourt_name() + ",");
        headerRun.addBreak();
        headerRun.addTab();
        headerRun.setText(currentAccused.getCourt_district() + "।");

        addParagraph(document, "", false);
        addParagraph(document, "মাধ্যমঃ সহকারী পুলিশ কমিশনার (প্রসিকিউশন), মহানগর কোর্ট, গাজীপুর।", false);

        addParagraph(document, "", false);
        XWPFParagraph subjectPara = document.createParagraph();
        XWPFRun subjectRun = subjectPara.createRun();
        subjectRun.setText("বিষয়ঃ বিনা তামিলে ওয়ারেন্ট ফেরত প্রদান প্রসঙ্গে।");
        subjectRun.setUnderline(UnderlinePatterns.SINGLE);

        addParagraph(document, "", false);
        
        String processNo = "";
        Map<String, String> pMap = currentAccused.getProcces();
        if (pMap != null && !pMap.isEmpty()) {
            TreeMap<String, String> sorted = new TreeMap<>((a, b) -> b.compareTo(a));
            sorted.putAll(pMap);
            processNo = sorted.firstEntry().getValue();
        }

        addParagraph(document, String.format("সূত্রঃ %s, ধারাঃ %s, প্রসেস নং- %s।", 
                currentAccused.getCase_number(), currentAccused.getSection(), processNo), false);

        addParagraph(document, "", false);
        addParagraph(document, "জনাব,", false);

        String bodyText = String.format("বিনীত নিবেদন এই যে, আসামি %s, পিতা- %s, সাং-%s, ওয়ার্ড নং %s, থানা- %s, %s এর পরোয়ানাটি থানায় প্রাপ্তি সাপেক্ষে অফিসার ইনচার্জ %s থানা, স্যার আমার নামে হাওলা করিলে, পরোয়ানাভুক্ত আসামীর বর্ণিত বর্তমান ঠিকানায় গ্রেফতার অভিযান পরিচালনা কালে জানা যায় যে, সূত্রে বর্ণিত মামলার পরোয়ানায় উল্লেখিত ঠিকানায় এই নামে কোন ব্যক্তি বসবাস করে না। বিধায় আসামীকে খুঁজিয়া পাওয়া সম্ভব হচ্ছে না। এমনকি স্থানীয় গণ্যমান্য ব্যক্তিবর্গ আসামীর বসবাস সম্পর্কে সুনির্দিষ্ট কোন তথ্য দিতে না পারায় পরোয়ানাটি তামিল করা সম্ভব হইল না।",
                currentAccused.getName(), currentAccused.getGuardian(), currentAccused.getAddress(), 
                currentAccused.getWard(), currentAccused.getPs(), currentAccused.getDist(), currentAccused.getPs());
        
        XWPFParagraph bodyPara = document.createParagraph();
        bodyPara.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun bodyRun = bodyPara.createRun();
        bodyRun.addTab(); // Added tab to first line of body text
        bodyRun.setText(bodyText);

        addParagraph(document, "", false);
        XWPFParagraph ataPara = document.createParagraph();
        XWPFRun ataRun = ataPara.createRun();
        ataRun.addTab();
        ataRun.setText("অতএব ,ইহা আপনার সদয় অবগতির ও পরবর্তী কার্যকরী ব্যবস্থা গ্রহণের জন্য প্রেরণ করিলাম।");

        addParagraph(document, "", false);

        saveDocument(document, "Report_" + currentAccused.getName().replace(" ", "_") + ".docx");
    }

    private void exportCertificateToDocx() {

        if (currentAccused == null) return;

        XWPFDocument document = new XWPFDocument();

        // Title
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("প্রত্যয়ন পত্র");
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setUnderline(UnderlinePatterns.SINGLE);

        addParagraph(document, "", false);
        addParagraph(document, "", false);

        String bodyText = String.format("এই মর্মে প্রত্যয়ন করা যাইতেছে যে, %s, পিতা- %s, সাং-%s, ওয়ার্ড নং-%s, থানা-%s, %s। উক্ত ব্যক্তি পূর্বে ভাড়াটিয়া হিসেবে বসবাস করিলেও বর্তমানে আমার জানামতে অত্র এলাকায় বসবাস করে না। আমি ব্যক্তিগতভাবে তাহাকে চিনি না।",
                currentAccused.getName(), currentAccused.getGuardian(), currentAccused.getAddress(),
                currentAccused.getWard(), currentAccused.getPs(), currentAccused.getDist());

        XWPFParagraph bodyPara = document.createParagraph();
        bodyPara.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun bodyRun = bodyPara.createRun();
        bodyRun.setFontSize(12);
        bodyRun.addTab(); // Added tab to first line of body text
        bodyRun.setText(bodyText);

        saveDocument(document, "Certificate_" + currentAccused.getName().replace(" ", "_") + ".docx");
    }

    private void addParagraph(XWPFDocument document, String text, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
    }

    private void saveDocument(XWPFDocument document, String fileName) {
        OutputStream outputStream = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    outputStream = getContentResolver().openOutputStream(uri);
                }
            } else {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                outputStream = new java.io.FileOutputStream(file);
            }

            if (outputStream != null) {
                document.write(outputStream);
                Toast.makeText(this, "Document saved in Downloads", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
