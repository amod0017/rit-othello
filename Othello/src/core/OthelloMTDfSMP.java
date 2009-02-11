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
		
		public MTDfJobRequest(JobRequest parentJob, BoardAndDepth item, int guess) {
			this.item = item;	
			this.parentJob = parentJob;
			this.guess = guess;
			searchWindow = new Window();
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
	
	protected class IterativeMTDfJobRequest extends JobRequest {
		BoardAndDepth item;		
		int guess;
		int nextSearchDepth;

		public IterativeMTDfJobRequest(OthelloBitBoard position, int depth, int turn, int guess) {
			item = new BoardAndDepth(position, depth, turn);
			parentJob = null;
			this.guess = guess;
			nextSearchDepth = sharedSearchDepth;
			if ((depth & 1) != (sharedSearchDepth & 1)) {
				++nextSearchDepth; //match evenndess / oddness
			}
		}

		public void childCompletionUpdate(JobRequest child) {
			if (complete) {
				System.out.println("Warning: Parent was was already complete...");
				return;
			}

			if (child instanceof MTDfJobRequest) {
				MTDfJobRequest childNode = (MTDfJobRequest)child;
				
				guess = childNode.guess;
				
				//final depth complete indicates completion
				if (childNode.item.getDepth() >= item.getDepth()) {
					reportJobComplete();
				} else {
					nextSearchDepth += 2;
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

			childJobs = new Vector<JobRequest>();

			BoardAndDepth nextItem = new BoardAndDepth(item, nextSearchDepth, item.getTurn());
			JobRequest s = new MTDfJobRequest(this, nextItem, guess);
			childJobs.add(s);
			jobQueue.add(s);
		}
		
		public void onExecute() {
			spawnChildJobs();
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
	
	protected IterativeMTDfJobRequest enqueueIterativeMTDfSMP(int guess) {
		IterativeMTDfJobRequest job = new IterativeMTDfJobRequest(rootNode,
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

		System.out.println("Parallel MTD(f) search");

		OthelloBitBoard test1 = new OthelloBitBoard(0x0000002C14000000L, 0x0000381028040000L);

		OthelloMTDfSMP testObj = new OthelloMTDfSMP();
		testObj.setMaxSearchDepth(10);
		testObj.setLevelsToSort(4);
		testObj.setRootNode(test1, WHITE);
		testObj.setSharedSearchDepth(1);
		
		IterativeMTDfJobRequest job = testObj.enqueueIterativeMTDfSMP(0);

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
