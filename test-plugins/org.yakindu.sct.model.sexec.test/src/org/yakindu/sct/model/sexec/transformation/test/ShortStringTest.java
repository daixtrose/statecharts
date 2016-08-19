package org.yakindu.sct.model.sexec.transformation.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.yakindu.sct.model.sexec.naming.ShortString;

import com.google.common.primitives.Shorts;

public class ShortStringTest {
	private ShortString shortstr;
	
	@Test
	public void originalStringTest()
	{
		shortstr = new ShortString("ShortStringTest");
		
		assertEquals("ShortStringTest", shortstr.getShortenedString());
	}
	
	@Test
	public void vocalsTest()
	{
		shortstr = new ShortString("YakinduStatechartTools");
		
		shortstr.removeVocals();
		
		assertEquals("YkndSttchrtTls", shortstr.getShortenedString());
		
		shortstr = new ShortString("ATest");
		
		shortstr.removeVocals();
		
		assertEquals("ATst", shortstr.getShortenedString());
	}
	
	@Test
	public void resetTest()
	{
		shortstr = new ShortString("Yakindu");
		
		shortstr.removeVocals();
		
		assertEquals("Yknd", shortstr.getShortenedString());
		
		shortstr.reset();
		
		assertEquals("Yakindu", shortstr.getShortenedString());
	}
	
	@Test
	public void rollBackTest()
	{
		shortstr = new ShortString("Yakindu_StatechartTools");
		
		shortstr.removeUnderscores();
		
		shortstr.removeVocals();
		
		shortstr.rollback();
		
		assertEquals("YakinduStatechartTools", shortstr.getShortenedString());
	}
	
	@Test
	public void basicCutCostTest()
	{
		shortstr = new ShortString("AAab_7");
		
		assertEquals(ShortString.cost_firstLetter + ShortString.cost_Uppercase, shortstr.getBaseCutCost(0));
		
		assertEquals(ShortString.cost_Uppercase, shortstr.getBaseCutCost(1));
		
		assertEquals(ShortString.cost_lowercase_vocals, shortstr.getBaseCutCost(2));
		
		assertEquals(ShortString.cost_lowercase_consonants, shortstr.getBaseCutCost(3));
		
		assertEquals(ShortString.cost_underscore, shortstr.getBaseCutCost(4));
		
		assertEquals(ShortString.cost_digit, shortstr.getBaseCutCost(5));
	}
	
	@Test
	public void cutCostFactorTest()
	{
		shortstr = new ShortString("012345");
		
		shortstr.removeIndex(0);
		assertEquals(11, shortstr.getCutCostFactor());
		
		shortstr.removeIndex(1);
		assertEquals(13, shortstr.getCutCostFactor());
		
		shortstr.removeIndex(2);
		assertEquals(15, shortstr.getCutCostFactor());
		
		shortstr.removeIndex(3);
		assertEquals(16, shortstr.getCutCostFactor());
		
		shortstr.removeIndex(4);
		assertEquals(18, shortstr.getCutCostFactor());
		
		shortstr.removeIndex(5);
		assertEquals(20, shortstr.getCutCostFactor());
	}
	
	@Test
	public void cutRatioTest1()
	{
		shortstr = new ShortString("ababababab");
		shortstr.removeVocals();
		
		assertEquals(0.5, shortstr.getCutRatio(), 0.01);
	}
	
	@Test
	public void underscoreTest()
	{
		shortstr = new ShortString("main_region_State_B");
		
		shortstr.removeUnderscores();
		
		assertEquals("mainregionStateB", shortstr.getShortenedString());
	}
	
	@Test
	public void costVocalsTest()
	{
		shortstr = new ShortString("YakinduStatechartTools");
		
		shortstr.removeVocals();
		
		int expectedCost = 8 * ShortString.cost_lowercase_vocals * shortstr.getCutCostFactor();
		
		assertEquals(expectedCost, shortstr.getCutCost());
	}
	
	@Test
	public void costFirstLetterTest()
	{
		shortstr = new ShortString("all");
		
		shortstr.removeVocals();
		
		int expectedCost = (
				1 * ShortString.cost_firstLetter +
				1 * ShortString.cost_lowercase_vocals
				) * shortstr.getCutCostFactor();
		
		assertEquals(expectedCost, shortstr.getCutCost());
	}
	
	@Test
	public void costFirstUppercaseTest()
	{
		shortstr = new ShortString("ATest");
		
		shortstr.removeIndex(0);
		
		int expectedCost = (
				1 * ShortString.cost_firstLetter + 
				1 * ShortString.cost_Uppercase
				) * shortstr.getCutCostFactor();
		
		assertEquals(expectedCost, shortstr.getCutCost());
	}
}