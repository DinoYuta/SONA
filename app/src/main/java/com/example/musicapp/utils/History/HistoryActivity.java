package com.example.musicapp.utils.History;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicapp.AppDatabaseHelper;
import com.example.musicapp.PlayHistory;
import com.example.musicapp.R;
import com.example.musicapp.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private AppDatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private ListView listViewHistory;
    private Button btnClearHistory;
    private HistoryAdapter adapter;
    private List<PlayHistory> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new AppDatabaseHelper(this);
        sessionManager = new SessionManager(this);

        listViewHistory = findViewById(R.id.listViewHistory);
        btnClearHistory = findViewById(R.id.btnClearHistory);

        loadHistory();

        btnClearHistory.setOnClickListener(v -> {
            boolean success = dbHelper.clearHistory(sessionManager.getUserId());
            if (success) {
                loadHistory();
                Toast.makeText(this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadHistory() {
        historyList = dbHelper.getHistoryByUser(sessionManager.getUserId());
        adapter = new HistoryAdapter(this, historyList);
        listViewHistory.setAdapter(adapter);
    }
}