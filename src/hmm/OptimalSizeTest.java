package hmm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class OptimalSizeTest
{
	private static Random RANDOM = new Random();

	public static void main(final String[] p_strings)
	{
		final Alphabet a = AlphabetFactory.newAlphabet(AlphabetType.CHAR);
		final String[] strings = DataLoader.loadStrings(0);
		final ProcessedString[] pStrings = a.getProcessedStrings(strings);
		findOptimalSize(5, 100, 20, 10, 1, a, pStrings);
	}

	private static void findOptimalSize(
		final int p_trainSetSize,
		final int p_testSetSize,
		final int p_numYokedHMMs,
		final int p_runsPerFit,
		final int p_numNodesStart,
		final Alphabet a,
		final ProcessedString[] pStrings)
	{
		for (int numNodes = p_numNodesStart;; numNodes++)
		{
			System.out.println("Num yoked: " + p_numYokedHMMs);
			System.out.println("Train set size: " + p_trainSetSize);
			System.out.println("Test set size: " + p_testSetSize);

			System.out.println("Nodes: " + numNodes);
			final double[] polls = new double[p_runsPerFit];
			System.out.print("Runs: ");
			for (int runIndex = 0; runIndex < p_runsPerFit; runIndex++)
			{
				System.out.print(runIndex + " (");
				final ProcessedString[][] trainTestSets =
					getTrainTestSets(p_numYokedHMMs, pStrings, p_trainSetSize, p_testSetSize);

				final ProcessedString[] testSet = trainTestSets[p_numYokedHMMs];
				final YokedQHMMSet yokedSet = new YokedQHMMSet();
				for (int hmmIndex = 0; hmmIndex < p_numYokedHMMs; hmmIndex++)
				{
					if (hmmIndex != 0)
					{
						System.out.print(" ");
					}
					System.out.print(hmmIndex);
					final ProcessedString[] trainSet = trainTestSets[hmmIndex];
					final HMM toTest = new HMM(numNodes, a);
					toTest.train(trainSet, false);
					final QHMM qhmm = new QHMM(toTest);

					yokedSet.addQHMM(qhmm);
				}

				System.out.print(") ");
				final double value = yokedSet.getPOLL(testSet);
				polls[runIndex] = value;
			}

			System.out.println();
			System.out.println("POLL mean: " + Utilities.average(polls));
			System.out.println("POLL sd: " + Utilities.standardDeviation(polls));
		}
	}

	private static ProcessedString[][] getTrainTestSets(
		final int p_numTrainSets,
		final ProcessedString[] p_strings,
		final int p_trainSetSize,
		final int p_testSetSize)
	{
		final HashSet<ProcessedString> trainSet = new HashSet<>();
		final ProcessedString[][] ret = new ProcessedString[p_numTrainSets + 1][];

		while (true)
		{
			final int index = RANDOM.nextInt(p_strings.length);
			trainSet.add(p_strings[index]);

			if (trainSet.size() == p_trainSetSize * p_numTrainSets)
			{
				final ProcessedString[] strings = trainSet.toArray(new ProcessedString[0]);
				Utilities.shuffle(strings);
				for (int i = 0; i < p_numTrainSets; i++)
				{
					ret[i] = Arrays.copyOfRange(strings, i * p_trainSetSize, (i + 1) * p_trainSetSize);
				}
				break;
			}
		}

		final HashSet<ProcessedString> testSet = new HashSet<>();

		while (true)
		{
			final int index = RANDOM.nextInt(p_strings.length);
			if (!trainSet.contains(p_strings[index]))
			{
				testSet.add(p_strings[index]);

				if (testSet.size() == p_testSetSize)
				{
					ret[p_numTrainSets] = testSet.toArray(new ProcessedString[0]);
					break;
				}
			}
		}

		return ret;
	}

	private static ProcessedString[][] getTrainTestSplit(
		final ProcessedString[] p_strings,
		final double p_trainSetFraction)
	{
		final ProcessedString[][] ret = new ProcessedString[2][];
		if (p_trainSetFraction < 0 || p_trainSetFraction > 1)
		{
			throw new RuntimeException();
		}

		final int trainSize = (int) (p_strings.length * p_trainSetFraction);
		final int testSize = p_strings.length - trainSize;
		ret[0] = new ProcessedString[trainSize];
		ret[1] = new ProcessedString[testSize];

		Utilities.shuffle(p_strings);

		for (int i = 0; i < trainSize + testSize; i++)
		{
			if (i < trainSize)
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

	@SuppressWarnings("unused")
	private static void testFullSet(final AlphabetType p_type)
	{
		final Alphabet a = AlphabetFactory.newAlphabet(p_type);
		final String[] strings = DataLoader.loadStrings(0);
		final ProcessedString[] pStrings = a.getProcessedStrings(strings);
		final ProcessedString[][] trainTestSplit = getTrainTestSplit(pStrings, .7);

		for (int numNodes = 3;; numNodes += 3)
		{
			System.out.println("Nodes: " + numNodes);

			final ProcessedString[] testSet = trainTestSplit[1];
			final ProcessedString[] trainSet = trainTestSplit[0];
			final YokedQHMMSet yokedSet = new YokedQHMMSet();

			final HMM toTest = new HMM(numNodes, a);
			toTest.train(trainSet, true);
			final QHMM qhmm = new QHMM(toTest);

			yokedSet.addQHMM(qhmm);

			final double value = yokedSet.getPOLL(testSet);

			System.out.println();
			System.out.println("POLL mean: " + value);
		}
	}

	@SuppressWarnings("unused")
	private static void testMaxVsYoked(final AlphabetType p_type)
	{
		final int numNodes = 5;
		final int numHMMs = 20;
		final int numRuns = 100;

		final Alphabet a = AlphabetFactory.newAlphabet(p_type);
		final String[] strings = DataLoader.loadStrings(0);
		final ProcessedString[] pStrings = a.getProcessedStrings(strings);
		Utilities.shuffle(pStrings);

		final ProcessedString[] trainSet = Arrays.copyOfRange(pStrings, 0, 100);
		final ProcessedString[] testSet = Arrays.copyOfRange(pStrings, 100, 200);

		final HMM[] hmms = new HMM[numHMMs];
		for (int i = 0; i < numHMMs; i++)
		{
			System.out.println("training HMM " + (i + 1));
			hmms[i] = new HMM(numNodes, a);
			hmms[i].train(trainSet, true);
		}

		final double[][] yokedPolls = new double[numHMMs][numRuns];
		final double[][] maxPolls = new double[numHMMs][numRuns];
		for (int runIndex = 0; runIndex < numRuns; runIndex++)
		{
			Utilities.shuffle(hmms);
			final YokedQHMMSet yokedSet = new YokedQHMMSet();
			for (int hmmIndex = 0; hmmIndex < numHMMs; hmmIndex++)
			{
				System.out.println("HMMs: " + (hmmIndex + 1));
				yokedSet.addQHMM(new QHMM(hmms[hmmIndex]));
				final double yokedPoll = yokedSet.getPOLL(testSet);

				final YokedQHMMSet singleton = new YokedQHMMSet();
				singleton.addQHMM(new QHMM(hmms[hmmIndex]));
				final double singletonPoll = singleton.getPOLL(testSet);
				final double previousBest = hmmIndex == 0 ? Double.NEGATIVE_INFINITY : maxPolls[hmmIndex - 1][runIndex];
				final double best = Math.max(singletonPoll, previousBest);

				System.out.println("Best POLL: " + best);
				System.out.println("Yoked POLL: " + yokedPoll);

				yokedPolls[hmmIndex][runIndex] = yokedPoll;
				maxPolls[hmmIndex][runIndex] = best;
			}
		}

		System.out.println();
		for (int hmmIndex = 0; hmmIndex < numHMMs; hmmIndex++)
		{
			System.out.println("Num HMMs: " + (hmmIndex + 1));
			final double[] yokedValues = yokedPolls[hmmIndex];
			final double[] maxValues = maxPolls[hmmIndex];

			final double yokedMean = Utilities.average(yokedValues);
			final double yokedSD = Utilities.standardDeviation(yokedValues);
			final double maxMean = Utilities.average(maxValues);
			final double maxSD = Utilities.standardDeviation(maxValues);

			System.out.println("Yoked POLL: " + yokedMean + " +/- " + yokedSD);
			System.out.println("Max POLL: " + maxMean + " +/- " + maxSD);
		}
	}
}
