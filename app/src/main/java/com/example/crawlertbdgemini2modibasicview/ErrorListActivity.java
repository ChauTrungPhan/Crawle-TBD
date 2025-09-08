package com.example.crawlertbdgemini2modibasicview;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crawlertbdgemini2modibasicview.databinding.ActivityErrorListBinding;
import com.example.crawlertbdgemini2modibasicview.databinding.ActivityMainBinding;
import com.example.crawlertbdgemini2modibasicview.utils.CrawlType;
import com.example.crawlertbdgemini2modibasicview.utils.SettingsRepository;

import java.util.List;

public class ErrorListActivity extends AppCompatActivity {
    ActivityErrorListBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityErrorListBinding.inflate(getLayoutInflater()); // Initialize View Binding
        //setContentView(R.layout.activity_error_list);
        setContentView(binding.getRoot());  // Set the root view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //RecyclerView recyclerView = findViewById(R.id.recyclerView);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //DBHelperThuoc dbHelper = new DBHelperThuoc(this);
        DBHelperThuoc dbHelper = DBHelperThuoc.getInstance(this.getApplicationContext());
        CrawlType crawlType = new SettingsRepository(this.getApplicationContext()).getSelectedCrawlType();
        List<ErrorUrl> errors = dbHelper.getListErrorUrls(crawlType);

        ErrorAdapter adapter = new ErrorAdapter(errors);
        binding.recyclerView.setAdapter(adapter);

    }
}