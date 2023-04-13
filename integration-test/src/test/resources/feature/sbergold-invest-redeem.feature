Feature: SberGold Invest SmartContract


  Scenario: process datafeed

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    When "SberGoldExchange" registered address for smart contract "GoldInvestRedeem"
    When "SberGoldExchange" create owner address
    When "SBER" registered token type "SBER_TechToken"
    When "SBER" registering IndexTrade token type for redeem smartcontract "GoldInvestRedeem"

    When "SberGoldExchange" registered data feed for "SberGoldBidQuotesDataFeed" with fields:
      | id              | typeId   | description |
      | QuoteEntryID    | string   |             |
      | Quotestimestamp | datetime |             |
      | Symbol          | string   |             |
      | Tenorvalue      | string   |             |
      | Valuedate       | date     |             |
      | Bandprice       | float    |             |
      | Toleranceprice  | float    |             |
      | Maxbandvolume   | numeric  |             |
      | QuoteCondition  | string   |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49                    |
      | Toleranceprice  | 50                    |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |

    And "SberGoldExchange" registered "GoldInvestRedeem" smart contract with smart contract template "IndexTradeRedeem" and feed "SberGoldBidQuotesDataFeed" and attributes:
      | key                   | value                  |
      | issuerAddress         | SberGoldExchange       |
      | redeemedTokenType     | IndexTrade             |
      | techTokenType         | SBER_TechToken         |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z |
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z |
      | quoteTTL              | 60                     |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49                    |
      | Toleranceprice  | 50                    |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | B                     |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02291                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49                    |
      | Toleranceprice  | 48                    |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state


    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02291                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49                    |
      | Toleranceprice  | 48                    |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state


  Scenario: Successful reedem token

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    When "SberGoldExchange" registered address for smart contract "GoldInvestRedeem"
    When "SberGoldExchange" create owner address
    When "SBER" registered token type "SBER_TechToken"
    When "SBER" registering IndexTrade token type for redeem smartcontract "GoldInvestRedeem"

    When "SberGoldExchange" registered data feed for "SberGoldBidQuotesDataFeed" with fields:
      | id              | typeId   | description |
      | QuoteEntryID    | string   |             |
      | Quotestimestamp | datetime |             |
      | Symbol          | string   |             |
      | Tenorvalue      | string   |             |
      | Valuedate       | date     |             |
      | Bandprice       | float    |             |
      | Toleranceprice  | float    |             |
      | Maxbandvolume   | numeric  |             |
      | QuoteCondition  | string   |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49.9257               |
      | Toleranceprice  | 49.91                 |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |

    And "SberGoldExchange" registered "GoldInvestRedeem" smart contract with smart contract template "IndexTradeRedeem" and feed "SberGoldBidQuotesDataFeed" and attributes:
      | key                   | value                  |
      | issuerAddress         | SberGoldExchange       |
      | redeemedTokenType     | IndexTrade             |
      | techTokenType         | SBER_TechToken         |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z |
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z |
      | quoteTTL              | 60                     |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value         |
      | QuoteEntryID    | f02290        |
      | Quotestimestamp | timestamp_now |
      | Symbol          | DXAU/RUB      |
      | Tenorvalue      | TOD           |
      | Valuedate       | 20220721      |
      | Bandprice       | 50            |
      | Toleranceprice  | 55            |
      | Maxbandvolume   | 100000        |
      | QuoteCondition  | A             |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued IndexTrade token "T1" for "Client1" with content:
      | key            | value    |
      | amount         | 100      |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |
    And "Client1" list his tokens
    When "Client1" sends his "T1" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 50    |

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued token "T1" for address of "SberGoldExchange" of type "SBER_TechToken" with value "1500"
    When "SBER" issued token "T2" for address of "SberGoldExchange" of type "SBER_TechToken" with value "3500"
    And "SberGoldExchange" list his tokens
    When "SberGoldExchange" sends his "T1" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "SberGoldExchange" sends his "T2" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens

  Scenario: Successful redeem token with price request lower than bandprice

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    When "SberGoldExchange" registered address for smart contract "GoldInvestRedeem"
    When "SberGoldExchange" create owner address
    When "SBER" registered token type "SBER_TechToken"
    When "SBER" registering IndexTrade token type for redeem smartcontract "GoldInvestRedeem"

    When "SberGoldExchange" registered data feed for "SberGoldBidQuotesDataFeed" with fields:
      | id              | typeId   | description |
      | QuoteEntryID    | string   |             |
      | Quotestimestamp | datetime |             |
      | Symbol          | string   |             |
      | Tenorvalue      | string   |             |
      | Valuedate       | date     |             |
      | Bandprice       | float    |             |
      | Toleranceprice  | float    |             |
      | Maxbandvolume   | numeric  |             |
      | QuoteCondition  | string   |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49.9257               |
      | Toleranceprice  | 49.91                 |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |

    And "SberGoldExchange" registered "GoldInvestRedeem" smart contract with smart contract template "IndexTradeRedeem" and feed "SberGoldBidQuotesDataFeed" and attributes:
      | key                   | value                  |
      | issuerAddress         | SberGoldExchange       |
      | redeemedTokenType     | IndexTrade             |
      | techTokenType         | SBER_TechToken         |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z |
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z |
      | quoteTTL              | 60                     |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value         |
      | QuoteEntryID    | f02290        |
      | Quotestimestamp | timestamp_now |
      | Symbol          | DXAU/RUB      |
      | Tenorvalue      | TOD           |
      | Valuedate       | 20220721      |
      | Bandprice       | 50            |
      | Toleranceprice  | 55            |
      | Maxbandvolume   | 100000        |
      | QuoteCondition  | A             |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued IndexTrade token "T1" for "Client1" with content:
      | key            | value    |
      | amount         | 100      |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "Client1" sends his "T1" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 40    |

    And "Client1" list his tokens
    And "SBER" checks his burnt issuer tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

