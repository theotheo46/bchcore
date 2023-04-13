Feature: Fungible tokens with regulation

  Scenario: Successful change token
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Regulator" changes token "T1" owned "Client1" with two new tokens with values "50" and "50"
    Then "Client1" sees two tokens in his list

  Scenario: Unsuccessful change token as amounts are not the same
    Given There is a client "SBER"
    Given There is a client "Client1"
    Given There is a client "Regulator"
    When "SBER" registered token type "SBC" with regulator "Regulator"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100" with regulation
    When "Regulator" approves transaction
    When "Client1" sees one token in his list
    When "Client1" tries to change token "T1" owned "Client1" with two new tokens with values "60" and "50" but gets errors as the amounts are not even
    Then "Client1" sees one token in his list

  Scenario: Successful change frozen token by this regulator
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
    When "Regulator" changes token "T1" owned "Client1" with two new tokens with values "50" and "50"
    Then "Client1" sees two tokens in his list

  Scenario: Unsuccessful change frozen token by other regulator
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
    Then "Client1" sees one token in his list
    Then "Regulator2" tries to change frozen token "T1" owned "Client1" with two new tokens with values "50" and "50" but gets errors as other regulator

  Scenario: Successful change token and burn
  Scenario: Successful change token and regulatory burn
  Scenario: Successful change token and transfer
  Scenario: Successful change token and regulatory transfer
  Scenario: Successful change token and merge
  Scenario: Successful change token and send to smart contract