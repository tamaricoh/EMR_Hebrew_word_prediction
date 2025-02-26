package com.dsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.apache.hadoop.mapreduce.Partitioner;

public class valuesJoinerStep {

    // Mapper class for the second step of sequence processing
    public static class MapperClass extends Mapper<Text, DoubleWritable, Text, Text> {
        private Text newKey = new Text();
        private Text newVal = new Text();

        @Override
        public void map(Text key, DoubleWritable value, Context context) throws IOException, InterruptedException {
            String prevKey = key.toString();
            String[] keyWords = prevKey.split("\\$\\$");
            String count = String.valueOf(value.get());

            if (keyWords.length == 1){ // w1**
                newKey.set(keyWords[0]);
                newVal.set("Single" + Defs.seperator + count);
                context.write(newKey, newVal);
            }
            else if(keyWords.length == 2){ // w1w2*
                newKey.set(keyWords[0] + Defs.delimiter + keyWords[1]);
                newVal.set("Double" + Defs.seperator + count);
                context.write(newKey, newVal);
            }
            else {
                newKey.set(keyWords[1]);
                newVal.set(prevKey + Defs.seperator + "C1:" + Defs.seperator + count);
                context.write(newKey, newVal);

                newKey.set(keyWords[2]);
                newVal.set(prevKey + Defs.seperator + "N1:" + Defs.seperator + count);
                context.write(newKey, newVal);

                newKey.set(keyWords[0] + Defs.delimiter + keyWords[1]);
                newVal.set(prevKey + Defs.seperator + "C2:" + Defs.seperator + count);
                context.write(newKey, newVal);

                newKey.set(keyWords[1] + Defs.delimiter + keyWords[2]);
                newVal.set(prevKey + Defs.seperator + "N2:" + Defs.seperator + count);
                context.write(newKey, newVal);

            }
        }
    }

    // Reducer class for the second step of sequence processing
    public static class ReducerClass extends Reducer<Text, Text, Text, Text> {

        private Text newVal = new Text();
        private Text newKey = new Text();

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String count = "";
            ArrayList<String> values_to_process = new ArrayList<>();
            for (Text value : values) {
                String valueStr = value.toString();
                if (valueStr.startsWith("Single") || valueStr.startsWith("Double")) {
                    String[] parts = valueStr.split("\\%\\%");
                    count = count.concat(parts[parts.length - 1]);
                } else {
                    values_to_process.add(valueStr);
                }
            }

            for (String value : values_to_process) {
                    String[] parts = value.split("\\%\\%");
                    newVal.set(parts[1] + Defs.seperator + count + Defs.seperator + parts[2]);
                    newKey.set(parts[0]);
                    context.write(newKey, newVal);
                }
        }
    }

    public static class PartitionerClass extends Partitioner<Text, Text> {
        @Override
        public int getPartition(Text key, Text value, int numPartitions) {
            return (numPartitions == 0) ? 0 : Math.abs(key.hashCode() % numPartitions);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, args[0]);

        job.setJarByClass(valuesJoinerStep.class);
        job.setMapperClass(MapperClass.class);
        job.setPartitionerClass(PartitionerClass.class);
        job.setReducerClass(ReducerClass.class);
        
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
        //job.setInputFormatClass(TextInputFormat.class);
		//job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
