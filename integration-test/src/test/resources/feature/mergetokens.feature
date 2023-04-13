Feature: Merge tokens user

  Scenario: Successful merge tokens

    Given There is a client "SBER"
    Given There is a client "Client1"
    When "SBER" registered token type "SB_A"

    When "SBER" issued token "A1" for "Client1" of type "SB_A" with value "200"
    And "SBER" issued token "A2" for "Client1" of type "SB_A" with value "300"
    And "SBER" issued token "A3" for "Client1" of type "SB_A" with value "500"
    And "Client1" sees 3 tokens in his list

    When "Client1" merged all his tokens

    And "Client1" list his tokens
    And "Client1" sees one token in his list
      #todo: remove double {"Client1" list his tokens} ???
    And "Client1" list his tokens

  Scenario: Can not merge tokens of different types

    Given There is a client "SBER"
    Given There is a client "Client1"
    When "SBER" registered token type "SB_A"
    When "SBER" registered token type "SB_B"

    When "SBER" issued token "A1" for "Client1" of type "SB_A" with value "200"
    And "SBER" issued token "A2" for "Client1" of type "SB_A" with value "300"
    And "SBER" issued token "A3" for "Client1" of type "SB_B" with value "500"
    And "Client1" sees 3 tokens in his list


    When "Client1" can not merge all his tokens

    And "Client1" list his tokens
    And "Client1" sees 3 tokens in his list

    And "Client1" list his tokens
