package ru.tinkoff.invest.openapi.example;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.invest.openapi.model.streaming.CandleInterval;

import java.util.Arrays;

/**
 * В данном классе происходит преобразование торговых параметров
 * и проверка агрегационного параметра на валидныи интервал
 *
 * @author Mikhail Khrychev
 * @version  1.0.0
 * @since 30.04.2021
 */

public class TradingParameters {
    @NotNull
    public final String ssoToken;
    @NotNull
    public final String[] tickers;
    @NotNull
    public final CandleInterval[] candleIntervals;
    public final boolean sandboxMode;

    public TradingParameters(@NotNull final String ssoToken,
                             @NotNull final String[] tickers,
                             @NotNull final CandleInterval[] candleIntervals,
                             final boolean sandboxMode) {
        this.ssoToken = ssoToken;
        this.tickers = tickers;
        this.candleIntervals = candleIntervals;
        this.sandboxMode = sandboxMode;
    }

    @NotNull
    public static TradingParameters fromProgramArgs(@NotNull final String ssoTokenArg,
                                                    @NotNull final String tickersArg,
                                                    @NotNull final String candleIntervalsArg,
                                                    @NotNull final String sandboxModeArg) {
        final var tickers = tickersArg.split(",");
        final var candleIntervals = Arrays.stream(candleIntervalsArg.split(","))
                .map(TradingParameters::parseCandleInterval)
                .toArray(CandleInterval[]::new);
        if (candleIntervals.length != tickers.length)
            throw new IllegalArgumentException("Количество переданных разрешающих интервалов свечей не совпадает с переданным количеством тикеров.");

        final var useSandbox = Boolean.parseBoolean(sandboxModeArg);

        return new TradingParameters(ssoTokenArg, tickers, candleIntervals, useSandbox);
    }

    private static CandleInterval parseCandleInterval(final String str) {
        switch (str) {
            case "1min":
                return CandleInterval._1MIN;
            case "2min":
                return CandleInterval._2MIN;
            case "3min":
                return CandleInterval._3MIN;
            case "5min":
                return CandleInterval._5MIN;
            case "10min":
                return CandleInterval._10MIN;
            default:
                throw new IllegalArgumentException("Не распознан разрешающий интервал!");
        }
    }
}
