package com.nyeggen.bpr.sampling;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.nyeggen.bpr.PreferenceTuple;

/**Getting a PreferenceTuple from an object of this class selects a person-
 * preferred item pair with equal odds for each person, and then equal odds for
 * each item that person has a positive interaction with.  Sampling from this
 * object approximately maximizes average area under curve per person.
 * 
 * It should usually be constructed with an InteractionsBuilder.*/
public class PersonSamplableInteractions implements SamplableInteractionList {

	private final RandomGenerator rng = new MersenneTwister();
	
	private final TIntObjectHashMap<int[]> personToItems;
	private final int[] uniqueItems;
	private final int[] uniquePeople;
	
	public PersonSamplableInteractions(TIntObjectHashMap<TIntHashSet> personToItemSet){
		personToItems = new TIntObjectHashMap<int[]>();
		uniquePeople = new int[personToItemSet.size()];
		final TIntHashSet items = new TIntHashSet();

		int i=0;
		final TIntObjectIterator<TIntHashSet> it = personToItemSet.iterator();
		while(it.hasNext()){
			it.advance();
			final int person = it.key();
			final TIntHashSet theseItems = it.value();

			uniquePeople[i++]=person;
			items.addAll(theseItems);
			
			personToItems.put(person, new int[theseItems.size()]);
			int j=0;
			for(int item:theseItems.toArray()) personToItems.get(person)[j++]=item;
		}
		uniqueItems = new int[items.size()];
		i=0;
		for (int item:items.toArray()) uniqueItems[i++]=item;
	}
	
	public PersonSamplableInteractions(Map<Integer, TIntHashSet> personToItemSet){
		personToItems = new TIntObjectHashMap<int[]>();
		uniquePeople = new int[personToItemSet.size()];
		final TIntHashSet items = new TIntHashSet();
		int i=0;
		for(Entry<Integer, TIntHashSet> entry:personToItemSet.entrySet()){
			final int person = entry.getKey();
			final TIntHashSet theseItems = entry.getValue();

			uniquePeople[i++]=person;
			items.addAll(theseItems);
			
			personToItems.put(person, new int[theseItems.size()]);
			int j=0;
			for(int item:theseItems.toArray()) personToItems.get(person)[j++]=item;
		}
		uniqueItems = new int[items.size()];
		i=0;
		for (int item:items.toArray()) uniqueItems[i++]=item;
	}
	
	@Override
	public PreferenceTuple get() {
		int p = uniquePeople[rng.nextInt(uniquePeople.length)];
		int[] prefItems = personToItems.get(p);
		int prefItem = prefItems[rng.nextInt(prefItems.length)];
		HashSet<Integer> prefItemSet = new HashSet<Integer>(prefItems.length);
		for (int item:prefItems){ prefItemSet.add(item); }
		
		int unPrefItem=uniqueItems[rng.nextInt(uniqueItems.length)];
		while(prefItemSet.contains(unPrefItem)){
			unPrefItem=uniqueItems[rng.nextInt(uniqueItems.length)];
		}
		
		return new PreferenceTuple(p,prefItem,unPrefItem);
	}

}
