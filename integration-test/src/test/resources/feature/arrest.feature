Feature: Token regulation functionality

  Scenario: Impossible to burn token when tokenId is frozen
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" freezes token "T1"
    When "Client1" sees one token in his list
    When "Client1" tries to burn his first token in his tokens list

  Scenario: Impossible to transfer token when tokenId is frozen
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" freezes token "T1"
    When "Client1" sees one token in his list
    When "Client1" tries to send his "T1" token to "Client2"

  Scenario: Possible to regulatory burn token when tokenId is frozen by this regulator
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
      #todo: replace ...with regulation to ...with regulation "string"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" freezes token "T1"
    When "Client1" sees one token in his list
    When "Regulator" tries to regulatory burn first frozen token by this regulator, in "Client1" tokens list

  Scenario: Impossible to regulatory burn token when tokenId is frozen by other regulator
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator1"
    Given There is a client "Regulator2"
    When "SBER" registered token type "SBC" with regulator "Regulator1"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation "Regulator1"
    When "Regulator1" checks his pending transactions
    When "Regulator1" approves transaction
    When "Client1" sees one token in his list
    When "Regulator1" freezes token "T1"
    When "Client1" sees one token in his list
    When "Regulator2" tries to regulatory burn first frozen token by other regulator, in "Client1" tokens list

  Scenario: Possible to regulatory transfer token when tokenId is frozen by this regulator
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    #todo: replace "...with regulation" to "...with regulation {string}"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" freezes token "T1"
    When "Client1" sees one token in his list
    When "Regulator" tries to regulatory transfer frozen token "T1" by this regulator to "Client2"

  Scenario: Impossible to regulatory transfer token when tokenId is frozen by other regulator
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Regulator1"
    Given There is a client "Regulator2"
    When "SBER" registered token type "SBC" with regulator "Regulator1"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation "Regulator1"
    When "Regulator1" checks his pending transactions
    When "Regulator1" approves transaction
    When "Client1" sees one token in his list
    When "Regulator1" freezes token "T1"
    When "Client1" sees one token in his list
    When "Regulator2" tries to regulatory transfer frozen token "T1" by other regulator to "Client2"

  Scenario: Burn token after unfreeze
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" freezes token "T1"
    When "Client1" sees one token in his list
    When "Client1" tries to burn his first token in his tokens list
    When "Regulator" unfreezes token "T1"
    When "Client1" burns his first token in his tokens list regulated by "Regulator"
    When "Regulator" checks his pending transactions
    When "Regulator" approves burn transaction
    When "Client1" sees no tokens in his list
    When "SBER" checks his burnt issuer tokens
    And "SBER" checks operations list
    And "Regulator" checks operations list
    And "Client1" checks operations list

  Scenario: Merge token after unfreeze
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SB_A" with regulator "Regulator"
    When "SBER" issued token "A1" for "Client1" of type "SB_A" with value "200" with regulation
    And "SBER" issued token "A2" for "Client1" of type "SB_A" with value "300" with regulation
    And "SBER" issued token "A3" for "Client1" of type "SB_A" with value "500" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    And "Client1" sees 3 tokens in his list
    When "Regulator" freezes token "A1"
    When "Client1" merged all his tokens but gets error as some tokens are frozen
    Then "Regulator" unfreezes token "A1"
    When "Client1" merged all his tokens
    And "Client1" list his tokens
    And "Client1" sees one token in his list

  Scenario: Send token to smart contract after unfreeze


  Scenario: Regulator performs regulator transfer
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" performs regulatory transfer "T1" to "Client2"
   # When "Client1" sees no tokens in his list
    When "Client2" sees one token in his list
    And "Regulator" checks operations list
    And "Client1" checks operations list
    And "Client2" checks operations list

  Scenario: Regulator performs regulator burn
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Client2"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" performs regulatory burn "T1" of "Client1"
    And "Regulator" checks operations list
    When "Client1" sees no tokens in his list
    And "Client1" checks operations list

  Scenario: Impossible to unfeeze frozen token by other regulator
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator1"
    Given There is a client "Regulator2"
    When "SBER" registered token type "SBC" with regulator "Regulator1"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation "Regulator1"
    When "Regulator1" checks his pending transactions
    When "Regulator1" approves transaction
    When "Client1" sees one token in his list
    When "Regulator1" freezes token "T1"
    When "Client1" sees one token in his list
    When "Regulator2" tries to unfreeze token "T1" frozen token by other regulator

  Scenario: Impossible to feeze token by other regulator
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator1"
    Given There is a client "Regulator2"
    When "SBER" registered token type "SBC" with regulator "Regulator1"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation "Regulator1"
    When "Regulator1" checks his pending transactions
    When "Regulator1" approves transaction
    When "Client1" sees one token in his list
    When "Regulator2" tries to freeze token "T1" regulated by other regulator but gets error as is not permitted