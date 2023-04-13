Feature: Messaging functionality

  Scenario: Send-generic-message
    Given There is a client "Client1"
    Given There is a client "Client2"
    When "Client1" send generic message "private message for client 2" to "Client2"
    When "Client1" list his private messages
    And "Client2" list his private messages
