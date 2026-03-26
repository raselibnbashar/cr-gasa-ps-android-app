package com.example.cr;

public class Officer {
    private String name_rank;
    private String mobile;
    private int sl_no;
    private String user_id;
    private long totalRecords = 0;
    private long runningRecords = 0;
    private long doneRecords = 0;

    public Officer() {}

    public String getName_rank() { return name_rank; }
    public void setName_rank(String name_rank) { this.name_rank = name_rank; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public int getSl_no() { return sl_no; }
    public void setSl_no(int sl_no) { this.sl_no = sl_no; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getRunningRecords() { return runningRecords; }
    public void setRunningRecords(long runningRecords) { this.runningRecords = runningRecords; }

    public long getDoneRecords() { return doneRecords; }
    public void setDoneRecords(long doneRecords) { this.doneRecords = doneRecords; }
}
