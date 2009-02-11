/**
 * 
 */
package core;

import java.util.Collections;
import java.util.Queue;
import java.util.Vector;

/**
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloAlphaBetaSMP extends OthelloAlphaBeta {
	Queue<JobRequest> jobQueue;
	int nodesInJobQueue;
	int sharedDepth;
	
	OthelloAlphaBetaSMP() {
		
	}

	protected class JobRequest {
		BoardAndDepth item;
		Window searchWindow;
		JobRequest parentJob;
		Vector<JobRequest> childJobs;
		int bestScore;
		boolean started;
		boolean complete;
		
		public JobRequest(OthelloBitBoard position, int depth, int turn, Window window) {
			item = new BoardAndDepth(position, depth, turn);
			searchWindow = new Window(window);
			bestScore = NOSCORE;
			parentJob = null;
			checkJobNecessity();
		}
		
		protected JobRequest(JobRequest parent, OthelloBitBoard position) {
			parentJob = parent;
			item = new BoardAndDepth(position, 
					parent.item.getDepth() - 1,
					parent.item.getTurn() ^ 1);
			
			searchWindow = new Window(
					-parent.searchWindow.beta,
					-parent.searchWindow.alpha
					);
			
			checkJobNecessity();
		}
		
		public void checkJobNecessity() {
			Window storedWindow = transpositionTable.get(item);

			++nodesSearched;

			if (storedWindow != null)
			{
				++nodesRetrieved;

				//check if we already know the result to be outside of what we care about
				if (storedWindow.alpha >= searchWindow.beta) {
					reportJobComplete(storedWindow.alpha);
				}
				if (storedWindow.beta <= searchWindow.alpha) {
					reportJobComplete(storedWindow.beta);
				}

				//align windows
				searchWindow.alpha = Math.max(searchWindow.alpha, storedWindow.alpha);
				searchWindow.beta = Math.min(searchWindow.beta, storedWindow.beta);
			}
			
			if (searchWindow.alpha == searchWindow.beta) {
				reportJobComplete(searchWindow.alpha); // result is already known
			}
		}
		
		public void reportJobComplete(int score) {
			bestScore = score;
			
			if (score <= searchWindow.alpha) { // if fail low
				searchWindow.beta = score; // we know that at BEST the score is this bad
			} else if (score >= searchWindow.beta) {
				searchWindow.alpha = score; // we know that the score is at LEAST this good
			} else {
				searchWindow.alpha = searchWindow.beta = score; // store exact value
			}
			
			if (transpositionTable.size() < maxTableEntries) {
				transpositionTable.put(item, searchWindow); // store results for future lookup
			}
			
			if (parentJob == null) { //root job is finishing
				
			} else {
				parentJob.childJobs.remove(this);
				if (parentJob.searchWindow.beta <= -bestScore || /*beta cutoff check*/
						parentJob.childJobs.isEmpty() /*moves exhausted check*/) {
					parentJob.reportJobComplete(-bestScore);
				}
			}
		}
		
		public void spawnChildJobs() {
			int turn = item.getTurn();
			childJobs = new Vector<JobRequest>(16);
			
			Vector<BoardAndWindow> moveList = new Vector<BoardAndWindow>();
			
			for (long likelyMoves = item.generateLikelyMoves(turn);
					likelyMoves != 0;
					likelyMoves &= (likelyMoves - 1)) {
				int movePos = BitUtil.ulog2(BitUtil.lowSetBit(likelyMoves));
				int moveX = OthelloBitBoard.xyTox(movePos);
				int moveY = OthelloBitBoard.xyToy(movePos);
				
				if (!item.moveIsLegal(moveX, moveY, turn)) {
					continue;
				}
				
				OthelloBitBoard newPosition = item.copyAndMakeMove(moveX, moveY, turn);
				
				//search the table for the most well-searched window relating to this new position
				Window tWindow = null;
				for (int i = maxSearchDepth; i >= sharedDepth && tWindow == null; --i) {
					tWindow = transpositionTable.get(new BoardAndDepth(newPosition, i, turn ^ 1));
				}
				
				if (tWindow == null) {
					tWindow = new Window(LOWESTSCORE, HIGHESTSCORE);
				}
				
				moveList.add(new BoardAndWindow(newPosition, tWindow)); //add entry and known info to list
			}
			
			if (moveList.isEmpty()) { // if NO move was found...
				if (item.canMove(turn ^ 1)) {
					// player loses turn
					JobRequest s = new JobRequest(this, item);
					childJobs.add(s);
					jobQueue.add(s);
				} else {
					//end of game
					reportJobComplete(evaluateEnd(item, turn));
				}
			} else {
				Collections.sort(moveList); // sort, placing most likely to cutoff first
				
				for (BoardAndWindow p : moveList) {
					//request all child jobs in sorted order
					JobRequest s = new JobRequest(this, p.board);
					childJobs.add(s);
					jobQueue.add(s);
				}
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
