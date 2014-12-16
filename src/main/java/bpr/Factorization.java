package bpr;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

public class Factorization {
	private final int k;
	//Only necessary for initialization of new factors.
	private final RandomGenerator rng;
	private final TIntObjectHashMap<ArrayRealVector> personFactors=new TIntObjectHashMap<ArrayRealVector>();
	private final TIntObjectHashMap<ArrayRealVector> itemFactors=new TIntObjectHashMap<ArrayRealVector>();
	
	/*Instead of a ring of locks, we could use ConcurrentHashMaps with a lock for each key*/
	private ReentrantLock[] itemLocks;
	private ReentrantLock[] personLocks;
	
	/**Construct a Factorization with the default item and person locking factors,
	 * and the given degree of factorization.*/
	public Factorization(int k){
		this(k, new MersenneTwister(),128,128);
	}
	/**Tuning these parameters is important for performance in an accuracy and
	 * operations/second sense.  Especially, the ratio between and scale of 
	 * itemLockingFactor and personLockingFactor will determine how much of the
	 * time you spend blocking when you fit the matrix concurrently.
	 * @param k is the degree of the factorization (higher => more bandwidth,
	 * less inference)
	 * @param rng is a custom random number generator
	 * @param itemLockingFactor controls the number of individually lockable
	 * buckets for item factors. More=>higher possible concurrency.
	 * @param personLockingFactor controls the number of individually lockable
	 * buckets for person factors.  More=> higher possible concurrency.*/
	public Factorization(int k, RandomGenerator rng, int itemLockingFactor, int personLockingFactor){
		this.k=k;
		this.rng = rng;
		itemLocks = new ReentrantLock[itemLockingFactor];
		personLocks = new ReentrantLock[personLockingFactor];
		for (int i=0; i<itemLockingFactor; i++) itemLocks[i] = new ReentrantLock();
		for (int i=0; i<personLockingFactor; i++) personLocks[i] = new ReentrantLock();
	}
	
	/**@return A k-long vector of standard-normally distributed reals, for
	 * initializing new person and item factors.*/
	protected ArrayRealVector newFactor(){
		final ArrayRealVector out = new ArrayRealVector(k);
		synchronized (rng) {
			for (int i=0;i<k;i++){
				out.setEntry(i, rng.nextGaussian());
			}
		}
		return out;
	}
	
	/**Perform one stochastic gradient descent step, using the given preference
	 * tuple.  Threadsafe, albeit locky - it is possible that a smart fitter
	 * could coordinate lockfree updating ahead of time.*/
	public void update(PreferenceTuple p, double learnRate, double regularization){
		personLocks[p.personID%personLocks.length].lock();
		final int minLockIdx = Math.min(p.lessPreferredItem,p.morePreferredItem)%itemLocks.length;
		final int maxLockIdx = Math.max(p.lessPreferredItem,p.morePreferredItem)%itemLocks.length;
		itemLocks[maxLockIdx].lock();
		if (minLockIdx!=maxLockIdx) itemLocks[minLockIdx].lock();
		
		try {
			final double morePreferredScore = predict(p.personID, p.morePreferredItem);
			final double lessPreferredScore = predict(p.personID, p.lessPreferredItem);
			final double dxuij = (1.0 / (1 + Math.exp(morePreferredScore - lessPreferredScore)));
			
			final ArrayRealVector personFactor = personFactors.get(p.personID);
			final ArrayRealVector iItemFactor = itemFactors.get(p.lessPreferredItem);
			final ArrayRealVector jItemFactor = itemFactors.get(p.morePreferredItem);
			for(int i=0;i<k;i++){
				final double wuf = personFactor.getEntry(i);
				final double hif = iItemFactor.getEntry(i);
				final double hjf = jItemFactor.getEntry(i);
				personFactor.addToEntry(i, -learnRate * (dxuij * (hif - hjf) - regularization * wuf));
				iItemFactor.addToEntry(i, -learnRate * (dxuij * wuf - regularization * hif));
				jItemFactor.addToEntry(i, -learnRate * (dxuij * -wuf - regularization * hjf));
			}
		} finally{
			itemLocks[minLockIdx].unlock();
			if (maxLockIdx!=minLockIdx) itemLocks[maxLockIdx].unlock();
			personLocks[p.personID % personLocks.length].unlock();
		}
	}
	/**@return the estimated score for the person-item combo (higher=>better)*/
	public double predict(int personID, int itemID){
		if (! itemFactors.containsKey(itemID)){
			itemLocks[itemID%itemLocks.length].lock();
			try {
				if (! itemFactors.containsKey(itemID)) 
					itemFactors.put(itemID, newFactor());
			} finally {
				itemLocks[itemID%itemLocks.length].unlock();
			}
		}
		if (! personFactors.containsKey(personID)){
			personLocks[personID%personLocks.length].lock();
			try {
				if (! personFactors.containsKey(personID))
					personFactors.put(personID, newFactor());
			} finally {
				personLocks[personID%personLocks.length].unlock();
			}
		}
		return itemFactors.get(itemID).dotProduct(personFactors.get(personID));
	}
	/**@param s The set of interactions to sample from
	 * @param nIters Number of iterations to run for
	 * @return The proportion of the time that the model ranked the items
	 * correctly (the C-statistic, or area under the ROC curve)*/
	public float areaUnderCurve(SamplableInteractionList s, int nIters){
		int successes = 0;
		for (int i=0;i<nIters;i++){
			PreferenceTuple prefs = s.get();
			if (predict(prefs.personID,prefs.morePreferredItem) >
			    predict(prefs.personID,prefs.lessPreferredItem)){
				successes++;
			}
		}
		return successes / (float)nIters;
	}
}
