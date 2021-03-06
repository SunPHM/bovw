package py.kmeans;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.lib.HashPartitioner;
import org.apache.mahout.clustering.iterator.ClusterWritable;
import org.apache.mahout.clustering.kmeans.RandomSeedGenerator;
import org.apache.mahout.common.distance.CosineDistanceMeasure;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

public class KMeans {
	
	public static int k = 30;
	public static int d = 128;
	private static int iterNum = 1;
	public static String centers = "/user/hadoop/cls/part-randomSeed"; 
	
	public static void main(String[] args) throws IOException{
		// setup an experiment with small data
		// writeVectors("data/vs.seq");
		// createSeeds(args[0], args[1]);
		// readCenters("seeds/part-randomSeed");
		// readData("data/vs.seq");
		// args = new String[3];
		// args[0] = "/home/yp/Desktop/fs.seq";
		// args[1] = args[1] + "/part-randomSeed";
		// args[2] = "output";
		// centers = args[1];
		run(args);
		// centers = args[1];
	}
	
	public static void run(String[] args) throws IOException{
		
		for (int i = 0; i < iterNum; i++){
			JobConf conf = new JobConf(KMeans.class);
			conf.setJobName("kmeans");
			
			conf.setOutputKeyClass(LongWritable.class);
			conf.setOutputValueClass(VectorWritable.class);
			
			conf.setMapperClass(KMeansMapper.class);
			conf.setCombinerClass(KMeansReducer.class);
			conf.setPartitionerClass(HashPartitioner.class);
			conf.setReducerClass(KMeansReducer.class);
				
			conf.setNumMapTasks(10);
			conf.setNumReduceTasks(2);
			
			conf.setInputFormat(SequenceFileInputFormat.class);
			conf.setOutputFormat(SequenceFileOutputFormat.class);
				
			FileInputFormat.setInputPaths(conf, new Path(args[0]));
			FileOutputFormat.setOutputPath(conf, new Path(args[2]));
				
			// centers = args[1];
			JobClient.runJob(conf);
		}
	}
	
	public static void readData(String filename) throws IOException{
		
		Configuration conf = new Configuration();
	    FileSystem fs = FileSystem.get(URI.create(filename), conf);
	    Path cfile = new Path(filename);
	    
	    SequenceFile.Reader reader = new SequenceFile.Reader(fs, cfile, conf);
	    LongWritable key = new LongWritable();
	    VectorWritable val = new VectorWritable();
	    
	    //System.out.println(reader.getKeyClassName());
	    //System.out.println(reader.getValueClassName());
	    
	    while(reader.next(key, val)){
	    	// output the key and value
	    	System.out.println(key);
	    	System.out.println(val);
	    }
	    
	    reader.close();
	}
	
	public static void readCenters(String filename) throws IOException{
		
		Configuration conf = new Configuration();
	    FileSystem fs = FileSystem.get(URI.create(filename), conf);
	    Path cfile = new Path(filename);
	    
	    SequenceFile.Reader reader = new SequenceFile.Reader(fs, cfile, conf);
	    Text key = new Text();
	    ClusterWritable val = new ClusterWritable();
	    
	    //System.out.println(reader.getKeyClassName());
	    //System.out.println(reader.getValueClassName());
	    
	    while(reader.next(key, val)){
	    	// output the key and value
	    	System.out.println(key);
	    	System.out.println(val.getValue().getCenter());
	    	//Cluster cl = val.getValue();
	    	//cl.getParameters();
	    	
	    	//val.getValue()
	    	
	    }
	    
	    reader.close();
	}
	
	public static void createSeeds(String inpath, String outpath) throws IOException{
		// write random seeds
        Path input = new Path(inpath);
        Configuration conf = new Configuration();
        Path output = new Path(outpath);
        RandomSeedGenerator.buildRandom(conf, input, output, k, new CosineDistanceMeasure());
        System.out.println("random generation is done");
	}
	
	public static void writeVectors(String filename) throws IOException{
		// write a few vectors to a sequence file
		double [][] pts = {{0, 1.0}, {1.0, 0},
				{1.0, 2.0}, {4.0, 5.0}, {0.5, 1.0},
				{1.5, 2.0}, {5.0, 6.0}, {5.0, 5.5}};
		
		Path path = new Path(filename);
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);
		SequenceFile.Writer writer = new SequenceFile.Writer(fs, conf, path, LongWritable.class, VectorWritable.class);
		VectorWritable vw = new VectorWritable();
		long rn = 0;
		
		for(int i = 0; i < pts.length; i++){
			Vector vec = new DenseVector(d);
			vec.assign(pts[i]);
			vw.set(vec);
			writer.append(new LongWritable(rn++), vw);
		}
		
		writer.close();	
	}
	
}
