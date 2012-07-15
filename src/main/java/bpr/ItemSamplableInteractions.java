package bpr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/**Getting records from an object of this class selects person-item pairs
 * with equal odds.  This means that popular items will be somewhat oversampled
 * and people with fewer interactions undersampled, relative to 
 * PersonSamplableInteractions.
 * 
 * It should usually be constructed with an InteractionsBuilder.*/
public class ItemSamplableInteractions implements SamplableInteractionList {

	private final RandomGenerator rng = new MersenneTwister();
	private final HashMap<Integer, int[]> personToItems;
	private final int[] uniqueItems;
	private final int[] uniquePeople;
	
	//Aligned arrays
	private final int[] items;
	private final int[] people;

	public ItemSamplableInteractions(Map<Integer, ? extends Set<Integer>> personToItemSet){
		ArrayList<Integer> itemBuilder = new ArrayList<Integer>(personToItemSet.size());
		ArrayList<Integer> peopleBuilder = new ArrayList<Integer>(personToItemSet.size());
		
		personToItems = new HashMap<Integer,int[]>();
		uniquePeople = new int[personToItemSet.size()];
		HashSet<Integer> allItems = new HashSet<Integer>();
		int i=0;
		for(Entry<Integer, ? extends Set<Integer>> entry:personToItemSet.entrySet()){
			int person = entry.getKey();
			Collection<Integer> theseItems = entry.getValue();

			uniquePeople[i++]=person;
			allItems.addAll(theseItems);
			
			personToItems.put(person, new int[theseItems.size()]);
			int j=0;
			for(int item:theseItems){
				personToItems.get(person)[j++]=item;
				itemBuilder.add(item);
				peopleBuilder.add(person);
			}
		}
		uniqueItems = new int[allItems.size()];
		i=0;
		for (int item:allItems)uniqueItems[i++]=item;
		
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
