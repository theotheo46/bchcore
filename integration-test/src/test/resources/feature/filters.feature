@filters
Feature: Filters


  Scenario: Filtering fungible/non-fungible token types
    Given [Filters] There is a client "Issuer"

    When [Filters] Client "Issuer" registers token type "FT1" with fungible gene
      And [Filters] Client "Issuer" registers token type "FT2" with fungible gene
      And [Filters] Client "Issuer" registers token type "NFT1" without fungible gene
      And [Filters] Client "Issuer" registers token type "NFT2" without fungible gene
      And [Filters] Client "Issuer" registers token type "NFT3" without fungible gene

    Then [Filters] Client "Issuer" have 2 token types filtered by fungible
      And [Filters] Client "Issuer" have 3 token types filtered by non-fungible


  Scenario: Filtering fungible/non-fungible tokens
    Given [Filters] There is a client "Issuer"
      And [Filters] There is a client "ClientFilter"

    When [Filters] Client "Issuer" registers token type "FT1" with fungible gene
      And [Filters] Client "Issuer" registers token type "NFT1" without fungible gene

      And [Filters] Client "Issuer" creates ID for token "T1" of type "FT1"
      And [Filters] Client "Issuer" issues token "T1" for client "ClientFilter" with value "100"

      And [Filters] Client "Issuer" creates ID for token "T2" of type "NFT1"
      And [Filters] Client "Issuer" issues token "T2" for client "ClientFilter" with value "101"

      And [Filters] Client "Issuer" creates ID for token "T3" of type "NFT1"
      And [Filters] Client "Issuer" issues token "T3" for client "ClientFilter" with value "102"

    Then [Filters] Client "ClientFilter" sees 1 token(s) in his list filtered by fungible
      And [Filters] Client "ClientFilter" sees 2 token(s) in his list filtered by non-fungible
