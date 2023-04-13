Feature: Fungible tokens

  Scenario: Successful change token
    Given There is a client "SBER"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
    When "Client1" sees one token in his list
    When "Client1" changes token "T1" with two new tokens with values "50" and "50"
    Then "Client1" sees two tokens in his list

  Scenario: Unsuccessful change token as amounts are not the same
    Given There is a client "SBER"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
    Then "Client1" sees one token in his list
    When "Client1" tries to change token "T1" with two new tokens with values "60" and "50" but gets errors as the amounts are not even
    Then "Client1" sees one token in his list

  Scenario: Successful change and burn token
    Given There is a client "SBER"
    Given There is a client "Client1"
    When "SBER" registered token type "SBC"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
    When "Client1" sees one token in his list
    When "Client1" changes token "T1" with two new tokens with values "40" and "60"
    Then "Client1" sees two tokens in his list
    When "Client1" burns his first token in his tokens list
    When "Client1" sees one token in his list
    When "SBER" checks his burnt issuer tokens
    When "Client1" checks his burnt issuer tokens
    And "SBER" checks operations list
    And "Client1" checks operations list

  Scenario: Successful change and merge

  Scenario: Successful change and transfer

  Scenario: Successful change and send to smart contract

  Scenario: Successful change and freeze

  Scenario: Unsuccessful change token when token is frozen
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator1"
    When "SBER" registered token type "SBC" with regulator "Regulator1"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation "Regulator1"
    When "Regulator1" checks his pending transactions
    When "Regulator1" approves transaction
    When "Client1" sees one token in his list
    When "Regulator1" freezes token "T1"
    When "Client1" sees one token in his list
    When "Client1" tries to change frozen token "T1" with two new tokens with values "50" and "50" but gets errors as token is frozen

  Scenario: Change token after unfreeze
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation "Regulator"
    When "Regulator" checks his pending transactions
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" freezes token "T1"
    When "Client1" sees one token in his list
    When "Client1" tries to change frozen token "T1" with two new tokens with values "50" and "50" but gets errors as token is frozen
    When "Regulator" unfreezes token "T1"
    When "Client1" sees one token in his list
    When "Client1" changes token "T1" with two new tokens with values "50" and "50"
    Then "Client1" sees two tokens in his list