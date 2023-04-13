Feature: ICO smart contract

  Scenario: ICO with positive rate
    Given There is a client "SBER5"
    Given There is a client "ICOIssuer5"
    Given There is a client "Client15"
    Given There is a client "Client25"
    Given There is a client "Client35"
    When "SBER5" registered token type "SBC5"


    When "ICOIssuer5" registering datafeed "TestFeed15"
    When "ICOIssuer5" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed15"
    When "ICOIssuer5" registered address for smart contract "ICO5"
    And smart contract "ICO5" burn extra data:
      | id            | typeId  | description                 |
      | accountNumber | numeric | Номер аккаунта пользователя |

    And "ICOIssuer5" registered "ICO5" smart contract with smart contract template "ICO" and feed "TestFeed15" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC5                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 264                    |
      | softcapSum            | 100                    |
      | investmentCoefficient | 33                     |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer5" awaits 3 seconds
    When "ICOIssuer5" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed15"

    When "SBER5" issued token "T15" for "Client15" of type "SBC5" with value "66"
    When "Client15" sees one token in his list
    When "SBER5" issued token "T25" for "Client15" of type "SBC5" with value "66"
    When "Client15" sees two tokens in his list
    When "Client15" list his tokens
    When "Client15" sends all his tokens to "ICO5" smart contract and wait tokens gone
    When "Client15" awaits 4 seconds
    When "Client15" sees no tokens in his list

    When "SBER5" issued token "T35" for "Client25" of type "SBC5" with value "66"
    When "Client25" sees one token in his list
    When "Client25" sends his "T35" token to "ICO5" smart contract
    When "Client25" awaits 4 seconds
    When "Client25" sees no tokens in his list

    When "SBER5" issued token "T45" for "Client35" of type "SBC5" with value "66"
    When "Client35" sees one token in his list
    When "Client35" sends his "T45" token to "ICO5" smart contract

    When "ICOIssuer5" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed15"
    When "ICOIssuer5" awaits 5 seconds
    When "Client35" sees one token in his list
    When "ICOIssuer5" checks "ICO5" smart contract state
    When "Client15" sees two tokens in his list
    When "Client15" burns all his tokens with extra data:
      | value               |
      | client 1 burn extra |

    When "Client25" sees one token in his list
    When "Client25" burns his first token in his tokens list with extra data:
      | value        |
      | 123456654321 |

    When "Client25" sees no tokens in his list
    When "Client35" sees one token in his list
    When "ICOIssuer5" sees four tokens in his list
    When "ICOIssuer5" checks his burnt issuer tokens
    When list smart contract "ICO5" accepted deals
    When "ICOIssuer5" checks all wallet events
    When "Client15" checks all wallet events

  @skip
  Scenario: ICO with concurrent token send
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" registered address for smart contract "ICO"
    And smart contract "ICO" burn extra data:
      | id            | typeId  | description                 |
      | accountNumber | numeric | Номер аккаунта пользователя |

    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 600                    |
      | softcapSum            | 400                    |
      | investmentCoefficient | 2                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"

    When "SBER" issued tokens "M" in quantity 10 for "Client1" of type "SBC" with value "100"
    When "Client1" list his tokens
    When "ICOIssuer" checks "ICO" smart contract state

    When "Client1" sends 10 his tokens concurrently to "ICO" smart contract
    When list smart contract "ICO" accepted deals
    When "Client1" awaits 5 seconds
    When list smart contract "ICO" accepted deals
    When "Client1" list his tokens
    When "Client1" sees 4 tokens in his list

    When "ICOIssuer" submitted value "2022-02-22T10:10:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 5 seconds
    When "ICOIssuer" checks "ICO" smart contract state

    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 5 seconds
    When "ICOIssuer" checks "ICO" smart contract state

    When "Client1" sees 10 tokens in his list
    When "ICOIssuer" sees 6 tokens in his list
    When "Client1" list his tokens
    When "ICOIssuer" list his tokens
    When "SBER" list his tokens

    When list smart contract "ICO" accepted deals

  @skip
  Scenario: ICO with concurrent token send and concurrent datafeed tick
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"

    When "ICOIssuer" registered address for smart contract "ICO"
    And smart contract "ICO" burn extra data:
      | id            | typeId  | description                 |
      | accountNumber | numeric | Номер аккаунта пользователя |

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When list data feeds
    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 800                    |
      | softcapSum            | 400                    |
      | investmentCoefficient | 2                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate           | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" checks "ICO" smart contract state
    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"

    When "SBER" issued tokens "M" in quantity 5 for "Client1" of type "SBC" with value "100"
    When "Client1" list his tokens
    When "ICOIssuer" checks "ICO" smart contract state

    When "Client1" sends all his tokens concurrently to "ICO" smart contract and "ICOIssuer" submit value "2022-02-22T10:20:01.0Z" to "TestFeed1" datafeed
    When list smart contract "ICO" accepted deals
    When "Client1" awaits 10 seconds
    When list smart contract "ICO" accepted deals
    When "Client1" list his tokens

    When "Client1" sees 5 tokens in his list
    When "ICOIssuer" sees 5 tokens in his list
    When "Client1" list his tokens
    When "ICOIssuer" list his tokens
    When "SBER" list his tokens

    When list smart contract "ICO" accepted deals

  @skip
  Scenario: ICO with concurrent token send in two stages
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"


    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" registered address for smart contract "ICO"
    And smart contract "ICO" burn extra data:
      | id            | typeId  | description                 |
      | accountNumber | numeric | Номер аккаунта пользователя |

    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 1000                   |
      | softcapSum            | 600                    |
      | investmentCoefficient | 2                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate           | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"

    When "SBER" issued tokens "M" in quantity 5 for "Client1" of type "SBC" with value "100"
    When "SBER" issued tokens "N" in quantity 10 for "Client1" of type "SBC" with value "100"
    When "Client1" list his tokens

    When "Client1" sends 5 his tokens concurrently to "ICO" smart contract
    When list smart contract "ICO" accepted deals
    When "Client1" awaits 5 seconds
    When list smart contract "ICO" accepted deals
    When "Client1" list his tokens
    When "Client1" sees 10 tokens in his list

    When "Client1" sends 10 his tokens concurrently to "ICO" smart contract
    When list smart contract "ICO" accepted deals
    When "Client1" awaits 5 seconds
    When list smart contract "ICO" accepted deals
    When "Client1" list his tokens
    When "Client1" sees 5 tokens in his list

    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 5 seconds
    When "Client1" sees 15 tokens in his list
    When "ICOIssuer" sees 10 tokens in his list
    When "Client1" list his tokens
    When "ICOIssuer" list his tokens
    When "SBER" list his tokens

    When list smart contract "ICO" accepted deals

  @skip
  Scenario: ICO with concurrent tokens with different value
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"


    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" registered address for smart contract "ICO"
    And smart contract "ICO" burn extra data:
      | id            | typeId  | description                 |
      | accountNumber | numeric | Номер аккаунта пользователя |

    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 700                    |
      | softcapSum            | 400                    |
      | investmentCoefficient | 2                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate           | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"

    When "SBER" issued tokens "M" in quantity 4 for "Client1" of type "SBC" with value "500"
    When "SBER" issued tokens "N" in quantity 4 for "Client1" of type "SBC" with value "100"
    When "Client1" list his tokens

    When "Client1" sends 8 his tokens concurrently to "ICO" smart contract
    When list smart contract "ICO" accepted deals
    When "Client1" awaits 5 seconds
    When list smart contract "ICO" accepted deals
    When "Client1" list his tokens
    #When "Client1" sees 5 tokens in his list

    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 5 seconds
    When "Client1" list his tokens
    When "ICOIssuer" list his tokens
    When "SBER" list his tokens
    When "Client1" sees 8 tokens in his list
