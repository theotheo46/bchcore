Feature: Send token functionality

  Scenario: Send-token
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    When [ICO] "SBER" registered token type "SBC" with recipient signature gene
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
    When "Client1" sees one token in his list
    When "Client2" registered address
    When [Transfer with accept] "Client1" sends his "T1" token to "Client2"
    When "Client2" checks operations list
    When "Client2" accepts token
    When "Client2" sees one token in his list
    When "Client1" sees no tokens in his list
    When "Client1" checks operations list
    When "Client2" checks operations list

  Scenario: Send-token-private-messaging
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    When [ICO] "SBER" registered token type "SBC" with recipient signature gene
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
    When "Client1" sees one token in his list
    When "Client1" proposes token "T1" to "Client2"
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Client2" requests token "T1" from "Client1"
    When [Transfer] "Client1" sends his "T1" token to "Client2"
    When "Client2" checks operations list
    When "Client2" accepts token
    When "Client2" sees one token in his list
    When "Client1" sees no tokens in his list
    When "Client1" checks operations list
    When "Client2" checks operations list

  Scenario: Send-token-private-messaging with accept regulation
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Regulator"
    When [ICO] "SBER" registered token type "SBC" with recipient signature gene with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" send regulatory notification "Message from REGULATOR" for transactions
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" checks operations list
    When "Client2" checks operations list
    Then "Client1" sees one token in his list
    When "Client1" proposes token "T1" to "Client2"
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Client2" requests token "T1" from "Client1"
    When "Client1" checks operations list
    When "Client2" checks operations list
    When [Transfer] "Client1" sends his "T1" token to "Client2"
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Client2" accepts token
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    Then "Client1" sees no tokens in his list
    Then "Client2" sees one token in his list
    When "Client1" checks operations list
    When "Client2" checks operations list

  Scenario: Send-token-through-accepts with accept regulation
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Regulator"
    When [ICO] "SBER" registered token type "SBC" with recipient signature gene with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" checks operations list
    When "Client2" checks operations list
    Then "Client1" sees one token in his list
    When "Client1" proposes token "T1" to "Client2"
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Client2" accepts proposal from "Client1"
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Client1" accepts request for token "T1" from "Client2" accept require
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Client2" accepts token
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Regulator" checks his pending transactions
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Regulator" approves transaction
    Then "Client1" sees no tokens in his list
    Then "Client2" sees one token in his list
    When "Client1" checks operations list
    When "Client2" checks operations list

  Scenario: Send token after unfreeze
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Regulator"
    When [ICO] "SBER" registered token type "SBC" with recipient signature gene with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" freezes token "T1"
    When "Client1" sees one token in his list
    When "Client2" registered address
    When [Transfer with accept] "Client1" sends his "T1" token to "Client2" but gets error as frozen token
    When "Regulator" checks his pending transactions
    When "Regulator" unfreezes token "T1"
    When "Client1" sees one token in his list
    When [Transfer with accept] "Client1" sends his "T1" token to "Client2"
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Client2" accepts token
    When "Client1" checks operations list
    When "Client2" checks operations list
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    Then "Client1" sees no tokens in his list
    Then "Client2" sees one token in his list