Feature: SberGold Redeem Invest SmartContract


  Scenario: process datafeed

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    When "SBER" registered token type "SBER_TechToken"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 50                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |

    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" registered "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 50                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | B                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02291                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 48                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state


    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02291                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 48                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state


  Scenario: Successful invest tokens

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    And "SBER" registered token type "SBER_TechToken"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49.9257                   |
      | Toleranceprice        | 49.91                     |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |

    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" registered "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | timestamp_now             |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 48                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SBER" issued token "T1" for "Client1" of type "SBER_TechToken" with value "5000"

    When "Client1" sends his "T1" token to "GoldInvest" smart contract and deal metadata:
      | id                  | value             |
      | price               | 49                |
      | volume              | 70                |

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvest" smart contract state


  Scenario: Successful invest tokens with no change

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    And "SBER" registered token type "SBER_TechToken"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 49                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |


    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" registered "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | timestamp_now             |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 96                        |
      | Toleranceprice        | 100                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SBER" issued token "T1" for "Client1" of type "SBER_TechToken" with value "96"

    When "Client1" sends his "T1" token to "GoldInvest" smart contract and deal metadata:
      | id                  | value             |
      | price               | 96                |
      | volume              | 1                 |

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvest" smart contract state


  Scenario: Successful invest tokens with price request higher than bandprice and receive change

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    And "SBER" registered token type "SBER_TechToken"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 49                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |


    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" registered "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |

    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | timestamp_now             |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 180                        |
      | Toleranceprice        | 100                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SBER" issued token "T1" for "Client1" of type "SBER_TechToken" with value "210"

    When "Client1" sends his "T1" token to "GoldInvest" smart contract and deal metadata:
      | id                  | value             |
      | price               | 200                |
      | volume              | 1                 |

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvest" smart contract state
    When "Client1" checks all wallet events

  Scenario: Can not register smartcontract with wrong issueraddress

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    And "SBER" registered token type "SBER_TechToken"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 49                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |

    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" can not register "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | abcdz#d               |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |


  Scenario: Can not register smartcontract with wrong token type

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    And "SBER" registered token type "SBER_TechToken"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 49                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |


    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering wrong IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" can not register "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |

  Scenario: Can not invest prior subscription date

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    And "SBER" registered token type "SBER_TechToken"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49.9257                   |
      | Toleranceprice        | 49.91                     |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |

    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" registered "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2023-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2024-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | timestamp_now             |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 48                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SBER" issued token "T1" for "Client1" of type "SBER_TechToken" with value "5000"

    When "Client1" can not sends his "T1" token to "GoldInvest" smart contract and deal metadata:
      | id                  | value             |
      | price               | 49                |
      | volume              | 70                |

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvest" smart contract state

  Scenario: Can not invest after subscription date

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    And "SBER" registered token type "SBER_TechToken"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49.9257                   |
      | Toleranceprice        | 49.91                     |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |

    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" registered "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2020-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2022-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | timestamp_now             |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 48                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SBER" issued token "T1" for "Client1" of type "SBER_TechToken" with value "5000"

    When "Client1" can not sends his "T1" token to "GoldInvest" smart contract and deal metadata:
      | id                  | value             |
      | price               | 49                |
      | volume              | 70                |

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvest" smart contract state


  Scenario: Can not invest after quoteTTL

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"

    And "SBER" registered token type "SBER_TechToken"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49.9257                   |
      | Toleranceprice        | 49.91                     |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |

    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" registered "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:30:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 48                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SBER" issued token "T1" for "Client1" of type "SBER_TechToken" with value "5000"

    When "Client1" can not sends his "T1" token to "GoldInvest" smart contract and deal metadata:
      | id                  | value             |
      | price               | 49                |
      | volume              | 70                |

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvest" smart contract state



  Scenario: Successful invest tokens with regulation

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"
    Given There is a client "Regulator"

    And "SBER" registered token type "SBER_TechToken" with regulator "Regulator"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49.9257                   |
      | Toleranceprice        | 49.91                     |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |

    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" registered "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z|
      | quoteTTL              | 60                    |
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | timestamp_now             |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 48                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SBER" issued token "T1" for "Client1" of type "SBER_TechToken" with value "5000" with regulation
    When "Regulator" checks his pending transactions
    And "Regulator" approves transaction

    When "Client1" sends his "T1" token to "GoldInvest" smart contract and deal metadata:
      | id                  | value             |
      | price               | 49                |
      | volume              | 70                |

    And "Client1" list his tokens
    When "Regulator" checks his pending transactions
    And "Regulator" approves transaction

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvest" smart contract state

  Scenario: Can not invest tokens with regulation after regulation delay

    Given There is a client "SBER"
    Given There is a client "SberGoldExchange"
    Given There is a client "Client1"
    Given There is a client "Regulator"

    And "SBER" registered token type "SBER_TechToken" with regulator "Regulator"

    When "SberGoldExchange" registered data feed for "SberGoldOfferQuotesDataFeed" with fields:
      | id                    | typeId        | description |
      | QuoteEntryID          | string        |             |
      | Quotestimestamp       | datetime      |             |
      | Symbol                | string        |             |
      | Tenorvalue            | string        |             |
      | Valuedate             | date          |             |
      | Bandprice             | float         |             |
      | Toleranceprice        | float         |             |
      | Maxbandvolume         | numeric       |             |
      | QuoteCondition        | string        |             |
    And "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | 20220718-07:29:42.940     |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49.9257                   |
      | Toleranceprice        | 49.91                     |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |

    When "SberGoldExchange" registered address for smart contract "GoldInvest"
    And "SberGoldExchange" registering IndexTrade token type for smartcontract "GoldInvest"
    And "SberGoldExchange" registered "GoldInvest" smart contract with smart contract template "IndexTrade" and feed "SberGoldOfferQuotesDataFeed" and attributes:
      | key                   | value                 |
      | issuerAddress         | undefined             |
      | investmentTokenType   | SBER_TechToken        |
      | hardcapSum            | 150000                |
      | softcapSum            | 1                     |
      | maxTransactionSum     | 150000                |
      | minTransactionSum     | 1                     |
      | subscriptionStartDate | 2022-10-10T08:00:00.0Z|
      | subscriptionEndDate   | 2023-10-10T20:00:00.0Z|
      | quoteTTL              | 3                    |
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SberGoldExchange" submitted for datafeed "SberGoldOfferQuotesDataFeed" value:
      | key                   |value                      |
      | QuoteEntryID          | f02290                    |
      | Quotestimestamp       | timestamp_now             |
      | Symbol                | DXAU/RUB                  |
      | Tenorvalue            | TOD                       |
      | Valuedate             | 20220721                  |
      | Bandprice             | 49                        |
      | Toleranceprice        | 48                        |
      | Maxbandvolume         | 100000                    |
      | QuoteCondition        | A                         |
    And "SberGoldExchange" awaits 1 seconds
    And "SberGoldExchange" checks "GoldInvest" smart contract state

    When "SBER" issued token "T1" for "Client1" of type "SBER_TechToken" with value "5000" with regulation
    When "Regulator" checks his pending transactions
    And "Regulator" approves transaction

    When "Client1" sends his "T1" token to "GoldInvest" smart contract and deal metadata:
      | id                  | value             |
      | price               | 49                |
      | volume              | 70                |

    And "Client1" list his tokens
    When "Regulator" awaits 6 seconds
    When "Regulator" checks his pending transactions
    And "Regulator" approves transaction

    And "Client1" list his tokens
    And "SberGoldExchange" list his tokens
    And "SberGoldExchange" checks "GoldInvest" smart contract state
    When "Client1" checks operations list
    When "Regulator" checks operations list
    When "SberGoldExchange" checks operations list

