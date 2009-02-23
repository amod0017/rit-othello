/**
 *
 */
package core;
import java.io.IOException;
import java.util.*;

import edu.rit.pj.Comm;
import edu.rit.pj.ParallelTeam;

/**
 * class containing Multi-threaded parallel MTD(f) and iterative-deepening MTD(f)
 * algorithms
 *
 * @author Nicholas Ver Hoeve
 */
public class OthelloMTDfSMP extends OthelloAlphaBetaSMP {
	int passes = 0;
	Map<OthelloBitBoard, Integer> threadAssignments;

	/**
	 * @param localTableSize
	 */
	public OthelloMTDfSMP(int localTableSize) {
		super(localTableSize);
		threadAssignments = new HashMap<OthelloBitBoard, Integer>();
		// TODO Auto-generated constructor stub
	}

	/**
	 *
	 */
	public OthelloMTDfSMP() {
		super();
		threadAssignments = new HashMap<OthelloBitBoard, Integer>();
	}

	/**
	 * class that represents an MTD(f) job. Will spawn various alpha-beta
	 * jobs.
	 *
	 * @author Nocholas Ver Hoeve
	 */
	protected class MTDfJobRequest extends JobRequest {
		BoardAndDepth item;
		Window searchWindow; // known window of the score of item

		int guess; // current guess
		int nullWindow; // value of the last null-window search
		int threadIndex = -1;

		/**
		 * Construct a new MTDfJobRequest
		 *
		 * @param position
		 * @param depth
		 * @param turn
		 * @param guess
		 */
		public MTDfJobRequest(OthelloBitBoard position, int depth, int turn, int guess) {
			item = new BoardAndDepth(position, depth, turn);
			searchWindow = new Window();
			parentJob = null;
			this.guess = guess;
		}

		/**
		 * Construct a new MTDfJobRequest from another job
		 *
		 * @param parentJob
		 * @param item
		 * @param guess
		 */
		public MTDfJobRequest(JobRequest parentJob, BoardAndDepth item, int guess) {
			this.item = item;
			this.parentJob = parentJob;
			this.guess = guess;
			searchWindow = new Window();
		}

		/**
		 * Whenever a child Job completes, it must call this function.
		 */
		public synchronized void childCompletionUpdate(JobRequest child) {
			if (complete) {
				System.out.println("Warning: Parent was was already complete...");
				return;
			}

			if (child instanceof AlphaBetaJobRequest) {
				AlphaBetaJobRequest childNode = (AlphaBetaJobRequest)child;

				int nullWindow = (guess == searchWindow.alpha) ? guess + 1 : guess;

				threadAssignments.put(childNode.item, childNode.threadUsed);

				guess = childNode.retreiveScore();

				if (guess < nullWindow) { // if it failed low
					searchWindow.beta = guess;
				} else { // it must have failed high
					searchWindow.alpha = guess;
				}

				if (searchWindow.alpha >= searchWindow.beta) {
					reportJobComplete();
				} else {
					spawnChildJobs(this.threadIndex);
				}
			}
		}

		/**
		 * Whenever a child Job completes, it must call this function.
		 */
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

		/**
		 * spawn all child jobs
		 *
		 * @param threadIndex : preferred thread index to create jobs in
		 */
		public void spawnChildJobs(int threadIndex) {
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

			Integer nextIndex = threadAssignments.get(item);

			if (nextIndex == null) {
				nextIndex = Math.abs(rand.nextInt()) % localSearches.size();
			}
			enqueueJob(s, nextIndex);
		}

		public void onExecute(int threadIndex) {
			this.threadIndex = threadIndex;
			spawnChildJobs(threadIndex);
		}

		/**
		 * Alpha-Beta jobs as children will call this to inquire about possibly
		 * shrinking the search window.
		 */
		public void updateChildWindow(Window window) {
			window.alpha = Math.max(searchWindow.alpha, window.alpha);
			window.beta = Math.min(searchWindow.beta, window.beta);
		}

		public int retreiveScore() {
			return guess;
		}
	}

	/**
	 * Class that represents an iterative depth-first search job
	 *
	 * @author Nicholas Ver Hoeve
	 */
	protected class IterativeMTDfJobRequest extends JobRequest {
		BoardAndDepth item; // item to analyze
		int guess; // current score/ next guess
		int nextSearchDepth; // next depth to scan

