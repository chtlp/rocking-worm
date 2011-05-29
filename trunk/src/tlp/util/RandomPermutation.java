package tlp.util;

import java.util.Random;

public class RandomPermutation {
	Random rand = null;
	
	public RandomPermutation(int seed) {
		rand = new Random(seed);
	}
	
	public void permute(Object[] arr) {
		for(int i=0; i<arr.length * 4; ++i) {
			int x = rand.nextInt(arr.length);
			int y = rand.nextInt(arr.length);
			
			Object o = arr[x];
			arr[x] = arr[y];
			arr[y] = o;
		}
	}
}
