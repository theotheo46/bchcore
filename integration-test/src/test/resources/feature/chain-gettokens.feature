@chain-gettokens
Feature: Get Tokens By Type Id


  Scenario: Positive scenario - getting tokens by type id
    Given [GetTokens] There is a client "NFT"

    When [GetTokens] client "NFT" registers token type "Type_1"
    And [GetTokens] client "NFT" registers token type "Type_2"
    And [GetTokens] client "NFT" registers token type "Type_3"

    And [GetTokens] client "NFT" issues token "Token_1" with type "Type_1" and value "100"
    And [GetTokens] client "NFT" issues token "Token_2" with type "Type_1" and value "200"
    And [GetTokens] client "NFT" issues token "Token_3" with type "Type_1" and value "300"

    And [GetTokens] client "NFT" issues token "Token_1" with type "Type_2" and value "100"
    And [GetTokens] client "NFT" issues token "Token_2" with type "Type_2" and value "200"
    And [GetTokens] client "NFT" issues token "Token_3" with type "Type_2" and value "300"
    And [GetTokens] client "NFT" issues token "Token_4" with type "Type_2" and value "400"

    And [GetTokens] client "NFT" issues token "Token_1" with type "Type_3" and value "100"
    And [GetTokens] client "NFT" issues token "Token_2" with type "Type_3" and value "200"
    And [GetTokens] client "NFT" issues token "Token_3" with type "Type_3" and value "300"
    And [GetTokens] client "NFT" issues token "Token_4" with type "Type_3" and value "400"
    And [GetTokens] client "NFT" issues token "Token_5" with type "Type_3" and value "500"

    Then [GetTokens] web users see tokens of type "Type_3"
    And [GetTokens] web users see tokens of type "Type_2"
    And [GetTokens] web users see tokens of type "Type_1"
