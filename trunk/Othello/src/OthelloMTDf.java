/**
 * 
 */

/**
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloMTDf extends OthelloAlphaBeta {
	int passes = 0;
	
	OthelloMTDf() {}
	
	/**
	 * MTD(f) search with default guess
	 * 
	 * @param position : position to analyze
	 * @param turn : current player turn
	 * @return
	 */
	int searchMTDf(OthelloBitBoard position, int turn) {
		return searchMTDf(position, 0, turn);
	}
	
	/**
	 * MTD(f) search with default guess
	 * 
	 * @param position : position to analyze
	 * @param guess : initial guess of final score
	 * @param turn : current player turn (WHITE or BLACK)
	 * @return
	 */
	int searchMTDf(OthelloBitBoard position, int guess, int turn) {
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
			guess = alphaBetaSearch(position, 
					nullWindow-1, nullWindow, turn);
			
			if (guess < nullWindow) { // if it failed low
				beta = guess;
			} else { // it must have failed high
				alpha = guess;
			}
		} while (alpha < beta); // do until window converges
		
		return guess;
	}
	
	/**
	 * MTD(f) in an iterative framework
	 * 
	 * @param position : position to analyze
	 * @param turn : current player turn (WHITE or BLACK)
	 * @return
	 */
	int iterativeMTDf(OthelloBitBoard position, int turn) {
		int guess = 0;
		
		int finalMaxDepth = maxSearchDepth;
		
		//repeat for 2, 4, 6, 8, etc depth
		//transposition table will retain some results
		for (maxSearchDepth = 0; maxSearchDepth <= finalMaxDepth; maxSearchDepth += 2) {
			guess = searchMTDf(position, guess, turn);
		}
		 
		return guess;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long begin = System.currentTimeMillis();

		System.out.println("MTD(f) search");
		OthelloBitBoard test1 = new OthelloBitBoard(0x0000002C14000000L, 0x0000381028040000L);
		//OthelloBitBoard test1 = new OthelloBitBoard(0xFF9F8D1D0D0F07D9L, 0x004002828200C824L);
		
		OthelloMTDf testObj = new OthelloMTDf();
		testObj.setMaxSearchDepth(12);
		testObj.setLevelsToSort(3);
		testObj.setMinDepthToStore(4);

		int score = testObj.iterativeMTDf(test1, OthelloBitBoard.WHITE);
		
		System.out.println("score: " + score);
		System.out.println("leaf nodes: " + testObj.getLeafCount());
		System.out.println("non-leaf nodes: " + testObj.getNodesSearched());
		System.out.println("nodes retreived: " + testObj.getNodesRetreived());
		System.out.println("passes: " + testObj.passes);
		
		System.out.println("time: " + (System.currentTimeMillis() - begin));
	}

}
