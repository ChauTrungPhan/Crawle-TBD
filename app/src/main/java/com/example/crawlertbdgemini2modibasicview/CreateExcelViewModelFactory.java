package com.example.crawlertbdgemini2modibasicview;

// File: app/src/main/java/com/example/crawlertbdgeminibasicview/CreateExcelViewModelFactory.java

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class CreateExcelViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;
    ExcelExporterOptimized_goc excelExporterOptimized;

    public CreateExcelViewModelFactory(@NonNull Application application, ExcelExporterOptimized_goc excelExporterOptimized) {
        this.application = application;
        this.excelExporterOptimized = excelExporterOptimized;
    }

    @NonNull
    @Override
//    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
//        if (modelClass.isAssignableFrom(MyViewModelCreateExcel_Gemini.class)) {
//            // CHỈNH SỬA: Đảm bảo MyViewModelCreateExcel_Gemini có constructor phù hợp
//            return (T) new MyViewModelCreateExcel_Gemini(application);
//        }
//        throw new IllegalArgumentException("Unknown ViewModel class");
//    }

    @SuppressWarnings("unchecked") // Bỏ qua cảnh báo ép kiểu không an toàn
    //Tuy nhiên, hãy sử dụng cách này một cách cẩn thận và chỉ khi bạn chắc chắn rằng ép kiểu là an toàn.
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MyViewModelCreateExcel_Gemini.class)) {
            return (T) new MyViewModelCreateExcel_Gemini(application, excelExporterOptimized);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}