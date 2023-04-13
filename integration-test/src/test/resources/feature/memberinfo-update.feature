Feature: Update Member Information

  Scenario: Admin succeed to update
    Given There is a client "Client1"
    When Member "Client1" list his member information
    When Admin successfully updates "Client1" member information
    And Member "Client1" list his member information


  Scenario: User without admin rights failed to update
    Given There is a client "Client1"
    Given There is a client "Client2"
    When Member "Client1" list his member information
    When "Client2" is failed to update "Client1" member information
    And Member "Client1" list his member information
