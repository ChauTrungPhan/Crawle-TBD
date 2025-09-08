package com.example.crawlertbdgemini2modibasicview;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class CrawlerViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;
//    public CrawlerViewModelFactory(Context context, WorkManager workManager) {
    public CrawlerViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked") // Bỏ qua cảnh báo ép kiểu không an toàn
    //Tuy nhiên, hãy sử dụng cách này một cách cẩn thận và chỉ khi bạn chắc chắn rằng ép kiểu là an toàn.
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MyViewModelCrawler_Gemini.class)) {
            return (T) new MyViewModelCrawler_Gemini(application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}