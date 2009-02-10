package core;
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
			//System.out.println("Window [" + (nullWindow - 1) + ", " + nullWindow + "] = " + guess);
			
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
		for (maxSearchDepth = 0; maxSearchDepth <= finalMaxDepth; maxSearchDepth += 2) {
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
		long begin = System.currentTimeMillis();

		System.out.println("MTD(f) search");
		OthelloBitBoard test1 = new OthelloBitBoard(0x0000002C14000000L, 0x0000381028040000L);
		//OthelloBitBoard test1 = new OthelloBitBoard(0xFF9F8D1D0D0F07D9L, 0x004002828200C824L);
		
		OthelloMTDf testObj = new OthelloMTDf();
		testObj.setMaxSearchDepth(12);
		testObj.setLevelsToSort(3);
		testObj.setMinDepthToStore(4);
		testObj.setRootNode(test1, WHITE);

		int score = testObj.iterativeMTDf();
		
		System.out.println("score: " + score);
		System.out.println("leaf nodes: " + testObj.getLeafCount());
		System.out.println("non-leaf nodes: " + testObj.getNodesSearched());
		System.out.println("nodes retreived: " + testObj.getNodesRetreived());
		System.out.println("passes: " + testObj.passes);
		
		System.out.println("time: " + (System.currentTimeMillis() - begin));
	}

}
