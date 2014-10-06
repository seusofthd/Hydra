package com.symlab.testoffloading;

import android.util.Log;

import com.symlab.hydra.lib.Offloadable;

public class NQueens extends Offloadable {

	private static final long	serialVersionUID = 5687713591581731140L;
	private static final String TAG = "NQueens";
	
	private int N;

	public int solveNQueens(int N, int start, int end) {
		this.N = N;

		byte[][] board = new byte[N][N];
		int countSolutions = 0;
		
		System.out.println("Finding solutions for " + N + "-queens puzzle.");
		
		for (int i = start; i < end; i++) {
			for (int j = 0; j < N; j++) {
				for (int k = 0; k < N; k++) {
					for (int l = 0; l < N; l++) {
						if (N == 4) {
							countSolutions += setAndCheckBoard(board, new int[]{i, j, k, l});
							continue;
						}
						for (int m = 0; m < N; m++) {
							if (N == 5) {
								countSolutions += setAndCheckBoard(board, new int[]{i, j, k, l, m});
								continue;
							}
							for (int n = 0; n < N; n++) {
								if (N == 6) {
									countSolutions += setAndCheckBoard(board, new int[]{i, j, k, l, m, n});
									continue;
								}
								for (int o = 0; o < N; o++) {
									if (N == 7) {
										countSolutions += setAndCheckBoard(board, new int[]{i, j, k, l, m, n, o});
										continue;
									}
									for (int p = 0; p < N; p++) {
											countSolutions += setAndCheckBoard(board, new int[]{i, j, k, l, m, n, o, p});
									}
								}
							}
						}
					}
				}
			}
		}

		System.out.println("Found " + countSolutions + " solutions.");
		return countSolutions;
	}

	
	private int setAndCheckBoard(byte[][] board, int... cols) {
		
		clearBoard(board);
		
		for (int i = 0; i < N; i++)
			board[i][cols[i]] = 1;
		
		if (isSolution(board)) return 1;
		
		return 0;
	}

	private void clearBoard(byte[][] board) {
		for (int i = 0; i < N; i ++) {
			for (int j = 0; j < N; j++) {
				board[i][j] = 0;
			}
		}
	}

	private boolean isSolution(byte[][] board) {

		int rowSum = 0;
		int colSum = 0;

		for (int i = 0; i < N; i++) {
			for (int j = 0;  j < N; j++) {
				rowSum += board[i][j];
				colSum += board[j][i];

				if (i == 0 || j == 0)
					if ( !checkDiagonal1(board, i, j) ) return false;

				if (i == 0 || j == N-1)
					if ( !checkDiagonal2(board, i, j) ) return false;

			}
			if (rowSum > 1 || colSum > 1) return false;
			rowSum = 0;
			colSum = 0;
		}

		return true;
	}

	private boolean checkDiagonal1(byte[][] board, int row, int col) {
		int sum = 0;
		int i = row;
		int j = col;
		while (i < N && j < N) {
			sum += board[i][j];
			i++;
			j++;
		}
		return sum <= 1;
	}

	private boolean checkDiagonal2(byte[][] board, int row, int col) {
		int sum = 0;
		int i = row;
		int j = col;
		while (i < N && j >=0) {
			sum += board[i][j];
			i++;
			j--;
		}
		return sum <= 1;
	}


	private void printBoard(byte[][] board) {
		for (int i = 0; i < N; i++) {
			StringBuilder row = new StringBuilder();
			for (int j = 0;  j < N; j++) {
				row.append(board[i][j]);
				if (j < N - 1)
					row.append(" ");
			}
			Log.i(TAG, row.toString());
		}
		Log.i(TAG, "\n");
	}

	@Override
	public void copyState(Offloadable state) {
		
	}
}

