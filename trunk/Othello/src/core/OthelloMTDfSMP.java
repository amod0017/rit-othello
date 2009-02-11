/**
 * 
 */
package core;

import java.util.Vector;

/**
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloMTDfSMP extends OthelloAlphaBetaSMP {
	
	int passes = 0;
	
	/**
	 * @param localTableSize
	 */
	public OthelloMTDfSMP(int localTableSize) {
		super(localTableSize);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	public OthelloMTDfSMP() {
		super();
	}
	
	protected class MTDfJobRequest extends JobRequest {
		BoardAndDepth item;
		Window searchWindow;
		
		int guess;
		int nullWindow;

		public MTDfJobRequest(OthelloBitBoard position, int depth, int turn, int guess) {
			item = new BoardAndDepth(position, depth, turn);
			searchWindow = new Window();
			parentJob = null;
			this.guess = guess;
		}

		public void childCompletionUpdate(JobRequest child) {
			if (complete) {
				System.out.println("Warning: Parent was was already complete...");
				return;
			}

			if (child instanceof AlphaBetaJobRequest) {
				AlphaBetaJobRequest childNode = (AlphaBetaJobRequest)child;
				
				int nullWindow = (guess == searchWindow.alpha) ? guess + 1 : guess;
				
				guess = childNode.getBestScore();
				System.out.println("guess: " + guess);
				System.out.println("nullwindow: " + childNode.searchWindow.beta);
				System.out.println("WINDOW:" + searchWindow.alpha + "    " + searchWindow.beta);

				if (guess < nullWindow) { // if it failed low
					searchWindow.beta = guess;
				} else { // it must have failed high
					searchWindow.alpha = guess;
				}
				
				if (searchWindow.alpha >= searchWindow.beta) {
					reportJobComplete();
				} else {
					spawnChildJobs();
				}
			}
		}
		
		public synchronized void reportJobComplete() {
			cancelAllChildJobs();
			
			if (cancelled) {
				System.out.println("Job completed after cancellation. Wasted time.");
			} else {
				if (parentJob == null) { //root job is finishing
	
				} else {
					parentJob.childCompletionUpdate(this);
				}
			}

			complete = true;
		}

		public void spawnChildJobs() {
			if (cancelled || complete) {
				System.out.println("cancelled: " + cancelled + "  complete: " + complete);
				return;
			}

			childJobs = new Vector<JobRequest>(1);

			++passes;
			
			Window nullWindow;
			if (guess == searchWindow.alpha) {
				nullWindow = new Window(guess, guess + 1);
			} else {
				nullWindow = new Window(guess - 1, guess);
			}

			//null window search about the guess
			JobRequest s = new AlphaBetaJobRequest(this, item, nullWindow);
			childJobs.add(s);
			jobQueue.add(s);
		}
		
		public void onExecute() {
			spawnChildJobs();
		}
		
		public void updateChildWindow(Window window) {
			window.alpha = Math.max(searchWindow.alpha, window.alpha);
			window.beta = Math.min(searchWindow.beta, window.beta);
		}
	}
	
	/**
	 * queue the job needed for parallel MTD(f)
	 * 
	 * @param guess
	 * @return
	 */
	protected MTDfJobRequest enqueueMTDfSMP(int guess) {
		MTDfJobRequest job = new MTDfJobRequest(rootNode,
				maxSearchDepth,
				rootNodeTurn,
				guess);
		jobQueue.add(job);
		return job;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long begin = System.currentTimeMillis();

		System.out.println("Alpha-Beta search");

		OthelloBitBoard test1 = new OthelloBitBoard(0x0000002C14000000L, 0x0000381028040000L);

		OthelloMTDfSMP testObj = new OthelloMTDfSMP();
		testObj.setMaxSearchDepth(10);
		testObj.setLevelsToSort(4);
		testObj.setRootNode(test1, WHITE);

		MTDfJobRequest job = testObj.enqueueMTDfSMP(0);

		// Jump Start
		System.out.println("Before Jump Start");
		testObj.jumpStart();
		System.out.println("After Jump Start");

		testObj.parallelExecution(2);

		System.out.println("score: " + job.guess);

		System.out.println("leaf nodes: " + testObj.getLeafCount());
		System.out.println("non-leaf nodes: " + testObj.getNodesSearched());
		System.out.println("nodes retreived: " + testObj.getNodesRetreived());
		System.out.println("table size: " + testObj.transpositionTable.size());

		System.out.println("totalJobsExecuted: " + testObj.getTotalJobsExecuted());
		System.out.println("leafJobsExecuted: " + testObj.getLeafJobsExecuted());
		System.out.println("jobsSkipped: " + testObj.getJobsSkipped());
		System.out.println("mtdf passes: " + testObj.passes);

		System.out.println("time: " + (System.currentTimeMillis() - begin));
	}
}
