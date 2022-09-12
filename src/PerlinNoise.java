import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.function.Function;
import javax.imageio.ImageIO;
import dimensional.DimensionalArray;
import dimensional.DimensionalVector;

public class PerlinNoise {

    static double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
	public static Function<Double, Double> smoothstep = n -> n < 0 ? 0 : n > 1 ? 1 : 3*Math.pow(n, 2) - 2*Math.pow(n, 3),
										   linear = n -> n;
	
	private int dimensions, cellWidth;
	private double seed;
	
	private Random rand;
	
	private DimensionalArray<Double> cells;
	private DimensionalArray<DimensionalVector> vectors;
	private Function<Double, Double> function = smoothstep;
	
	
	public PerlinNoise(int cellWidth, Random rand, int ... widths) {
		dimensions = widths.length;
		this.cellWidth = cellWidth;
		this.rand = rand;
		
		vectors = new DimensionalArray<>(n -> {
			double arr[] = new double[dimensions];
			for(int i = 0; i < arr.length; i++)
				arr[i] = rand.nextGaussian();
			DimensionalVector out = new DimensionalVector(arr);
			out.normalize();
			return out;
		}, widths);
		for(int i = 0; i < widths.length; i++) {
			widths[i]--;
			widths[i] *= cellWidth;
		}
		cells = new DimensionalArray<>(widths);
	}
	
	public PerlinNoise(int cellWidth, int ... widths) {
		this(cellWidth, new Random(), widths);
	}
	
	public void defineCell(int ... indexes) {
		if(cells.get(indexes) == null) {
			Queue<Double> queue = new LinkedList<>();
			int ind[] = new int[indexes.length];
			for(int i = 0; i < ind.length; i++)
				ind[i] = indexes[i]/cellWidth;
			//System.out.println(Arrays.toString(ind));
			
			for(int i = 0; i < Math.pow(2, ind.length); i++) {
				double offset[] = new double[dimensions];
				int corner[] = new int[dimensions];
				for(int j = 0; j < offset.length; j++) {
				    
					corner[j] = ind[j]+(i>>j&1);
				    //System.out.println(indexes[j]+" "+((double)indexes[j]/cellWidth)+" "+corner[j]);
				    offset[j] = (double)indexes[j]/cellWidth-corner[j];
					
				}
				
				if(Math.abs(DimensionalVector.dotProduct(vectors.get(corner), new DimensionalVector(offset))) > 1)
				    System.out.println(Arrays.toString(indexes)+" "+i+" "+DimensionalVector.dotProduct(vectors.get(corner), new DimensionalVector(offset)));
				queue.add(DimensionalVector.dotProduct(vectors.get(corner), new DimensionalVector(offset)));
			}
			
			String log = "";
			int step = (int)Math.pow(2, dimensions);
			while(queue.size() > 1) {
			    log += queue+"\n";
				double n0 = queue.poll(), n1 = queue.poll();
				queue.add(n0+function.apply(dimensions-(Math.log(step--)/Math.log(2)))*(n1-n0));
			}
			
			//if(Math.abs(stack.peek()) > 1)
			    //System.out.println(stack.peek()+" "+Arrays.toString(ind));
			min = Math.min(min, queue.peek());
			max = Math.max(max, queue.peek());
			
			if(Math.abs(queue.peek()) > 1)
			    System.out.println(log+Arrays.toString(ind)+" "+queue.peek());
			cells.set(queue.poll(), indexes);
		}
	}
	
	public void defineAllCells() {
		int ind[] = new int[dimensions];
		for(int i = 0; i < cells.size(); i++) {
			defineCell(ind);
			int n = 0;
			do {
				ind[n] = (ind[n]+1)%cells.size(n);
				n++;
			} while(n < dimensions && ind[n-1] == 0);
		}
	}
	
	public double get(int ... indexes) {
		if(indexes.length != dimensions)
			throw new IllegalArgumentException("Number of indexes must equal the dimension of this noise");
		defineCell(indexes);
		return cells.get(indexes);
	}
	
	public int size() {
		return cells.size();
	}
	
	public int size(int dimension) {
		return cells.size(dimension);
	}
	
	public BufferedImage getImage() {
		if(dimensions != 2)
			throw new IllegalArgumentException("Only 2 dimensions are supported rn");
			
		defineAllCells();
		for(int i = 0; i < cells.size(0); i++) {
		    for(int j = 0; j < cells.size(1); j++)
		        System.out.printf("%6.2f", cells.get(i, j));
		    System.out.println();
		}
		System.out.println(min+" "+max);
		
		BufferedImage img = new BufferedImage(cells.size(0), cells.size(1), BufferedImage.TYPE_INT_RGB);
		int arr[] = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
		for(int i = 0; i < arr.length; i++) {
			int n = (int)(255*cells.getData().get(i));
			if(n < 0 || n > 255)
			    System.out.println(n);
			arr[i] = new Color(n, n, n).getRGB();
		}
		return img;
	}
	
	public static void main(String[] args) throws IOException {
		PerlinNoise test = new PerlinNoise(3, 3, 3);
		System.out.println(test.size(0)+" "+test.size(1));
		ImageIO.write(test.getImage(), "png", new File("noise.png"));
	}
	
}
