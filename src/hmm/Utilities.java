package hmm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Utilities 
{
	private static final Random RANDOM = new Random();
	public static final double TOL = .0001;
	
	public static void main(String[] p_args)
	{
		double[] test = {1, 2, 3, 5};
		System.out.println(standardDeviation(test));
	}
	
	public static double randProb()
	{
		return RANDOM.nextDouble();
	}
	
	public static double[] convertToSimplex(double[] p_values)
	{
		double runningTotal = 0;
		
		for(double bd: p_values)
		{
			runningTotal += bd;
		}
		
		double[] ret = new double[p_values.length];
		for(int i = 0; i < p_values.length; i++)
		{
			ret[i] = p_values[i] / runningTotal;
		}
		
		return ret;
	}
	
	public static LogNum sum(Collection<? extends SimplexUnit> p_simplex)
	{
		LogNum ret = LogNum.ZERO;
		for(SimplexUnit su: p_simplex)
		{
			ret = LogNum.add(ret, su.getLikelihood());
		}
		return ret;
	}
	
	public static double[] randSimplex(int p_size)
	{
		double[] ret = new double[p_size];
		double runningTotal = 0;
		for(int i = 0; i < p_size; i++)
		{
			ret[i] = Utilities.randProb();
			runningTotal += ret[i];
		}
		
		for(int i = 0; i < p_size; i++)
		{
			ret[i] = ret[i] / runningTotal;
		}
		return ret;
	}
	
	public static SimplexUnit selectSimplexUnit(Collection<? extends SimplexUnit> p_units)
	{
		LogNum target = new LogNum(randProb());
		LogNum runningTotal = LogNum.ZERO;
		for(SimplexUnit su: p_units)
		{
			runningTotal = LogNum.add(runningTotal, su.getLikelihood());
			if(runningTotal.compareTo(target) >= 0)
			{
				return su;
			}
		}
		
		throw new RuntimeException("Bad random selection");
	}
	
	public static int selectSimplexUnitIndex(double[] m_probs)
	{
		double target = randProb();
		double runningTotal = 0;
		for(int i = 0; i < m_probs.length; i++)
		{
			runningTotal += m_probs[i];
			if(runningTotal >= target)
			{
				return i;
			}
		}
		
		throw new RuntimeException("Bad random selection");
	}
	
	public static boolean checkSumToOne(Collection<? extends SimplexUnit> p_units)
	{
		LogNum runningTotal = LogNum.ZERO;
		for(SimplexUnit su: p_units)
		{
			runningTotal = LogNum.add(runningTotal, su.getLikelihood());
		}
		
		boolean ret = isWithinTolerance(runningTotal.doubleValue(), 1, TOL);
		return ret;
	}
	
	public static boolean checkSumToOne(LogNum[] p_simplex)
	{
		LogNum runningTotal = LogNum.ZERO;
		for(LogNum bd: p_simplex)
		{
			runningTotal = LogNum.add(runningTotal, bd);
		}
		
		return isWithinTolerance(runningTotal.doubleValue(), 1, TOL);
	}
	
	public static void checkSumToOne(double[] p_simplex)
	{
		double runningTotal = 0;
		for(double bd: p_simplex)
		{
			runningTotal += bd;
		}
		
		if(!isWithinTolerance(runningTotal, 1, TOL))
		{
			throw new RuntimeException();
		}
	}
	
	public static boolean isWithinLogTolerance(double p_value, double p_target, double p_logTol)
	{
		boolean ret = Math.abs(Math.log(p_value) - Math.log(p_target)) <= p_logTol;
		
		return ret;
	}
	
	public static boolean isWithinTolerance(double p_value, double p_target, double p_tol)
	{
		boolean ret = Math.abs(p_value - p_target) <= p_tol;
		
		return ret;
	}

	public static LogNum[] randBDSimplex(int p_length) 
	{
		LogNum[] ret = new LogNum[p_length];
		LogNum runningTotal = LogNum.ZERO;
		for(int i = 0; i < p_length; i++)
		{
			ret[i] = new LogNum(Utilities.randProb());
			runningTotal = LogNum.add(runningTotal, ret[i]);
		}
		
		for(int i = 0; i < p_length; i++)
		{
			ret[i] = LogNum.divide(ret[i], runningTotal);
		}
		return ret;
	}

	public static int selectSimplexUnitIndex(LogNum[] m_probs) 
	{
		LogNum target = new LogNum(randProb());
		LogNum runningTotal = LogNum.ZERO;
		for(int i = 0; i < m_probs.length; i++)
		{
			runningTotal = LogNum.add(runningTotal, m_probs[i]);
			if(runningTotal.compareTo(target) >= 0)
			{
				return i;
			}
		}
		
		throw new RuntimeException("Bad random selection");
	}
	
	public static void print2DArray(Object[][] p_values)
	{
		for(int i = 0; i < p_values.length; i++)
		{
			System.out.println(Arrays.toString(p_values[i]));
		}
	}
	
	public static void print3DArray(Object[][][] p_values)
	{
		for(int i = 0; i < p_values.length; i++)
		{
			print2DArray(p_values[i]);
			System.out.println("/");
		}
	}
	
	public static double average(double[] p_values)
	{
		if(p_values == null || p_values.length == 0)
		{
			return Double.NaN;
		}
		
		double sum = 0;
		for(double d: p_values)
		{
			if(Double.isInfinite(d))
			{
				throw new RuntimeException();
			}
			sum += d;
		}
		return sum/p_values.length;
	}
	
	public static double standardDeviation(double[] p_values)
	{
		double sum = 0;
		double squareSum = 0;
		for(double d : p_values)
		{
			sum += d;
			squareSum += (d * d);
		}
		
		double SS = squareSum - (sum * sum)/p_values.length;
		return Math.sqrt(SS/(p_values.length - 1));
	}
	
	public static double sum(double[][] p_values)
	{
		double sum = 0;
		for(double[] dArray: p_values)
		{
			for(double d: dArray)
			{
				sum += d;
			}
		}
		return sum;
	}
	
	public static double average(double[][] p_values)
	{
		double sum = 0;
		int count = 0;
		for(double[] dArray: p_values)
		{
			for(double d: dArray)
			{
				sum += d;
				count++;
			}
		}
		return sum/count;
	}

	public static void checkAllNonzero(Collection<? extends SimplexUnit> p_coll)
	{
		for(SimplexUnit su: p_coll)
		{
			if(su.getLikelihood().isZero())
			{
				throw new RuntimeException();
			}
		}
	}
	
	public static void checkAllNonzero(LogNum[] m_nextOutputProbs) 
	{
		for(LogNum bd: m_nextOutputProbs)
		{
			if(bd.isZero())
			{
				//throw new RuntimeException();
			}
		}
	}
	
	public static void shuffle(Object[] p_array)
	{
		List<Object> list = new ArrayList<Object>();
		for (Object i: p_array) 
		{
			list.add(i);
		}

		Collections.shuffle(list);

		for (int i = 0; i < list.size(); i++) 
		{
			p_array[i] = list.get(i);
		} 
	}
}
