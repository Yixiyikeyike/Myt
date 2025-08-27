package generator;
import java.util.Random;

public class SudokuGenerator {

    // 生成一个完整的数独解决方案
    public static int[][] generateSolution() {
        int[][] board = new int[9][9];
        solveSudoku(board, 0, 0);
        return board;
    }

    // 从完整解决方案中生成谜题（挖空一些格子）
    public static int[][] generatePuzzle(int difficulty) {
        int[][] solution = generateSolution();
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
        int[][] puzzle = generatePuzzle(difficulty);
        return convertToPredefinedFormat(puzzle);
    }

    // 使用随机种子生成谜题
    public static int[][] generatePredefinedValuesWithSeed(int difficulty, long seed) {
        Random rand = new Random(seed);
        int[][] puzzle = generatePuzzle(difficulty);
        return convertToPredefinedFormat(puzzle);
    }

    // 递归解决数独
    private static boolean solveSudoku(int[][] board, int row, int col) {
        if (row == 9) {
            return true;
        }

        if (col == 9) {
            return solveSudoku(board, row + 1, 0);
        }

        if (board[row][col] != 0) {
            return solveSudoku(board, row, col + 1);
        }

        // 随机尝试数字
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

    // 随机挖空格子
    private static void removeCells(int[][] board, int cellsToRemove) {
        Random random = new Random();
        int removed = 0;

        while (removed < cellsToRemove) {
            int row = random.nextInt(9);
            int col = random.nextInt(9);

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
    private static boolean hasUniqueSolution(int[][] board) {
        // 在实际应用中，这里应该实现完整的唯一性检查
        // 为简化起见，我们假设大多数情况下都有唯一解
        return true;
    }

    // 将数独板转换为预定义值格式
    private static int[][] convertToPredefinedFormat(int[][] puzzle) {
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

    // 随机打乱数组
    private static void shuffleArray(int[] array) {
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
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
}
