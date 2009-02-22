/**
 *
 */
package core;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelTeam;

/**
 * Class containing a parallel implementation of the Alpha=Beta Algorithm
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

	int sharedTransposeLevel = 3;

	JobRequest rootJob = null;

	List<OthelloAlphaBeta> localSearches;
	List<Queue<JobRequest>> localJobs;
	Random rand;

	/**
	 * Construct a new OthelloAlphaBetaSMP object with custom local table size
	 */
	OthelloAlphaBetaSMP(int localTableSize) {
		super();
		this.localTableSize = localTableSize;
		jobQueue = new ArrayBlockingQueue<JobRequest>(100, true);
		rand = new Random();
		transpositionTable = Collections.synchronizedMap(transpositionTable);
	}

	/**
	 * Construct a new OthelloAlphaBetaSMP object
	 */
	OthelloAlphaBetaSMP() {
		super();
		localTableSize = 250000;
		jobQueue = new ArrayBlockingQueue<JobRequest>(100, true);
		rand = new Random();
		transpositionTable = Collections.synchronizedMap(transpositionTable);
	}

	/**
	 * enqueue a new alpha-beta job request
	 *
	 * @param alpha
	 * @param beta
	 * @return the root job
	 */
	protected AlphaBetaJobRequest enqueueAlphaBetaSMP(int alpha, int beta) {
		AlphaBetaJobRequest job = new AlphaBetaJobRequest(rootNode,
				maxSearchDepth,
				rootNodeTurn,
				new Window(alpha, beta));
		jobQueue.add(job);
		rootJob = job;
		return job;
	}

	/**
	 * Special transposition table class. This is a map, but it is only partially
	 * functional. It is designed to be "tiered", where top level nodes are shared in
	 * a global map, while near-leaf nodes are local to the thread.
	 *
	 * @author Nicholas Ver Hoeve
	 */
	static class SplitTranspositionTable implements Map<BoardAndDepth, Window> {
		Map<BoardAndDepth, Window> shared;
		Map<BoardAndDepth, Window> local;
		int split; // level in the tree in which we switch to 'local' table

		SplitTranspositionTable(Map<BoardAndDepth, Window> shared, int split) {
			this.shared = shared;
			this.split = split;
			local = new HashMap<BoardAndDepth, Window>(100000);
		}

		public void clear() {
			shared.clear();
			local.clear();
		}

		public boolean containsKey(Object arg0) {
			return shared.containsKey(arg0) || local.containsKey(arg0);
		}

		public boolean containsValue(Object arg0) {
			return false;
		}

		public Set<Entry<BoardAndDepth, Window>> entrySet() {
			return null;
		}

		public Window get(Object arg0) {
			if (arg0 instanceof BoardAndDepth) {
				BoardAndDepth b = (BoardAndDepth)arg0;

				if (b.getDepth() > split) {
					return local.get(b);
				} else {
					return shared.get(b);
				}

			}

			return null;
		}

		public boolean isEmpty() {
			return shared.isEmpty() && local.isEmpty();
		}

		public Set<BoardAndDepth> keySet() {
			//NOT SUPPORTED
			return null;
		}

		public Window put(BoardAndDepth arg0, Window arg1) {
			Window old = get(arg0);
			if (arg0.getDepth() > split) {
				local.put(arg0, arg1);
			} else {
				shared.put(arg0, arg1);
			}
			return old;
		}

		public void putAll(Map<? extends BoardAndDepth, ? extends Window> arg0) {
			// TODO Auto-generated method stub

		}

		public Window remove(Object arg0) {
			//NOT SUPPORTED
			return null;
		}

		public int size() {
			return shared.size() + local.size();
		}

		public Collection<Window> values() {
			//NOT SUPPORTED
			return null;
		}

	}

	/**
	 * Parent job for all job requests
	 *
	 * @author Nicholas Ver Hoeve
	 */
	protected class JobRequest {
		JobRequest parentJob;
		List<JobRequest> childJobs;

		boolean started = false;
		boolean complete = false;
		boolean cancelled = false;

		//always called by a child to notify the parent of its completion
		public void childCompletionUpdate(JobRequest child) {}

		// always called when the job is executing
		public void onExecute(int threadIndex) {}

		//called to also hangle certain other business when executed
		public void executeJob(int threadIndex) {
			started = true;
			if (!cancelled && !complete) {
				onExecute(threadIndex);
				complete = (childJobs == null) || childJobs.isEmpty();
			}
		}

		//the child may request a smaller search window
		public void updateChildWindow(Window window) {}

		//cancel every child job in the queue, recursively if necessary
		protected void cancelAllChildJobs() {
			if (childJobs != null) {
				for (JobRequest j : childJobs) {
					j.cancelled = true;
					if (!(j.cancelled || j.complete)) {
						j.cancelAllChildJobs();
					}
				}

				childJobs.clear();
			}
		}
	};

	protected class AlphaBetaJobRequest extends JobRequest {
		BoardAndDepth item;
		Window searchWindow; // possible score window of item

		int bestScore;
		protected int threadUsed = -1; // thread used on this job

		/**
		 * contruct a new AlphaBetaJobRequest
		 *
		 * @param position
		 * @param depth
		 * @param turn
		 * @param window
		 */
		public AlphaBetaJobRequest(OthelloBitBoard position, int depth, int turn, Window window) {
			item = new BoardAndDepth(position, depth, turn);
			searchWindow = new Window(window);
			parentJob = null;
			init();
		}

		/**
		 * contruct a new AlphaBetaJobRequest from a parent job
		 *
		 * @param parent
		 * @param position
		 */
		protected AlphaBetaJobRequest(AlphaBetaJobRequest parent, OthelloBitBoard position) {
			parentJob = parent;
			item = new BoardAndDepth(position,
					parent.item.getDepth() - 1,
					parent.item.getTurn() ^ 1);

			searchWindow = new Window();
			parent.updateChildWindow(searchWindow);
			init();
		}

		/**
		 * contruct a new AlphaBetaJobRequest from a parent job
		 *
		 * @param parent
		 * @param position
		 */
		protected AlphaBetaJobRequest(JobRequest parent, BoardAndDepth item, Window window) {
			parentJob = parent;
			this.item = item;
			searchWindow = window;
			parent.updateChildWindow(searchWindow);
			init();
		}

		private void init() {
			bestScore = NOSCORE;
			checkJobNecessity();
		}

		/**
		 *
		 * @return true if job is still worth doing
		 */
		public boolean checkJobNecessity() {

			// Look up the chain to see if this is no longer needed
			// For instance a parent is completed or canceled
			for (JobRequest j = parentJob;
				j != null;
				j = j.parentJob) {
				if ( j.cancelled || j.complete ) {
					return false;
				}
			}

			Window storedWindow = transpositionTable.get(item);

			if (parentJob != null) {
				parentJob.updateChildWindow(searchWindow);
			}

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

		/**
		 * called by a child job to report that it is complete
		 */
		public synchronized void childCompletionUpdate(JobRequest child) {
			if (complete) {
				System.out.println("Warning: Parent was was already complete...");
				return;
			}

			if (child instanceof AlphaBetaJobRequest) {
				AlphaBetaJobRequest childNode = (AlphaBetaJobRequest)child;

				//negamax scoring
				bestScore = Math.max(bestScore, -childNode.bestScore);

				childJobs.remove(child);
				if (bestScore >= searchWindow.beta || /*beta cutoff check*/
						childJobs.isEmpty() /*moves exhausted check*/) {
					reportJobComplete(bestScore);
				}
			}
		}

		/**
		 * declare this job complete
		 *
		 * @param score : score of the position
		 */
		public synchronized void reportJobComplete(int score) {
			bestScore = score;
			cancelAllChildJobs();

			Window storedWindow = transpositionTable.get(item);
			if (storedWindow == null) {
				storedWindow = new Window();
			}

			if (score <= searchWindow.alpha) { // if fail low
				storedWindow.beta = score; // we know that at BEST the score is this bad
			} else if (score >= searchWindow.beta) {
				storedWindow.alpha = score; // we know that the score is at LEAST this good
			} else {
				storedWindow.alpha = storedWindow.beta = score; // store exact value
			}

			if (transpositionTable.size() < maxTableEntries) {
				transpositionTable.put(item, storedWindow); // store results for future lookup
			}

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
		 * create all child jobs of this job.
		 *
		 * @param threadIndex : thread index to create jobs on
		 */
		public void spawnChildJobs(int threadIndex) {
			if (cancelled || complete || childJobs != null) {
				System.out.println("cancelled: " + cancelled + "  complete: " + complete);
				return;
			}

			int turn = item.getTurn();
			childJobs = Collections.synchronizedList(new Vector<JobRequest>(16));

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
				for (int i = maxSearchDepth;
					i >= (maxSearchDepth - item.getDepth()) && tWindow == null;
					--i) {
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
					enqueueChildJob(item, threadIndex);
				} else {
					//end of game
					reportJobComplete(evaluateEnd(item, turn));
				}
			} else {
				Collections.sort(moveList); // sort, placing most likely to cutoff first

				for (BoardAndWindow p : moveList) {
					//request all child jobs in sorted order
					enqueueChildJob(p.board, threadIndex);
				}
			}
		}

		/**
		 * private function for enqueing a child job on a certain thread
		 *
		 * @param newPosition
		 * @param threadIndex
		 */
		private void enqueueChildJob(OthelloBitBoard newPosition, int threadIndex) {
			JobRequest s = new AlphaBetaJobRequest(this, newPosition);
			childJobs.add(s);
			enqueueJob(s, threadIndex);
		}

		/**
		 * called upon execution of the job
		 */
		public void onExecute(int threadIndex) {
			threadUsed = threadIndex;

			if (checkJobNecessity()) {
				if ((maxSearchDepth - item.getDepth()) >= sharedSearchDepth) {
					//actually do the sequential search if deep enough down the tree

					OthelloAlphaBeta localSearch;
					if (threadIndex == -1){
						localSearch = new OthelloAlphaBeta(localTableSize);
					} else {
						localSearch = localSearches.get(threadIndex);
					}

					localSearch.setRootNode(item, item.getTurn());

					//bulk of slowness that is meant to run in parallel
					int score = localSearch.alphaBetaSearch(searchWindow.alpha, searchWindow.beta);
					//System.out.println("Window [" + searchWindow.alpha + ", " + searchWindow.beta + "] = " + score);
					//System.out.println("leaves:" + getLeafCount());

					//stats tracking (maybe switch off for parallel performance)
					leafCount += localSearch.getLeafCount();
					nodesSearched += localSearch.getNodesSearched();

					++leafJobsExecuted;

					reportJobComplete(score);
				} else {
					//if still shallow in the tree, unroll into more jobs
					spawnChildJobs(threadIndex);
				}
			}
		}

		/**
		 * child alpha-beta calls will call this to enquire about an updated smaller window.
		 */
		public void updateChildWindow(Window window) {
			if (parentJob != null && (parentJob instanceof AlphaBetaJobRequest)) {
				((AlphaBetaJobRequest)parentJob).updateChildWindow(searchWindow);
			}
			window.alpha = Math.max(-searchWindow.beta, window.alpha);
			window.beta = Math.min(-Math.max(bestScore, searchWindow.alpha), window.beta);
		}

		public int getBestScore() {
			return bestScore;
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

	public int getSharedSearchDepth() {
		return sharedSearchDepth;
	}

	public void setSharedSearchDepth(int sharedDepth) {
		sharedSearchDepth = sharedDepth;
	}

	public void resetCounters() {
		super.resetCounters();
		leafJobsExecuted = 0;
		totalJobsExecuted = 0;
	}

	/**
	 * stick a new job on the queue
	 *
	 * @param job : job to execute
	 * @param threadIndex :  thread index to use
	 */
	protected void enqueueJob(JobRequest job, int threadIndex) {
		if (threadIndex == -1) {
			jobQueue.add(job);
		} else {
			localJobs.get(threadIndex).add(job);
		}
	}

	/**
	 * prepare the objects that will be used for sequential alpha-beta calls
	 *
	 * @param m : number of objects to prepare
	 */
	private void prepareLocalSearches(int m) {
		if (localSearches == null) {
			localSearches = new Vector<OthelloAlphaBeta>(1);
		}

		for (int i = localSearches.size(); i < m; ++i) {
			OthelloAlphaBeta localSearch = new OthelloAlphaBeta(
					new SplitTranspositionTable(transpositionTable, maxSearchDepth - sharedTransposeLevel));

			localSearch.setMaxSearchDepth(maxSearchDepth - sharedSearchDepth);
			localSearch.setLevelsToSort(levelsToSort - sharedSearchDepth);
			localSearch.setValueOfDraw(valueOfDraw);
			localSearch.setMinDepthToStore(3);

			localSearches.add(localSearch);
		}
	}

	/**
	 * prepare m new job queues for m threads
	 *
	 * @param m : number of queues/threads
	 */
	private void prepareLocalJobQueues(int m) {
		localJobs = new ArrayList<Queue<JobRequest>>(m);

		for (int i = 0; i < m; ++i) {
			localJobs.add(new LinkedBlockingQueue<JobRequest>(2000));
		}

		//randomize jobs into local queues
		int counter = 0;
		while(!jobQueue.isEmpty()) {
			JobRequest j = jobQueue.poll();

			if (j != null) {
				counter++;
				int index = counter % m;
				localJobs.get(index).add(j);
			}
		}
	}

	/**
	 * retreive a job from the provided list at thread <index>
	 * if no jobs are available, then steal a job.
	 *
	 * @param localList : list of job queues
	 * @param index : current thread index
	 * @return a new job to execute
	 */
	private JobRequest pullJob(List<Queue<JobRequest>> localList, int index) {
		JobRequest	j = localList.get(index).poll();

		if (j != null) {
			return j;
		}

		//else steal a job
		int end = Math.abs(rand.nextInt()) % localList.size();
		int start = (end + 1) % localList.size();

		for (int i = start; i != end; i = ((i+1)% localList.size())) {
			j = localList.get(i).poll();

			if (j != null) {
				System.out.println("Stole job from" + i);
				return j;
			}
		}

		return null;
	}

	/**
	 * Execution of the job queue
	 *
	 * @param threadIndex : the index of the thread executing this function
	 */
	public void executeJobQueue(int threadIndex) {
		while (!(rootJob.complete || rootJob.cancelled)) {
			JobRequest j = jobQueue.poll();

			if (j != null) {
				j.executeJob(threadIndex);
				if (j.complete) {
					++totalJobsExecuted;
				}
			}
		}
	}

	/**
	 * Parallel execution of the job queue
	 *
	 * @param threads: the number of threads to apply
	 * @param jumpstart: the number of jobs to execute before going parallel
	 */
	public void parallelExecution(int threads, int jumpstart) {
		// Manually adjust the number of threads for ease.
		try {
			prepareLocalSearches(threads);
			prepareLocalJobQueues(threads);
			jumpStart(jumpstart);

			new ParallelTeam(threads).execute(new ParallelRegion() {
				public void run() throws Exception {
					System.out.println( getThreadIndex() + " started" );
					List<Queue<JobRequest>> localList = new ArrayList<Queue<JobRequest>>();
					localList.addAll(localJobs);

					while (!(rootJob.complete || rootJob.cancelled)) {
						JobRequest j = pullJob(localList, getThreadIndex());

						if (j != null) {
							j.executeJob(getThreadIndex());
							if (j.complete) {
								++totalJobsExecuted;
							}
						}
					}
					System.out.println( getThreadIndex() + " says its done");
				}
			});
		} catch (Exception e) {
			System.exit(1);
		}
	}

	/**
	 * Prime the queue by processing one job to create more jobs
	 */
	protected void jumpStart(int n) {
		prepareLocalSearches(1);

		for (int i = 0; i < n && !jobQueue.isEmpty(); ++i) {
			jobQueue.poll().executeJob(-1);
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
		testObj.setMaxSearchDepth(11);
		testObj.setLevelsToSort(4);
		testObj.setRootNode(test1, WHITE);
		testObj.setSharedSearchDepth(2);

		AlphaBetaJobRequest job = testObj.enqueueAlphaBetaSMP(LOWESTSCORE, HIGHESTSCORE);

		testObj.parallelExecution(ParallelTeam.getDefaultThreadCount(), 1);

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
