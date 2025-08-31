package utils;

import android.content.Context;
import android.content.SharedPreferences;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPreferencesUtil {
    private static final String PREFS_NAME = "SudokuPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_GAME_RECORDS = "game_records";
    private static final Gson gson = new Gson();

    // 保存用户名
    public static void saveUsername(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    // 获取用户名
    public static String getUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, null);
    }

    // 清除用户名
    public static void clearUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_USERNAME).apply();
    }

    // 保存游戏记录
    public static void saveGameRecord(Context context, GameRecord record) {
        List<GameRecord> records = getGameRecords(context);
        records.add(record);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(records);
        prefs.edit().putString(KEY_GAME_RECORDS, json).apply();
    }

    // 获取所有游戏记录
    public static List<GameRecord> getGameRecords(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_GAME_RECORDS, null);

        if (json == null) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<GameRecord>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // 计算平均分
    public static double calculateAverageScore(Context context) {
        List<GameRecord> records = getGameRecords(context);
        if (records.isEmpty()) {
            return 0.0;
        }

        double sum = 0;
        for (GameRecord record : records) {
            sum += record.getScore();
        }
        return sum / records.size();
    }

    // 游戏记录数据类
    public static class GameRecord {
        private int stars;
        private String time;
        private double score;
        private long timestamp;

        public GameRecord(int stars, String time, double score) {
            this.stars = stars;
            this.time = time;
            this.score = score;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public int getStars() { return stars; }
        public String getTime() { return time; }
        public double getScore() { return score; }
        public long getTimestamp() { return timestamp; }
    }
    private static final String KEY_HIGH_SCORE = "high_score";

    // 保存最高分
    public static void saveHighScore(Context context, int score) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_HIGH_SCORE, score).apply();
    }

    // 获取最高分
    public static int getHighScore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_HIGH_SCORE, 0);
    }

    // 检查并更新最高分
    public static boolean checkAndUpdateHighScore(Context context, int score) {
        int currentHigh = getHighScore(context);
        if (score > currentHigh) {
            saveHighScore(context, score);
            return true;
        }
        return false;
    }
    public static void saveDifficulty(Context context, int difficulty) {
        SharedPreferences prefs = context.getSharedPreferences("SudokuPrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt("difficulty", difficulty).apply();
    }

    public static int getDifficulty(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SudokuPrefs", Context.MODE_PRIVATE);
        return prefs.getInt("difficulty", 2); // 默认中等难度
    }
}