package hmm;

import java.util.TreeMap;

public class CharAlphabet implements Alphabet
{
	private static final char TERMINATOR = '\uE000';

	public static boolean isTerminator(final char p_char)
	{
		return p_char == TERMINATOR;
	}

	public static void main(final String[] p_args)
	{
		final CharAlphabet a = new CharAlphabet();
		final String test = "abcdefgabcdefg";
		final Integer[] idSeq = a.getIDs(test);
		for (final int i : idSeq)
		{
			System.out.println(i);
		}
	}

	private final TreeMap<Object, Integer> m_outputs;

	private final TreeMap<Integer, Object> m_inverse;

	private int m_index = 0;

	public CharAlphabet()
	{
		m_outputs = new TreeMap<>();
		m_inverse = new TreeMap<>();
		ensureID(TERMINATOR);
	}

	public void ensureIDs(final String p_string)
	{
		getIDs(p_string);
	}

	@Override
	public Integer[] getAllIDs()
	{
		return m_outputs.values().toArray(new Integer[0]);
	}

	public int getID(final Object p_char)
	{
		ensureID(p_char);
		return m_outputs.get(p_char);
	}

	public Integer[] getIDs(final String p_input)
	{
		final Integer[] ret = new Integer[p_input.length() + 1];
		for (int i = 0; i < p_input.length(); i++)
		{
			final char c = p_input.charAt(i);
			ret[i] = getID(c);
		}

		ret[p_input.length()] = getID(TERMINATOR);
		return ret;
	}

	@Override
	public ProcessedString getProcessedString(final String p_string)
	{
		final Integer[] ids = getIDs(p_string);
		final ProcessedString ret = new ProcessedString(p_string, ids);
		return ret;
	}

	@Override
	public ProcessedString[] getProcessedStrings(final String[] p_strings)
	{
		final ProcessedString[] ret = new ProcessedString[p_strings.length];

		for (int i = 0; i < ret.length; i++)
		{
			ret[i] = getProcessedString(p_strings[i]);
		}

		return ret;
	}

	@Override
	public Object lookupID(final Integer p_ID)
	{
		return m_inverse.get(p_ID);
	}

	@Override
	public int size()
	{
		return m_outputs.size();
	}

	private void ensureID(final Object p_char)
	{
		if (!m_outputs.containsKey(p_char))
		{
			m_outputs.put(p_char, m_index);
			m_inverse.put(m_index, p_char);

			m_index++;
		}
	}
}
