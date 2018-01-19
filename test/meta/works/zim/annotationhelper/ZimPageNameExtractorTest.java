package meta.works.zim.annotationhelper;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by robert on 2016-10-05 15:26.
 */
public
class ZimPageNameExtractorTest
{
	private
	ZimPageNameExtractor zimPageNameExtractor;

	@BeforeMethod
	public
	void setUp() throws Exception
	{
		zimPageNameExtractor=new ZimPageNameExtractor(true);
	}

	@DataProvider(name = "outputInputPairs")
	public
	Object[][] createData1()
	{
		return new Object[][]
			{
				{":LAS:407", "file:///mnt/shared/Podcasts/All Jupiter Broadcasting Videos/linuxactionshowep407-432p.mp4"},
				{":BSD:136", "bsd-0136-432p.mp4"},
				{":CR:193", "cr-0193-432p.mp4"},
				{":LUP:158", "lup-0158-432p.mp4"},
				{":LAS:406", "linuxactionshowep406-432p.mp4"},
				{":LAS:426", "linuxactionshowep426b-432p.mp4"}, // (note the 'b')... !
				{":TTT:236", "T3-0236-432p.mp4"},
				{":TechSNAP:257", "techsnap-0257-432p.mp4"},
				{":Unfilter:198", "unfilter-0198-432p.mp4"},
				{":WTR:53", "WTR-0053-432p.mp4"},
				{":UE:3", "ue-003.mp4"},
				{":UE:DC2", "ue-Diamond-Collection-02.mp4"},
				{":SN:562", "sn0562_h264m_864x480_500.mp4"},
				{":TWIT:578", "twit0578_h264m_864x480_500.mp4"},
				{":Agenda31:83", "Agenda31.A31-083.May28.2016.mp3"},
				{":DWW:2016:0829", "dww20160829.mp3"},
				{":Dudmanovi:16", "Dudmanovi.cz-016-20151402.mp3"},
				{":Podcast:GnuWorldOrder:X:9", "gnuWorldOrder_Xx09.webm"},
				{":Podcast:GnuWorldOrder:12:0", "gnuWorldOrder_12x0.opus"},
				{":GoingLinux:305", "glp305.mp3"},
				{":HPR:2016", "hpr2016.ogg"},
				{":Transmit:36", "Transmit36.mp3"},
				{":KernelPanic:128", "KernelPanic_128.ogg"},
				{":LinuxVoice:s04:e13", "lv_s04e13.mp3"},
				{":LinuxLuddites:86", "LinuxLuddites086.mp3"},
				{":NA:854", "NA-854-2016-08-25-Final.mp3"},
				{":NA:1234", "NA-1234-20200825.mp3"},
				{":SMLR:208", "SMLR-E208.mp3"},
				{":LinuxLink:672", "tllts_672-08-31-16.ogg"},
				{":UbuntuPodcast:s09:e31", "ubuntupodcast_s09e31.mp3"},
                {":Triangulation:271", "tri0271_h264m_864x480_500.mp4"},
                {":TechGuy:1340", "ttg1340_h264m_864x480_500.mp4"},
				{":CommonSenseWithDanCarlin:299", "cswdcc99.mp3"},
				{":CommonSenseWithDanCarlin:312", "cswdcd12.mp3"},
				{":MartinHash:189", "file:///mnt/shared/Podcasts/PRay TeLL, Dr. Hash/2016-04-15_189_obesity.mp3"},
				{":MartinHash:189", "file:///mnt/shared/Podcasts/PRay%20TeLL,%20Dr.%20Hash/2016-04-15_189_obesity.mp3"},
				//{":2016:4", "2016-04-15_189_obesity.mp3"}
				{":LNL:0", "LNL00.ogg"},
				{":LNL:2", "LNL02.ogg"},
				{":FreeTalk:Live:2017:02:01", "FTL2017-02-01.mp3"},
				{":FreeTalk:Digest:2017:02:12", "FTL Digest 2017-02-12.mp3"},
				{":FreeTalk:Digest:2017:02:16", "FTLDigest2017-02-16.mp3"},
				{":FreeTalk:Digest:2017:06:18", "FTLDigest2017-06-18-2017.mp3"}, //was probably a mis-type on there side
				{":FreeTalk:Digest:2017:06:28", "FTLDigest2017-6-28.mp3"}, //Missing zeros, now?!
				{":FreeTalk:Digest:2017:06:02", "FTLDigest2017-6-2.mp3"}, //Might as well test for unzeroed day!
				{":FreeTalk:Digest:2017:08:16", "FTLDigest2017-08016.mp3"}, //now we have too many zeros? This must be manually done!
				{":FreeTalk:Digest:2017:10:23", "FTLDigest20171023.mp3"}, //uggh... no separators at all?!?!
				{":FreeTalk:Digest:2017:12:13", "FTL Digest 20171213.mp3"},
				{":BillBurr:MMPC:2017:02:03", "MMPC_2-3-17.mp3"},
				{":BillBurr:MMPC:2017:12:13", "MMPC_12-13-17.mp3"},
				{":BillBurr:TAMMP:2017:02:09", "TAMMP_2-9-17.mp3"},
				{":BillBurr:TAMMP:2017:12:19", "TAMMP_12-19-17.mp3"},
				{":OffTheAirLive:354", "Off_The_Air_Live_354_3-25-17.mp3"},
				{":LinuxAction:Special:1", "LinuxActionSpecial1-NewLokiAppCenter.mp4"},
				{":LinuxAction:News:1", "lan-001.mp4"},
				{":AskNoah:5", "asknoah-0005.mp4"},
				{":Podcast:LibertarianChristian:32", "/mnt/shared/Podcasts/The Libertarian Christian Podcast/Ep_32_-_Called_to_Freedom.mp3'"},
				{":Podcast:LibertarianChristian:32", "/mnt/shared/Podcasts/The%20Libertarian%20Christian%20Podcast/Ep_32_-_Called_to_Freedom.mp3'"},
				{":Podcast:LibertyWeekly:LiberationLibrary:3", "/mnt/yard/Podcasts/The Liberty Weekly Podcast/LL_3_The_Truth_About_Judicial_Review.mp3"},
				{":Podcast:LibertyWeekly:LiberationLibrary:4", "/mnt/yard/Podcasts/The Liberty Weekly Podcast/Liberation_Library_4_Blood_Makes_the_Green_Grass_Grow_Conditioning_Soldiers_to_Kill.mp3"},
				{":Podcast:LibertyWeekly:Episode:47", "/mnt/yard/Podcasts/The Liberty Weekly Podcast/Keith_Knight_Dont_Tread_on_Anyone_Ep._47.mp3"},
			};
	}

	@Test(dataProvider = "outputInputPairs")
	public
	void testGetZimPageNameFor(String expectedOutput, String input) throws Exception
	{
		assertEquals(zimPageNameExtractor.getZimPageNameFor(input), expectedOutput);

		/*dnw?
		if (input.contains(" "))
		{
			assertEquals(zimPageNameExtractor.getZimPageNameFor(input.replaceAll(" ", "%20")), expectedOutput);
		}
		*/
	}

	/* DNW
	@DataProvider(name = "bestEffortPairs")
	public
	Object[][] bestEffortData1()
	{
		return new Object[][]
			{
				{":A-b-c:2", "a-b-c-2.ogg"},
				{":A-b-c:2", "a-b-c-2-extra.ogg"},
				{":Alpha-beta-delta:2", "alpha-beta-delta-2.ogg"},
			};
	}

	@Test(dataProvider = "bestEffortPairs")
	public
	void testBestEffortPageExtraction(String expectedOutput, String input) throws Exception
	{
		final
		String actualOutput=zimPageNameExtractor.getZimPageNameFor(input);
		assertTrue(zimPageNameExtractor.lastStrategyWasBestEffort());
		assertEquals(actualOutput, expectedOutput);
	}
	*/
}