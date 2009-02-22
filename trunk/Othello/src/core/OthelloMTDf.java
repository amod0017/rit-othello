package core;

import java.util.List;
/**
 * 
 */

/**
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloMTDf extends OthelloAlphaBeta {
	int passes = 0;
	
	public OthelloMTDf() { 
		super(); 
	}
	
	public OthelloMTDf(int tableSize) {
		super(tableSize); 
	}
	
	/**
	 * MTD(f) search with default guess
	 * 
	 * @param position : position to analyze
	 * @param turn : current player turn
	 * @return
	 */
	public int searchMTDf() {
		return searchMTDf(0);
	}
	
	/**
	 * MTD(f) search with default guess
	 * 
	 * @param position : position to analyze
	 * @param guess : initial guess of final score
	 * @param turn : current player turn (WHITE or BLACK)
	 * @return
	 */
	public int searchMTDf(int guess) {
		int alpha = LOWESTSCORE;
		int beta  = HIGHESTSCORE;
		int nullWindow;
		
		do {
			++passes;
			nullWindow = guess;
			if (guess == alpha) {
				nullWindow = guess + 1; // make sure nullWindow-1 >= guess
			}

			//null window search about the guess
			guess = alphaBetaSearch(nullWindow-1, nullWindow);
			System.out.println("Window [" + (nullWindow - 1) + ", " + nullWindow + "] = " + guess);
			System.out.println("leaves:" + this.getLeafCount());
			
			if (guess < nullWindow) { // if it failed low
				beta = guess;
			} else { // it must have failed high
				alpha = guess;
			}
		} while (alpha < beta); // do until window converges
		
		scoreOfConfiguration = guess;
		return guess;
	}
	
	/**
	 * MTD(f) in an iterative framework
	 * 
	 * @param position : position to analyze
	 * @param turn : current player turn (WHITE or BLACK)
	 * @return
	 */
	public int iterativeMTDf() {
		int guess = 0;
		
		int finalMaxDepth = maxSearchDepth;
		
		//repeat for 2, 4, 6, 8, etc depth
		//transposition table will retain some results
		for (maxSearchDepth = (finalMaxDepth & 1); 
			maxSearchDepth <= finalMaxDepth; 
			maxSearchDepth += 2) {
			//System.out.println("Searching at..." + maxSearchDepth);
			guess = searchMTDf(guess);
		}
		
		maxSearchDepth = finalMaxDepth;
		
		return guess;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: OthelloAlphaBeta [filename]");
			return;
		}
		
		//read in initial time
		long begin = System.currentTimeMillis();
		
		boolean iterative = true;
		int guess = 0;
		
		System.out.println("MTD(f) search");
		
		OthelloMTDf search = new OthelloMTDf();
		List<String> fileArgs = search.readInputFile(args[0]);
		if (fileArgs == null) {
			return;
		}
		//read in optional file arguments
		String t = findSetting(fileArgs, "IterativeMTDF");
		try {
			if (t != null) {
				iterative = Boolean.parseBoolean(t);
			}
			t = findSetting(fileArgs, "InitialGuess");
			if (t != null) {
				guess = Integer.parseInt(t);
			}
		} catch (Exception e) {
			System.out.println("File Argument error");
		}
		
		//do primary search
		int score;
		if (iterative) {
			score = search.iterativeMTDf();
		} else {
			score = search.searchMTDf(guess);
		}
		
		long searchTime = (System.currentTimeMillis() - begin);
		double leafNodesPerSec = ((double)(search.getLeafCount() * 1000) / (double)searchTime);

		System.out.println("score: " + score);
		System.out.println("leaf nodes: " + search.getLeafCount());
		System.out.println("non-leaf nodes: " + search.getNodesSearched());
		System.out.println("Leaf nodes/sec:" + (long)leafNodesPerSec);
		System.out.println("nodes retreived: " + search.getNodesRetreived());
		System.out.println("table size: " + search.transpositionTable.size());
		
		System.out.println("Search time: " + searchTime);
		
		//do re-search to locate the best move. Not part of main search.
		long r2 = System.currentTimeMillis();
		int bestMove = search.retreiveBestMove();
	
		System.out.println("BestMove: (" + xyTox(bestMove) + ", " + xyToy(bestMove) + ")");
		System.out.println("re-search time: " + (System.currentTimeMillis() - r2));
	}
}
