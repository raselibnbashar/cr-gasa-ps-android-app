package com.example.cr;

import java.io.Serializable;
import java.util.Map;

public class Accused implements Serializable {
    private String key;
    private String name;
    private String guardian;
    private String address;
    private String ward;
    private String ps;
    private String dist;
    private String case_number;
    private String section;
    private String court_name;
    private String court_district;
    private int officer_sl_no;
    private int active;
    private int step;
    private Map<String, String> procces;

    public Accused() {
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGuardian() { return guardian; }
    public void setGuardian(String guardian) { this.guardian = guardian; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getPs() { return ps; }
    public void setPs(String ps) { this.ps = ps; }

    public String getDist() { return dist; }
    public void setDist(String dist) { this.dist = dist; }

    public String getCase_number() { return case_number; }
    public void setCase_number(String case_number) { this.case_number = case_number; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getCourt_name() { return court_name; }
    public void setCourt_name(String court_name) { this.court_name = court_name; }

    public String getCourt_district() { return court_district; }
    public void setCourt_district(String court_district) { this.court_district = court_district; }

    public int getOfficer_sl_no() { return officer_sl_no; }
    public void setOfficer_sl_no(int officer_sl_no) { this.officer_sl_no = officer_sl_no; }

    public int getActive() { return active; }
    public void setActive(int active) { this.active = active; }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public Map<String, String> getProcces() { return procces; }
    public void setProcces(Map<String, String> procces) { this.procces = procces; }
}
