package generator;
import java.util.Random;

public class SudokuGenerator {
    // 使用静态Random对象，确保所有随机操作使用同一个种子
    private static Random seededRandom;
    // 生成一个完整的数独解决方案（使用种子）
    public static int[][] generateSolution(long seed) {
        seededRandom = new Random(seed);
        int[][] board = new int[9][9];
        solveSudoku(board, 0, 0);
        return board;
    }

    // 从完整解决方案中生成谜题（挖空一些格子）
    public static int[][] generatePuzzle(int difficulty, long seed) {
        int[][] solution = generateSolution(seed);
        int[][] puzzle = new int[9][9];

        // 复制完整解决方案
        for (int i = 0; i < 9; i++) {
            System.arraycopy(solution[i], 0, puzzle[i], 0, 9);
        }

        // 根据难度级别挖空格子
        int cellsToRemove;
        switch (difficulty) {
            case 1: // 简单
                cellsToRemove = 30;
                break;
            case 2: // 中等
                cellsToRemove = 40;
                break;
            case 3: // 困难
                cellsToRemove = 50;
                break;
            default: // 默认中等
                cellsToRemove = 40;
        }

        removeCells(puzzle, cellsToRemove);
        return puzzle;
    }

    // 生成预定义值数组格式
    public static int[][] generatePredefinedValues(int difficulty) {
        long seed = System.currentTimeMillis();
        int[][] puzzle = generatePuzzle(difficulty, seed);
        return convertToPredefinedFormat(puzzle);
    }

    // 使用随机种子生成谜题
    public static int[][] generatePredefinedValuesWithSeed(int difficulty, long seed) {
        seededRandom = new Random(seed);
        int[][] puzzle = generatePuzzle(difficulty, seed);
        return convertToPredefinedFormat(puzzle);
    }

    // 递归解决数独（确保不修改预填数字）
    private static boolean solveSudoku(int[][] board, int row, int col) {
        if (row == 9) {
            return true;
        }

        if (col == 9) {
            return solveSudoku(board, row + 1, 0);
        }

        // 跳过预填数字
        if (board[row][col] != 0) {
            return solveSudoku(board, row, col + 1);
        }

        // 随机尝试数字（使用种子随机）
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        shuffleArray(numbers);

        for (int num : numbers) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num;
                if (solveSudoku(board, row, col + 1)) {
                    return true;
                }
                board[row][col] = 0;
            }
        }

        return false;
    }

    // 检查数字是否有效
    private static boolean isValid(int[][] board, int row, int col, int num) {
        // 检查行
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == num) {
                return false;
            }
        }

        // 检查列
        for (int i = 0; i < 9; i++) {
            if (board[i][col] == num) {
                return false;
            }
        }

        // 检查3x3宫格
        int boxRow = (row / 3) * 3;
        int boxCol = (col / 3) * 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[boxRow + i][boxCol + j] == num) {
                    return false;
                }
            }
        }

        return true;
    }

    // 随机挖空格子（使用种子随机）
    private static void removeCells(int[][] board, int cellsToRemove) {
        int removed = 0;

        while (removed < cellsToRemove) {
            int row = seededRandom.nextInt(9);
            int col = seededRandom.nextInt(9);

            if (board[row][col] != 0) {
                int temp = board[row][col];
                board[row][col] = 0;

                // 确保谜题有唯一解
                if (hasUniqueSolution(board)) {
                    removed++;
                } else {
                    board[row][col] = temp;
                }
            }
        }
    }

    // 检查谜题是否有唯一解（简化版）
// 检查数独谜题是否有唯一解
    private static boolean hasUniqueSolution(int[][] board) {
        // 创建副本以避免修改原始板
        int[][] boardCopy = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(board[i], 0, boardCopy[i], 0, 9);
        }

        // 尝试找到所有解决方案
        return countSolutions(boardCopy, 0, 0) == 1;
    }

    // 递归计算数独解决方案的数量
    private static int countSolutions(int[][] board, int row, int col) {
        // 如果已经到达最后一行，找到一个解
        if (row == 9) {
            return 1;
        }

        // 如果到达列尾，转到下一行
        if (col == 9) {
            return countSolutions(board, row + 1, 0);
        }

        // 如果单元格已有数字，跳过
        if (board[row][col] != 0) {
            return countSolutions(board, row, col + 1);
        }

        int solutions = 0;

        // 尝试1-9的数字
        for (int num = 1; num <= 9 && solutions < 2; num++) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num;
                solutions += countSolutions(board, row, col + 1);
                board[row][col] = 0; // 回溯
            }
        }

        return solutions;
    }

    // 将数独板转换为预定义值格式
    public static int[][] convertToPredefinedFormat(int[][] puzzle) {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (puzzle[i][j] != 0) {
                    count++;
                }
            }
        }

        int[][] predefinedValues = new int[count][3];
        int index = 0;

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (puzzle[i][j] != 0) {
                    predefinedValues[index][0] = i; // 行
                    predefinedValues[index][1] = j; // 列
                    predefinedValues[index][2] = puzzle[i][j]; // 值
                    index++;
                }
            }
        }

        return predefinedValues;
    }

    // 随机打乱数组（使用种子随机）
    private static void shuffleArray(int[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = seededRandom.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    // 打印数独板（用于调试）
    public static void printBoard(int[][] board) {
        for (int i = 0; i < 9; i++) {
            if (i % 3 == 0 && i != 0) {
                System.out.println("------+-------+------");
            }
            for (int j = 0; j < 9; j++) {
                if (j % 3 == 0 && j != 0) {
                    System.out.print("| ");
                }
                System.out.print(board[i][j] == 0 ? ". " : board[i][j] + " ");
            }
            System.out.println();
        }
    }
    // 生成与预填数字匹配的解决方案
    public static int[][] generateSolutionForPuzzle(int[][] predefinedValues) {
        int[][] board = new int[9][9];

        // 应用预填数字
        for (int[] predefined : predefinedValues) {
            int row = predefined[0];
            int col = predefined[1];
            int value = predefined[2];
            board[row][col] = value;
        }

        // 解决剩余部分
        solveSudoku(board, 0, 0);
        return board;
    }

}
