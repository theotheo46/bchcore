Feature: Transaction regulation functionality

  Scenario: Approve issue transaction scenario
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions with issue extradata
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Client1" checks operations list

  Scenario: Approve issue with notice transaction scenario
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions with issue extradata
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    And "Regulator" checks operations list
    When "Regulator" checks his pending transactions with issue extradata
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Client1" checks operations list

  Scenario: Reject issue transaction scenario
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" rejects first transaction in list
    When "Client1" sees no tokens in his list
    When "Client1" checks operations list
    When "SBER" checks operations list
    When "Regulator" checks operations list

  Scenario: Reject issue transaction with notice scenario
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    And "Regulator" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" rejects first transaction in list
    When "Client1" sees no tokens in his list
    When "Client1" checks operations list
    When "SBER" checks operations list
    When "Regulator" checks operations list


  Scenario: Approve deal transaction scenario
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"

    When "ICOIssuer" registered address for smart contract "ICO"
    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 100                    |
      | softcapSum            | 100                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-23T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    And "Regulator" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When [Regulation] "Client1" sends his "T1" token to "ICO" smart contract
    When "Regulator" checks his pending transactions
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    And "Regulator" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" approves deals
    When "ICOIssuer" checks "ICO" smart contract state
    When "SBER" checks operations list
    When "Regulator" checks operations list
    When "Client1" checks operations list
    When list smart contract "ICO" accepted deals

  Scenario: Reject deal transaction scenario
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"

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
      | hardcapSum            | 100                    |
      | softcapSum            | 100                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-23T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    And "Regulator" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When [Regulation] "Client1" sends his "T1" token to "ICO" smart contract
    When "Regulator" checks his pending transactions
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    And "Regulator" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" rejects deal transaction
    When "ICOIssuer" checks "ICO" smart contract state
    When "Regulator" checks operations list
    When "SBER" checks operations list
    When "Client1" checks operations list
    When list smart contract "ICO" accepted deals

  Scenario: Approve burn transaction scenario
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"

    When "ICOIssuer" registered address for smart contract "ICO"
    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 100                    |
      | softcapSum            | 100                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-23T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    And "Regulator" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When [Regulation] "Client1" burns his first token in his tokens list
    When "Regulator" checks his pending transactions
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    And "Regulator" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" approves burn transaction
    When "Client1" sees no tokens in his list
    When "Regulator" checks operations list
    When "SBER" checks operations list
    When "Client1" checks operations list
    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When list smart contract "ICO" accepted deals
    When "ICOIssuer" checks all wallet events
    When "Client1" checks all wallet events
    When "Regulator" checks all wallet events

  Scenario: Reject burn transaction scenario
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"

    When "ICOIssuer" registered address for smart contract "ICO"
    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 100                    |
      | softcapSum            | 100                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-23T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When [Regulation] "Client1" burns his first token in his tokens list
    When "Regulator" checks his pending transactions
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    And "Regulator" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" rejects burn transaction
    When "Client1" sees one token in his list
    When "Regulator" checks operations list
    When "SBER" checks operations list
    When "Client1" checks operations list
    When list smart contract "ICO" accepted deals

  Scenario: Approve smart contract transaction
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"

    When "ICOIssuer" registered address for smart contract "ICO"
    And smart contract "ICO" regulators are:
      | name      | capabilities |
      | Regulator | ALL          |
    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 400                    |
      | softcapSum            | 400                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-23T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "SBER" checks "ICO" smart contract regulation
    When "Regulator" approves "ICO" smart contract
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "200" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Client1" list his tokens
    When "SBER" issued token "T2" for "Client1" of type "SBC" with value "200" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees two tokens in his list
    When "Client1" list his tokens
    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When "Client1" sends all his tokens to "ICO" smart contract
    When "Client1" sees two tokens in his list
    When "Regulator" checks his pending transactions
    When "Regulator" approves deals
    When "ICOIssuer" checks "ICO" smart contract state
    When "ICOIssuer" sees no tokens in his list
    When "Regulator" checks his pending transactions
    When "ICOIssuer" checks operations list
    When "Regulator" approves deals
    When "Regulator" approves issues
    When "ICOIssuer" checks operations list
    When "Client1" sees two tokens in his list
    When "ICOIssuer" sees two tokens in his list
    When "Regulator" checks operations list
    When "SBER" checks operations list
    When "Client1" checks operations list
    When list smart contract "ICO" accepted deals

  Scenario: Approve smart contract transaction with multiple tokens
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"

    When "ICOIssuer" registered address for smart contract "ICO"
    And smart contract "ICO" regulators are:
      | name      | capabilities |
      | Regulator | ALL          |
    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 400                    |
      | softcapSum            | 400                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-23T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "SBER" checks "ICO" smart contract regulation
    When "Regulator" approves "ICO" smart contract
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees 1 tokens in his list
    When "SBER" issued token "T2" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees 2 tokens in his list
    When "SBER" issued token "T3" for "Client1" of type "SBC" with value "200" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees 3 tokens in his list
    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When [Regulation] "Client1" sends his "T1" token to "ICO" smart contract
    When [Regulation] "Client1" sends his "T2" token to "ICO" smart contract
    When [Regulation] "Client1" sends his "T3" token to "ICO" smart contract
    When "Client1" sees 3 tokens in his list
    When "Regulator" checks his pending transactions
    When "Regulator" approves deals
    When "Regulator" approves issues
    When "ICOIssuer" checks "ICO" smart contract state
    When "Client1" sees 3 tokens in his list
    When "ICOIssuer" sees no tokens in his list
    When "Regulator" checks his pending transactions
    When "Regulator" approves deals
    When "Client1" sees 3 tokens in his list
    When "ICOIssuer" sees 3 tokens in his list
    When "Regulator" checks operations list
    When "SBER" checks operations list
    When "Client1" checks operations list
    When list smart contract "ICO" accepted deals

  Scenario: Reject smart contract deal
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"

    When "ICOIssuer" registered address for smart contract "ICO"
    And smart contract "ICO" regulators are:
      | name      | capabilities |
      | Regulator | ALL          |
    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 400                    |
      | softcapSum            | 400                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-23T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "SBER" checks "ICO" smart contract regulation
    When "Regulator" approves "ICO" smart contract
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "200" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Client1" list his tokens
    When "SBER" issued token "T2" for "Client1" of type "SBC" with value "200" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees two tokens in his list
    When "Client1" list his tokens
    When "Client1" sends all his tokens to "ICO" smart contract
    When "Client1" list his tokens
    When "Regulator" checks his pending transactions
    When "Regulator" approves deals
    When "ICOIssuer" checks "ICO" smart contract state
    When "Client1" sees two tokens in his list
    When "ICOIssuer" sees no tokens in his list
    When "Regulator" checks his pending transactions
    When "Regulator" rejects deal transaction
    When "Client1" list his tokens
    When "ICOIssuer" list his tokens
    When "Regulator" checks operations list
    When "ICOIssuer" checks operations list
    When "SBER" checks operations list
    When "Client1" checks operations list
    When list smart contract "ICO" accepted deals


  Scenario: Approve smart contract transaction with multiple tokens burn one and merge others
    Given There is a client "SBER"
    Given There is a client "ICOIssuer"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"

    When "ICOIssuer" registering datafeed "TestFeed1"
    When "ICOIssuer" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"

    When "ICOIssuer" registered address for smart contract "ICO"
    And smart contract "ICO" regulators are:
      | name      | capabilities |
      | Regulator | ALL          |
    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and feed "TestFeed1" and attributes:
      | key                   | value                  |
      | investmentTokenType   | SBC                    |
      | issuerAddress         | undefined              |
      | hardcapSum            | 400                    |
      | softcapSum            | 400                    |
      | investmentCoefficient | 1                      |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-23T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "ICOIssuer" submitted value "2022-02-22T10:00:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When "ICOIssuer" checks "ICO" smart contract state
    When "SBER" checks "ICO" smart contract regulation
    When "Regulator" approves "ICO" smart contract
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees 1 tokens in his list
    When "SBER" issued token "T2" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees 2 tokens in his list
    When "SBER" issued token "T3" for "Client1" of type "SBC" with value "200" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees 3 tokens in his list
    When "ICOIssuer" submitted value "2022-02-22T10:20:01.0Z" for datafeed "TestFeed1"
    When "ICOIssuer" awaits 3 seconds
    When [Regulation] "Client1" sends his "T1" token to "ICO" smart contract
    When [Regulation] "Client1" sends his "T2" token to "ICO" smart contract
    When [Regulation] "Client1" sends his "T3" token to "ICO" smart contract
    When "Client1" sees 3 tokens in his list
    When "Regulator" checks his pending transactions
    When "Regulator" approves deals
    When "Regulator" approves issues
    When "ICOIssuer" checks "ICO" smart contract state
    When "Client1" sees 3 tokens in his list
    When "ICOIssuer" sees no tokens in his list
    When "Regulator" checks his pending transactions
    When "Regulator" approves deals
    When "Client1" sees 3 tokens in his list
    When "ICOIssuer" sees 3 tokens in his list
    When "Regulator" checks operations list
    When "SBER" checks operations list
    When "Client1" checks operations list
    When list smart contract "ICO" accepted deals
    When [Regulation] "Client1" burns his first token in his tokens list
    When "Regulator" checks his pending transactions
    When "Regulator" approves burn transaction

    When "Client1" merged all his tokens

    And "Client1" list his tokens
    And "Client1" sees one token in his list