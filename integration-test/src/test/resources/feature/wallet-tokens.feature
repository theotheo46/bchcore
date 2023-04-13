Feature: Wallet token functionality

  Scenario: Getting wallet token
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Regulator"
    When [ICO] "SBER" registered token type "SBC" with recipient signature gene with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    Then "Client1" sees one token in his list
    When "Client1" proposes token "T1" to "Client2"
    When "Client2" accepts proposal from "Client1"
    When "Client1" accepts request for token "T1" from "Client2" accept require
    When "Client2" accepts token
    When "Regulator" freezes token "T1"
    When "Regulator" see token "T1" has restrictions and ongoing operation