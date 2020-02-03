package ru.tinkoff.invest.openapi.automata;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Объект представляющий для стратегии информацию о ситуации на рынке.
 */
public class TradingState {

    @Nullable public final Orderbook orderbook;
    @Nullable public final Candle candle;
    @Nullable public final InstrumentInfo instrumentInfo;
    @NotNull public final List<Order> orders;
    @NotNull public final Map<Currency, Position> currencies;
    @NotNull public final Map<String, Position> positions;

    private final Currency instrumentCurrency;

    public TradingState(@Nullable final Orderbook orderbook,
                        @Nullable final Candle candle,
                        @Nullable final InstrumentInfo instrumentInfo,
                        @NotNull final List<Order> orders,
                        @NotNull final Map<Currency, Position> currencies,
                        @NotNull final Map<String, Position> positions,
                        @NotNull final Currency instrumentCurrency) {
        this.orderbook = orderbook;
        this.candle = candle;
        this.instrumentInfo = instrumentInfo;
        this.orders = orders;
        this.currencies = currencies;
        this.positions = positions;
        this.instrumentCurrency = instrumentCurrency;
    }

    @NotNull
    public TradingState withNewCandle(@NotNull final Candle candle) {
        return new TradingState(
                this.orderbook,
                candle,
                this.instrumentInfo,
                this.orders,
                this.currencies,
                this.positions,
                this.instrumentCurrency
        );
    }

    @NotNull
    public TradingState withNewOrderbook(@NotNull final Orderbook orderbook) {
        return new TradingState(
                orderbook,
                this.candle,
                this.instrumentInfo,
                this.orders,
                this.currencies,
                this.positions,
                this.instrumentCurrency
        );
    }

