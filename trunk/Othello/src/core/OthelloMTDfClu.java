package core;


import java.io.IOException;

import edu.rit.pj.Comm;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelSection;
import edu.rit.pj.ParallelTeam;

/**
 * Cluster Design
 *
 * @author Nicholas Ver Hoeve
 * @author Joseph Pecoraro
 */
public class OthelloMTDfClu extends OthelloAlphaBeta {

	// World Details
	static Comm world;
	static int size;
	static int rank;

	int passes = 0;

	public OthelloMTDfClu() {
		super();
	}

	public OthelloMTDfClu(int tableSize) {
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
		for (maxSearchDepth = (finalMaxDepth & 1); maxSearchDepth <= finalMaxDepth; maxSearchDepth += 2) {
			System.out.println("Searching at..." + maxSearchDepth);



			guess = searchMTDf(guess);
		}

		maxSearchDepth = finalMaxDepth;

		return guess;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// Job Scheduler
		try {
			Comm.init(args);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Begin
		long begin = System.currentTimeMillis();

		// World Details
		world = Comm.world();
		size = world.size();
		rank = world.rank();


		// Debug
		System.out.println("MTD(f) search");

	    try {

	    	// Master handles the root but also has a worker like everyone else
		    if (rank == 0) {
		    	new ParallelTeam(2).execute (new ParallelRegion() {
		    		public void run() throws Exception {
		    			execute (new ParallelSection() {
		    				public void run() throws Exception {
		    					masterSection();
		    				}
		    			},
		    			new ParallelSection() {
		    				public void run() throws Exception {
		    					workerSection();
		    				}
		    			});
		    		}
		    	});

		    // Worker only does a worker section
		    } else {
		    	workerSection();
		    }

	    } catch (Exception e) {
	    	System.err.println("Exception thrown from a thread.");
	    	e.printStackTrace();
	    	System.exit(1);
	    }


	    // End
	    System.out.println("time: " + (System.currentTimeMillis() - begin));


	}


	/**
	 * Master - Always handles the root node
	 */
	private static void masterSection() {

		// Game board to test
    	//OthelloBitBoard test1 = new OthelloBitBoard(0xFF9F8D1D0D0F07D9L, 0x004002828200C824L);
		OthelloBitBoard test1 = new OthelloBitBoard(0x0000002C14000000L, 0x0000381028040000L);
		OthelloMTDfClu testObj = new OthelloMTDfClu();
		testObj.setMaxSearchDepth(12);
		testObj.setLevelsToSort(3);
		testObj.setMinDepthToStore(4);
		testObj.setRootNode(test1, WHITE);

		// Jump start
		int score = testObj.iterativeMTDf();

		System.out.println("score: " + score);
		System.out.println("leaf nodes: " + testObj.getLeafCount());
		System.out.println("non-leaf nodes: " + testObj.getNodesSearched());
		System.out.println("nodes retrieved: " + testObj.getNodesRetreived());
		System.out.println("passes: " + testObj.passes);

	}



	/**
	 * Worker - Gather incoming work nodes, and process them
	 */
	private static void workerSection() throws IOException {


	}



}


