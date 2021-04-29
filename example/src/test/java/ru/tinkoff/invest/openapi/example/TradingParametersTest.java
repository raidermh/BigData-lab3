package ru.tinkoff.invest.openapi.example;

import junit.framework.TestCase;
import org.junit.Test;



public class TradingParametersTest extends TestCase {

    final String ssoTokenArg = "qwerty";
    final String tickersArg = "TCS";
    final String candleIntervalsArg = "1min";
    final String sandboxModeArg = "true";

    @Test
    public void testFromProgramArgs() {

       TradingParameters tp2 = TradingParameters.fromProgramArgs(ssoTokenArg, tickersArg, candleIntervalsArg, sandboxModeArg);

        String expected1 = "qwerty";
        String expected2 = "TCS";
        String expected3 = "1min";
        boolean expected4 = true;

        String actual1 = tp2.ssoToken;
        String[] arrActual2 = tp2.tickers;
        String actual2 = arrActual2[0];
        Object[] objActual3 = tp2.candleIntervals;
        Object actual3 = objActual3[0].toString();
        boolean actual4 = tp2.sandboxMode;

        assertEquals(actual1, expected1);
        assertEquals(actual2, expected2);
        assertEquals(actual3, expected3);
        assertEquals(actual4, expected4);
    }
}