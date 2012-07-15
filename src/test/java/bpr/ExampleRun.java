package bpr;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Random;

public class ExampleRun {

	@Test
	public final void exampleRun() {
		Random rng = new Random();
		InteractionsBuilder builder = new InteractionsBuilder();
		Factorization fzation = new Factorization(10);
		
		//100 possible items, 100 people, 10 items / person.
		for(int i=0; i<1000; i++){
			for(int j=0; j<20; j++){
				//Even people are 3x more likely to interact w/ an even item,
				//and vice versa
				if(rng.nextFloat() < 0.75) {
					if (i%2==0) builder.addInteraction(i, rng.nextInt(50)*2);
					else builder.addInteraction(i, rng.nextInt(50)*2+1);
				} else {
					builder.addInteraction(i, rng.nextInt(100));
				}
			}
		}
		
		SamplableInteractionList sampler = builder.makePersonSamplableInteractions();
		
		for (int i=0;i<200000;i++) fzation.update(sampler.get(), 0.05, 0.05);
		
		int successCount=0;
		int validCount=0;
		for (int i=0;i<10000;i++){
			int person = rng.nextInt(1000);
			int item1 = rng.nextInt(100);
			int item2 = rng.nextInt(100);
			if(item1%2 != item2%2) {
				validCount++;
				
				int odd = item1%2==1 ? item1 : item2;
				int even = item1%2==0 ? item1 : item2;
				
				if(person%2==0 && fzation.predict(person, even) > fzation.predict(person, odd))
					successCount++;
				if(person%2==1 && fzation.predict(person, odd) > fzation.predict(person, even))
					successCount++;
			}
		}
		System.out.print("Area Under Curve for simulation: ");
		System.out.println((double)successCount/(double)validCount);
		assertTrue((double)successCount/(double)validCount > 0.51);
	}
}
