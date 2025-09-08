package com.example.crawlertbdgemini2modibasicview;

import java.io.File;

public interface ExportCallback {
    void onProgress(int progress, int max);
    void onCompleted(File outputFile);
    void onError(Exception e);
}

