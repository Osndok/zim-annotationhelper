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
		zimPageNameExtractor=new ZimPageNameExtractor();
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
				//{"???", "dww20160829.mp3"},
				{":Dudmanovi:16", "Dudmanovi.cz-016-20151402.mp3"},
				{":GNUWorldOrder:9", "gnuWorldOrder_Xx09.webm"},
				{":GoingLinux:305", "glp305.mp3"},
				{":HPR:2016", "hpr2016.ogg"},
				{":Transmit:36", "Transmit36.mp3"},
				{":KernelPanic:128", "KernelPanic_128.ogg"},
				{":LinuxVoice:s04e13", "lv_s04e13.mp3"},
				{":LinuxLuddites:86", "LinuxLuddites086.mp3"},
				{":NA:854", "NA-854-2016-08-25-Final.mp3"},
				{":NA:1234", "NA-1234-20200825.mp3"},
				{":SMLR:208", "SMLR-E208.mp3"},
				{":LinuxLink:672", "tllts_672-08-31-16.ogg"},
				{":UbuntuPodcast:s09e31", "ubuntupodcast_s09e31.mp3"},
                {":Triangulation:271", "tri0271_h264m_864x480_500.mp4"},
                {":TechGuy:1340", "ttg1340_h264m_864x480_500.mp4"},
			};
	}

	@Test(dataProvider = "outputInputPairs")
	public
	void testGetZimPageNameFor(String expectedOutput, String input) throws Exception
	{
		assertEquals(zimPageNameExtractor.getZimPageNameFor(input), expectedOutput);
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