    @NotNull
    public TradingState withNewInstrumentInfo(@NotNull final InstrumentInfo instrumentInfo) {
        final List<Order> orders;
        final Map<Currency, Position> currencies;
        final Map<String, Position> positions;
        final boolean currentCanTrade = Objects.nonNull(this.instrumentInfo) && this.instrumentInfo.canTrade;
        if (instrumentInfo.canTrade != currentCanTrade && !instrumentInfo.canTrade) {
            orders = new LinkedList<>();

            final BigDecimal buyOrdersAmount = this.orders.stream()
                .filter(o -> o.operation == Operation.Buy)
                .reduce(BigDecimal.ZERO,
                        (acc, order) -> order.price.multiply(BigDecimal.valueOf(order.executedLots * instrumentInfo.lot)),
                        BigDecimal::add);
            currencies = this.currencies.entrySet().stream()
                .peek(entry -> {
                    if (this.instrumentCurrency == entry.getKey()) {
                        final Position newValue = entry.getValue()
                            .withBlocked(entry.getValue().blocked.subtract(buyOrdersAmount))
                            .withBalance(entry.getValue().balance.add(buyOrdersAmount));
                        entry.setValue(newValue);
                    }
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            final BigDecimal sellOrdersValue = this.orders.stream()
                    .filter(o -> o.operation == Operation.Buy)
                    .reduce(BigDecimal.ZERO,
                            (acc, order) -> BigDecimal.valueOf(order.executedLots * instrumentInfo.lot),
                            BigDecimal::add);
            positions = this.positions.entrySet().stream()
                    .peek(entry -> {
                        if (this.instrumentInfo.figi.equals(entry.getKey())) {
                            final Position newValue = entry.getValue()
                                    .withBlocked(entry.getValue().blocked.subtract(sellOrdersValue))
                                    .withBalance(entry.getValue().balance.add(sellOrdersValue));
                            entry.setValue(newValue);
                        }
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            orders = this.orders;
            currencies = this.currencies;
            positions = this.positions;
        }

        return new TradingState(
                this.orderbook,
                this.candle,
                instrumentInfo,
                orders,
                currencies,
                positions,
                this.instrumentCurrency
        );
    }

    @NotNull
    public TradingState withExecutedOrder(@NotNull final String id) {
        Objects.requireNonNull(instrumentInfo);

        Order executedOrder = null;
        for (final Order o : this.orders) {
            if (o.id.equals(id)) {
                executedOrder = o;
            }
        }

        final List<Order> filtered = this.orders.stream().filter(o -> !o.id.equals(id)).collect(Collectors.toList());

        final Map<Currency, Position> currenciesCopy = new HashMap<>(this.currencies);
        final Map<String, Position> positionsCopy = new HashMap<>(this.positions);
        if (Objects.nonNull(executedOrder)) {
            final BigDecimal executedLots = BigDecimal.valueOf(executedOrder.executedLots).multiply(BigDecimal.valueOf(instrumentInfo.lot));
            final BigDecimal amount = executedOrder.price.multiply(executedLots);
            final Position currentCurrencyPosition = currenciesCopy.getOrDefault(this.instrumentCurrency, Position.empty);
            final Position newCurrencyValue = executedOrder.operation == Operation.Buy
                ? currentCurrencyPosition.withBlocked(currentCurrencyPosition.blocked.subtract(amount))
                : currentCurrencyPosition.withBalance(currentCurrencyPosition.balance.add(amount));
            currenciesCopy.put(this.instrumentCurrency, newCurrencyValue);

            final Position currentPosition = positionsCopy.getOrDefault(executedOrder.figi, Position.empty);
            final Position newPositionValue = executedOrder.operation == Operation.Buy
                    ? currentPosition.withBalance(currentPosition.balance.add(executedLots))
                    : currentPosition.withBlocked(currentPosition.blocked.subtract(executedLots));
            positionsCopy.put(executedOrder.figi, newPositionValue);
        }

        return new TradingState(
                this.orderbook,
                this.candle,
                this.instrumentInfo,
                filtered,
                currenciesCopy,
                positionsCopy,
                this.instrumentCurrency
        );
    }

    @NotNull
    public TradingState withCancelledOrder(@NotNull final String id) {
        Objects.requireNonNull(instrumentInfo);

        Order cancelledOrder = null;
        for (final Order o : this.orders) {
            if (o.id.equals(id)) {
                cancelledOrder = o;
            }
        }

        final List<Order> filtered = this.orders.stream().filter(o -> o.id.equals(id)).collect(Collectors.toList());

        final Map<Currency, Position> currenciesCopy = new HashMap<>(this.currencies);
        final Map<String, Position> positionsCopy = new HashMap<>(this.positions);
        if (Objects.nonNull(cancelledOrder)) {
            final BigDecimal requestedLots = BigDecimal.valueOf(cancelledOrder.requestedLots).multiply(BigDecimal.valueOf(instrumentInfo.lot));
            final BigDecimal amount = cancelledOrder.price.multiply(requestedLots);
            final Position currentCurrencyPosition = currenciesCopy.getOrDefault(this.instrumentCurrency, Position.empty);
            final Position newCurrencyValue = cancelledOrder.operation == Operation.Buy
                    ? currentCurrencyPosition.withBlocked(currentCurrencyPosition.blocked.subtract(amount)).withBalance(currentCurrencyPosition.balance.add(amount))
                    : currentCurrencyPosition;
            currenciesCopy.put(this.instrumentCurrency, newCurrencyValue);

            final Position currentPosition = positionsCopy.getOrDefault(cancelledOrder.figi, Position.empty);
            final Position newPositionValue = cancelledOrder.operation == Operation.Buy
                    ? currentPosition
                    : currentPosition.withBlocked(currentPosition.blocked.subtract(requestedLots)).withBalance(currentPosition.balance.add(requestedLots));
            positionsCopy.put(cancelledOrder.figi, newPositionValue);
        }

        return new TradingState(
                this.orderbook,
                this.candle,
                this.instrumentInfo,
                filtered,
                currenciesCopy,
                positionsCopy,
                this.instrumentCurrency
        );
    }

    @NotNull
    public TradingState withChangedOrder(@NotNull final String id,
                                         @NotNull final Order.Status status,
                                         int executedLots) {
        final List<Order> updated = this.orders.stream().map(o -> {
            if (o.id.equals(id)) {
                return new Order(
                        o.id,
                        o.operation,
                        status,
                        o.rejectReason,
                        o.requestedLots,
                        executedLots,
                        o.commission,
                        o.price,
                        o.figi
                );
            } else {
                return o;
            }
        }).collect(Collectors.toList());

        return new TradingState(
                this.orderbook,
                this.candle,
                this.instrumentInfo,
                updated,
                this.currencies,
                this.positions,
                this.instrumentCurrency
        );
    }

    @NotNull
    public TradingState withPlacedOrder(@NotNull final Order newOrder) {
        Objects.requireNonNull(instrumentInfo);

        final List<Order> newOrders = new LinkedList<>(this.orders);
        newOrders.add(newOrder);

        final Map<Currency, Position> currenciesCopy = new HashMap<>(this.currencies);
        final Map<String, Position> positionsCopy = new HashMap<>(this.positions);

        final BigDecimal requestedLots = BigDecimal.valueOf(newOrder.requestedLots).multiply(BigDecimal.valueOf(instrumentInfo.lot));
        final BigDecimal amount = newOrder.price.multiply(requestedLots);
        final Position currentCurrencyPosition = currenciesCopy.getOrDefault(this.instrumentCurrency, Position.empty);
        final Position newCurrencyValue = newOrder.operation == Operation.Buy
                ? currentCurrencyPosition.withBlocked(currentCurrencyPosition.blocked.add(amount)).withBalance(currentCurrencyPosition.balance.subtract(amount))
                : currentCurrencyPosition;
        currenciesCopy.put(this.instrumentCurrency, newCurrencyValue);

        final Position currentPosition = positionsCopy.getOrDefault(newOrder.figi, Position.empty);
        final Position newPositionValue = newOrder.operation == Operation.Buy
                ? currentPosition
                : currentPosition.withBlocked(currentPosition.blocked.add(requestedLots)).withBalance(currentPosition.balance.subtract(requestedLots));
        positionsCopy.put(newOrder.figi, newPositionValue);

        return new TradingState(
                this.orderbook,
                this.candle,
                this.instrumentInfo,
                newOrders,
                currenciesCopy,
                positionsCopy,
                this.instrumentCurrency
        );
    }

    @Override
    public String toString() {
        return "TradingState{" +
                "orderbook=" + orderbook +
                ", candle=" + candle +
                ", instrumentInfo=" + instrumentInfo +
                ", orders=" + orders +
                ", currencies=" + currencies +
                ", positions=" + positions +
                ", instrumentCurrency=" + instrumentCurrency +
                '}';
    }

    public static class Candle {

        @NotNull public final BigDecimal openPrice;
        @NotNull public final BigDecimal closingPrice;
        @NotNull public final BigDecimal highestPrice;
        @NotNull public final BigDecimal lowestPrice;
        @NotNull public final BigDecimal tradingValue;
        @NotNull public final ZonedDateTime dateTime;
        @NotNull public final CandleInterval interval;
        @NotNull public final String figi;

        public Candle(@NotNull final BigDecimal openPrice,
                      @NotNull final BigDecimal closingPrice,
                      @NotNull final BigDecimal highestPrice,
                      @NotNull final BigDecimal lowestPrice,
                      @NotNull final BigDecimal tradingValue,
                      @NotNull final ZonedDateTime dateTime,
                      @NotNull final CandleInterval interval,
                      @NotNull final String figi) {
            this.openPrice = openPrice;
            this.closingPrice = closingPrice;
            this.highestPrice = highestPrice;
            this.lowestPrice = lowestPrice;
            this.tradingValue = tradingValue;
            this.dateTime = dateTime;
            this.interval = interval;
            this.figi = figi;
        }

        public enum CandleInterval {
            ONE_MIN, TWO_MIN, THREE_MIN, FIVE_MIN, TEN_MIN, QUARTER_HOUR, HALF_HOUR, HOUR, DAY, WEEK, MONTH
        }

        @Override
        public String toString() {
            return "Candle{" +
                    "openPrice=" + openPrice +
                    ", closingPrice=" + closingPrice +
                    ", highestPrice=" + highestPrice +
                    ", lowestPrice=" + lowestPrice +
                    ", tradingValue=" + tradingValue +
                    ", dateTime=" + dateTime +
                    ", interval=" + interval +
                    ", figi='" + figi + '\'' +
                    '}';
        }
    }

    public static class Orderbook {

        public final int depth;
        @NotNull public final List<StakeState> bids;
        @NotNull public final List<StakeState> asks;
        @NotNull public final String figi;

        public Orderbook(final int depth,
                         @NotNull final List<StakeState> bids,
                         @NotNull final List<StakeState> asks,
                         @NotNull final String figi) {
            this.depth = depth;
            this.bids = bids;
            this.asks = asks;
            this.figi = figi;
        }

        public static class StakeState {
            @NotNull public final BigDecimal price;
            public final int count;

            public StakeState(@NotNull final BigDecimal price, final int count) {
                this.price = price;
                this.count = count;
            }

            @Override
            public String toString() {
                return "StakeState{" +
                        "price=" + price +
                        ", count=" + count +
                        '}';
            }
        }

        @Override
        public String toString() {
            return "Orderbook{" +
                    "depth=" + depth +
                    ", bids=" + bids +
                    ", asks=" + asks +
                    ", figi='" + figi + '\'' +
                    '}';
        }
    }

    public static class InstrumentInfo {
        public final boolean canTrade;
        @NotNull public final BigDecimal minPriceIncrement;
        public final int lot;
        @Nullable public final BigDecimal accruedInterest;
        @Nullable public final BigDecimal limitUp;
        @Nullable public final BigDecimal limitDown;
        @NotNull public final String figi;

        public InstrumentInfo(final boolean canTrade,
                              @NotNull final BigDecimal minPriceIncrement,
                              final int lot,
                              @Nullable final BigDecimal accruedInterest,
                              @Nullable final BigDecimal limitUp,
                              @Nullable final BigDecimal limitDown,
                              @NotNull final String figi) {
            this.canTrade = canTrade;
            this.minPriceIncrement = minPriceIncrement;
            this.lot = lot;
            this.accruedInterest = accruedInterest;
            this.limitUp = limitUp;
            this.limitDown = limitDown;
            this.figi = figi;
        }

        @Override
        public String toString() {
            return "InstrumentInfo{" +
                    "canTrade=" + canTrade +
                    ", minPriceIncrement=" + minPriceIncrement +
                    ", lot=" + lot +
                    ", figi='" + figi + '\'' +
                    '}';
        }
    }

    public static class Order {

        public final String id;
        public final Operation operation;
        public final Status status;
        public final String rejectReason;
        public final int requestedLots;
        public final int executedLots;
        public final MoneyAmount commission;
        public final BigDecimal price;
        public final String figi;

        public Order(final String id,
                     final Operation operation,
                     final Status status,
                     final String rejectReason,
                     final int requestedLots,
                     final int executedLots,
                     final MoneyAmount commission,
                     final BigDecimal price,
                     final String figi) {
            if (Objects.isNull(id)) {
                throw new IllegalArgumentException("Идентификатор не может быть null.");
            }
            if (Objects.isNull(operation)) {
                throw new IllegalArgumentException("Тип операции не может быть null.");
            }
            if (Objects.isNull(status)) {
                throw new IllegalArgumentException("Статус не может быть null.");
            }
            if (Objects.isNull(price)) {
                throw new IllegalArgumentException("Цена не может быть null.");
            }
            if (Objects.isNull(figi)) {
                throw new IllegalArgumentException("Идентификатор инструмента не может быть null.");
            }

            this.id = id;
            this.operation = operation;
            this.status = status;
            this.rejectReason = rejectReason;
            this.requestedLots = requestedLots;
            this.executedLots = executedLots;
            this.commission = commission;
            this.price = price;
            this.figi = figi;
        }

        public enum Status {
            New, PartiallyFill, Fill, Cancelled, Replaced, PendingCancel, Rejected, PendingReplace, PendingNew
        }

        @Override
        public String toString() {
            return "Order{" +
                    "id='" + id + '\'' +
                    ", operation=" + operation +
                    ", status=" + status +
                    //", rejectReason='" + rejectReason + '\'' +
                    ", requestedLots=" + requestedLots +
                    ", executedLots=" + executedLots +
                    //", commission=" + commission +
                    ", price=" + price +
                    ", figi='" + figi + '\'' +
                    '}';
        }
    }

    public static class MoneyAmount {
        @NotNull public final Currency currency;
        @NotNull public final BigDecimal value;

        public MoneyAmount(@NotNull final Currency currency,
                           @NotNull final BigDecimal value) {
            this.currency = currency;
            this.value = value;
        }

        @Override
        public String toString() {
            return "MoneyAmount{" +
                    "currency=" + currency +
                    ", value=" + value +
                    '}';
        }
    }

    public enum Currency {
        RUB, USD, EUR
    }

    public enum Operation {
        Buy, Sell
    }

    public static class Position {
        @NotNull public final BigDecimal balance;
        @NotNull public final BigDecimal blocked;

        public static Position empty = new Position(BigDecimal.ZERO, BigDecimal.ZERO);

        public Position(@NotNull final BigDecimal balance,
                        @Nullable final BigDecimal blocked) {
            this.balance = balance;
            this.blocked = Objects.nonNull(blocked) ? blocked : BigDecimal.ZERO;
        }

        @NotNull
        public Position withBalance(@NotNull final BigDecimal newBalance) {
            return new Position(newBalance, this.blocked);
        }

        @NotNull
        public Position withBlocked(@NotNull final BigDecimal newBlocked) {
            return new Position(this.balance, newBlocked);
        }

        @Override
        public String toString() {
            return "Position{" +
                    "balance=" + balance +
                    ", blocked=" + blocked +
                    '}';
        }
    }

}
