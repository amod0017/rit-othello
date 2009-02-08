import java.util.*;

/**
 * 
 */

/**
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloAlphaBeta {
	Map<HashBoard, Window> transpositionTable;
	int minDepthToStore;
	int valueOfDraw;
	
	static final int NOSCORE = 0x80000000;
	static final int LOWESTSCORE = 0x80000001;
	static final int HIGHESTSCORE = 0x7FFFFFFF;
	
	int leafCount = 0;
	int nodesSearched = 0;
	int nodesRetrieved = 0;
	
	/*
	 * This class bundles alpha and beta so the range can be stored in a map
	 */
	public static class Window {
		int alpha;
		int beta;
		
		Window() {}
		Window(int alpha, int beta) {
			this.alpha = alpha;
			this.beta = beta;
		}
	}
	
	/*
	 * This class is needed because the depth of the board when analyzed must
	 * be included and factored into the hashcode.
	 */
	public static class HashBoard extends OthelloBitBoard {
		int depth;
		
		public HashBoard(OthelloBitBoard board, int depth) {
			white = board.white;
			black = board.black;
		}
		
		public int hashCode() {
			return super.hashCode() ^ (depth * 136385313);
		}
		
		public boolean equals(Object other) {
			return super.equals(other) && 
				(other instanceof HashBoard) && 
				((HashBoard)other).depth == depth;
		}
	};
	
	OthelloAlphaBeta(int minDepthToStore) {
		this.valueOfDraw = 0;
		this.minDepthToStore = minDepthToStore;
		transpositionTable = new HashMap<HashBoard, Window>();
	}
	
	public int alphaBetaSearch(OthelloBitBoard position, int alpha, int beta, 
			int turn, int depth) {
		HashBoard hashBoard = new HashBoard(position, depth);
		Window storedWindow = transpositionTable.get(hashBoard);
		
		++nodesSearched;
		
		if (storedWindow != null)
		{
			++nodesRetrieved;
			
			//if we know that this stored position
			if (storedWindow.alpha >= beta) {
				return storedWindow.alpha;
			}
			if (storedWindow.beta <= alpha) {
				return storedWindow.alpha;
			}
			
			//align windows
			alpha = Math.min(alpha, storedWindow.alpha);
			beta = Math.min(beta, storedWindow.beta);
		} else {
			storedWindow = new Window(); // go ahead and allocate
		}
		
		if (alpha == beta) {
			return alpha;
		}
		
		int bestScore = NOSCORE;
		
		for (long likelyMoves = position.generateLikelyMoves(turn);
				likelyMoves != 0;
				likelyMoves &= (likelyMoves - 1)) {
			int movePos = BitUtil.ulog2(BitUtil.lowSetBit(likelyMoves));
			int moveX = OthelloBitBoard.xyTox(movePos);
			int moveY = OthelloBitBoard.xyToy(movePos);
			
			if (!position.moveIsLegal(moveX, moveY, turn)) {
				continue;
			}
			
			OthelloBitBoard newPosition = position.copyAndMakeMove(moveX, moveY, turn);
			
			int newScore;
			if (depth <= 1) { // base case
				newScore = evaluateLeaf(newPosition, turn);
				++leafCount;
			} else {//recurse
				if (depth < minDepthToStore) {
					newScore = alphaBetaNoTable(newPosition, -beta, 
							-Math.max(alpha, bestScore), turn ^ 1, depth - 1);
				} else {
					newScore = alphaBetaSearch(newPosition, -beta, 
							-Math.max(alpha, bestScore), turn ^ 1, depth - 1);
				}
			}
			
			if (newScore > bestScore) {
				bestScore = newScore;
			}	
		}
		
		if (bestScore == NOSCORE) { // if NO move was found... the game is over here
			if (position.canMove(turn ^ 1)) {
				// player loses turn
				if (depth < minDepthToStore) {
					bestScore = alphaBetaNoTable(position, -beta, -alpha, turn ^ 1, depth - 1);
				} else {
					bestScore = alphaBetaSearch(position, -beta, -alpha, turn ^ 1, depth - 1);
				}
			} else {
				//end of game
				bestScore = evaluateEnd(position, turn);
			}
		}
		
		if (bestScore <= alpha) { // if fail low
			storedWindow.beta = bestScore; // we know that at BEST the score is this bad
		} else if (bestScore >= beta) { // if fail high
			storedWindow.alpha = bestScore; // we know that at WORST the score is this good
		} else {
			storedWindow.alpha = storedWindow.beta = bestScore; // store exact value
		}
		
		transpositionTable.put(hashBoard, storedWindow); // store results for future lookup
		
		return bestScore;
	}
	
	public int alphaBetaNoTable(OthelloBitBoard position, int alpha, int beta, 
			int turn, int depth) {
		++nodesSearched;
		int bestScore = NOSCORE;
		
		for (long likelyMoves = position.generateLikelyMoves(turn);
				likelyMoves != 0;
				likelyMoves &= (likelyMoves - 1)) {
			int movePos = BitUtil.ulog2(BitUtil.lowSetBit(likelyMoves));
			int moveX = OthelloBitBoard.xyTox(movePos);
			int moveY = OthelloBitBoard.xyToy(movePos);
			
			if (!position.moveIsLegal(moveX, moveY, turn)) {
				continue;
			}
			
			OthelloBitBoard newPosition = position.copyAndMakeMove(moveX, moveY, turn);
			
			int newScore;
			if (depth <= 1) { // base case
				newScore = evaluateLeaf(newPosition, turn);
				++leafCount;
			} else {//recurse
				newScore = alphaBetaNoTable(newPosition, -beta, 
						-Math.max(alpha, bestScore), turn ^ 1, depth - 1);
			}
			
			if (newScore > bestScore) {
				bestScore = newScore;
			}	
		}
		
		if (bestScore == NOSCORE) { // if NO move was found...
			if (position.canMove(turn ^ 1)) {
				// player loses turn
				bestScore = alphaBetaNoTable(position, -beta, -alpha, turn ^ 1, depth - 1);
			} else {
				//end of game
				bestScore = evaluateEnd(position, turn);
			}
		}
		
		return bestScore;
	}
	
	public static int evaluateLeaf(OthelloBitBoard position, int state) {
		return (position.countPieces(state) - position.countPieces(state ^ 1));
	}
	
	private int evaluateEnd(OthelloBitBoard position, int state) {
		int pieceDiff = position.countPieces(state) - position.countPieces(state ^ 1);
		
		if (pieceDiff < 0) {
			return LOWESTSCORE; // LOSE
		} else if (pieceDiff == 0) {
			return valueOfDraw;
		} else {
			return HIGHESTSCORE; //win
		}
	}

	public int getLeafCount() {
		return leafCount;
	}

	public int getNodesSearched() {
		return nodesSearched;
	}
	
	public int getNodesRetreived() {
		return nodesRetrieved;
	}
	
	public void resetCounters() {
		leafCount = 0;
		nodesSearched = 0;
		nodesRetrieved = 0;
	}

	/**
	 * @param args
	 * 
	 * run alpha-beta tests
	 */
	public static void main(String[] args) {
		long begin = System.currentTimeMillis();

		OthelloBitBoard test1 = new OthelloBitBoard();
		
		OthelloAlphaBeta testObj = new OthelloAlphaBeta(4);

		int score = testObj.alphaBetaSearch(test1, LOWESTSCORE, HIGHESTSCORE, OthelloBitBoard.WHITE, 9);
		
		System.out.println("score: " + score);
		System.out.println("leaf nodes: " + testObj.getLeafCount());
		System.out.println("non-leaf nodes: " + testObj.getNodesSearched());
		System.out.println("nodes retreived: " + testObj.getNodesRetreived());
		
		System.out.println("time: " + (System.currentTimeMillis() - begin));
	}
}
