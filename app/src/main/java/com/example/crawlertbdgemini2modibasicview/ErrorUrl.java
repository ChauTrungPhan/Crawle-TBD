package com.example.crawlertbdgemini2modibasicview;

public class ErrorUrl {
    //private int id;
    private String url;
//    private long parentId ;
//    private int level;      //= page
    private int status; // 0 = lỗi, 1 = thành công
    private String errorMessage;

    //public ErrorUrl(int id, int parentId, int level,String url, int status, String errorMessage) {
    //public ErrorUrl(String url, long parentId, int level, int status, String errorMessage) {
    public ErrorUrl(String url, int status, String errorMessage) {
        //this.id = id;
        this.url = url;
        //this.parentId = parentId;
        //this.level = level;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    //public int getId() { return id; }

//    public long getParentId() {
//        return parentId;
//    }
//    public int getLevel() {
//        return level;
//    }

    public String getUrl() { return url; }
    public int getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
}

