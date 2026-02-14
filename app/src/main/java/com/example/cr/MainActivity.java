package com.example.cr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private ImageView themeToggleIcon;
    private TextView totalAccused, runningAccused, doneAccused, searchTotalItem;
    private EditText searchBarMain;
    private DatabaseReference databaseReference;
    
    private RecyclerView accusedRecyclerView;
    private AccusedAdapter accusedAdapter;
    private List<Accused> accusedList;
    private List<Accused> allActiveAccusedList;

    private View homeContent;
    private View fragmentContainer;
    private ImageView navHome, navList, navMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);

        themeToggleIcon = findViewById(R.id.themeToggleIcon);
        totalAccused = findViewById(R.id.totalAccused);
        runningAccused = findViewById(R.id.runningAccused);
        doneAccused = findViewById(R.id.doneAccused);
        searchTotalItem = findViewById(R.id.searchTotalItem);
        searchBarMain = findViewById(R.id.searchBarMain);
        
        homeContent = findViewById(R.id.home_content);
        fragmentContainer = findViewById(R.id.fragment_container);
        navHome = findViewById(R.id.home);
        navList = findViewById(R.id.list);
        navMore = findViewById(R.id.more);

        accusedRecyclerView = findViewById(R.id.accusedFrontList);
        accusedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        accusedList = new ArrayList<>();
        allActiveAccusedList = new ArrayList<>();
        accusedAdapter = new AccusedAdapter(accusedList);
        accusedRecyclerView.setAdapter(accusedAdapter);

        // Set click listener to open Detail Activity
        accusedAdapter.setOnItemClickListener(accused -> {
            Intent intent = new Intent(MainActivity.this, AccusedDetailActivity.class);
            intent.putExtra("accused", accused);
            startActivity(intent);
        });

        // Add Accused Button Click Listener
        findViewById(R.id.newAccusedBtn).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAccusedActivity.class);
            startActivity(intent);
        });

        updateIcon(isDarkMode);

        // Initialize Firebase Reference
        databaseReference = FirebaseDatabase.getInstance().getReference("data");

        // Listen for data changes to count records and update list
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allActiveAccusedList.clear();
                long total = 0;
                long running = 0;
                long done = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Accused accused = child.getValue(Accused.class);
                        if (accused != null) {
                            accused.setKey(child.getKey());
                            total++;

                            // Check status: active == 1 (Running) or active == 0 (Done)
                            DataSnapshot statusSnap = child.child("status");
                            if (statusSnap.exists()) {
                                Object activeVal = statusSnap.child("active").getValue();
                                if (activeVal != null) {
                                    String activeStr = activeVal.toString();
                                    if (activeStr.equals("1")) {
                                        running++;
                                        allActiveAccusedList.add(accused);
                                    } else if (activeStr.equals("0")) {
                                        done++;
                                    }
                                }
                            }

                            // Still update step for UI logic (e.g. colors) if available
                            if (statusSnap.hasChild("step")) {
                                Object stepActiveVal = statusSnap.child("step").getValue();
                                if (stepActiveVal != null) {
                                    try {
                                        accused.setStep(Integer.parseInt(stepActiveVal.toString()));
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }
                    }
                    // Sort descending (last serial to first)
                    Collections.reverse(allActiveAccusedList);

                    filter(searchBarMain.getText().toString());
                    totalAccused.setText(String.valueOf(total));
                    runningAccused.setText(String.valueOf(running));
                    doneAccused.setText(String.valueOf(done));
                } else {
                    accusedList.clear();
                    accusedAdapter.notifyDataSetChanged();
                    searchTotalItem.setText("০ টি তথ্য পাওয়া গেছে");
                    totalAccused.setText("0");
                    runningAccused.setText("0");
                    doneAccused.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors.
            }
        });

        searchBarMain.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        navHome.setOnClickListener(v -> showHome());
        navList.setOnClickListener(v -> loadFragment(new ListFragment(), navList));
        navMore.setOnClickListener(v -> loadFragment(new MoreFragment(), navMore));

        // Home screen card clicks
        findViewById(R.id.runningWarrant).setOnClickListener(v -> loadFragment(ListFragment.newInstance("running"), navList));
        findViewById(R.id.totalWarrant).setOnClickListener(v -> loadFragment(ListFragment.newInstance("total"), navList));
        ((View) findViewById(R.id.doneWarrant).getParent()).setOnClickListener(v -> loadFragment(ListFragment.newInstance("done"), navList));

        // Default selection
        showHome();

        findViewById(R.id.appTitleSwitchDiv).setOnClickListener(v -> {
            boolean currentMode = sharedPreferences.getBoolean("isDarkMode", false);
            boolean nextMode = !currentMode;

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isDarkMode", nextMode);
            editor.apply();

            if (nextMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!isDarkMode);
        controller.setAppearanceLightNavigationBars(!isDarkMode);
    }

    private void showHome() {
        homeContent.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        updateNavUI(navHome);
    }

    private void loadFragment(Fragment fragment, ImageView selectedNav) {
        homeContent.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
        
        updateNavUI(selectedNav);
    }

    private void updateNavUI(ImageView selectedNav) {
        int emerald = ContextCompat.getColor(this, R.color.emerald_400);
        int white = Color.WHITE;

        navHome.setColorFilter(white);
        navList.setColorFilter(white);
        navMore.setColorFilter(white);

        selectedNav.setColorFilter(emerald);
    }

    private void filter(String text) {
        accusedList.clear();
        if (text.isEmpty()) {
            accusedList.addAll(allActiveAccusedList);
        } else {
            String query = text.toLowerCase().trim();
            for (Accused item : allActiveAccusedList) {
                boolean matchesProcess = false;
                Map<String, String> processMap = item.getProcces();
                if (processMap != null) {
                    for (String value : processMap.values()) {
                        if (value != null && value.toLowerCase().contains(query)) {
                            matchesProcess = true;
                            break;
                        }
                    }
                }

                if ((item.getName() != null && item.getName().toLowerCase().contains(query)) ||
                    (item.getGuardian() != null && item.getGuardian().toLowerCase().contains(query)) ||
                    (item.getCase_number() != null && item.getCase_number().toLowerCase().contains(query)) ||
                    matchesProcess) {
                    accusedList.add(item);
                }
            }
        }
        accusedAdapter.notifyDataSetChanged();
        searchTotalItem.setText(accusedList.size() + " টি তথ্য পাওয়া গেছে");
    }

    private void updateIcon(boolean isDarkMode) {
        if (isDarkMode) {
            themeToggleIcon.setImageResource(R.drawable.ic_sun);
        } else {
            themeToggleIcon.setImageResource(R.drawable.ic_moon);
        }
    }
}