# transfer check section


  Scenario: Successful reedem tokens with multiple transfer

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    When "SberGoldExchange" registered address for smart contract "GoldInvestRedeem"
    When "SberGoldExchange" create owner address
    When "SBER" registered token type "SBER_TechToken"
    When "SBER" registering IndexTrade token type for redeem smartcontract "GoldInvestRedeem"

    When "SberGoldExchange" registered data feed for "SberGoldBidQuotesDataFeed" with fields:
      | id              | typeId   | description |
      | QuoteEntryID    | string   |             |
      | Quotestimestamp | datetime |             |
      | Symbol          | string   |             |
      | Tenorvalue      | string   |             |
      | Valuedate       | date     |             |
      | Bandprice       | float    |             |
      | Toleranceprice  | float    |             |
      | Maxbandvolume   | numeric  |             |
      | QuoteCondition  | string   |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49.9257               |
      | Toleranceprice  | 49.91                 |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |

    And "SberGoldExchange" registered "GoldInvestRedeem" smart contract with smart contract template "IndexTradeRedeem" and feed "SberGoldBidQuotesDataFeed" and attributes:
      | key                   | value                  |
      | issuerAddress         | SberGoldExchange       |
      | redeemedTokenType     | IndexTrade             |
      | techTokenType         | SBER_TechToken         |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z |
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z |
      | quoteTTL              | 60                     |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value         |
      | QuoteEntryID    | f02290        |
      | Quotestimestamp | timestamp_now |
      | Symbol          | DXAU/RUB      |
      | Tenorvalue      | TOD           |
      | Valuedate       | 20220721      |
      | Bandprice       | 50            |
      | Toleranceprice  | 55            |
      | Maxbandvolume   | 100000        |
      | QuoteCondition  | A             |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued IndexTrade token "T1" for "Client1" with content:
      | key            | value    |
      | amount         | 100      |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "Client1" sends his "T1" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 50    |

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued token "T1" for address of "SberGoldExchange" of type "SBER_TechToken" with value "1500"
    When "SBER" issued token "T2" for address of "SberGoldExchange" of type "SBER_TechToken" with value "4500"
    And "SberGoldExchange" list his tokens
    When "SberGoldExchange" sends his "T1" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "SberGoldExchange" sends his "T2" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    And "SBER" checks his burnt issuer tokens

  Scenario: Successful reedem tokens with multiple transfer case 4

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    When "SberGoldExchange" registered address for smart contract "GoldInvestRedeem"
    When "SberGoldExchange" create owner address
    When "SBER" registered token type "SBER_TechToken"
    When "SBER" registering IndexTrade token type for redeem smartcontract "GoldInvestRedeem"

    When "SberGoldExchange" registered data feed for "SberGoldBidQuotesDataFeed" with fields:
      | id              | typeId   | description |
      | QuoteEntryID    | string   |             |
      | Quotestimestamp | datetime |             |
      | Symbol          | string   |             |
      | Tenorvalue      | string   |             |
      | Valuedate       | date     |             |
      | Bandprice       | float    |             |
      | Toleranceprice  | float    |             |
      | Maxbandvolume   | numeric  |             |
      | QuoteCondition  | string   |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49.9257               |
      | Toleranceprice  | 49.91                 |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |

    And "SberGoldExchange" registered "GoldInvestRedeem" smart contract with smart contract template "IndexTradeRedeem" and feed "SberGoldBidQuotesDataFeed" and attributes:
      | key                   | value                  |
      | issuerAddress         | SberGoldExchange       |
      | redeemedTokenType     | IndexTrade             |
      | techTokenType         | SBER_TechToken         |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z |
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z |
      | quoteTTL              | 60                     |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value         |
      | QuoteEntryID    | f02290        |
      | Quotestimestamp | timestamp_now |
      | Symbol          | DXAU/RUB      |
      | Tenorvalue      | TOD           |
      | Valuedate       | 20220721      |
      | Bandprice       | 100           |
      | Toleranceprice  | 105           |
      | Maxbandvolume   | 100000        |
      | QuoteCondition  | A             |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued IndexTrade token "T1" for "Client1" with content:
      | key            | value    |
      | amount         | 10       |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "SBER" issued IndexTrade token "T2" for "Client1" with content:
      | key            | value    |
      | amount         | 4        |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "SBER" issued IndexTrade token "T3" for "Client1" with content:
      | key            | value    |
      | amount         | 4        |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "SBER" issued IndexTrade token "T4" for "Client1" with content:
      | key            | value    |
      | amount         | 5        |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "Client1" sends his "T1" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "Client1" sends his "T2" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "Client1" sends his "T3" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "Client1" sends his "T4" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued token "R1" for address of "SberGoldExchange" of type "SBER_TechToken" with value "400"
    When "SBER" issued token "R2" for address of "SberGoldExchange" of type "SBER_TechToken" with value "200"
    When "SBER" issued token "R3" for address of "SberGoldExchange" of type "SBER_TechToken" with value "2000"
    When "SBER" issued token "R4" for address of "SberGoldExchange" of type "SBER_TechToken" with value "1000"

    And "SberGoldExchange" list his tokens
    When "SberGoldExchange" sends his "R1" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "Client1" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "SberGoldExchange" sends his "R2" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    When "SberGoldExchange" sends his "R3" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    When "SberGoldExchange" sends his "R4" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    And "SBER" checks his burnt issuer tokens

  Scenario: Successful reedem tokens with multiple transfer case 6

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    When "SberGoldExchange" registered address for smart contract "GoldInvestRedeem"
    When "SberGoldExchange" create owner address
    When "SBER" registered token type "SBER_TechToken"
    When "SBER" registering IndexTrade token type for redeem smartcontract "GoldInvestRedeem"

    When "SberGoldExchange" registered data feed for "SberGoldBidQuotesDataFeed" with fields:
      | id              | typeId   | description |
      | QuoteEntryID    | string   |             |
      | Quotestimestamp | datetime |             |
      | Symbol          | string   |             |
      | Tenorvalue      | string   |             |
      | Valuedate       | date     |             |
      | Bandprice       | float    |             |
      | Toleranceprice  | float    |             |
      | Maxbandvolume   | numeric  |             |
      | QuoteCondition  | string   |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49.9257               |
      | Toleranceprice  | 49.91                 |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |

    And "SberGoldExchange" registered "GoldInvestRedeem" smart contract with smart contract template "IndexTradeRedeem" and feed "SberGoldBidQuotesDataFeed" and attributes:
      | key                   | value                  |
      | issuerAddress         | SberGoldExchange       |
      | redeemedTokenType     | IndexTrade             |
      | techTokenType         | SBER_TechToken         |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z |
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z |
      | quoteTTL              | 60                     |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value         |
      | QuoteEntryID    | f02290        |
      | Quotestimestamp | timestamp_now |
      | Symbol          | DXAU/RUB      |
      | Tenorvalue      | TOD           |
      | Valuedate       | 20220721      |
      | Bandprice       | 100           |
      | Toleranceprice  | 105           |
      | Maxbandvolume   | 100000        |
      | QuoteCondition  | A             |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued IndexTrade token "T1" for "Client1" with content:
      | key            | value    |
      | amount         | 10       |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "SBER" issued IndexTrade token "T2" for "Client1" with content:
      | key            | value    |
      | amount         | 30       |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "SBER" issued IndexTrade token "T3" for "Client1" with content:
      | key            | value    |
      | amount         | 4        |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "SBER" issued IndexTrade token "T4" for "Client1" with content:
      | key            | value    |
      | amount         | 5        |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "Client1" sends his "T1" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "Client1" sends his "T2" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "Client1" sends his "T3" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "Client1" sends his "T4" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued token "R1" for address of "SberGoldExchange" of type "SBER_TechToken" with value "1000"
    When "SBER" issued token "R2" for address of "SberGoldExchange" of type "SBER_TechToken" with value "400"
    When "SBER" issued token "R3" for address of "SberGoldExchange" of type "SBER_TechToken" with value "2000"
    When "SBER" issued token "R4" for address of "SberGoldExchange" of type "SBER_TechToken" with value "600"
    When "SBER" issued token "R5" for address of "SberGoldExchange" of type "SBER_TechToken" with value "200"
    When "SBER" issued token "R6" for address of "SberGoldExchange" of type "SBER_TechToken" with value "500"
    When "SBER" issued token "R7" for address of "SberGoldExchange" of type "SBER_TechToken" with value "1000"


    And "SberGoldExchange" list his tokens
    When "SberGoldExchange" sends his "R1" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "Client1" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "SberGoldExchange" sends his "R2" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    When "SberGoldExchange" sends his "R3" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    When "SberGoldExchange" sends his "R4" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    When "SberGoldExchange" sends his "R5" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    When "SberGoldExchange" sends his "R6" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    When "SberGoldExchange" sends his "R7" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens
    And "SBER" checks his burnt issuer tokens

  Scenario: Successful reedem tokens with existing balance

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    When "SberGoldExchange" registered address for smart contract "GoldInvestRedeem"
    When "SberGoldExchange" create owner address
    When "SBER" registered token type "SBER_TechToken"
    When "SBER" registering IndexTrade token type for redeem smartcontract "GoldInvestRedeem"

    When "SBER" issued token "R1" for address of "SberGoldExchange" of type "SBER_TechToken" with value "10000"
    When "SBER" issued token "R2" for address of "SberGoldExchange" of type "SBER_TechToken" with value "555"
    And "SberGoldExchange" list his tokens

    When "SberGoldExchange" registered data feed for "SberGoldBidQuotesDataFeed" with fields:
      | id              | typeId   | description |
      | QuoteEntryID    | string   |             |
      | Quotestimestamp | datetime |             |
      | Symbol          | string   |             |
      | Tenorvalue      | string   |             |
      | Valuedate       | date     |             |
      | Bandprice       | float    |             |
      | Toleranceprice  | float    |             |
      | Maxbandvolume   | numeric  |             |
      | QuoteCondition  | string   |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49.9257               |
      | Toleranceprice  | 49.91                 |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |

    And "SberGoldExchange" registered "GoldInvestRedeem" smart contract with smart contract template "IndexTradeRedeem" and feed "SberGoldBidQuotesDataFeed" and attributes:
      | key                   | value                  |
      | issuerAddress         | SberGoldExchange        |
      | redeemedTokenType     | IndexTrade             |
      | techTokenType         | SBER_TechToken         |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z |
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z |
      | quoteTTL              | 60                     |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value         |
      | QuoteEntryID    | f02290        |
      | Quotestimestamp | timestamp_now |
      | Symbol          | DXAU/RUB      |
      | Tenorvalue      | TOD           |
      | Valuedate       | 20220721      |
      | Bandprice       | 100           |
      | Toleranceprice  | 105           |
      | Maxbandvolume   | 100000        |
      | QuoteCondition  | A             |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" sends his "R1" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "Client1" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "SberGoldExchange" sends his "R2" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens

    When "SBER" issued IndexTrade token "T1" for "Client1" with content:
      | key            | value    |
      | amount         | 10       |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "SBER" issued IndexTrade token "T2" for "Client1" with content:
      | key            | value    |
      | amount         | 30       |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "SBER" issued IndexTrade token "T3" for "Client1" with content:
      | key            | value    |
      | amount         | 4        |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "SBER" issued IndexTrade token "T4" for "Client1" with content:
      | key            | value    |
      | amount         | 5        |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |

    When "Client1" sends his "T1" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "Client1" sends his "T2" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "Client1" sends his "T3" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "Client1" sends his "T4" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 100   |
    And "Client1" list his tokens
    And "SBER" checks his burnt issuer tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state



  Scenario: Successful reedem token with regulation

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"
    Given There is a client "Regulator"

    When "SberGoldExchange" registered address for smart contract "GoldInvestRedeem"
    When "SberGoldExchange" create owner address
    When "SBER" registered token type "SBER_TechToken"
    When "SBER" registering IndexTrade token type for redeem smartcontract "GoldInvestRedeem" with regulator "Regulator"

    When "SberGoldExchange" registered data feed for "SberGoldBidQuotesDataFeed" with fields:
      | id              | typeId   | description |
      | QuoteEntryID    | string   |             |
      | Quotestimestamp | datetime |             |
      | Symbol          | string   |             |
      | Tenorvalue      | string   |             |
      | Valuedate       | date     |             |
      | Bandprice       | float    |             |
      | Toleranceprice  | float    |             |
      | Maxbandvolume   | numeric  |             |
      | QuoteCondition  | string   |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49.9257               |
      | Toleranceprice  | 49.91                 |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |

    And "SberGoldExchange" registered "GoldInvestRedeem" smart contract with smart contract template "IndexTradeRedeem" and feed "SberGoldBidQuotesDataFeed" and attributes:
      | key                   | value                  |
      | issuerAddress         | SberGoldExchange       |
      | redeemedTokenType     | IndexTrade             |
      | techTokenType         | SBER_TechToken         |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z |
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z |
      | quoteTTL              | 60                     |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value         |
      | QuoteEntryID    | f02290        |
      | Quotestimestamp | timestamp_now |
      | Symbol          | DXAU/RUB      |
      | Tenorvalue      | TOD           |
      | Valuedate       | 20220721      |
      | Bandprice       | 50            |
      | Toleranceprice  | 55            |
      | Maxbandvolume   | 100000        |
      | QuoteCondition  | A             |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued IndexTrade token "T1" for "Client1" with regulation and content:
      | key            | value    |
      | amount         | 100      |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |
    And "Client1" list his tokens
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    And "Client1" list his tokens

    When "Client1" sends his "T1" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 50    |
    When "Client1" checks operations list
    And "Regulator" checks operations list
    And "SberGoldExchange" checks operations list

    And "Client1" list his tokens
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" checks operations list
    And "Regulator" checks operations list
    And "SberGoldExchange" checks operations list

    And "Client1" list his tokens

    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued token "T1" for address of "SberGoldExchange" of type "SBER_TechToken" with value "1500"
    When "SBER" issued token "T2" for address of "SberGoldExchange" of type "SBER_TechToken" with value "3500"
    And "SberGoldExchange" list his tokens
    When "SberGoldExchange" sends his "T1" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    When "SberGoldExchange" sends his "T2" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens

    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" checks operations list
    And "Regulator" checks operations list
    And "SberGoldExchange" checks operations list
    And "SBER" checks operations list

    When "SBER" checks all wallet events
    And "SberGoldExchange" checks all wallet events
    And "Regulator" checks all wallet events
    And "Client1" checks all wallet events

    
  Scenario: Successful reedem token with available balance with regulation

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"
    Given There is a client "Regulator"

    When "SberGoldExchange" registered address for smart contract "GoldInvestRedeem"
    When "SberGoldExchange" create owner address
    When "SBER" registered token type "SBER_TechToken"
    When "SBER" registering IndexTrade token type for redeem smartcontract "GoldInvestRedeem" with regulator "Regulator"

    When "SberGoldExchange" registered data feed for "SberGoldBidQuotesDataFeed" with fields:
      | id              | typeId   | description |
      | QuoteEntryID    | string   |             |
      | Quotestimestamp | datetime |             |
      | Symbol          | string   |             |
      | Tenorvalue      | string   |             |
      | Valuedate       | date     |             |
      | Bandprice       | float    |             |
      | Toleranceprice  | float    |             |
      | Maxbandvolume   | numeric  |             |
      | QuoteCondition  | string   |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value                 |
      | QuoteEntryID    | f02290                |
      | Quotestimestamp | 20220718-07:29:42.940 |
      | Symbol          | DXAU/RUB              |
      | Tenorvalue      | TOD                   |
      | Valuedate       | 20220721              |
      | Bandprice       | 49.9257               |
      | Toleranceprice  | 49.91                 |
      | Maxbandvolume   | 100000                |
      | QuoteCondition  | A                     |

    And "SberGoldExchange" registered "GoldInvestRedeem" smart contract with smart contract template "IndexTradeRedeem" and feed "SberGoldBidQuotesDataFeed" and attributes:
      | key                   | value                  |
      | issuerAddress         | SberGoldExchange       |
      | redeemedTokenType     | IndexTrade             |
      | techTokenType         | SBER_TechToken         |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z |
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z |
      | quoteTTL              | 60                     |
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued token "T1" for address of "SberGoldExchange" of type "SBER_TechToken" with value "10000"
    And "SberGoldExchange" list his tokens
    When "SberGoldExchange" sends his "T1" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldBidQuotesDataFeed" value:
      | key             | value         |
      | QuoteEntryID    | f02290        |
      | Quotestimestamp | timestamp_now |
      | Symbol          | DXAU/RUB      |
      | Tenorvalue      | TOD           |
      | Valuedate       | 20220721      |
      | Bandprice       | 50            |
      | Toleranceprice  | 55            |
      | Maxbandvolume   | 100000        |
      | QuoteCondition  | A             |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued IndexTrade token "T1" for "Client1" with regulation and content:
      | key            | value    |
      | amount         | 100      |
      | price          | 49       |
      | symbol         | DXAU/RUB |
      | tenorValue     | TOD      |
      | valueDate      | 20220721 |
      | bandPrice      | 49       |
      | tolerancePrice | 48       |
      | maxBandVolume  | 100000   |
    And "Client1" list his tokens
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    And "Client1" list his tokens

    When "Client1" sends his "T1" token to "GoldInvestRedeem" smart contract and deal metadata:
      | id    | value |
      | price | 50    |
    When "Client1" checks operations list
    And "Regulator" checks operations list
    And "SberGoldExchange" checks operations list

    And "Client1" list his tokens
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" checks operations list
    And "Regulator" checks operations list
    And "SberGoldExchange" checks operations list

    And "Client1" list his tokens

    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state

    When "SBER" issued token "T1" for address of "SberGoldExchange" of type "SBER_TechToken" with value "10000"
    And "SberGoldExchange" list his tokens
    When "SberGoldExchange" sends his "T1" token to "GoldInvestRedeem" smart contract
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvestRedeem" smart contract state
    And "Client1" list his tokens

    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" checks operations list
    And "Regulator" checks operations list
    And "SberGoldExchange" checks operations list
    And "SBER" checks operations list