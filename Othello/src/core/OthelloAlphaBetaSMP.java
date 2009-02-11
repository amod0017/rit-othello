/**
 * 
 */
package core;

import java.util.Collections;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloAlphaBetaSMP extends OthelloAlphaBeta {
	Queue<JobRequest> jobQueue;
	
	int sharedSearchDepth = 2;
	int localTableSize;

	int totalJobsExecuted;
	int leafJobsExecuted;
	int jobsSkipped;
	
	OthelloAlphaBetaSMP(int localTableSize) {
		this.localTableSize = localTableSize;
		jobQueue = new ArrayBlockingQueue<JobRequest>(4000, true);
	}
	
	OthelloAlphaBetaSMP() {
		localTableSize = 250000;
		jobQueue = new ArrayBlockingQueue<JobRequest>(4000, true);
	}
	
	protected AlphaBetaJobRequest enqueueAlphaBetaSMP(int alpha, int beta) {
		AlphaBetaJobRequest job = new AlphaBetaJobRequest(rootNode, 
				maxSearchDepth, 
				rootNodeTurn, 
				new Window(alpha, beta));
		jobQueue.add(job);
		return job;
	}
	
	protected class JobRequest {
		JobRequest parentJob;
		Vector<JobRequest> childJobs;
		
		boolean started = false;
		boolean complete = false;
		boolean cancelled = false;
		
		public void spawnChildJobs() {}
		public void childCompletionUpdate(JobRequest child) {}
		public void onExecute() {}
		public void executeJob() {
			started = true;
			if (!cancelled && !complete) {
				onExecute();
				complete = true;
			}
		}
	};

	protected class AlphaBetaJobRequest extends JobRequest {
		BoardAndDepth item;
		Window searchWindow;
		
		int bestScore;

		public AlphaBetaJobRequest(OthelloBitBoard position, int depth, int turn, Window window) {
			item = new BoardAndDepth(position, depth, turn);
			searchWindow = new Window(window);
			bestScore = NOSCORE;
			parentJob = null;
			checkJobNecessity();
		}
		
		protected AlphaBetaJobRequest(AlphaBetaJobRequest parent, OthelloBitBoard position) {
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
		
		public boolean checkJobNecessity() {
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
			
			return !complete;
		}
		
		public void childCompletionUpdate(JobRequest child) {
			if (complete) {
				System.out.println("Warning: Parent was was already complete...");
				return;
			}
			
			if (child instanceof AlphaBetaJobRequest) {
				AlphaBetaJobRequest childNode = (AlphaBetaJobRequest)child;
				
				childJobs.remove(child);
				if (searchWindow.beta <= -childNode.bestScore || /*beta cutoff check*/
						childJobs.isEmpty() /*moves exhausted check*/) {
					
					for (JobRequest j : childJobs) {
						j.cancelled = true;
					}
					
					reportJobComplete(-childNode.bestScore);
				}
			}
		}
		
		public void reportJobComplete(int score) {
			bestScore = score;
			
			if (complete) {
				System.out.println("Warning: Was already complete...");
			}
			
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
				parentJob.childCompletionUpdate(this);
			}
			
			complete = true;
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
				for (int i = maxSearchDepth; i >= sharedSearchDepth && tWindow == null; --i) {
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
					JobRequest s = new AlphaBetaJobRequest(this, item);
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
					JobRequest s = new AlphaBetaJobRequest(this, p.board);
					childJobs.add(s);
					jobQueue.add(s);
				}
			}
		}
		
		public void onExecute() {
			if (checkJobNecessity()) {
				if (item.getDepth() > sharedSearchDepth) {
					OthelloAlphaBeta localSearch = new OthelloAlphaBeta(localTableSize);
					localSearch.setMaxSearchDepth(maxSearchDepth - sharedSearchDepth);
					localSearch.setLevelsToSort(levelsToSort - sharedSearchDepth);
					localSearch.setValueOfDraw(valueOfDraw);
					localSearch.setRootNode(item, WHITE);
					localSearch.setMinDepthToStore(3);
					
					//bulk of slowness that is meant to run in parallel
					int score = localSearch.alphaBetaSearch(searchWindow.alpha, searchWindow.beta);
					
					//stats tracking (maybe switch off for parallel performance)
					leafCount += localSearch.getLeafCount();
					nodesSearched += localSearch.getNodesSearched();
					
					++leafJobsExecuted;
					
					reportJobComplete(score);
				} else {
					spawnChildJobs();
				}
			}
		}
		
		public int getSharedSearchDepth() {
			return sharedSearchDepth;
		}

		public void setSharedSearchDepth(int sharedDepth) {
			sharedSearchDepth = sharedDepth;
		}
	}
	
	public int getTotalJobsExecuted() {
		return totalJobsExecuted;
	}

	public int getLeafJobsExecuted() {
		return leafJobsExecuted;
	}

	public int getJobsSkipped() {
		return jobsSkipped;
	}
	
	public void resetCounters() {
		super.resetCounters();
		leafJobsExecuted = 0;
		totalJobsExecuted = 0;
	}
	
	/**
	 * Parallel execution of the job queue
	 */
	public void executeJobQueue() {
		while (!jobQueue.isEmpty()) {
			jobQueue.poll().executeJob();
			++totalJobsExecuted;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long begin = System.currentTimeMillis();

		System.out.println("Alpha-Beta search");
	
		OthelloBitBoard test1 = new OthelloBitBoard(0x0000002C14000000L, 0x0000381028040000L);
		
		OthelloAlphaBetaSMP testObj = new OthelloAlphaBetaSMP();
		testObj.setMaxSearchDepth(12);
		testObj.setLevelsToSort(3);
		testObj.setRootNode(test1, WHITE);
		
		AlphaBetaJobRequest job = testObj.enqueueAlphaBetaSMP(LOWESTSCORE, HIGHESTSCORE);
		testObj.executeJobQueue();

		System.out.println("score: " + job.bestScore);
		
		System.out.println("leaf nodes: " + testObj.getLeafCount());
		System.out.println("non-leaf nodes: " + testObj.getNodesSearched());
		System.out.println("nodes retreived: " + testObj.getNodesRetreived());
		System.out.println("table size: " + testObj.transpositionTable.size());
		
		System.out.println("totalJobsExecuted: " + testObj.getTotalJobsExecuted());
		System.out.println("leafJobsExecuted: " + testObj.getLeafJobsExecuted());
		System.out.println("jobsSkipped: " + testObj.getJobsSkipped());
		
		System.out.println("time: " + (System.currentTimeMillis() - begin));
	}
}
