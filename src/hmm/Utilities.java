package hmm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Utilities
{
	private static final Random	RANDOM	= new Random();
	public static final double	TOL		= .0001;

	public static double average(final double[] p_values)
	{
		if (p_values == null || p_values.length == 0)
		{
			return Double.NaN;
		}

		double sum = 0;
		for (final double d : p_values)
		{
			if (Double.isInfinite(d))
			{
				throw new RuntimeException();
			}
			sum += d;
		}
		return sum / p_values.length;
	}

	public static double average(final double[][] p_values)
	{
		double sum = 0;
		int count = 0;
		for (final double[] dArray : p_values)
		{
			for (final double d : dArray)
			{
				sum += d;
				count++;
			}
		}
		return sum / count;
	}

	public static void checkAllNonzero(final Collection<? extends SimplexUnit> p_coll)
	{
		for (final SimplexUnit su : p_coll)
		{
			if (su.getLikelihood().isZero())
			{
				throw new RuntimeException();
			}
		}
	}

	public static void checkAllNonzero(final LogNum[] m_nextOutputProbs)
	{
		for (final LogNum bd : m_nextOutputProbs)
		{
			if (bd.isZero())
			{
				//throw new RuntimeException();
			}
		}
	}

	public static boolean checkSumToOne(final Collection<? extends SimplexUnit> p_units)
	{
		LogNum runningTotal = LogNum.ZERO;
		for (final SimplexUnit su : p_units)
		{
			runningTotal = LogNum.add(runningTotal, su.getLikelihood());
		}

		final boolean ret = isWithinTolerance(runningTotal.doubleValue(), 1, TOL);
		return ret;
	}

	public static void checkSumToOne(final double[] p_simplex)
	{
		double runningTotal = 0;
		for (final double bd : p_simplex)
		{
			runningTotal += bd;
		}

		if (!isWithinTolerance(runningTotal, 1, TOL))
		{
			throw new RuntimeException();
		}
	}

	public static boolean checkSumToOne(final LogNum[] p_simplex)
	{
		LogNum runningTotal = LogNum.ZERO;
		for (final LogNum bd : p_simplex)
		{
			runningTotal = LogNum.add(runningTotal, bd);
		}

		return isWithinTolerance(runningTotal.doubleValue(), 1, TOL);
	}

	public static double[] convertToSimplex(final double[] p_values)
	{
		double runningTotal = 0;

		for (final double bd : p_values)
		{
			runningTotal += bd;
		}

		final double[] ret = new double[p_values.length];
		for (int i = 0; i < p_values.length; i++)
		{
			ret[i] = p_values[i] / runningTotal;
		}

		return ret;
	}

	public static boolean isWithinLogTolerance(final double p_value, final double p_target, final double p_logTol)
	{
		final boolean ret = Math.abs(Math.log(p_value) - Math.log(p_target)) <= p_logTol;

		return ret;
	}

	public static boolean isWithinTolerance(final double p_value, final double p_target, final double p_tol)
	{
		final boolean ret = Math.abs(p_value - p_target) <= p_tol;

		return ret;
	}

	public static void main(final String[] p_args)
	{
		final double[] test = {1, 2, 3, 5};
		System.out.println(standardDeviation(test));
	}

	public static void print2DArray(final Object[][] p_values)
	{
		for (final Object[] p_value : p_values)
		{
			System.out.println(Arrays.toString(p_value));
		}
	}

	public static void print3DArray(final Object[][][] p_values)
	{
		for (final Object[][] p_value : p_values)
		{
			print2DArray(p_value);
			System.out.println("/");
		}
	}

	public static LogNum[] randBDSimplex(final int p_length)
	{
		final LogNum[] ret = new LogNum[p_length];
		LogNum runningTotal = LogNum.ZERO;
		for (int i = 0; i < p_length; i++)
		{
			ret[i] = new LogNum(Utilities.randProb());
			runningTotal = LogNum.add(runningTotal, ret[i]);
		}

		for (int i = 0; i < p_length; i++)
		{
			ret[i] = LogNum.divide(ret[i], runningTotal);
		}
		return ret;
	}

	public static double randProb()
	{
		return RANDOM.nextDouble();
	}

	public static double[] randSimplex(final int p_size)
	{
		final double[] ret = new double[p_size];
		double runningTotal = 0;
		for (int i = 0; i < p_size; i++)
		{
			ret[i] = Utilities.randProb();
			runningTotal += ret[i];
		}

		for (int i = 0; i < p_size; i++)
		{
			ret[i] = ret[i] / runningTotal;
		}
		return ret;
	}

	public static SimplexUnit selectSimplexUnit(final Collection<? extends SimplexUnit> p_units)
	{
		final LogNum target = new LogNum(randProb());
		LogNum runningTotal = LogNum.ZERO;
		for (final SimplexUnit su : p_units)
		{
			runningTotal = LogNum.add(runningTotal, su.getLikelihood());
			if (runningTotal.compareTo(target) >= 0)
			{
				return su;
			}
		}

		throw new RuntimeException("Bad random selection");
	}

	public static int selectSimplexUnitIndex(final double[] m_probs)
	{
		final double target = randProb();
		double runningTotal = 0;
		for (int i = 0; i < m_probs.length; i++)
		{
			runningTotal += m_probs[i];
			if (runningTotal >= target)
			{
				return i;
			}
		}

		throw new RuntimeException("Bad random selection");
	}

	public static int selectSimplexUnitIndex(final LogNum[] m_probs)
	{
		final LogNum target = new LogNum(randProb());
		LogNum runningTotal = LogNum.ZERO;
		for (int i = 0; i < m_probs.length; i++)
		{
			runningTotal = LogNum.add(runningTotal, m_probs[i]);
			if (runningTotal.compareTo(target) >= 0)
			{
				return i;
			}
		}

		throw new RuntimeException("Bad random selection");
	}

	public static void shuffle(final Object[] p_array)
	{
		final List<Object> list = new ArrayList<>();
		for (final Object i : p_array)
		{
			list.add(i);
		}

		Collections.shuffle(list);

		for (int i = 0; i < list.size(); i++)
		{
			p_array[i] = list.get(i);
		}
	}

	public static double standardDeviation(final double[] p_values)
	{
		double sum = 0;
		double squareSum = 0;
		for (final double d : p_values)
		{
			sum += d;
			squareSum += d * d;
		}

		final double SS = squareSum - sum * sum / p_values.length;
		return Math.sqrt(SS / (p_values.length - 1));
	}

	public static LogNum sum(final Collection<? extends SimplexUnit> p_simplex)
	{
		LogNum ret = LogNum.ZERO;
		for (final SimplexUnit su : p_simplex)
		{
			ret = LogNum.add(ret, su.getLikelihood());
		}
		return ret;
	}

	public static double sum(final double[][] p_values)
	{
		double sum = 0;
		for (final double[] dArray : p_values)
		{
			for (final double d : dArray)
			{
				sum += d;
			}
		}
		return sum;
	}
}
