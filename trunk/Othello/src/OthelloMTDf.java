/**
 * 
 */

/**
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloMTDf extends OthelloAlphaBeta {

	OthelloMTDf(int minDepthToStore) {
		super(minDepthToStore);
	}
	
	int searchMTDf(OthelloBitBoard position, int turn, int depth) {
		return searchMTDf(position, 0, turn, depth);
	}
	
	int searchMTDf(OthelloBitBoard position, int guess, int turn, int depth) {
		int alpha = 0x80000000;
		int beta  = 0x7FFFFFFF;
		int nullWindow;
		
		do {
			nullWindow = guess;
			if (guess == alpha)
			{
				nullWindow = guess + 1;
			}

			guess = alphaBetaSearch(position, 
					nullWindow-1, nullWindow, turn, depth);
			
			if (guess < nullWindow) { // if it failed low
				beta = guess;
			} else { // it must have failed high
				alpha = guess;
			}
		} while (alpha < beta);
		
		return guess;
	}
	
	int iterativeMTDf(OthelloBitBoard position, int turn, int depth) {
		int guess = 0;
		for (int d = 0; d < depth; d += 2) {
			guess = searchMTDf(position, guess, turn, d);
		}
		 
		return guess;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
