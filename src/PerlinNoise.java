import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;
import java.util.function.Function;

import javax.imageio.ImageIO;

import dimensional.DimensionalArray;
import dimensional.DimensionalVector;

public class PerlinNoise {

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
			Stack<Double> stack = new Stack<>();
			int ind[] = new int[indexes.length];
			for(int i = 0; i < ind.length; i++)
				ind[i] = indexes[i]/cellWidth;
			
			for(int i = 0; i < Math.pow(2, ind.length); i++) {
				double arr[] = new double[dimensions];
				int corner[] = new int[dimensions];
				for(int j = 0; j < arr.length; j++) {
					corner[j] = ind[j]+i>>j&1;
					arr[j] = (double)indexes[j]-corner[j]*cellWidth;
				}
				System.out.println(Arrays.toString(corner)+" "+vectors.get(corner)+" "+vectors.get(corner).getMagnitude()+" "+Arrays.toString(arr)+" "+DimensionalVector.dotProduct(vectors.get(corner), new DimensionalVector(arr)));
				stack.push(DimensionalVector.dotProduct(vectors.get(corner), new DimensionalVector(arr)));
			}
			
			int step = (int)Math.pow(2, dimensions);
			while(stack.size() > 1) {
				double n0 = stack.pop(), n1 = stack.pop();
				stack.push(n0+function.apply(dimensions-(Math.log(step--)/Math.log(2)))*(n1-n0));
			}
			
			cells.set(stack.pop(), indexes);
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
		
		BufferedImage img = new BufferedImage(cells.size(0), cells.size(1), BufferedImage.TYPE_INT_RGB);
		int arr[] = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
		for(int i = 0; i < arr.length; i++) {
			int n = (int)(255*cells.getData().get(i));
			arr[i] = new Color(n, n, n).getRGB();
		}
		return img;
	}
	
	public static void main(String[] args) throws IOException {
		PerlinNoise test = new PerlinNoise(10, 10, 10);
		System.out.println(test.size(0)+" "+test.size(1));
		ImageIO.write(test.getImage(), "png", new File("noise.png"));
	}
	
}
