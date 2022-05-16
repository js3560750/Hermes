package com.aixi.lv.model.constant;

import java.time.LocalDate;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Js
 *
 * 交易对（币种）
 *
 *
 * 天级MACD需要至少60天的数据
 * 4小时级MACD需要至少10天的数据，因此至少要上市10天后的币种才能列进来
 */
public enum Symbol {

    /**************************    主流    ****************************/

    BTCUSDT("BTCUSDT", LocalDate.of(2020, 1, 1), true),

    ETHUSDT("ETHUSDT", LocalDate.of(2020, 1, 1), true),

    BNBUSDT("BNBUSDT", LocalDate.of(2020, 1, 1), true),

    /**************************    20倍    ****************************/

    DOGEUSDT("DOGEUSDT", LocalDate.of(2020, 1, 1), true),

    LUNAUSDT("LUNAUSDT", LocalDate.of(2020, 8, 24), true),

    FTMUSDT("FTMUSDT", LocalDate.of(2020, 1, 1), true),

    NEARUSDT("NEARUSDT", LocalDate.of(2020, 10, 19), true),

    MATICUSDT("MATICUSDT", LocalDate.of(2020, 1, 1), true),

    SANDUSDT("SANDUSDT", LocalDate.of(2020, 8, 17), true),

    SOLUSDT("SOLUSDT", LocalDate.of(2020, 8, 17), true),

    ADAUSDT("ADAUSDT", LocalDate.of(2020, 1, 1), true),

    ONEUSDT("ONEUSDT", LocalDate.of(2020, 1, 1), true),

    MANAUSDT("MANAUSDT", LocalDate.of(2020, 8, 10), true),

    AXSUSDT("AXSUSDT", LocalDate.of(2020, 11, 9), true),

    COTIUSDT("COTIUSDT", LocalDate.of(2020, 3, 2), true),

    CHRUSDT("CHRUSDT", LocalDate.of(2020, 5, 11), true),

    AVAXUSDT("AVAXUSDT", LocalDate.of(2020, 9, 28), true),

    /**************************    5倍    ****************************/

    ROSEUSDT("ROSEUSDT", LocalDate.of(2020, 11, 23), true),

    SHIBUSDT("SHIBUSDT", LocalDate.of(2021, 5, 17), true),

    DUSKUSDT("DUSKUSDT", LocalDate.of(2020, 1, 1), true),

    OMGUSDT("OMGUSDT", LocalDate.of(2020, 1, 1), true),

    /**************************    负增长    ****************************/

    ICPUSDT("ICPUSDT", LocalDate.of(2021, 5, 17), true),

    SLPUSDT("SLPUSDT", LocalDate.of(2021, 5, 3), true),

    SUPERUSDT("SUPERUSDT", LocalDate.of(2021, 3, 29), true),

    /**************************    新币种-用于灵活账户    ****************************/

    GALAUSDT("GALAUSDT", LocalDate.of(2021, 9, 14), true),

    PEOPLEUSDT("PEOPLEUSDT", LocalDate.of(2021, 12, 24), true),

    ACHUSDT("ACHUSDT", LocalDate.of(2022, 1, 11), true),

    RAREUSDT("RAREUSDT", LocalDate.of(2021, 10, 12), true),

    DIAUSDT("DIAUSDT", LocalDate.of(2020, 9, 14), false),

    ALPACAUSDT("ALPACAUSDT", LocalDate.of(2021, 8, 16), false),

    INJUSDT("INJUSDT", LocalDate.of(2020, 10, 26), false),

    IDEXUSDT("IDEXUSDT", LocalDate.of(2021, 9, 13), false),

    STRAXUSDT("STRAXUSDT", LocalDate.of(2020, 11, 23), false),

    XRPUSDT("XRPUSDT", LocalDate.of(2020, 1, 1), true),

    LTCUSDT("LTCUSDT", LocalDate.of(2020, 1, 1), false),

    BETAUSDT("BETAUSDT", LocalDate.of(2021, 10, 11), true),

    TRXUSDT("TRXUSDT", LocalDate.of(2020, 1, 1), true),

    DOTUSDT("DOTUSDT", LocalDate.of(2020, 8, 24), true),

    LINKUSDT("LINKUSDT", LocalDate.of(2020, 1, 1), false),

    ATOMUSDT("ATOMUSDT", LocalDate.of(2020, 1, 1), true),

    FILUSDT("FILUSDT", LocalDate.of(2020, 10, 19), false),

    ETCUSDT("ETCUSDT", LocalDate.of(2020, 1, 1), false),

    VETUSDT("VETUSDT", LocalDate.of(2020, 1, 1), false),

    EOSUSDT("EOSUSDT", LocalDate.of(2020, 1, 1), false),

    THETAUSDT("THETAUSDT", LocalDate.of(2020, 1, 1), false),

    NEOUSDT("NEOUSDT", LocalDate.of(2020, 1, 1), false),

    XLMUSDT("XLMUSDT", LocalDate.of(2020, 1, 1), false),

    KAVAUSDT("KAVAUSDT", LocalDate.of(2020, 1, 1), false),

    CRVUSDT("CRVUSDT", LocalDate.of(2020, 8, 17), false),

    COCOSUSDT("COCOSUSDT", LocalDate.of(2021, 1, 25), false),

    KLAYUSDT("KLAYUSDT", LocalDate.of(2021, 6, 28), false),

    OOKIUSDT("OOKIUSDT", LocalDate.of(2021, 12, 27), false),

    RADUSDT("RADUSDT", LocalDate.of(2021, 10, 11), false),

    STPTUSDT("STPTUSDT", LocalDate.of(2020, 3, 30), false),

    TORNUSDT("TORNUSDT", LocalDate.of(2021, 6, 14), false),

    KMDUSDT("KMDUSDT", LocalDate.of(2020, 8, 17), false),

    HBARUSDT("HBARUSDT", LocalDate.of(2020, 1, 1), false),

    GMTUSDT("GMTUSDT", LocalDate.of(2022, 3, 10), true),

    APEUSDT("APEUSDT", LocalDate.of(2022, 3, 18), true),

    ;

    Symbol(String code, LocalDate saleDate, Boolean backFlag) {
        this.code = code;
        this.saleDate = saleDate;
        this.backFlag = backFlag;
    }

    /**
     * 交易对编码
     */
    @Getter
    private String code;

    /**
     * 首次交易日期，最早到2020年1月1日，因为不会回测这之前的数据
     */
    @Getter
    private LocalDate saleDate;

    /**
     * 是否可用于回测
     * true  : 可以
     * false : 不可用
     */
    @Getter
    private Boolean backFlag;

    public static Symbol getByCode(String code) {

        for (Symbol item : Symbol.values()) {
            if (StringUtils.equals(item.getCode(), code)) {
                return item;
            }
        }

        throw new RuntimeException("非法 Symbol | input = " + code);

    }
}
