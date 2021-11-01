package hmm;

public class AlphabetFactory
{
	public static Alphabet newAlphabet(final AlphabetType p_type)
	{
		switch (p_type)
		{
			case CHAR:
				return new CharAlphabet();
			case WORD:
				return new Lexicon();
			default:
				throw new RuntimeException();
		}
	}
}
