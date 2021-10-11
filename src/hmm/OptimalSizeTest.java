package hmm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class OptimalSizeTest 
{
	private static Random RANDOM = new Random();
	
	public static void main(String[] p_strings)
	{
		Alphabet a = AlphabetFactory.newAlphabet(AlphabetType.CHAR);
		String[] strings = DataLoader.loadStrings(0);
		ProcessedString[] pStrings = a.getProcessedStrings(strings);
		findOptimalSize(5, 100, 20, 10, 1, a, pStrings);
	}

	@SuppressWarnings("unused")
	private static void testMaxVsYoked(AlphabetType p_type) 
	{
		final int numNodes = 5;
		final int numHMMs = 20;
		final int numRuns = 100;
		
		Alphabet a = AlphabetFactory.newAlphabet(p_type);
		String[] strings = DataLoader.loadStrings(0);
		ProcessedString[] pStrings = a.getProcessedStrings(strings);
		Utilities.shuffle(pStrings);
		
		ProcessedString[] trainSet = Arrays.copyOfRange(pStrings, 0, 100);
		ProcessedString[] testSet = Arrays.copyOfRange(pStrings, 100, 200);
		
		HMM[] hmms = new HMM[numHMMs];
		for(int i = 0; i < numHMMs; i++)
		{
			System.out.println("training HMM " + (i + 1));
			hmms[i] = new HMM(numNodes, a);
			hmms[i].train(trainSet, true);
		}
		
		double[][] yokedPolls = new double[numHMMs][numRuns];
		double[][] maxPolls = new double[numHMMs][numRuns];
		for(int runIndex = 0; runIndex < numRuns; runIndex++)
		{
			Utilities.shuffle(hmms);
			YokedQHMMSet yokedSet = new YokedQHMMSet();
			for(int hmmIndex = 0; hmmIndex < numHMMs; hmmIndex++)
			{
				System.out.println("HMMs: " + (hmmIndex + 1));
				yokedSet.addQHMM(new QHMM(hmms[hmmIndex]));
				double yokedPoll = yokedSet.getPOLL(testSet);
				
				YokedQHMMSet singleton = new YokedQHMMSet();
				singleton.addQHMM(new QHMM(hmms[hmmIndex]));
				double singletonPoll = singleton.getPOLL(testSet);
				double previousBest = hmmIndex == 0 ? Double.NEGATIVE_INFINITY : maxPolls[hmmIndex - 1][runIndex];
				double best = Math.max(singletonPoll, previousBest);
				
				System.out.println("Best POLL: " + best);
				System.out.println("Yoked POLL: " + yokedPoll);
				
				yokedPolls[hmmIndex][runIndex] = yokedPoll;
				maxPolls[hmmIndex][runIndex] = best;
			}
		}
		
		System.out.println();
		for(int hmmIndex = 0; hmmIndex < numHMMs; hmmIndex++)
		{
			System.out.println("Num HMMs: " + (hmmIndex + 1));
			double[] yokedValues = yokedPolls[hmmIndex];
			double[] maxValues = maxPolls[hmmIndex];
			
			double yokedMean = Utilities.average(yokedValues);
			double yokedSD = Utilities.standardDeviation(yokedValues);
			double maxMean = Utilities.average(maxValues);
			double maxSD = Utilities.standardDeviation(maxValues);
			
			System.out.println("Yoked POLL: " + yokedMean + " +/- " + yokedSD);
			System.out.println("Max POLL: " + maxMean + " +/- " + maxSD);
		}
	}
	
	@SuppressWarnings("unused")
	private static void testFullSet(AlphabetType p_type)
	{
		Alphabet a = AlphabetFactory.newAlphabet(p_type);
		String[] strings = DataLoader.loadStrings(0);
		ProcessedString[] pStrings = a.getProcessedStrings(strings);
		ProcessedString[][] trainTestSplit = getTrainTestSplit(pStrings, .7);
		
		for(int numNodes = 3; ;numNodes += 3)
		{			
			System.out.println("Nodes: " + numNodes);
			
			ProcessedString[] testSet = trainTestSplit[1];
			ProcessedString[] trainSet = trainTestSplit[0];
			YokedQHMMSet yokedSet = new YokedQHMMSet();

			HMM toTest = new HMM(numNodes, a);
			toTest.train(trainSet, true);
			QHMM qhmm = new QHMM(toTest);
				
			yokedSet.addQHMM(qhmm);
			
			double value = yokedSet.getPOLL(testSet);

			System.out.println();
			System.out.println("POLL mean: " + value);
		}
	}

	private static void findOptimalSize(int p_trainSetSize, int p_testSetSize,
			int p_numYokedHMMs, int p_runsPerFit, int p_numNodesStart,
			Alphabet a, ProcessedString[] pStrings) 
	{
		for(int numNodes = p_numNodesStart; ;numNodes++)
		{
			System.out.println("Num yoked: " + p_numYokedHMMs);
			System.out.println("Train set size: " + p_trainSetSize);
			System.out.println("Test set size: " + p_testSetSize);
			
			System.out.println("Nodes: " + numNodes);
			double[] polls = new double[p_runsPerFit];
			System.out.print("Runs: ");
			for(int runIndex = 0; runIndex < p_runsPerFit; runIndex++)
			{
				System.out.print(runIndex + " (");
				ProcessedString[][] trainTestSets = getTrainTestSets(p_numYokedHMMs, pStrings, p_trainSetSize, p_testSetSize);
				
				ProcessedString[] testSet = trainTestSets[p_numYokedHMMs];
				YokedQHMMSet yokedSet = new YokedQHMMSet();
				for(int hmmIndex = 0; hmmIndex < p_numYokedHMMs; hmmIndex++)
				{
					if(hmmIndex != 0)
					{
						System.out.print(" ");
					}
					System.out.print(hmmIndex);
					ProcessedString[] trainSet = trainTestSets[hmmIndex];
					HMM toTest = new HMM(numNodes, a);
					toTest.train(trainSet, false);
					QHMM qhmm = new QHMM(toTest);
					
					yokedSet.addQHMM(qhmm);
				}
				
				System.out.print(") ");
				double value = yokedSet.getPOLL(testSet);
				polls[runIndex] = value;
			}

			System.out.println();
			System.out.println("POLL mean: " + Utilities.average(polls));
			System.out.println("POLL sd: " + Utilities.standardDeviation(polls));
		}
	}
	
	private static ProcessedString[][] getTrainTestSplit(ProcessedString[] p_strings, double p_trainSetFraction)
	{
		ProcessedString[][] ret = new ProcessedString[2][];
		if(p_trainSetFraction < 0 || p_trainSetFraction > 1)
		{
			throw new RuntimeException();
		}
		
		int trainSize =(int)( p_strings.length * p_trainSetFraction);
		int testSize = p_strings.length - trainSize;
		ret[0] = new ProcessedString[trainSize];
		ret[1] = new ProcessedString[testSize];
		
		Utilities.shuffle(p_strings);
		
		for(int i = 0; i < trainSize + testSize; i++)
		{
			if(i < trainSize)
			{
				ret[0][i] = p_strings[i]; 
			}
			else
			{
				ret[1][i - trainSize] = p_strings[i];
			}
		}
		
		return ret;
	}
	
	private static ProcessedString[][] getTrainTestSets(int p_numTrainSets, ProcessedString[] p_strings, int p_trainSetSize, int p_testSetSize)
	{
		HashSet<ProcessedString> trainSet = new HashSet<ProcessedString>();
		ProcessedString[][] ret = new ProcessedString[p_numTrainSets + 1][];
		
		while(true)
		{
			int index = RANDOM.nextInt(p_strings.length);
			trainSet.add(p_strings[index]);
			
			if(trainSet.size() == p_trainSetSize * p_numTrainSets)
			{
				ProcessedString[] strings = trainSet.toArray(new ProcessedString[0]);
				Utilities.shuffle(strings);
				for(int i = 0; i < p_numTrainSets; i++)
				{
					ret[i] = Arrays.copyOfRange(strings, i*p_trainSetSize, (i+1)*p_trainSetSize);
				}
				break;
			}
		}
		
		HashSet<ProcessedString> testSet = new HashSet<ProcessedString>();
		
		while(true)
		{
			int index = RANDOM.nextInt(p_strings.length);
			if(!trainSet.contains(p_strings[index]))
			{
				testSet.add(p_strings[index]);
				
				if(testSet.size() == p_testSetSize)
				{
					ret[p_numTrainSets] = testSet.toArray(new ProcessedString[0]);
					break;
				}
			}
		}
		
		return ret;
	}
}