#    When "ICOIssuer" sees 3 tokens in his list

    When list smart contract "ICO" accepted deals

  @skip
  Scenario: ICO with concurrent token send in two stages with different values
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" registered address for smart contract "ICO"
    And smart contract "ICO" burn extra data:
      | id            | typeId  | description                 |
      | accountNumber | numeric | Номер аккаунта пользователя |

    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 1100                   |
      | softcapSum            | 800                    |
      | investmentCoefficient | 2                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate           | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"

    When "SBER" issued tokens "M" in quantity 5 for "Client1" of type "SBC" with value "400"
    When "Client1" sends 5 his tokens concurrently to "ICO" smart contract
    When list smart contract "ICO" accepted deals
    When "Client1" awaits 5 seconds
    When list smart contract "ICO" accepted deals
    When "Client1" list his tokens
    When "Client1" sees 3 tokens in his list

    When "SBER" issued tokens "N" in quantity 9 for "Client1" of type "SBC" with value "100"
    When "Client1" list his tokens

    When "Client1" sends 12 his tokens concurrently to "ICO" smart contract
    When list smart contract "ICO" accepted deals
    When "Client1" awaits 7 seconds
    When list smart contract "ICO" accepted deals
    When "Client1" list his tokens
    When "Client1" sees 9 tokens in his list

    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 5 seconds
    When "Client1" sees 14 tokens in his list
    When "ICOIssuer" sees 5 tokens in his list
    When "Client1" list his tokens
    When "ICOIssuer" list his tokens
    When "SBER" list his tokens

    When list smart contract "ICO" accepted deals


  Scenario: ICO with one token value non proportional to coeffecient
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Client3"
    And "SBER" registered token type "SBC"

    When "ICOIssuer" registering datafeed "TestFeed1"
    And "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
    And "ICOIssuer" registered address for smart contract "ICO"
    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 30                     |
      | softcapSum            | 20                     |
      | investmentCoefficient | 3                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate           | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" awaits 2 seconds
    And "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"

    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "10"
    And "Client1" sees one token in his list
    And "SBER" issued token "T2" for "Client1" of type "SBC" with value "11"
    And "Client1" sees 2 tokens in his list

    When "Client1" can not send all his tokens to "ICO" smart contract
    And "Client1" sees 2 tokens in his list

    When "Client1" list his tokens
    And "ICOIssuer" list his tokens
    And list smart contract "ICO" accepted deals

    When "SBER" issued token "N1" for "Client2" of type "SBC" with value "9"
    And "Client2" sees one token in his list
    And "SBER" issued token "N2" for "Client2" of type "SBC" with value "11"
    And "Client2" sees 2 tokens in his list

    When "Client2" can not send all his tokens to "ICO" smart contract
    And "Client2" sees 2 tokens in his list

    When "Client2" list his tokens
    And "ICOIssuer" list his tokens
    And list smart contract "ICO" accepted deals

    When "SBER" issued token "M1" for "Client3" of type "SBC" with value "9"
    And "Client3" sees one token in his list
    And "SBER" issued token "M2" for "Client3" of type "SBC" with value "11"
    And "Client3" sees 2 tokens in his list
    And "SBER" issued token "M3" for "Client3" of type "SBC" with value "4"
    And "Client3" sees 3 tokens in his list

    When "Client3" can not send all his tokens to "ICO" smart contract
    And "Client3" sees 3 tokens in his list

    When "Client3" list his tokens
    And "ICOIssuer" list his tokens
    And list smart contract "ICO" accepted deals


  Scenario: ICO with same tokens ids in deal
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" registered address for smart contract "ICO"

    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 1100                   |
      | softcapSum            | 100                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate           | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"

    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
    When "Client1" sees one token in his list

    When "Client1" sends his "T1" token to "ICO" smart contract in one deal multiple times
    When "Client1" awaits 4 seconds

    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 5 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "Client1" list his tokens
    When "ICOIssuer" list his tokens
    When list smart contract "ICO" accepted deals


  Scenario: ICO with same tokens ids in multiple deals concurrently
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" registered address for smart contract "ICO"

    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 1100                   |
      | softcapSum            | 100                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate           | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"

    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
    When "Client1" sees one token in his list

    When "Client1" sends his "T1" token to "ICO" smart contract in multiple deals concurrently
    When "Client1" awaits 4 seconds

    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 5 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "Client1" list his tokens
    When "ICOIssuer" list his tokens
    When list smart contract "ICO" accepted deals