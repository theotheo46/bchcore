@token-types
Feature: Wallet token types


  Scenario: List owned token types
    Given [Token Types] There is a client "Issuer-1"
      And [Token Types] There is a client "Issuer-2"

    When [Token Types] Client "Issuer-1" registers token type "Issuer-1/Token-1"
      And [Token Types] Client "Issuer-1" registers token type "Issuer-1/Token-2"
      And [Token Types] Client "Issuer-2" registers token type "Issuer-2/Token-1"
      And [Token Types] Client "Issuer-2" registers token type "Issuer-2/Token-2"
      And [Token Types] Client "Issuer-2" registers token type "Issuer-2/Token-3"

    Then [Token Types] Client "Issuer-1" sees 2 token types in his list
      And [Token Types] Client "Issuer-2" sees 3 token types in his list
