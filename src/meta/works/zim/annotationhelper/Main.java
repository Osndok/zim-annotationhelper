package meta.works.zim.annotationhelper;

public
class Main
{

	public static
	void main(String[] args)
	{
		if (args.length==0)
		{
			new VlcMediaPlayer().start();
			new RhythmBoxMediaPlayer().start();
		}
		else
		if (args.length==1)
		{
			final
			String option=args[0];

			if (option.equals("vlc"))
			{
				VlcMediaPlayer.main(args);
			}
			else
			if (option.equals("rb"))
			{
				RhythmBoxMediaPlayer.main(args);
			}
			else
			{
				throw new UnsupportedOperationException("unknown option: "+option);
			}
		}
		else
		{
			throw new UnsupportedOperationException("expecting one arg (for now)");
		}
	}
}