		/**
		 * Construct a new IterativeMTDfJobRequest
		 *
		 * @param position
		 * @param depth
		 * @param turn
		 * @param guess
		 */
		public IterativeMTDfJobRequest(OthelloBitBoard position, int depth, int turn, int guess) {
			item = new BoardAndDepth(position, depth, turn);
			parentJob = null;
			this.guess = guess;
			nextSearchDepth = sharedSearchDepth + 1;
			if ((depth & 1) != (nextSearchDepth & 1)) {
				++nextSearchDepth; //match evenndess / oddness
			}
		}

		/**
		 * Whenever a child Job completes, it must call this function.
		 */
		public synchronized void childCompletionUpdate(JobRequest child) {
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
					spawnChildJobs(Math.abs(rand.nextInt()) % localSearches.size());
				}
			}
		}

		/**
		 * calling this function indicates that the job has finished
		 */
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

		/**
		 * spawn all child jobs
		 *
		 * @param threadIndex : preferred thread index to create jobs in
		 */
		public void spawnChildJobs(int threadIndex) {
			if (cancelled || complete) {
				System.out.println("cancelled: " + cancelled + "  complete: " + complete);
				return;
			}

			childJobs = new Vector<JobRequest>();

			BoardAndDepth nextItem = new BoardAndDepth(item, nextSearchDepth, item.getTurn());
			JobRequest s = new MTDfJobRequest(this, nextItem, guess);
			childJobs.add(s);
			enqueueJob(s, Math.abs(rand.nextInt()) % localSearches.size());
		}

		/**
		 * called when this job is being executed
		 *
		 * @param threadIndex : thread index executing this job
		 */
		public void onExecute(int threadIndex) {
			spawnChildJobs(threadIndex);
		}

		public int retreiveScore() {
			return guess;
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
		rootJob = job;
		return job;
	}


	/**
	 * Enqueue an iterative-deepening MTD(f) search job
	 *
	 * @param guess : an initial guess
	 * @return the root job
	 */
	protected IterativeMTDfJobRequest enqueueIterativeMTDfSMP(int guess) {
		IterativeMTDfJobRequest job = new IterativeMTDfJobRequest(rootNode,
				maxSearchDepth,
				rootNodeTurn,
				guess);
		jobQueue.add(job);
		rootJob = job;
		return job;
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

		if (args.length != 1) {
			System.out.println("Usage: OthelloAlphaBeta [filename]");
			return;
		}

		//read in initial time
		long begin = System.currentTimeMillis();

		boolean iterative = true;
		int guess = 0;

		System.out.println("Parallel MTD(f) search");

		OthelloMTDfSMP search = new OthelloMTDfSMP();
		List<String> fileArgs = search.readInputFile(args[0]);
		if (fileArgs == null) {
			return;
		}
		//read in optional file arguments
		String t = findSetting(fileArgs, "IterativeMTDF");
		try {
			if (t != null) {
				iterative = Boolean.parseBoolean(t);
			}
			t = findSetting(fileArgs, "InitialGuess");
			if (t != null) {
				guess = Integer.parseInt(t);
			}
			t = findSetting(fileArgs, "SharedSearchDepth");
			if (t != null) {
				search.setSharedSearchDepth(Integer.parseInt(t));
			}
			t = findSetting(fileArgs, "SharedTableLevel");
			if (t != null) {
				search.setSharedTableLevel(Integer.parseInt(t));
			}
		} catch (Exception e) {
			System.out.println("File Argument error");
		}

		//do primary search
		if (iterative) {
			search.enqueueIterativeMTDfSMP(guess);
		} else {
			search.enqueueMTDfSMP(guess);
		}

		search.parallelExecution(ParallelTeam.getDefaultThreadCount(), 1);

		long searchTime = (System.currentTimeMillis() - begin);
		double leafNodesPerSec = ((double)(search.getLeafCount() * 1000) / (double)searchTime);

		System.out.println("score: " + search.getSearchScore());
		System.out.println("leaf nodes: " + search.getLeafCount());
		System.out.println("non-leaf nodes: " + search.getNodesSearched());
		System.out.println("Leaf nodes/sec:" + (long)leafNodesPerSec);
		System.out.println("nodes retreived: " + search.getNodesRetreived());
		System.out.println("table size: " + search.transpositionTable.size());

		System.out.println("Search time: " + searchTime);

		//do re-search to locate the best move. Not part of main search.
		long r2 = System.currentTimeMillis();
		int bestMove = search.retreiveBestMove();

		System.out.println("BestMove: (" + xyTox(bestMove) + ", " + xyToy(bestMove) + ")");
		System.out.println("re-search time: " + (System.currentTimeMillis() - r2));
	}
}
