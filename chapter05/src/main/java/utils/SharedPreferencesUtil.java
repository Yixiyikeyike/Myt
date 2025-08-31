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
    private static final String KEY_SAVED_GAME = "saved_game";

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

    // 保存游戏状态
    public static void saveGameState(Context context, int[][] sudokuData, int[][] noteData,
                                     int errorCount, int filledCells, long elapsedTime) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 将游戏数据转换为JSON字符串
        Gson gson = new Gson();
        editor.putString(KEY_SAVED_GAME + "_sudoku", gson.toJson(sudokuData));
        editor.putString(KEY_SAVED_GAME + "_notes", gson.toJson(noteData));
        editor.putInt(KEY_SAVED_GAME + "_errors", errorCount);
        editor.putInt(KEY_SAVED_GAME + "_filled", filledCells);
        editor.putLong(KEY_SAVED_GAME + "_time", elapsedTime);

        editor.apply();
    }

    // 检查是否有保存的游戏
    public static boolean hasSavedGame(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_SAVED_GAME + "_sudoku");
    }

    // 加载游戏状态
    public static SavedGameState loadGameState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Gson gson = new Gson();
        String sudokuJson = prefs.getString(KEY_SAVED_GAME + "_sudoku", "");
        String notesJson = prefs.getString(KEY_SAVED_GAME + "_notes", "");

        Type type = new TypeToken<int[][]>(){}.getType();
        int[][] sudokuData = gson.fromJson(sudokuJson, type);
        int[][] noteData = gson.fromJson(notesJson, type);

        int errorCount = prefs.getInt(KEY_SAVED_GAME + "_errors", 0);
        int filledCells = prefs.getInt(KEY_SAVED_GAME + "_filled", 0);
        long elapsedTime = prefs.getLong(KEY_SAVED_GAME + "_time", 0);

        return new SavedGameState(sudokuData, noteData, errorCount, filledCells, elapsedTime);
    }

    // 清除保存的游戏
    public static void clearSavedGame(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove(KEY_SAVED_GAME + "_sudoku");
        editor.remove(KEY_SAVED_GAME + "_notes");
        editor.remove(KEY_SAVED_GAME + "_errors");
        editor.remove(KEY_SAVED_GAME + "_filled");
        editor.remove(KEY_SAVED_GAME + "_time");

        editor.apply();
    }

    // 保存的游戏状态数据类
    public static class SavedGameState {
        public int[][] sudokuData;
        public int[][] noteData;
        public int errorCount;
        public int filledCells;
        public long elapsedTime;

        public SavedGameState(int[][] sudokuData, int[][] noteData, int errorCount,
                              int filledCells, long elapsedTime) {
            this.sudokuData = sudokuData;
            this.noteData = noteData;
            this.errorCount = errorCount;
            this.filledCells = filledCells;
            this.elapsedTime = elapsedTime;
        }
    }
}