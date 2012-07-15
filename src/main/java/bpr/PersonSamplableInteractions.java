package bpr;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/**Getting a PreferenceTuple from an object of this class selects a person-
 * preferred item pair with equal odds for each person, and then equal odds for
 * each item that person has a positive interaction with.  Sampling from this
 * object approximately maximizes average area under curve per person.
 * 
 * It should usually be constructed with an InteractionsBuilder.*/
public class PersonSamplableInteractions implements SamplableInteractionList, Serializable {

	private final RandomGenerator rng = new MersenneTwister();
	
	private final HashMap<Integer, int[]> personToItems;
	private final int[] uniqueItems;
	private final int[] uniquePeople;
	
	public PersonSamplableInteractions(Map<Integer, ? extends Set<Integer>> personToItemSet){
		personToItems = new HashMap<Integer,int[]>();
		uniquePeople = new int[personToItemSet.size()];
		HashSet<Integer> items = new HashSet<Integer>();
		int i=0;
		for(Entry<Integer, ? extends Set<Integer>> entry:personToItemSet.entrySet()){
			int person = entry.getKey();
			Collection<Integer> theseItems = entry.getValue();

			uniquePeople[i++]=person;
			items.addAll(theseItems);
			
			personToItems.put(person, new int[theseItems.size()]);
			int j=0;
			for(int item:theseItems) personToItems.get(person)[j++]=item;
		}
		uniqueItems = new int[items.size()];
		i=0;
		for (int item:items) uniqueItems[i++]=item;
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
