package com.nyeggen.bpr.sampling;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.nyeggen.bpr.PreferenceTuple;

/**Getting records from an object of this class selects person-item pairs
 * with equal odds.  This means that popular items will be somewhat oversampled
 * and people with fewer interactions undersampled, relative to 
 * PersonSamplableInteractions.
 * 
 * It should usually be constructed with an InteractionsBuilder.*/
public class ItemSamplableInteractions implements SamplableInteractionList {

	private final RandomGenerator rng = new MersenneTwister();
	private final TIntObjectHashMap<int[]> personToItems;
	private final int[] uniqueItems;
	private final int[] uniquePeople;
	
	//Aligned arrays
	private final int[] items;
	private final int[] people;

	public ItemSamplableInteractions(TIntObjectHashMap<TIntHashSet> personToItemSet){
		final TIntArrayList itemBuilder = new TIntArrayList(personToItemSet.size());
		final TIntArrayList peopleBuilder = new TIntArrayList(personToItemSet.size());

		personToItems = new TIntObjectHashMap<int[]>();
		uniquePeople = new int[personToItemSet.size()];
		final TIntHashSet allItems = new TIntHashSet();
		int i=0;
		final TIntObjectIterator<TIntHashSet> it = personToItemSet.iterator();
		while(it.hasNext()){
			it.advance();
			final int person = it.key();
			final TIntHashSet theseItems = it.value();

			uniquePeople[i++]=person;
			allItems.addAll(theseItems);
			
			personToItems.put(person, new int[theseItems.size()]);
			int j=0;
			for(int item:theseItems.toArray()){
				personToItems.get(person)[j++]=item;
				itemBuilder.add(item);
				peopleBuilder.add(person);
			}

		}
		uniqueItems = new int[allItems.size()];
		i=0;
		for (int item:allItems.toArray()) uniqueItems[i++]=item;
		
		items = new int[itemBuilder.size()];
		people = new int[peopleBuilder.size()];
		for (i=0;i<items.length;i++){
			items[i]=itemBuilder.get(i);
			people[i]=peopleBuilder.get(i);
		}

	}
	
	public ItemSamplableInteractions(Map<Integer, TIntHashSet> personToItemSet){
		TIntArrayList itemBuilder = new TIntArrayList(personToItemSet.size());
		TIntArrayList peopleBuilder = new TIntArrayList(personToItemSet.size());
		
		personToItems = new TIntObjectHashMap<int[]>();
		uniquePeople = new int[personToItemSet.size()];
		final TIntHashSet allItems = new TIntHashSet();
		int i=0;
		for(Entry<Integer, TIntHashSet> entry:personToItemSet.entrySet()){
			int person = entry.getKey();
			final TIntHashSet theseItems = entry.getValue();

			uniquePeople[i++]=person;
			allItems.addAll(theseItems);
			
			personToItems.put(person, new int[theseItems.size()]);
			int j=0;
			for(int item:theseItems.toArray()){
				personToItems.get(person)[j++]=item;
				itemBuilder.add(item);
				peopleBuilder.add(person);
			}
		}
		uniqueItems = new int[allItems.size()];
		i=0;
		for (int item:allItems.toArray()) uniqueItems[i++]=item;
		
		items = new int[itemBuilder.size()];
		people = new int[peopleBuilder.size()];
		for (i=0;i<items.length;i++){
			items[i]=itemBuilder.get(i);
			people[i]=peopleBuilder.get(i);
		}
	}
	
	@Override
	public PreferenceTuple get() {
		int idx = rng.nextInt(people.length);
		int p = people[idx];
		int prefItem = items[idx];
		
		int[] prefItems = personToItems.get(p);
		HashSet<Integer> prefItemSet = new HashSet<Integer>(prefItems.length);
		for (int item:prefItems){ prefItemSet.add(item); }
		
		int unPrefItem=uniqueItems[rng.nextInt(uniqueItems.length)];
		while(prefItemSet.contains(unPrefItem)){
			unPrefItem=uniqueItems[rng.nextInt(uniqueItems.length)];
		}
		
		return new PreferenceTuple(p,prefItem,unPrefItem);
	}

}
