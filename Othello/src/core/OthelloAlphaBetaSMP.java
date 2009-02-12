/**
 *
 */
package core;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelTeam;

/**
 *
 * @author Nicholas Ver Hoeve
 */
public class OthelloAlphaBetaSMP extends OthelloAlphaBeta {
	Queue<JobRequest> jobQueue;

	int sharedSearchDepth = 1;
	int localTableSize;

	int totalJobsExecuted;
	int leafJobsExecuted;
	int jobsSkipped;

	int sharedTransposeLevel = 3;
	
	JobRequest rootJob = null;

	List<OthelloAlphaBeta> localSearches;
	List<Queue<JobRequest>> localJobs;

	Map<BoardAndDepth, Window> sharedTable;

	Random rand;

	OthelloAlphaBetaSMP(int localTableSize) {
		this.sharedTable = Collections.synchronizedMap(new HashMap<BoardAndDepth, Window>(10000));
		this.localTableSize = localTableSize;
		jobQueue = new ArrayBlockingQueue<JobRequest>(100, true);
		rand = new Random();
	}

	OthelloAlphaBetaSMP() {
		this.sharedTable = Collections.synchronizedMap(new HashMap<BoardAndDepth, Window>(10000));
		localTableSize = 250000;
		jobQueue = new ArrayBlockingQueue<JobRequest>(100, true);
		rand = new Random();
	}

	protected AlphaBetaJobRequest enqueueAlphaBetaSMP(int alpha, int beta) {
		AlphaBetaJobRequest job = new AlphaBetaJobRequest(rootNode,
				maxSearchDepth,
				rootNodeTurn,
				new Window(alpha, beta));
		jobQueue.add(job);
		rootJob = job;
		return job;
	}
	
	static class SplitTranspositionTable implements Map<BoardAndDepth, Window> {
		Map<BoardAndDepth, Window> shared;
		Map<BoardAndDepth, Window> local;
		int split;
		
		SplitTranspositionTable(Map<BoardAndDepth, Window> shared, int split) {
			this.shared = shared;
			this.split = split;
			local = new HashMap<BoardAndDepth, Window>(100000);
		}
		
		@Override
		public void clear() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean containsKey(Object arg0) {
			return shared.containsKey(arg0) || local.containsKey(arg0);
		}

		@Override
		public boolean containsValue(Object arg0) {
			return false;
		}

		@Override
		public Set<Entry<BoardAndDepth, Window>> entrySet() {
			return null;
		}

		@Override
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

		@Override
		public boolean isEmpty() {
			
			return false;
		}

		@Override
		public Set<BoardAndDepth> keySet() {
			
			return null;
		}

		@Override
		public Window put(BoardAndDepth arg0, Window arg1) {
			Window old = get(arg0);
			if (arg0.getDepth() > split) {
				local.put(arg0, arg1);
			} else {
				shared.put(arg0, arg1);
			}
			return old;
		}

		@Override
		public void putAll(Map<? extends BoardAndDepth, ? extends Window> arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Window remove(Object arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Collection<Window> values() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	protected class JobRequest {
		JobRequest parentJob;
		List<JobRequest> childJobs;

		boolean started = false;
		boolean complete = false;
		boolean cancelled = false;

		public void childCompletionUpdate(JobRequest child) {}
		public void onExecute(int threadIndex) {}
		public void executeJob(int threadIndex) {
			started = true;
			if (!cancelled && !complete) {
				onExecute(threadIndex);
				complete = (childJobs == null) || childJobs.isEmpty();
			}
		}
		public void updateChildWindow(Window window) {}

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
		Window searchWindow;

		int bestScore;
		protected int threadUsed = -1;

		public AlphaBetaJobRequest(OthelloBitBoard position, int depth, int turn, Window window) {
			item = new BoardAndDepth(position, depth, turn);
			searchWindow = new Window(window);
			parentJob = null;
			init();
		}

		protected AlphaBetaJobRequest(AlphaBetaJobRequest parent, OthelloBitBoard position) {
			parentJob = parent;
			item = new BoardAndDepth(position,
					parent.item.getDepth() - 1,
					parent.item.getTurn() ^ 1);

			searchWindow = new Window();
			parent.updateChildWindow(searchWindow);
			init();
		}

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

		private void enqueueChildJob(OthelloBitBoard newPosition, int threadIndex) {
			JobRequest s = new AlphaBetaJobRequest(this, newPosition);
			childJobs.add(s);
			enqueueJob(s, threadIndex);
		}

		public void onExecute(int threadIndex) {
			threadUsed = threadIndex;
			
			if (checkJobNecessity()) {
				if ((maxSearchDepth - item.getDepth()) >= sharedSearchDepth) {
					OthelloAlphaBeta localSearch;
					if (threadIndex == -1){
						localSearch = new OthelloAlphaBeta(localTableSize);
					} else {
						localSearch = localSearches.get(threadIndex);
					}
					
					localSearch.setMaxSearchDepth(maxSearchDepth - sharedSearchDepth);
					localSearch.setLevelsToSort(levelsToSort - sharedSearchDepth);
					localSearch.setValueOfDraw(valueOfDraw);
					localSearch.setRootNode(item, item.getTurn());
					localSearch.setMinDepthToStore(3);

					//bulk of slowness that is meant to run in parallel
					int score = localSearch.alphaBetaSearch(searchWindow.alpha, searchWindow.beta);
					System.out.println("Window [" + searchWindow.alpha + ", " + searchWindow.beta + "] = " + score);
					System.out.println("leaves:" + getLeafCount());

					//stats tracking (maybe switch off for parallel performance)
					leafCount += localSearch.getLeafCount();
					nodesSearched += localSearch.getNodesSearched();

					++leafJobsExecuted;

					reportJobComplete(score);
				} else {
					spawnChildJobs(threadIndex);
				}
			}
		}

		public void updateChildWindow(Window window) {
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

	protected void enqueueJob(JobRequest job, int threadIndex) {
		if (threadIndex == -1) {
			jobQueue.add(job);
		} else {
			localJobs.get(threadIndex).add(job);
		}
	}

	private void prepareLocalSearches(int m) {
		if (localSearches == null) {
			localSearches = new Vector<OthelloAlphaBeta>(1);
		}

		for (int i = localSearches.size(); i < m; ++i) {
			localSearches.add(new OthelloAlphaBeta(
					new SplitTranspositionTable(sharedTable, maxSearchDepth - sharedTransposeLevel)));
		}
	}

	private void prepareLocalJobQueues(int m) {
		localJobs = new ArrayList<Queue<JobRequest>>(m);

		for (int i = 0; i < m; ++i) {
			localJobs.add(new ArrayBlockingQueue<JobRequest>(2000, true));
		}

		//randomize jobs into local queues
		while(!jobQueue.isEmpty()) {
			JobRequest j = jobQueue.poll();

			if (j != null) {
				int index = Math.abs(rand.nextInt()) % m;
				localJobs.get(index).add(j);
			}
		}
	}

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
	 */
	public void executeJobQueue(int threadIndex) {
		while (!(rootJob.complete || rootJob.cancelled)) {
			JobRequest j = jobQueue.poll();

			if (j != null) {
				j.executeJob(threadIndex);
				++totalJobsExecuted;
			}
		}
	}

	/**
	 * Parallel execution of the job queue
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
							++totalJobsExecuted;
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

		AlphaBetaJobRequest job = testObj.enqueueAlphaBetaSMP(LOWESTSCORE, HIGHESTSCORE);

		testObj.parallelExecution(2, 1);

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
