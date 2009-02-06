import java.util.*;

/**
 * 
 */

/**
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloAlphaBeta {
	Map<BoardHash, Window> transpositionTable;
	int minDepthToStore;
	
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
	public static class BoardHash {
		int hash;
		
		public BoardHash(OthelloBitBoard board, int depth) {
			int depthHash = depth * 136385313;
			hash = board.hashCode() ^ depthHash;
		}
		
		public int hashCode() {
			return hash;
		}
	};
	
	OthelloAlphaBeta(int minDepthToStore) {
		this.minDepthToStore = minDepthToStore;
		transpositionTable = new HashMap<BoardHash, Window>();
	}
	
	public int AlphaBetaSearch(OthelloBitBoard position, int alpha, int beta, 
			int turn, int depth) {
		BoardHash hash = new BoardHash(position, depth);
		Window storedWindow = transpositionTable.get(hash);
		
		if (storedWindow != null)
		{
			//if we know that this stored position
			if (storedWindow.alpha >= beta) {
				return storedWindow.alpha;
			}
			if (storedWindow.beta <= alpha) {
				return storedWindow.alpha;
			}
		} else {
			storedWindow = new Window(); // go ahead and allocate
		}
		
		//align windows
		alpha = Math.min(alpha, storedWindow.alpha);
		beta = Math.min(beta, storedWindow.beta);
		int bestScore = 0x80000000; // initialized to 'very low'
		
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
			} else {//recurse
				if (depth < minDepthToStore) {
					newScore = AlphaBetaNoTable(newPosition, -beta, 
							-Math.max(alpha, bestScore), turn ^ 1, depth - 1);
				} else {
					newScore = AlphaBetaSearch(newPosition, -beta, 
							-Math.max(alpha, bestScore), turn ^ 1, depth - 1);
				}
				
			}
			
			if (newScore > bestScore) {
				bestScore = newScore;
			}	
		}
		
		if (bestScore <= alpha) { // if fail low
			storedWindow.beta = bestScore; // we know that at BEST the score is this bad
		} else if (bestScore >= beta) { // if fail high
			storedWindow.alpha = bestScore; // we know that at WORST the score is this good
		} else {
			storedWindow.alpha = storedWindow.beta = bestScore; // store exact value
		}
		
		transpositionTable.put(hash, storedWindow); // store results for future lookup
		
		return bestScore;
	}
	
	public int AlphaBetaNoTable(OthelloBitBoard position, int alpha, int beta, 
			int turn, int depth) {
		int bestScore = 0x80000000; // initialized to 'very low'
		
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
			} else {//recurse
				newScore = AlphaBetaNoTable(newPosition, -beta, 
						-Math.max(alpha, bestScore), turn ^ 1, depth - 1);
			}
			
			if (newScore > bestScore) {
				bestScore = newScore;
			}	
		}
		
		return bestScore;
	}
	
	private int evaluateLeaf(OthelloBitBoard position, int state) {
		return (position.countPieces(state) - position.countPieces(state ^ 1)) << 4;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
