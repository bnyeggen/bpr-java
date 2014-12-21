package com.nyeggen.bpr.interaction;

import gnu.trove.set.hash.TIntHashSet;

import java.util.concurrent.ConcurrentHashMap;

import com.nyeggen.bpr.sampling.ItemSamplableInteractions;
import com.nyeggen.bpr.sampling.PersonSamplableInteractions;

/**A version of InteractionsBuilder that can handle concurrent inserts.*/
public class ConcurrentHashInteractionsBuilder implements InteractionsBuilder {
	
	private final ConcurrentHashMap<Integer, TIntHashSet> personToItemSet = new ConcurrentHashMap<Integer, TIntHashSet>();

	@Override
	public PersonSamplableInteractions makePersonSamplableInteractions(){
		return new PersonSamplableInteractions(personToItemSet);
	}
	
	@Override
	public ItemSamplableInteractions makeItemSamplableInteractions(){
		return new ItemSamplableInteractions(personToItemSet);
	}
	
	@Override
	public void addInteraction(int personID, int itemID){
		personToItemSet.putIfAbsent(personID, new TIntHashSet()).add(itemID);
	}
	
	@Override
	public void removePerson(int personID){
		personToItemSet.remove(personID);
	}
	
	@Override
	public void removeItem(int itemID){
		for(TIntHashSet items:personToItemSet.values()){
			items.remove(itemID);
		}
	}
}
