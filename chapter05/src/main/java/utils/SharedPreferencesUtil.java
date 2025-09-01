package utils;

import android.content.Context;
import android.content.SharedPreferences;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import generator.SudokuGenerator;

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
                                     int errorCount, int filledCells, long elapsedTime,
                                     int hintCount, int[][] fullSolution) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 统一使用Gson转换所有数组数据
        Gson gson = new Gson();
        editor.putString(KEY_SAVED_GAME + "_sudoku", gson.toJson(sudokuData));
        editor.putString(KEY_SAVED_GAME + "_notes", gson.toJson(noteData));

        // 确保fullSolution总是有效
        if (fullSolution == null || !isValidSolution(sudokuData, fullSolution)) {
            fullSolution = generateNewSolution(context, sudokuData);
        }
        editor.putString(KEY_SAVED_GAME + "_solution", gson.toJson(fullSolution));

        editor.putInt(KEY_SAVED_GAME + "_errors", errorCount);
        editor.putInt(KEY_SAVED_GAME + "_filled", filledCells);
        editor.putLong(KEY_SAVED_GAME + "_time", elapsedTime);
        editor.putInt(KEY_SAVED_GAME + "_hints", hintCount);

        editor.apply();
    }

    // 添加解决方案验证方法
    private static boolean isValidSolution(int[][] puzzle, int[][] solution) {
        if (solution == null || solution.length != 9 || solution[0].length != 9) {
            return false;
        }

        // 检查解决方案是否与谜题一致
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (puzzle[i][j] != 0 && puzzle[i][j] != solution[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }

    // 辅助方法：根据当前数独数据生成解决方案
    private static int[][] generateNewSolution(Context context, int[][] sudokuData) {
        // 这里需要实现根据当前数独数据生成解决方案的逻辑
        // 你可以调用SudokuGenerator中的相关方法
        // 例如：
        List<int[]> predefinedValues = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (sudokuData[i][j] != 0) {
                    predefinedValues.add(new int[]{i, j, sudokuData[i][j]});
                }
            }
        }
        return SudokuGenerator.generateSolutionForPuzzle(predefinedValues.toArray(new int[0][]));
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
        Type type = new TypeToken<int[][]>(){}.getType();

        // 添加空值检查
        String sudokuJson = prefs.getString(KEY_SAVED_GAME + "_sudoku", null);
        String notesJson = prefs.getString(KEY_SAVED_GAME + "_notes", null);
        String solutionJson = prefs.getString(KEY_SAVED_GAME + "_solution", null);

        int[][] sudokuData = sudokuJson != null ? gson.fromJson(sudokuJson, type) : new int[9][9];
        int[][] noteData = notesJson != null ? gson.fromJson(notesJson, type) : new int[9][9];
        int[][] fullSolution = solutionJson != null ? gson.fromJson(solutionJson, type) : null;

        return new SavedGameState(
                sudokuData,
                noteData,
                prefs.getInt(KEY_SAVED_GAME + "_errors", 0),
                prefs.getInt(KEY_SAVED_GAME + "_filled", 0),
                prefs.getLong(KEY_SAVED_GAME + "_time", 0),
                prefs.getInt(KEY_SAVED_GAME + "_hints", 0),
                fullSolution
        );
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
        editor.remove(KEY_SAVED_GAME + "_hints");
        editor.remove(KEY_SAVED_GAME + "_solution");

        editor.apply();
    }

    // 保存的游戏状态数据类
    public static class SavedGameState {
        public int[][] sudokuData;
        public int[][] noteData;
        public int errorCount;
        public int filledCells;
        public long elapsedTime;
        public int hintCount; // 添加这个字段
        // 在SavedGameState类中添加
        public int[][] fullSolution;

        // 修改构造函数
        public SavedGameState(int[][] sudokuData, int[][] noteData, int errorCount,
                              int filledCells, long elapsedTime, int hintCount, int[][] fullSolution) {
            this.sudokuData = sudokuData;
            this.noteData = noteData;
            this.errorCount = errorCount;
            this.filledCells = filledCells;
            this.elapsedTime = elapsedTime;
            this.hintCount = hintCount;
            this.fullSolution = fullSolution;
        }
    }
}