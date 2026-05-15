package com.example.cr;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ReportDownloadActivity extends AppCompatActivity {

    private RecyclerView rvDistricts;
    private ProgressBar progressBar;
    private Button btnDownloadSelected, btnDownloadAll, btnDownloadMaster;
    private DatabaseReference databaseReference;
    private int officerSlNo;
    private String reportType = "ac";
    private Map<String, List<Accused>> districtMap = new HashMap<>();
    private List<Accused> allAccusedList = new ArrayList<>();
    private List<Accused> allOriginalAccusedList = new ArrayList<>();
    private List<String> districtList = new ArrayList<>();
    private Set<String> selectedDistricts = new HashSet<>();
    private DistrictAdapter adapter;
    private Map<Integer, String> officerMap = new HashMap<>();
    private TextInputEditText etSearchKeyword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_download);

        officerSlNo = getIntent().getIntExtra("officer_sl_no", -1);
        reportType = getIntent().getStringExtra("report_type");
        if (reportType == null) reportType = "ac";

        ImageView backBtn = findViewById(R.id.backBtn);
        rvDistricts = findViewById(R.id.rvDistricts);
        progressBar = findViewById(R.id.progressBar);
        btnDownloadSelected = findViewById(R.id.btnDownloadSelected);
        btnDownloadAll = findViewById(R.id.btnDownloadAll);
        btnDownloadMaster = findViewById(R.id.btnDownloadMaster);
        etSearchKeyword = findViewById(R.id.etSearchKeyword);

        backBtn.setOnClickListener(v -> finish());

        rvDistricts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DistrictAdapter();
        rvDistricts.setAdapter(adapter);

        btnDownloadSelected.setOnClickListener(v -> {
            if (selectedDistricts.isEmpty()) {
                Toast.makeText(this, "অনুগ্রহ করে ডিস্ট্রিক্ট নির্বাচন করুন", Toast.LENGTH_SHORT).show();
                return;
            }
            for (String dist : selectedDistricts) {
                generateReport(dist);
            }
            Toast.makeText(this, "নির্বাচিত রিপোর্টগুলো ডাউনলোড করা হয়েছে", Toast.LENGTH_SHORT).show();
        });

        btnDownloadAll.setOnClickListener(v -> {
            if (districtList.isEmpty()) {
                Toast.makeText(this, "ডাউনলোড করার মত কোন তথ্য নেই", Toast.LENGTH_SHORT).show();
                return;
            }
            for (String dist : districtList) {
                generateReport(dist);
            }
            Toast.makeText(this, "সকল ডিস্ট্রিক্ট রিপোর্ট আলাদাভাবে ডাউনলোড করা হয়েছে", Toast.LENGTH_SHORT).show();
        });

        btnDownloadMaster.setOnClickListener(v -> {
            if (allAccusedList.isEmpty()) {
                Toast.makeText(this, "ডাউনলোড করার মত কোন তথ্য নেই", Toast.LENGTH_SHORT).show();
                return;
            }
            if ("final".equals(reportType)) {
                generateFinalDocx(allAccusedList, "সকল_জেলা_একত্রে");
            } else {
                generateDocx(allAccusedList, "সকল_জেলা_একত্রে");
            }
            Toast.makeText(this, "সকল জেলার তথ্য একত্রে ডাউনলোড করা হয়েছে", Toast.LENGTH_SHORT).show();
        });

        etSearchKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        databaseReference = FirebaseDatabase.getInstance().getReference("data");
        fetchOfficers();
    }

    private void fetchOfficers() {
        FirebaseDatabase.getInstance().getReference("officer")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        officerMap.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                Integer slNo = ds.child("sl_no").getValue(Integer.class);
                                String name = ds.child("name_rank").getValue(String.class);
                                if (slNo != null && name != null) {
                                    officerMap.put(slNo, name);
                                }
                            }
                        }
                        fetchData();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);
        
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allOriginalAccusedList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Accused accused = child.getValue(Accused.class);
                        if (accused != null) {
                            accused.setKey(child.getKey());
                            
                            int active = 0;
                            DataSnapshot statusSnap = child.child("status");
                            if (statusSnap.exists()) {
                                Object activeVal = statusSnap.child("active").getValue();
                                if (activeVal != null) {
                                    try {
                                        active = Integer.parseInt(activeVal.toString());
                                        accused.setActive(active);
                                    } catch (NumberFormatException ignored) {}
                                }
                                Object stepVal = statusSnap.child("step").getValue();
                                if (stepVal != null) {
                                    try {
                                        accused.setStep(Integer.parseInt(stepVal.toString()));
                                    } catch (NumberFormatException ignored) {}
                                }
                            }

                            if (active == 1) {
                                allOriginalAccusedList.add(accused);
                            }
                        }
                    }
                }
                filterData(etSearchKeyword.getText().toString());
                progressBar.setVisibility(View.GONE);
                
                // If opened from MoreFragment (officerSlNo == -1), update title or message
                if (officerSlNo == -1) {
                    ((TextView)findViewById(R.id.tvSelectDistrict)).setText("আদালতের জেলা নির্বাচন করুন (সকল অফিসার)");
                } else {
                    ((TextView)findViewById(R.id.tvSelectDistrict)).setText("আদালতের জেলা নির্বাচন করুন");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        };

        if (officerSlNo != -1) {
            databaseReference.orderByChild("officer_sl_no").equalTo(officerSlNo).addListenerForSingleValueEvent(valueEventListener);
        } else {
            databaseReference.addListenerForSingleValueEvent(valueEventListener);
        }
    }

    private void filterData(String query) {
        String filterQuery = query.toLowerCase().trim();
        districtMap.clear();
        districtList.clear();
        allAccusedList.clear();

        for (Accused accused : allOriginalAccusedList) {
            boolean matches = true;
            if (!filterQuery.isEmpty()) {
                matches = false;
                if (accused.getSection() != null && accused.getSection().toLowerCase().contains(filterQuery)) {
                    matches = true;
                } else if (accused.getName() != null && accused.getName().toLowerCase().contains(filterQuery)) {
                    matches = true;
                } else if (accused.getCase_number() != null && accused.getCase_number().toLowerCase().contains(filterQuery)) {
                    matches = true;
                }
            }

            if (matches) {
                String dist = accused.getCourt_district();
                if (dist == null || dist.isEmpty()) dist = "অজানা";

                if (!districtMap.containsKey(dist)) {
                    districtMap.put(dist, new ArrayList<>());
                    districtList.add(dist);
                }
                districtMap.get(dist).add(accused);
                allAccusedList.add(accused);
            }
        }
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void generateReport(String district) {
        List<Accused> list = districtMap.get(district);
        if (list == null || list.isEmpty()) return;
        
        if ("final".equals(reportType)) {
            generateFinalDocx(list, district);
        } else {
            generateDocx(list, district);
        }
    }

    private void generateDocx(List<Accused> list, String titleSuffix) {
        XWPFDocument document = new XWPFDocument();

        // Set Landscape Orientation
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSz = sectPr.addNewPgSz();
        pageSz.setOrient(STPageOrientation.LANDSCAPE);
        pageSz.setW(BigInteger.valueOf(15840)); // 11 inches
        pageSz.setH(BigInteger.valueOf(12240)); // 8.5 inches

        // Title
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setFontFamily("SolaimanLipi");
        titleRun.setText("গ্রেফতারী পরোয়ানা জারির রেজিস্টার");
        titleRun.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);

        XWPFParagraph subTitlePara = document.createParagraph();
        subTitlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun subTitleRun = subTitlePara.createRun();
        subTitleRun.setFontSize(12);
        subTitleRun.setFontFamily("SolaimanLipi");
        subTitleRun.setText("[১৭৩ নিয়মের (জ) দফা দ্রষ্টব্য]");
        subTitleRun.addBreak();
        subTitleRun.setText("পি.আর.বি ফরম নং ১২ ক");

        XWPFParagraph distPara = document.createParagraph();
        distPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun distRun = distPara.createRun();
        distRun.setFontFamily("SolaimanLipi");
        distRun.setText("আদালতের জেলা: " + titleSuffix + " , মোট পরোয়ানা: " + list.size());

        // Table
        XWPFTable table = document.createTable(1, 8);
        table.setWidth("100%");

        // Table Header
        String[] headers = {"ক্রমিক নং", "গ্রেফতারী পরোয়ানা রেজিস্টার নম্বর", "বিজ্ঞ কোর্টের নাম",
                           "কোর্টের প্রসেস নং", "জিআর/সিআর মামলা নং- ও ধারা", "নাম ও ঠিকানা", 
                           "থানায় প্রাপ্তির তারিখ", "মন্তব্য"};
        
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            if (cell == null) cell = headerRow.createCell();
            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            r.setBold(true);
            r.setFontSize(10);
            r.setFontFamily("SolaimanLipi");
            r.setText(headers[i]);
        }

        // Data Rows
        int count = 1;
        for (Accused accused : list) {
            XWPFTableRow row = table.createRow();

            setCellText(row.getCell(0), convertToBanglaNumber(String.valueOf(count++)));

            // Cell 1: গ্রেফতারী পরোয়ানা রেজিস্টার নম্বর (from procces["2026"] or procces["2025"])
            String procVal = "";
            if (accused.getProcces() != null) {
                if (accused.getProcces().containsKey("2026")) {
                    procVal = accused.getProcces().get("2026");
                } else if (accused.getProcces().containsKey("2025")) {
                    procVal = accused.getProcces().get("2025");
                }
            }
            setCellText(row.getCell(1), procVal);
            
            // Cell 2: বিজ্ঞ কোর্টের নাম
            setCellText(row.getCell(2), accused.getCourt_name() != null ? accused.getCourt_name() : "");

            // Cell 3: কোর্টের প্রসেস নং (from cp["2026"] or cp["2025"])
            String cpVal = "";
            if (accused.getCp() != null) {
                if (accused.getCp().containsKey("2026")) {
                    cpVal = accused.getCp().get("2026");
                } else if (accused.getCp().containsKey("2025")) {
                    cpVal = accused.getCp().get("2025");
                }
            }
            setCellText(row.getCell(3), cpVal);

            // Cell 4: জিআর/সিআর মামলা নং- ও ধারা
            XWPFTableCell cell4 = row.getCell(4);
            XWPFParagraph p4 = cell4.getParagraphs().get(0);
            XWPFRun r4 = p4.createRun();
            r4.setFontFamily("SolaimanLipi");
            r4.setFontSize(10);
            r4.setText(accused.getCase_number() != null ? accused.getCase_number() : "");
            r4.addBreak();
            r4.setText("ধারা- " + (accused.getSection() != null ? accused.getSection() : ""));

            // Cell 5: নাম ও ঠিকানা
            XWPFTableCell cell5 = row.getCell(5);
            XWPFParagraph p5 = cell5.getParagraphs().get(0);
            XWPFRun r5 = p5.createRun();
            r5.setFontFamily("SolaimanLipi");
            r5.setFontSize(10);
            r5.setText(accused.getName() != null ? accused.getName() : "");
            r5.addBreak();
            r5.setText((accused.getGuardian() != null ? accused.getGuardian() : ""));
            r5.addBreak();
            r5.setText(accused.getAddress() != null ? accused.getAddress() : "");
            r5.addBreak();
            r5.setText("ওয়ার্ডঃ " + accused.getWard() + ", থানাঃ " + accused.getPs() + ", জেলাঃ " + accused.getDist());

            // Cell 6: থানায় প্রাপ্তির তারিখ
            setCellText(row.getCell(6), "০১/০১/২০২৬");

            // Cell 7: মন্তব্য
            String remarks = "";
            if (accused.getActive() == 0) {
                if (accused.getStep() == 2) remarks = "নিস্পত্তি (তামিল)";
                else if (accused.getStep() == 3) remarks = "নিস্পত্তি (রিকল)";
                else if (accused.getStep() == 4) remarks = "নিস্পত্তি (অন্যান্য)";
            }
            setCellText(row.getCell(7), remarks);
        }

        saveDocument(document, "Register_" + titleSuffix.replace(" ", "_") + ".docx");
    }


    private void generateFinalDocx(List<Accused> list, String titleSuffix) {
        XWPFDocument document = new XWPFDocument();

        // Set Landscape Orientation
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSz = sectPr.addNewPgSz();
        pageSz.setOrient(STPageOrientation.LANDSCAPE);
        pageSz.setW(BigInteger.valueOf(15840)); // 11 inches
        pageSz.setH(BigInteger.valueOf(12240)); // 8.5 inches

        // Title
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setFontFamily("SolaimanLipi");
        titleRun.setText("গ্রেফতারী পরোয়ানা জারির রেজিস্টার");
        titleRun.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);

        XWPFParagraph subTitlePara = document.createParagraph();
        subTitlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun subTitleRun = subTitlePara.createRun();
        subTitleRun.setFontSize(12);
        subTitleRun.setFontFamily("SolaimanLipi");
        subTitleRun.setText("[১৭৩ নিয়মের (জ) দফা দ্রষ্টব্য]");
        subTitleRun.addBreak();
        subTitleRun.setText("পি.আর.বি ফরম নং ১২ ক");

        XWPFParagraph distPara = document.createParagraph();
        distPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun distRun = distPara.createRun();
        distRun.setFontFamily("SolaimanLipi");
        distRun.setText("আদালতের জেলা: " + titleSuffix + " , মোট পরোয়ানা: " + list.size());

        // Table
        XWPFTable table = document.createTable(1, 12);
        table.setWidth("100%");

        // Table Header
        String[] headers = {"ক্রমিক নং", "কোর্টের প্রসেস নং", "গ্রেফতারী পরোয়ানা রেজিষ্টার নম্বর",
                "যে ব্যক্তির বিরুদ্ধে গ্রেফতারী পরোয়ানা জারী করা হইয়াছে তাহার নাম, পিতার নাম ও নিবাস",
                "গ্রেফতারী পরোয়ানা নং ও ধরন (আই আইনের ধারা) এবং তারিখ", "যে আদালত কর্তৃক জারী করা হইয়াছে তাহার নাম",
                "থানায় প্রাপ্তির তারিখ", "প্রদানের তারিখ সহ যে অফিসারের নিকট জারী করার জন্য প্রদান করা হইয়াছে",
                "(আদালতে) ফেরত দিবার তারিখ", "পরোয়ানা যে তারিখে থানায় ফেরত আসার তারিখ", "আদালতে ফেরত যাওয়ার তারিখ",
                "কাজ সম্পাদিত অথবা অসম্পাদিত হইলে গৃহীত ব্যবস্থা এবং মন্তব্য"};

        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            if (cell == null) cell = headerRow.createCell();
            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            r.setBold(true);
            r.setFontSize(8);
            r.setFontFamily("SolaimanLipi");
            r.setText(headers[i]);
        }

        // Data Rows
        int count = 1;
        for (Accused accused : list) {
            XWPFTableRow row = table.createRow();

            setCellText(row.getCell(0), String.valueOf(count++));

            // Cell 1: কোর্টের প্রসেস নং (from cp["2026"] or cp["2025"])
            String cpVal = "";
            if (accused.getCp() != null) {
                if (accused.getCp().containsKey("2026")) {
                    cpVal = accused.getCp().get("2026");
                } else if (accused.getCp().containsKey("2025")) {
                    cpVal = accused.getCp().get("2025");
                }
            }
            setCellText(row.getCell(1), cpVal);

            // Cell 2: গ্রেফতারী পরোয়ানা রেজিষ্টার নম্বর (from procces["2026"] or procces["2025"])
            String procVal = "";
            if (accused.getProcces() != null) {
                if (accused.getProcces().containsKey("2026")) {
                    procVal = accused.getProcces().get("2026");
                } else if (accused.getProcces().containsKey("2025")) {
                    procVal = accused.getProcces().get("2025");
                }
            }
            setCellText(row.getCell(2), procVal);

            // Cell 3: Name, Father, Address
            XWPFTableCell cell3 = row.getCell(3);
            XWPFParagraph p3 = cell3.getParagraphs().get(0);
            XWPFRun r3 = p3.createRun();
            r3.setFontFamily("SolaimanLipi");
            r3.setFontSize(10);
            r3.setText(accused.getName() != null ? accused.getName() : "");
            r3.addBreak();
            r3.setText("পিতা- " + (accused.getGuardian() != null ? accused.getGuardian() : ""));
            r3.addBreak();
            r3.setText(accused.getAddress() != null ? accused.getAddress() : "");
            r3.addBreak();
            r3.setText("ওয়ার্ডঃ " + accused.getWard() + ", থানাঃ " + accused.getPs() + ", জেলাঃ " + accused.getDist());

            // Cell 4: Warrant No & Section
            XWPFTableCell cell4 = row.getCell(4);
            XWPFParagraph p4 = cell4.getParagraphs().get(0);
            XWPFRun r4 = p4.createRun();
            r4.setFontFamily("SolaimanLipi");
            r4.setFontSize(10);
            r4.setText(accused.getCase_number() != null ? accused.getCase_number() : "");
            r4.addBreak();
            r4.setText((accused.getSection() != null ? accused.getSection() : ""));

            // Cell 5: Court
            setCellText(row.getCell(5), accused.getCourt_name() != null ? accused.getCourt_name() : "");

            // Cell 6: Received Date (placeholder)
            setCellText(row.getCell(6), "");

            // Cell 7: Assigned Officer
            XWPFTableCell cell7 = row.getCell(7);
            XWPFParagraph p7 = cell7.getParagraphs().get(0);
            XWPFRun r7 = p7.createRun();
            r7.setFontFamily("SolaimanLipi");
            r7.setFontSize(10);
            String oName = officerMap.get(accused.getOfficer_sl_no());
            r7.setText(oName != null ? oName : "Unknown");

            // Rest cells empty
            setCellText(row.getCell(8), "");
            setCellText(row.getCell(9), "");
            setCellText(row.getCell(10), "");

            String remarks = "";
            if (accused.getActive() == 0) {
                if (accused.getStep() == 2) remarks = "নিস্পত্তি (তামিল)";
                else if (accused.getStep() == 3) remarks = "নিস্পত্তি (রিকল)";
                else if (accused.getStep() == 4) remarks = "নিস্পত্তি (অন্যান্য)";
            }
            setCellText(row.getCell(11), remarks);
        }

        saveDocument(document, "Register_" + titleSuffix.replace(" ", "_") + ".docx");
    }

    private void setCellText(XWPFTableCell cell, String text) {
        if (cell == null) return;
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r = p.createRun();
        r.setFontFamily("SolaimanLipi");
        r.setFontSize(10);
        r.setText(text);
    }

    private String convertToBanglaNumber(String input) {
        if (input == null) return "";
        char[] banglaDigits = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
        char[] englishDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            boolean found = false;
            for (int j = 0; j < englishDigits.length; j++) {
                if (input.charAt(i) == englishDigits[j]) {
                    builder.append(banglaDigits[j]);
                    found = true;
                    break;
                }
            }
            if (!found) {
                builder.append(input.charAt(i));
            }
        }
        return builder.toString();
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
                outputStream = new FileOutputStream(file);
            }

            if (outputStream != null) {
                document.write(outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class DistrictAdapter extends RecyclerView.Adapter<DistrictAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_district, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String district = districtList.get(position);
            holder.tvDistrictName.setText(district);
            holder.tvCount.setText(districtMap.get(district).size() + " টি রেকর্ড");
            
            holder.cbDistrict.setChecked(selectedDistricts.contains(district));
            holder.cbDistrict.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedDistricts.add(district);
                } else {
                    selectedDistricts.remove(district);
                }
                btnDownloadSelected.setVisibility(selectedDistricts.isEmpty() ? View.GONE : View.VISIBLE);
            });

            holder.itemView.setOnClickListener(v -> generateReport(district));
        }

        @Override
        public int getItemCount() {
            return districtList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDistrictName, tvCount;
            CheckBox cbDistrict;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDistrictName = itemView.findViewById(R.id.tvDistrictName);
                tvCount = itemView.findViewById(R.id.tvCount);
                cbDistrict = itemView.findViewById(R.id.cbDistrict);
            }
        }
    }
}
