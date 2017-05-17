package hw2;

import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class csci_576_hw2 {
	// Max time to run program.
	// With fewer centroids, it can be difficult to get a good error in a decent time so
	// run optimization steps for maxTime in milliseconds.
	static int maxTime = 60000;
	static int numChannels = 1;
	static int quantError = 16;
	
	private static double[][] quant(double[][] inputPixels, int numCentroids, int height, int width, int curChannel) {
		
		double[][] newPixelArray = new double[height][width];
		List<Coords> vectors = new ArrayList<Coords>();
		List<Coords> centroids = new ArrayList<Coords>();
		
		//Initialize the centroids.
		int dimension, spacing;
		
		int remaining = 0;
		int num = 0;
		
		// If we're dealing with 3 channels, take the cubed root of each channel.
		if (numChannels == 3){
			num = (int) Math.cbrt(numCentroids);
			dimension = (int) Math.floor(Math.log(num)/Math.log(2));
			remaining = num - (dimension*dimension);
		}
		else{
			dimension = (int) Math.ceil(Math.log(numCentroids)/Math.log(2));
			remaining = numCentroids - (dimension*dimension);
		}
		spacing = (int) (256 / dimension);
		
		// Place any remaining centroids at random.	
		if (remaining > 0){
			Random ran = new Random();
			while (remaining > 0){
				
				Coords centroid = new Coords();
				centroid.x = ran.nextInt(256) + 0;
				centroid.y = ran.nextInt(256) + 0;
				centroids.add(centroid);
				remaining--;
			}
		}
		
		// Assign the centroids based on the spacing.
		for(int m = spacing / 2; m < 256; m = spacing + m){
			for(int n = spacing / 2; n <= 256; n = spacing + n){
				Coords centroid = new Coords();
				centroid.x = n;
				centroid.y = m;
				centroids.add(centroid);
			}
		}
		System.out.println("# centroids" + centroids.size());
//		
		// Create a vector by using two adjacent points.
		for(int j = 0; j < height; j++){
			for(int i = 0; i < width / 2; i++){
				Coords vector = new Coords();
				vector.x = (int) inputPixels[j][i * 2];
				vector.y = (int) inputPixels[j][i * 2 + 1];
				// Add this vector to the vector list
				vectors.add(vector);
			}
		}
		
		// Calculate the best centroids
		List<Coords> newPoints = calcCentroids(centroids, vectors);
		
		//Assign values to 
		int k = 0;
		for(int j = 0; j < newPixelArray.length; j++){
			for(int i = 0; i < newPixelArray[j].length; i += 2){
				newPixelArray[j][i] = newPoints.get(k).x;
				newPixelArray[j][i+1] = newPoints.get(k).y;
				k++;
			}
		}
		return newPixelArray;
	}
	
	private static List<Coords> calcCentroids(List<Coords> centroids, List<Coords> vectors) {
        
        double min; 
        boolean recalcCentroids = true;
        Date startDate = new Date();
        long startTime = startDate.getTime();
        
        // Calculate the centroid nearest to each vector
        while(recalcCentroids){
        	for(int i = 0; i < vectors.size(); i++){
        		min = 999999999;
        		int clusterindex = 0;
        		for(int j = 0; j < centroids.size(); j++){
        			double distance = eucDistance(vectors.get(i), centroids.get(j));
        			if(distance < min){
        				min = distance;
        				clusterindex = j;
        			}
        		}
        		// Assign the vector to this cluster.
        		vectors.get(i).cluster = clusterindex;
        	}

        	// Calculate the values for each centroid based on 
        	// which vectors were mapped to it.
        	int totalX = 0;
            int totalY = 0;
            int numVectors = 0;
        	
        	for(int i = 0; i < centroids.size(); i++){
        		totalX = 0;
        		totalY = 0;
        		numVectors = 0;
                for(int j = 0; j < vectors.size(); j++){
                    if(vectors.get(j).cluster == i){
                        totalX += vectors.get(j).x;
                        totalY += vectors.get(j).y;
                        numVectors++;
                    }
                }
                // Set the centroid to the center of the cluster.
                if(numVectors > 0){
                    centroids.get(i).x = totalX / numVectors;
                    centroids.get(i).y = totalY / numVectors;
                }
            }
            
            double totaldiff = 0;
            for(int i = 0; i < vectors.size(); i++){
            	double deltaY = Math.abs(vectors.get(i).y - centroids.get(vectors.get(i).cluster).y);
            	double deltaX = Math.abs(vectors.get(i).x - centroids.get(vectors.get(i).cluster).x);
            	double delta = deltaX + deltaY;
            	totaldiff += delta;
            }
            
            recalcCentroids = false;
            if ((totaldiff / vectors.size()) > quantError){
            	recalcCentroids = true;
            }
            
            //Run each channel for a pre-determinded amount of time, otherwise it can run away.
            Date timeNow = new Date();
            if ((timeNow.getTime() - startTime) > maxTime){
            	System.out.println("Timeout, that's close enough.");
            	recalcCentroids = false;	
            }
        }
        
        for(int i = 0; i < vectors.size(); i++){
        	for(int j = 0; j < centroids.size(); j++){
        		if(vectors.get(i).cluster == j){
        			vectors.get(i).x = centroids.get(j).x;
        			vectors.get(i).y = centroids.get(j).y;
        		}
        	}
        }
		return vectors;
	}

	private static double eucDistance(Coords p, Coords c){
        return Math.sqrt(Math.pow((c.x - p.x), 2) + Math.pow((c.y - p.y), 2));
    }

	public static void main(String[] args) throws IOException {
	    	
	    	String fileName = args[0];
	    	int numVector = Integer.parseInt(args[1]);
	    	numChannels = 1;
	    	// If the image file extension is rgb, then set the number of channels to 3. 
	    	String fileExt = fileName.replaceAll("^.*\\.([a-zA-Z]*)", "$1");
	    	if (fileExt.equalsIgnoreCase("rgb")){
	    		System.out.println("RGB - 3 Channel Image Input. N should have a cube root to be computed correctly.");
	    		numChannels = 3;	
	    	}
	    	else{
	    		System.out.println("Grey - 1 Channel Image Input");
	    	}
	    	
	    	int width = 352;
	    	int height = 288;
	    	
	    	// Display the original image to the left of the modified image.
	    	
	    	BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	    	BufferedImage newImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		    try {
			    File file = new File(fileName);
			    InputStream is = new FileInputStream(file);
		    
			    long len = file.length();
			    byte[] bytes = new byte[(int)len*3];
			    // Read the file bytes into bytes array.
			    int offset = 0;
			    int numRead = 0;
			    while (offset < (bytes.length) && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
		          offset += numRead;
			    }
			    is.close();
		    
			    byte r, g, b, grey;
			    int pixelValue;
		    	int ind = 0;
				  for(int y = 0; y < height; y++){
					for(int x = 0; x < width; x++){
						
						if (numChannels == 3){
							r = bytes[ind];
							g = bytes[ind+height * width];
							b = bytes[ind+height * width * 2]; 
							
							pixelValue = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						}
						else{
							grey = bytes[ind];
							pixelValue = 0xff000000 | ((grey & 0xff) << 16) | ((grey & 0xff) << 8) | (grey & 0xff);
						}
						img.setRGB(x, y, pixelValue);
						ind++;
					}
				}
				  
				    
				double pixelArr[][][] = new double[3][height][width];
		    	ind = 0;
		    	double newPixelArr[][][] = new double[3][height][width];
		    	for(int channelIdx = 0; channelIdx < numChannels; channelIdx++){
				  for(int j = 0; j < height; j++){
					for(int i = 0; i < width; i++){
				 
						byte color = 0;
						if (channelIdx == 0){
							color = bytes[ind];
						}
						else if(channelIdx == 1){
							color = bytes[ind+height*width];
						}
						else{
							color = bytes[ind+height*width*2];
						}
						int intColor = color & 0xff;		    
					    pixelArr[channelIdx][j][i] = intColor;
						ind++;
					}
				  }
				  ind = 0;
				  newPixelArr[channelIdx] = quant(pixelArr[channelIdx], numVector, height, width, channelIdx);
		    	}
		    	for(int j = 0; j < height; j++){
					for(int i = 0; i < width; i++){
						pixelValue = 0;
						if(numChannels == 1){
							int y = (int) newPixelArr[0][j][i];
							pixelValue = ((0 << 24) + (y << 16) + (y << 8) + y);
						}
						else{
							pixelValue = ((0 << 24) + ((int) newPixelArr[0][j][i] << 16) + ((int) newPixelArr[1][j][i] << 8) + (int) newPixelArr[2][j][i]);
						}
						newImg.setRGB(i, j, pixelValue);
					}
				}
				
		    } catch (FileNotFoundException e) {
		      e.printStackTrace();
		    } catch (IOException e) {
		      e.printStackTrace();
		    }
    	
		    JPanel  panel = new JPanel ();
		    panel.add (new JLabel (new ImageIcon (img)));
		    panel.add (new JLabel (new ImageIcon (newImg)));
		    JFrame frame = new JFrame();
		    
		    JLabel lbText1 = new JLabel("Width: " + width + " - Height: " + height);
			lbText1.setHorizontalAlignment(SwingConstants.CENTER);
			
		    frame.getContentPane().add(panel);
		    frame.pack();
		    frame.setVisible(true);
		    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   
		
		 }
}

class Coords{
	public int x;
	public int y;
	public int cluster;
	
	public Coords(){	
	}
	
	public Coords(int x, int y, int cluster){
		super();
		this.x = x;
		this.y = y;
		this.cluster = cluster;
	}

}