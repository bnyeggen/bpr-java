package com.nyeggen.bpr;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import com.nyeggen.bpr.sampling.SamplableInteractionList;

public class BPRFactorization {
	private final int k;
	//Only necessary for initialization of new factors.
	private final Random rng;
	private final TIntObjectHashMap<double[]> personFactors=new TIntObjectHashMap<double[]>();
	private final TIntObjectHashMap<double[]> itemFactors=new TIntObjectHashMap<double[]>();
	
	/*Instead of a ring of locks, we could use ConcurrentHashMaps with a lock for each key*/
	private ReentrantLock[] itemLocks;
	private ReentrantLock[] personLocks;
	
	/**Construct a Factorization with the default item and person locking factors,
	 * and the given degree of factorization.*/
	public BPRFactorization(int k){
		this(k, new Random(),128,128);
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
	public BPRFactorization(int k, Random rng, int itemLockingFactor, int personLockingFactor){
		this.k=k;
		this.rng = rng;
		itemLocks = new ReentrantLock[itemLockingFactor];
		personLocks = new ReentrantLock[personLockingFactor];
		for (int i=0; i<itemLockingFactor; i++) itemLocks[i] = new ReentrantLock();
		for (int i=0; i<personLockingFactor; i++) personLocks[i] = new ReentrantLock();
	}
	
	/**@return A k-long vector of standard-normally distributed reals, for
	 * initializing new person and item factors.*/
	protected double[] newFactor(){
		final double[] out = new double[k];
		synchronized (rng) {
			for (int i=0;i<k;i++){
				out[i] = rng.nextGaussian();
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
			
			final double[] personFactor = personFactors.get(p.personID);
			final double[] iItemFactor = itemFactors.get(p.lessPreferredItem);
			final double[] jItemFactor = itemFactors.get(p.morePreferredItem);
			for(int i=0;i<k;i++){
				final double wuf = personFactor[i];
				final double hif = iItemFactor[i];
				final double hjf = jItemFactor[i];
				personFactor[i] += -learnRate * (dxuij * (hif - hjf) - regularization * wuf);
				iItemFactor[i]  += -learnRate * (dxuij * wuf - regularization * hif);
				jItemFactor[i]  += -learnRate * (dxuij * -wuf - regularization * hjf);
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
		return dot(itemFactors.get(itemID), personFactors.get(personID));
	}
	
	private static double dot(double[] a, double[] b){
		double out = 0;
		final int len = a.length;
		for(int i=0; i<len; i++){
			out += a[i]*b[i];
		}
		return out;
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
