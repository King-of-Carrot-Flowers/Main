package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DetailActivity extends AppCompatActivity {
    private DBManager dbManager;
    private int activityId;
    private ActivityBean activityBean;
    private String currentUserId;
    private TextView tvTitle, tvType, tvTime, tvLocation, tvOrganizer, tvApplyCount, tvDescription;
    private Button btnCollect, btnApply, btnDebugPrint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("活动详情");

        activityId = getIntent().getIntExtra("ACTIVITY_ID", -1);
        currentUserId = getIntent().getStringExtra("USER_ID");
        if (activityId == -1) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (currentUserId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        dbManager = new DBManager(this);
        loadActivityDetail();
        setButtonListeners();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvType = findViewById(R.id.tv_type);
        tvTime = findViewById(R.id.tv_time);
        tvLocation = findViewById(R.id.tv_location);
        tvOrganizer = findViewById(R.id.tv_organizer);
        tvApplyCount = findViewById(R.id.tv_apply_count);
        tvDescription = findViewById(R.id.tv_description);
        btnCollect = findViewById(R.id.btn_collect);
        btnApply = findViewById(R.id.btn_apply);
        btnDebugPrint = findViewById(R.id.btn_debug_print);
    }

    private void loadActivityDetail() {
        new Thread(() -> {
            activityBean = dbManager.getActivityById(activityId);
            if (activityBean == null) {
                new Handler(Looper.getMainLooper()).post(this::finish);
                return;
            }
            int currentPeople = dbManager.getCurrentPeopleByActivityId(activityId);
            boolean isApplied = dbManager.isUserApplied(currentUserId, activityId);
            boolean isCollected = dbManager.isUserCollected(currentUserId, activityId);
            boolean isFull = currentPeople >= activityBean.getMaxPeople();

            new Handler(Looper.getMainLooper()).post(() -> {
                tvTitle.setText(activityBean.getTitle());
                tvType.setText("活动类型：" + activityBean.getType());
                tvTime.setText("活动时间：" + activityBean.getTime());
                tvLocation.setText("活动地点：" + activityBean.getLocation());
                tvOrganizer.setText("主办方：" + activityBean.getOrganizer());
                tvApplyCount.setText("报名情况：" + currentPeople + "/" + activityBean.getMaxPeople());
                tvDescription.setText("活动详情：\n" + activityBean.getDescription());

                btnCollect.setText(isCollected ? "已收藏" : "收藏");
                btnCollect.setBackgroundTintList(getResources().getColorStateList(
                        isCollected ? android.R.color.darker_gray : R.color.orange));
                updateApplyButtonStatus(isFull, isApplied);
            });
        }).start();
    }

    private void updateApplyButtonStatus(boolean isFull, boolean isApplied) {
        if (isApplied) {
            btnApply.setText("取消报名");
            btnApply.setBackgroundTintList(getResources().getColorStateList(R.color.red));
            btnApply.setEnabled(true);
        } else if (isFull) {
            btnApply.setText("人数已满");
            btnApply.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
            btnApply.setEnabled(false);
        } else {
            btnApply.setText("报名参加");
            btnApply.setBackgroundTintList(getResources().getColorStateList(R.color.blue));
            btnApply.setEnabled(true);
        }
    }

    private void setButtonListeners() {
        // 收藏按钮逻辑修复
        btnCollect.setOnClickListener(v -> new Thread(() -> {
            boolean isCollected = dbManager.isUserCollected(currentUserId, activityId);
            boolean success;

            if (isCollected) {
                success = dbManager.uncollectActivity(currentUserId, activityId);
                if (success) {
                    // 【修复】取消注释：取消收藏时，取消提醒
                    ReminderUtils.cancelReminder(DetailActivity.this, activityId);
                }
            } else {
                success = dbManager.collectActivity(currentUserId, activityId);
                if (success) {
                    // 【修复】取消注释：收藏时，设置提醒
                    ReminderUtils.setReminder(DetailActivity.this, currentUserId, activityBean);
                }
            }

            if (success) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnCollect.setText(isCollected ? "收藏" : "已收藏");
                    btnCollect.setBackgroundTintList(getResources().getColorStateList(
                            isCollected ? R.color.orange : android.R.color.darker_gray));
                    Toast.makeText(DetailActivity.this,
                            isCollected ? "取消收藏成功" : "收藏成功",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start());

        btnApply.setOnClickListener(v -> new Thread(() -> {
            boolean isApplied = dbManager.isUserApplied(currentUserId, activityId);
            int currentPeople = dbManager.getCurrentPeopleByActivityId(activityId);
            boolean isFull = currentPeople >= activityBean.getMaxPeople();
            if (isFull && !isApplied) return;

            boolean success;
            if (isApplied) {
                success = dbManager.cancelApplyActivity(currentUserId, activityId);
                currentPeople--;
            } else {
                success = dbManager.applyActivity(currentUserId, activityId);
                currentPeople++;
            }

            if (success) {
                final int c = currentPeople;
                new Handler(Looper.getMainLooper()).post(() -> {
                    tvApplyCount.setText("报名情况：" + c + "/" + activityBean.getMaxPeople());
                    updateApplyButtonStatus(c >= activityBean.getMaxPeople(), !isApplied);
                    Toast.makeText(DetailActivity.this,
                            isApplied ? "取消报名成功" : "报名成功",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start());

        btnDebugPrint.setOnClickListener(v -> printAllTableData());
    }

    private void printAllTableData() {
        new Thread(() -> {
            dbManager.printUsersTable();
            dbManager.printActivitiesTable();
            dbManager.printRegistrationsTable();
            dbManager.printFavoritesTable();
            dbManager.printRemindersTable();
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(this, "已打印所有表数据到Logcat", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }
}