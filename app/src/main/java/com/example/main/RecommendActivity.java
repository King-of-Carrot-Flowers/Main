package com.example.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class RecommendActivity extends AppCompatActivity {
    private String currentUserId;
    private DBManager dbManager;
    private ListView lvRecommend;
    private RecommendAdapter adapter;
    private List<RecommendItem> recommendItems = new ArrayList<>();

    //
    private String aiResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend);

        // 配置导航栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("个性化推荐");


        // 获取当前用户ID
        currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化控件
        dbManager = new DBManager(this);
        lvRecommend = findViewById(R.id.lv_recommend);
        adapter = new RecommendAdapter();
        lvRecommend.setAdapter(adapter);

        // 加载推荐数据
        loadRecommendData();
    }


    // 加载推荐数据（调用大模型）
    private void loadRecommendData() {
        new Thread(() -> {
            // 1. 获取用户已互动和未互动的活动
            List<ActivityBean> interacted = dbManager.getUserInteractedActivities(currentUserId);
            List<ActivityBean> candidate = dbManager.getUninteractedActivities(currentUserId);

            if (interacted.isEmpty() || candidate.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(this, "暂无足够数据生成推荐", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            // 2. 调用大模型获取推荐结果
            aiResult = AiApiUtils.getAiRecommendations(currentUserId, interacted, candidate);

            // 3. 解析大模型返回的JSON
            parseAiResult(aiResult, candidate);

            // 4. 更新UI
            new Handler(Looper.getMainLooper()).post(() -> adapter.notifyDataSetChanged());
        }).start();
    }

    // 解析大模型返回的结果
    private void parseAiResult(String aiResult, List<ActivityBean> candidate) {
        try {
            JSONObject resultJson = new JSONObject(aiResult);
            JSONArray recommendations = resultJson.getJSONArray("recommendations");

            for (int i = 0; i < recommendations.length(); i++) {
                JSONObject item = recommendations.getJSONObject(i);
                int activityId = item.getInt("activity_id");
                String reason = item.getString("reason");

                // 匹配候选活动中的详情
                for (ActivityBean activity : candidate) {
                    if (activity.getId() == activityId) {
                        recommendItems.add(new RecommendItem(activity, reason));
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            // 降级策略：按类型推荐
//            fallbackToTypeRecommend(candidate);
        }
    }

    // 降级策略：按用户兴趣类型推荐
//    private void fallbackToTypeRecommend(List<ActivityBean> candidate) {
//        List<String> tags = dbManager.getUserInterestTags(currentUserId);
//        for (ActivityBean activity : candidate) {
//            if (tags.contains(activity.getType()) && recommendItems.size() < 3) {
//                recommendItems.add(new RecommendItem(activity, "该活动符合你的兴趣类型"));
//            }
//        }
//    }

    // 自定义适配器
    private class RecommendAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return recommendItems.size();
        }

        @Override
        public Object getItem(int position) {
            return recommendItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_recommend, parent, false);
                holder = new ViewHolder();
                holder.tvTitle = convertView.findViewById(R.id.tv_activity_title);
                holder.tvType = convertView.findViewById(R.id.tv_activity_type);
                holder.tvTime = convertView.findViewById(R.id.tv_activity_time);
                holder.tvReason = convertView.findViewById(R.id.tv_recommend_reason);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // 设置数据
            RecommendItem item = recommendItems.get(position);
            holder.tvTitle.setText(item.activity.getTitle());
            holder.tvType.setText("类型：" + item.activity.getType());
            holder.tvTime.setText("时间：" + item.activity.getTime());
            holder.tvReason.setText("推荐理由：" + item.reason);

            // 点击跳转到活动详情
            convertView.setOnClickListener(v -> {
                ActivityBean activity = item.activity;
                startActivity(new Intent(RecommendActivity.this, DetailActivity.class)
                        .putExtra("ACTIVITY_ID", activity.getId())
                        .putExtra("USER_ID", currentUserId));
            });

            return convertView;
        }

        class ViewHolder {
            TextView tvTitle, tvType, tvTime, tvReason;
        }
    }

    // 推荐项实体类
    public static class RecommendItem {
        ActivityBean activity;
        String reason;

        public RecommendItem(ActivityBean activity, String reason) {
            this.activity = activity;
            this.reason = reason;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}