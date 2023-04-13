Feature: Restore wallet test


  Scenario: Restore Wallet - check tokens after restore
    Given Create wallet for "Client1"
    When "Client1" registered new token type
    When "Client1" issue new token
    When "Client1" issue new token
    When "Client1" issue new token
    When Restore wallet for client "Client1"
    When Check token list in wallet for client "Client1"
    When "Client1" issue new token
    When Restore wallet for client "Client1"
    When Check token list in wallet for client "Client1"
#    When "Client1" checks operations list

  Scenario: Restore Wallet - check profiles after restore

    Given Create wallet for "Client1"


    When "Client1" create new profile
    And "Client1" create new profile

    And Restore wallet for client "Client1"

    Then Check 2 profiles in wallet for client "Client1"
